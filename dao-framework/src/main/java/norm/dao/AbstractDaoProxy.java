package norm.dao;

import norm.dao.annotations.EntityCommonName;
import norm.dao.annotations.EntityIdentifier;
import norm.dao.annotations.EntityAccessorHelper;
import norm.dao.exceptions.DaoRuntimeException;
import norm.dao.exceptions.NotSetException;
import org.apache.log4j.Logger;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractDaoProxy implements DaoProxy {
    private static final Logger log = Logger.getLogger(AbstractDaoProxy.class);

    private static final Map<Class, Class<?>> PROXIED_INTERFACES = new HashMap<>();
    protected static final Map<Class, Map<Method, MethodAttributes>> ENTITY_METHODS = new HashMap<>();
    protected static final Map<Class, List<String>> ENTITY_IDENTITY_FIELDS = new HashMap<>();
    private static final Object SYNC = new Object();
    private static final Pattern PATTERN_CAPITAL_LETTER = Pattern.compile("[A-Z]");

    private boolean lock = false;
    private long lockingThread = -1;

    protected final Map<String, Object> data;
    Set<String> modifiedFields;
    protected Class<?> type;
    private String commonName;

    /**The constructor.
     @param data a map where entity properties are the keys.
     @param type the entity class.*/
    AbstractDaoProxy(Map<String, Object> data, Class<?> type)
    {
        //No extra fields such as "instantiatedDate" should be added to "data"! Otherwise "equals" will be screwed...
        this.data = Collections.synchronizedMap(data);

        if (!this.data.containsKey("__revision"))
            this.data.put("__revision", -1L);

        if (!type.isAnnotationPresent(EntityCommonName.class))
            throw new NullPointerException("EntityCommonName needs to be defined");
        else
            this.commonName = type.getAnnotation(EntityCommonName.class).value();

        this.type = type;

        this.modifiedFields = new HashSet<>();
    }

    @SuppressWarnings("unchecked")
    protected static <ST> ST newInstance(final Class<ST> type, final DaoProxy daoProxy) {
        //Do we know the entity already?
        if (ENTITY_METHODS.get(type) == null) {
            synchronized (SYNC) {
                //Executed once!
                if (ENTITY_METHODS.get(type) == null) {
                    //Methods
                    Map<Method, MethodAttributes> methods = new HashMap<>();
                    for (Method m : type.getMethods())
                    {
                        String methodName = m.getName();
                        String fieldName;
                        String action;

                        if (m.isAnnotationPresent(EntityAccessorHelper.class)) {
                            fieldName = m.getAnnotation(EntityAccessorHelper.class).fieldName();
                            action = m.getAnnotation(EntityAccessorHelper.class).action();
                        }
                        else {
                            Matcher match = PATTERN_CAPITAL_LETTER.matcher(methodName);

                            int lastCapitalIndex = -1;
                            if(match.find())
                                lastCapitalIndex = match.start();

                            action = methodName.substring(0,lastCapitalIndex);
                            fieldName = methodName.substring(lastCapitalIndex, lastCapitalIndex+1).toLowerCase()
                                    + methodName.substring(lastCapitalIndex+1, methodName.length());
                        }

                        methods.put(m, new MethodAttributes(type.getName(), methodName, fieldName, action));
                    }
                    log.info(type.getName()+" Methods: "+methods);
                    ENTITY_METHODS.put(type, methods);

                    //Identity Fields
                    List<String> identityFields = new ArrayList<>();
                    for (Field field : type.getFields()) {
                        if (field.isAnnotationPresent(EntityIdentifier.class)) {
                            identityFields.add(field.getAnnotation(EntityIdentifier.class).position(), field.getName());
                        }
                    }
                    log.info(type.getName()+" Identity Fields: "+identityFields);
                    ENTITY_IDENTITY_FIELDS.put(type, identityFields);
                }
            }
        }

        //get proxy class
        Class<?> proxyClass;
        if (PROXIED_INTERFACES.containsKey(type)) {
            proxyClass = PROXIED_INTERFACES.get(type);
        }
        else {
            proxyClass = Proxy.getProxyClass(AbstractDaoProxy.class.getClassLoader(), type);
            PROXIED_INTERFACES.put(type, proxyClass);
        }

        //create a new proxied instance
        try {
            return (ST) proxyClass.getConstructor(InvocationHandler.class).newInstance(daoProxy);
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException | InstantiationException e){
            throw new DaoRuntimeException("Unexpected proxy exception:"+e.toString()+" for daoProxy:"+daoProxy);
        }
    }

    /**Transformer
        The purpose is to load with new data but preserve the old data that has been modified as is and transform
        into a new entity class*/
    protected static <ST> ST newInstance(AbstractDaoProxy sourceDao, AbstractDaoProxy targetDao, Class<ST> newType) {
        if (sourceDao == null) throw new NullPointerException("sourceDao cannot be null");
        if (targetDao == null) throw new NullPointerException("targetDao cannot be null");

        synchronized (sourceDao.data) {
            synchronized (targetDao.data) {
                //merge source data into target data (old) if not exists
                for (String srcKey : sourceDao.data.keySet()) {
                    if (targetDao.modifiedFields.contains(srcKey)) continue;
                    targetDao.data.put(srcKey, sourceDao.data.get(srcKey));
                }
            }
        }

        return newInstance(newType, targetDao);
    }

    public synchronized void lock(boolean lock) {
        if (lock) {
            if (lockingThread < 0) {
                lockingThread = Thread.currentThread().getId();
                log.debug("LOCK ThreadId:"+lockingThread+" "+this);
            }
        }
        else if (lockingThread > 0 && lockingThread != Thread.currentThread().getId()) {
            throw new UnsupportedOperationException("Not original locking thread!");
        }
        else {
            log.debug("UNLOCK ThreadId:"+lockingThread+" "+this);
            lockingThread = -1;
        }

        this.lock = lock;
    }

    public boolean isLocked() {
        return lock;
    }

    protected long getLockingThreadId() {
        return lockingThread;
    }

    public Object invoke(Object proxy, Method m, Object[] args)
            throws InvocationTargetException, IllegalAccessException {
        if (isLocked() && getLockingThreadId() != Thread.currentThread().getId()) {
            try {
                log.info(Thread.currentThread().getId()+" is LOCKED");
                synchronized (data) { data.wait(2*60*1000); } //unlock after 2 minutes. in config perhaps?
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting to access a locked entity!", e);
            }
        }

        //Default handling.
        Map<Method, MethodAttributes> methods = ENTITY_METHODS.get(type);
        if (methods.containsKey(m)) {
            MethodAttributes method = methods.get(m);

            switch (method.action) {
                case "get":
                case "is":
                    if (method.fieldName.equals("__identity")) {
                        data.put("__identity", getIdentity());
                        return data.get(method.fieldName);
                    }

                    if (data.containsKey(method.fieldName))
                        return data.get(method.fieldName);

                    if (!data.containsKey(method.fieldName+"$new")) {
                        //Although this shouldn't happen, if an entity field is accessed and the field
                        // hasn't been loaded yet (and it is not lazy loadable), throw an error.
                        //It can happen if an inbound object is incomplete, but we leave the handling to the client
                        log.error("["+method.fieldName + "] is not set yet in object: " + data);
                        throw new NotSetException("["+method.fieldName + "] is not set yet.");
                    }
                    return data.get(method.fieldName+"$new");
                case "set":
                    //shouldn't happen, but just in case...
                    if (isLocked()) throw new IllegalAccessException("Cannot modify an Entity while it is locked");
                    synchronized (data) {
                        log.trace("Set "+method.fieldName+" = "+args[0]);
                        data.put(method.fieldName, args[0]);
                        modifiedFields.add(method.fieldName);
                    }
                    return null;
                case "add":
                    if (isLocked()) throw new IllegalAccessException("Cannot modify an Entity while it is locked");
                    synchronized (data) {
                        List newEntitiesArray =
                                data.containsKey(method.fieldName+"$new") ?
                                        (List) data.get(method.fieldName+"$new") : new ArrayList<>();
                        newEntitiesArray.add(args[0]);
                        data.put(method.fieldName+"$new", newEntitiesArray);
                        modifiedFields.add(method.fieldName);
                    }
                    return null;
                default:
                    throw new IllegalAccessException("Method '" + m + "' is unsupported");
            }
        }
        else {
            switch (m.getName()) {
                case "toString":
                    return this.toString();
                case "hashCode":
                    return this.hashCode();
                case "equals":
                    return this.equals(args[0]);
                default:
                    //return m.invoke(this, args);
                    throw new IllegalAccessException("Unhandled Method: "+m);
            }
        }
    }

    public String getIdentity() {
        String identity = null;
        for (String identityField : ENTITY_IDENTITY_FIELDS.get(type))
        {
            if (!data.containsKey(identityField) && !data.containsKey(identityField+"$new"))
                throw new NotSetException("["+identityField+"] is not set yet.");

            identity = (identity == null ? "" : (identity + "."))
                    + (data.containsKey(identityField) ? data.get(identityField) : data.get(identityField+"$new"));
        }
        return commonName+"["+identity+"]";
    }

    public String getCommonName() {
        return commonName;
    }

    public Set<String> getModifiedFields() {
        return modifiedFields;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public Class getType() {
        return type;
    }

    public String toString() {
        return type.getSimpleName()+":"+String.valueOf(data)+"(modifiedFields:"+String.valueOf(modifiedFields)+")";
    }

    public int hashCode() {
        return data.hashCode();
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof Entity))
            return false;

        try {
            DaoProxy daoProxy = (DaoProxy) Proxy.getInvocationHandler(obj);
            return data.equals(daoProxy.getData());
        }
        catch(Throwable e) {
            return false;
        }
    }

    public static class MethodAttributes {
        private final String interfaceName;
        private final String methodName;
        private final String fieldName;
        private final String action;

        MethodAttributes(String interfaceName, String methodName, String fieldName, String action) {
            this.interfaceName = interfaceName;
            this.methodName = methodName;
            this.fieldName = fieldName;
            this.action = action;
        }

        public String getInterfaceName() {
            return interfaceName;
        }

        public String getMethodName() {
            return methodName;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getAction() {
            return action;
        }

        public String toString() {
            return "{fieldName:" + String.valueOf(fieldName) + ", " +
                    "action:" + String.valueOf(action) + "}";
        }
    }
}

