package norm.dao;

import org.apache.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Set;

/***/
public class GenericDaoProxy<T> extends AbstractDaoProxy {
    private final Logger log = Logger.getLogger(this.getClass());
    private CustomDaoInvocationHandler customDaoInvocationHandler;

    /**The DAO factory.

     @param type the entity class.
     @param data a map where entity properties are the keys.
     @param customDaoInvocationHandler InvocationHandler.invoke implementation. Nullable.
                                        Override if you want to customize the default behaviour of the
     */
    public static <ST> ST newInstance(Class<ST> type, Map<String, Object> data,
                                      CustomDaoInvocationHandler customDaoInvocationHandler) {
        return newInstance(type, new GenericDaoProxy<>(data, type, customDaoInvocationHandler));
    }

    /**Translate a proxied entity to GenericDaoProxy.
     @param proxiedEntity the proxied entity.*/
    public static <ST> GenericDaoProxy<ST> getDaoFromProxiedEntity(ST proxiedEntity) {
        try {
            return (GenericDaoProxy<ST>)Proxy.getInvocationHandler(proxiedEntity);
        }
        catch (ClassCastException e) {
            throw new ClassCastException("The entity ["+proxiedEntity+"] is not proxied via "+GenericDaoProxy.class);
        }
        catch (IllegalArgumentException p) {
            throw new IllegalArgumentException("The entity ["+proxiedEntity+"] is not a proxied instance");
        }
    }

    /**Translate a proxied entity from one type to another.
     @param sourceEntity the source entity from a type T.
     @param newType the destination type of class NT.*/
    public <NT> NT transform(/*@NotNull*/ T sourceEntity, Class<NT> newType) {
        if (sourceEntity == null) throw new NullPointerException("sourceEntity cannot be null");

        synchronized (this.data) {
            Set<String> modifiedFields = this.modifiedFields;
            GenericDaoProxy<NT> dao = new GenericDaoProxy<>(this.data, newType, customDaoInvocationHandler);
            dao.modifiedFields = modifiedFields;
            return newInstance(getDaoFromProxiedEntity(sourceEntity), dao, newType);
        }
    }

    private GenericDaoProxy(Map<String, Object> data, Class<T> type,
                            CustomDaoInvocationHandler customDaoInvocationHandler) {
        super(data, type);
        this.customDaoInvocationHandler = customDaoInvocationHandler;
    }

    /**A wrapper around the AbstractDaoProxy.invoke(proxy, m, args)
      Executes a custom InvocationHandler, if provided, instead of the AbstractDaoProxy one*/
    @Override
    public Object invoke(Object proxy, Method m, Object[] args)
            throws UnsupportedOperationException, InvocationTargetException, IllegalAccessException {
        if (isLocked() && getLockingThreadId() != Thread.currentThread().getId()) {
            try {
                log.info(Thread.currentThread().getName()+" is LOCKED");
                synchronized (data) { data.wait(2*60*1000); }
            } catch (InterruptedException e) {
                log.warn("Exception while waiting to access a locked entity", e);
            }
        }

        Map<Method, MethodAttributes> methods = ENTITY_METHODS.get(type);

        if (customDaoInvocationHandler != null && methods.containsKey(m)) {
            MethodAttributes method = methods.get(m);

            return customDaoInvocationHandler.invoke(this, method, proxy, m, args);
        }
        return invoke_default(proxy, m, args);
    }

    public Object invoke_default(Object proxy, Method m, Object[] args)
            throws UnsupportedOperationException, InvocationTargetException, IllegalAccessException {
        return super.invoke(proxy, m, args);
    }
}

