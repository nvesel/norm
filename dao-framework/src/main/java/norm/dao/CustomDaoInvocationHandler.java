package norm.dao;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 Serves as default InvocationHandler.invoke which ultimately points to AbstractDaoProxy.invoke(proxy, m, args).
 If custom behaviour is desired, extend (and override invoke) and pass as argument to GenericDaoProxy DAO factory.
 */
public class CustomDaoInvocationHandler {
    public Object invoke(GenericDaoProxy dao, AbstractDaoProxy.MethodAttributes method,
                         Object proxy, Method m, Object[] args)
            throws UnsupportedOperationException, InvocationTargetException, IllegalAccessException {
        return dao.invoke_default(proxy, m, args);
    }
}

