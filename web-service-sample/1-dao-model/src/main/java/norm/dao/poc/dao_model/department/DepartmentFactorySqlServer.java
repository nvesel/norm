package norm.dao.poc.dao_model.department;

import norm.dao.AbstractDaoProxy;
import norm.dao.EntityFactoryCommon;
import norm.dao.CustomDaoInvocationHandler;
import norm.dao.GenericDaoProxy;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

//A simple factory utilizing a generic dao
public class DepartmentFactorySqlServer extends EntityFactoryCommon<DepartmentDAO> implements DepartmentFactory {
    private final Logger log = Logger.getLogger(this.getClass());

    @Autowired
    public DepartmentFactorySqlServer(String sqlStatementsFileName) throws IOException, ClassNotFoundException {
        super(DepartmentDAO.class, sqlStatementsFileName);

        //We are using the generic DAO, but we need custom functionality in GenericDaoInvocationHandler
        //Override the invoke method of the generic DAO in order to handle lazy loading of department employees
        customDaoInvocationHandler = new CustomDaoInvocationHandler() {
            @Override
            public Object invoke(GenericDaoProxy dao, AbstractDaoProxy.MethodAttributes method, Object proxy, Method m, Object[] args) throws UnsupportedOperationException, InvocationTargetException, IllegalAccessException {
                //Lazy Loaded
                if ("getEmployees".equals(method.getMethodName())) {
                    log.warn("TODO: getEmployees");//TODO
                    synchronized (dao.getData()) {
                        dao.getData().put("employees", new ArrayList<>());
                    }
                    return null;
                }
                return dao.invoke_default(proxy, m, args);
            }
        };
    }
}
