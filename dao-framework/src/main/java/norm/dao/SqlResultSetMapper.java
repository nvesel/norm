package norm.dao;

import norm.dao.exceptions.DaoRuntimeException;
import org.apache.log4j.Logger;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

public class SqlResultSetMapper {
    private final Logger log = Logger.getLogger(this.getClass());

    private SelectQuery selectQuery;
    private Map<String, EntityProperty> entityFields;
    private TreeSet<EntityProperty> entityIdentifierFields;
    private int nestedObjectLevel = 0;
    private String nestedObjectName = "";
    private Map<String, Map<String, Object>> processedEntites = new HashMap<>();
    private String parentEntityUri = "";

    public SqlResultSetMapper(SelectQuery selectQuery, Map<String, EntityProperty> entityFields) {
        this.selectQuery = selectQuery;
        this.entityFields = entityFields;
        this.entityIdentifierFields = getEntityIdentifierFields(entityFields);
    }

    private SqlResultSetMapper(SelectQuery selectQuery,
                               Map<String, EntityProperty> entityFields,
                               int nestedObjectLevel,
                               String nestedObjectName,
                               Map<String, Map<String, Object>> processedEntites,
                               String parentEntityUri) {
        this.selectQuery = selectQuery;
        this.entityFields = entityFields;
        this.entityIdentifierFields = getEntityIdentifierFields(entityFields);
        this.nestedObjectLevel = nestedObjectLevel;
        this.nestedObjectName = nestedObjectName;
        this.processedEntites = processedEntites;
        this.parentEntityUri = parentEntityUri;
    }

    public SelectQuery getSelectQuery() {
        return selectQuery;
    }

    public Map<String, Object> mapRow(final ResultSet rs, int rowNum) throws SQLException {
        log.debug("---------------------------- query:"+selectQuery.getQueryName()+", nestedObjectLevel:"
                +nestedObjectLevel+", rowNum:"+rowNum+" ----------------------------");
        if (selectQuery.getColumnDataTypeMap().isEmpty()) {
            //should happen only the first time a query is executed
            log.info("Get column data types for query:"+selectQuery.getQueryName());
            ResultSetMetaData meta = rs.getMetaData();
            for (int ci = 1; ci <= meta.getColumnCount(); ci++) {
                String columnName = meta.getColumnLabel(ci);
                String columnType = meta.getColumnTypeName(ci);
                selectQuery.getColumnDataTypeMap().put(columnName.toLowerCase(), columnType.toLowerCase());
            }
        }

        if (nestedObjectLevel == 0)
            nestedObjectName = "";

        Set<String> processedNestedEntities = new HashSet<>();

        //ORM
        Map<String, String[]> columnEntityFieldMap = selectQuery.getColumnEntityFieldMap();
        Map<String, Object> data = new HashMap<>();


//TODO: do it better

        //First go through the primitive data type fields in order to construct the current entity identifier.
        for (String columnName : columnEntityFieldMap.keySet())
        {
            columnName = columnName.toLowerCase();

            String[] entityFieldArray = columnEntityFieldMap.get(columnName);

            //skip columns that are not matching the nestedObjectName we are currently working with.
            if (entityFieldArray.length <= nestedObjectLevel || (nestedObjectLevel > 0
                    && !entityFieldArray[nestedObjectLevel - 1].equals(nestedObjectName))) {
                log.debug("Skip column "+columnName+" in the context of nestedObjectLevel="+nestedObjectLevel
                        +" and nestedObjectName="+nestedObjectName);
                continue;
            }

            String fieldName = entityFieldArray[nestedObjectLevel];

            EntityProperty entityProperty = entityFields.get(fieldName);
            if (entityProperty != null)
            {
                if (entityProperty.getFactory() == null)//simple property
                {
                    log.debug("Map column: "+columnName+" to "
                            +((nestedObjectName.length() == 0)?"":nestedObjectName+".")+fieldName);

                    Object value = null;
                    if (rs.getString(columnName) != null) {
                        switch (selectQuery.getColumnDataTypeMap().get(columnName)) {
                            case "char":
                            case "varchar":
                            case "uniqueidentifier":
                                value = rs.getString(columnName);
                                break;
                            case "bigint":
                                value = rs.getLong(columnName);
                                break;
                            case "int":
                            case "integer":
                            case "smallint":
                            case "tinyint":
                                value = rs.getInt(columnName);
                                break;
                            case "numeric":
                            case "decimal":
                                value = rs.getBigDecimal(columnName);
                                break;
                            case "double":
                            case "float":
                                value = rs.getDouble(columnName);
                                break;
                            case "real":
                                value = rs.getFloat(columnName);
                                break;
                            case "datetime":
                            case "date":
                                value = new Date(rs.getDate(columnName).getTime());
                                break;
                            case "bit":
                                value = rs.getBoolean(columnName);
                                break;
                            default:
                                throw new DaoRuntimeException(selectQuery.getQueryName()
                                        + ": Unknown SQL data type '" + selectQuery.getColumnDataTypeMap().get(columnName)
                                        + "' for column:" + columnName);
                        }
                    }

                    data.put(entityProperty.getName(), value);
                }
            }
            else {
                log.warn("There is no known EntityProperty named "+fieldName+" to be mapped to column:"+columnName);
            }
        }




        String thisEntityIdentity = getIdentity(data);
        String thisEntityUri = parentEntityUri+"/"+(nestedObjectName.length() == 0 ? "" : nestedObjectName+"/")
                +thisEntityIdentity;




        //Now process columns that define nested entities
        for (String columnName : columnEntityFieldMap.keySet()) {

            String[] entityFieldArray = columnEntityFieldMap.get(columnName);

            //skip columns that are not matching the nestedObjectName we are currently working with.
            if (entityFieldArray.length <= nestedObjectLevel || (nestedObjectLevel > 0
                    && !entityFieldArray[nestedObjectLevel - 1].equals(nestedObjectName))) {
                log.debug("Skip column "+columnName+" in the context of nestedObjectLevel="+nestedObjectLevel
                        +" and nestedObjectName="+nestedObjectName);
                continue;
            }

            String fieldName = entityFieldArray[nestedObjectLevel];

            EntityProperty entityProperty = entityFields.get(fieldName);

            if (entityProperty != null)
            {
                if (entityProperty.getFactory() != null) {//nested entity
                    log.debug("Map column: "+columnName+" to "+((nestedObjectName.length() == 0)?"":nestedObjectName+".")
                            +fieldName);

                    nestedObjectName = entityFieldArray[nestedObjectLevel++];
                    String nestedEntityKey = nestedObjectLevel+"."+nestedObjectName+"."+fieldName;
                    if (!processedNestedEntities.contains(nestedEntityKey))
                    {
                        log.debug("Map a nested Entity. Factory:"+ entityProperty.getFactory());

                        processedNestedEntities.add(nestedEntityKey);

                        SqlResultSetMapper nestedSqlResultSetMapper = new SqlResultSetMapper(
                                this.selectQuery,
                                entityProperty.getFactory().getEntityFields(),
                                this.nestedObjectLevel,
                                this.nestedObjectName,
                                this.processedEntites,
                                thisEntityUri
                        );

                        //Class mapperType;
                        //if (entityProperty.isArray())
                        //    mapperType = entityProperty.getArrayDataType();
                        //else
                        //    mapperType = entityProperty.getDataType();

                        Object entity = entityProperty.getFactory().getMapperInstance(nestedSqlResultSetMapper)
                                .mapRow(rs, rowNum);

                        data.put(entityProperty.getName(), entity);
                    }
                    else {
                        log.debug("Already mapped - skip now. (nestedEntityKey:"+nestedEntityKey+")");
                    }
                    nestedObjectName = entityFieldArray[--nestedObjectLevel];
                }
            }
        }




        //Merge new data to existing data
        Map<String, Object> existingData = processedEntites.get(thisEntityUri);
        if (existingData == null || thisEntityIdentity == null)
            existingData = new HashMap<>();

        for (String property : data.keySet())
        {
            EntityProperty entityProperty = entityFields.get(property);

            if (entityProperty != null && entityProperty.getFactory() != null && entityProperty.isArray()) {
                if (!existingData.containsKey(property))
                    existingData.put(property, new ArrayList<>());

                ArrayList list = (ArrayList)existingData.get(property);
                if (data.get(property) != null)
                    list.add(data.get(property));
                existingData.put(property, list);
            }
            else {
                existingData.put(property, data.get(property));
            }
        }

        existingData.put("__identity", thisEntityIdentity);

        if (thisEntityIdentity != null)
            processedEntites.put(thisEntityUri, existingData);

        return existingData;
    }

    private TreeSet<EntityProperty> getEntityIdentifierFields(Map<String, EntityProperty> entityFields) {
        Comparator<EntityProperty> comparator = new Comparator<EntityProperty>() {
            @Override
            public int compare(EntityProperty entityProperty1, EntityProperty entityProperty2) {
                return (entityProperty1.getIdentifierPosition() > entityProperty2.getIdentifierPosition())?1:
                        (entityProperty1.getIdentifierPosition() < entityProperty2.getIdentifierPosition())?-1:0;
            }
        };

        TreeSet<EntityProperty> entityIdentifierFields = new TreeSet<>(comparator);
        for (EntityProperty entityProperty : entityFields.values()) {
            if (entityProperty.isIdentifier()) {
                entityIdentifierFields.add(entityProperty);
            }
        }
        return entityIdentifierFields;
    }

    private String getIdentity(Map<String, Object> data) {
        String identity = "";
        for (EntityProperty entityProperty : entityIdentifierFields)
        {
            if (!data.containsKey(entityProperty.getName()) || data.get(entityProperty.getName()) == null)
                    return null;

            identity = identity + ((identity.length() == 0)?"":".") + data.get(entityProperty.getName());//Date?
        }
        return identity;
    }
}

