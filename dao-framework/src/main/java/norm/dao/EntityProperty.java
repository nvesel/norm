package norm.dao;

import org.apache.log4j.Logger;

//A simple helper POJO where we keep info about Entity Properties
public class EntityProperty {
    private final Logger log = Logger.getLogger(this.getClass());

    private final String name;
    private final Class<?> dataType;
    private final boolean isIdentifier;
    private final Integer identifierPosition;
    private final Class<?> arrayDataType;
    private final boolean isArray;
    private final Class<EntityFactory> factoryClass;
    private final boolean immutable;

    /**
     * <p>A simple helper class where we keep info about Entity Properties</p>
     * @param name: The field name
     * @param dataType: Field data dataType
     * @param isIdentifier: if true, the field will not be persisted
     * @param identifierPosition: signifies the position of a filed when building complex identifiers
     * @param isArray: is Array
     * @param arrayDataType: array item type
     * @param factoryClass: Optional DAO Factory class. (If field is nested property with its own factory)
     * @param immutable: In conjunction with factoryClass, it instructs the persist method,
     *                   whether to propagate persist operation to the nested entity or not
     */
    public EntityProperty(
            String name,
            Class<?> dataType,
            boolean isIdentifier,
            Integer identifierPosition,
            boolean isArray,
            Class<?> arrayDataType,
            Class<EntityFactory> factoryClass,
            boolean immutable)
    {
        this.name = name;
        this.dataType = dataType;
        this.isIdentifier = isIdentifier;
        this.identifierPosition = identifierPosition;
        this.isArray = isArray;
        this.arrayDataType = arrayDataType;
        this.factoryClass = factoryClass;
        this.immutable = immutable;

        log.info("new EntityProperty:"+this.toString());
    }

    public boolean isIdentifier() {
        return isIdentifier;
    }

    public Integer getIdentifierPosition() {
        return identifierPosition;
    }

    public String getName() {
        return name;
    }

    public Class<?> getDataType() {
        return dataType;
    }

    public Class<?> getArrayDataType() {
        return arrayDataType;
    }

    public boolean isArray() {
        return isArray;
    }

    public EntityFactory getFactory() {
        if (factoryClass == null)
            return null;
        return FactoriesRegistryBean.getEntityFactory(factoryClass);
    }

    public boolean immutable() {
        return immutable;
    }

    public String toString() {
        return "{name:" + String.valueOf(name) + ", " +
                "dataType:" + String.valueOf(dataType) + ", " +
                "isIdentifier:" + String.valueOf(isIdentifier) + ", " +
                "isArray:" + String.valueOf(isArray) + ", " +
                "arrayDataType:" + String.valueOf(arrayDataType) + ", " +
                "immutable:" + String.valueOf(immutable) + ", " +
                "factory:" + String.valueOf(factoryClass) + "}";
    }
}

