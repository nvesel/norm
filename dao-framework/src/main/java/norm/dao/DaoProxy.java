package norm.dao;

import java.lang.reflect.InvocationHandler;
import java.util.Map;
import java.util.Set;

public interface DaoProxy extends InvocationHandler {
    Set<String> getModifiedFields();
    Map<String, Object> getData();
    Class getType();
    void lock(boolean lock);
    String getIdentity();
    String getCommonName();
}
