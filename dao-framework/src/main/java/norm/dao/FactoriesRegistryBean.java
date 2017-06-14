package norm.dao;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

//get factory instance by factory interface
public class FactoriesRegistryBean {
    private static final Logger log = Logger.getLogger(FactoriesRegistryBean.class);

    private static Map<Class, EntityFactory> factoriesRegistryMap = new HashMap<>();

    public void setFactoriesRegistryMap(Map<Class, EntityFactory> factoriesRegistryMap) {
        log.info("Factories Registry Map:"+factoriesRegistryMap);
        FactoriesRegistryBean.factoriesRegistryMap = factoriesRegistryMap;
    }

    public static <T extends EntityFactory> T getEntityFactory(Class<T> factoryClass) {
        log.trace("getEntityFactory:"+factoryClass.getName());
        if (!factoriesRegistryMap.containsKey(factoryClass))
            log.error("ATTENTION: A factory requested:"+factoryClass.getName()+", does not exist!!!");
        return (T) factoriesRegistryMap.get(factoryClass);
    }
}

