package norm.dao;

import norm.dao.exceptions.ConcurrentModificationException;
import norm.dao.exceptions.DaoRuntimeException;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Persist {
    private static final Logger log = Logger.getLogger(Persist.class);

    //normally contains an ID from last insert or update operations. The next operation can obtain it.
    private static final ThreadLocal<String> lastEntityId = new ThreadLocal<>();

    public static String getLastEntityId() {
        return lastEntityId.get();
    }

    //signifies whether the entity was modified - respectfully it might need to be evicted from the cache.
    private static final ThreadLocal<Boolean> entityUpdated = new ThreadLocal<>();

    public enum Operation {
        INSERT_UNIQUE(1),
        INSERT_MERGE(2),
        INSERT(3),
        UPDATE(4);

        private int operation;

        Operation(int operation) {
            this.operation = operation;
        }

        public int getOperation() {
            return this.operation;
        }

        public String getOperationName() {
            switch (this) {
                case INSERT_UNIQUE: return "INSERT_UNIQUE";
                case INSERT_MERGE: return "INSERT_MERGE";
                case INSERT: return "INSERT";
                case UPDATE: return "UPDATE";
                default: return null;}
        }
    }

    public static boolean persist(final DaoProxy daoProxy,
                               Map<String, EntityProperty> entityFields,
                               List<PersistQuery> persistQueries,
                               JdbcOperations jdbcOperations,
                               Operation operation) {

        synchronized (daoProxy.getData()) {
            //Will block any change operation on the DAO from foreign threads
            daoProxy.lock(true);
            //the DAO will be unlocked when the wrapping transaction is committed/rolled back
            //Although the dao data is synchronized, this scope is for the current entity.
            // A transaction can span across multiple entities.

            log.debug("PERSIST: "+operation.getOperationName()+" "
                    +daoProxy.getType().getSimpleName()+":"+daoProxy.getData());

            for (final PersistQuery persistQuery : persistQueries) {//normally will be just one

                if ((operation.getOperation() & persistQuery.getPersistOperation().getOperation()) == 0)
                    continue;

                //Will save us an update if nothing was modified.
                if ((operation.getOperation() & Operation.UPDATE.operation) > 0) {
                    log.debug("daoProxy.getModifiedFields():" + daoProxy.getModifiedFields());
                    boolean modified = false;
                    for (PersistQuery.Column column : persistQuery.getResultColumns()) {
                        if (column.getEntityField() != null
                                && daoProxy.getModifiedFields().contains(column.getEntityField())) {
                            modified = true;
                            break;
                        }
                    }
                    if (!modified)
                        continue;
                }

                log.debug("Will execute persistQuery:"+persistQuery.toString());

                PreparedStatementCreatorCustom preparedStatementCreator = new PreparedStatementCreatorCustom() {
                    PreparedStatement preparedStatement;

                    @Override
                    public PreparedStatement getPreparedStatement() {
                        return preparedStatement;
                    }

                    @Override
                    public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                        PreparedStatement ps = con.prepareStatement(persistQuery.getQuery(),
                                ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                        int i = 1;
                        for (PersistQuery.Column condColumn : persistQuery.getConditionColumns())
                        {
                            if (condColumn.getEntityField() != null
                                    && !entityFields.containsKey(condColumn.getEntityField()))
                                throw new DaoRuntimeException("Unknown entity field:"
                                        +condColumn.getEntityField());

                            Object value = daoProxy.getData().get(condColumn.getEntityField());

                            if (condColumn.getMethod() != null) {
                                try {
                                    value = condColumn.getMethod().invoke(value);
                                } catch (IllegalAccessException | InvocationTargetException e) {
                                    throw new DaoRuntimeException("Executing "
                                            +daoProxy.getType().getName()+"."
                                            +condColumn.getMethod().getName(), e);
                                }
                            }

                            log.debug("Set Condition[" + i + "] ("+condColumn.getEntityField()+") = '"
                                    + value + "'");
                            ps.setObject(i++, value);
                        }
                        preparedStatement = ps;
                        return ps;
                    }
                };

                ResultSetExtractor<Boolean> resultSetExtractor = new ResultSetExtractor<Boolean>() {
                    @Override
                    public Boolean extractData(ResultSet rs) throws SQLException
                    {
                        boolean isUpdate = rs.next();

                        if (!isUpdate && persistQuery.getPersistOperation() == Operation.UPDATE) {
                            log.warn("Update query " + persistQuery.getDescription()
                                    + " didn't find a matching record to update. "+daoProxy.getData());
                            throw new ConcurrentModificationException("Entity was not found. " +
                                    "Perhaps it has been modified or deleted since it was fetched. " +
                                    "Re-fetch and try again.");
                        }

                        if (isUpdate && persistQuery.getPersistOperation() == Operation.INSERT_UNIQUE) {
                            log.warn("Insert query " + persistQuery.getDescription()
                                    + " found a duplicated record. "+daoProxy.getData());
                            throw new ConcurrentModificationException("Entity already exists. Cannot insert.");
                        }

                        if (isUpdate && rs.last() != rs.first())//do we need this?
                            throw new DaoRuntimeException("Query "+persistQuery.getDescription()
                                    +" returned more than one row.");

                        log.debug("Record found:"+isUpdate);

                        if (!isUpdate)
                            rs.moveToInsertRow();

                        //persisting
                        int affectedColumns = 0;
                        for (PersistQuery.Column column : persistQuery.getResultColumns())
                        {
                            if (column.getInOut() == PersistQuery.Column.InOut.OUTPUT)
                                continue;//we will process those after the persisting is done

                            String fieldName = (column.getEntityField() == null)?
                                    column.getMethod().getName() : column.getEntityField();
                            String columnName = column.getColumnName();

                            log.debug("fieldName:" + fieldName + " mapped to columnName:" + columnName);

                            Object value = null;
                            if (isUpdate)
                            {
                                value = rs.getObject(columnName);
                                rs.updateObject(columnName, value);
                            }

                            try
                            {
                                if (columnName.equals("__revision"))
                                {
                                    if (isUpdate) {
                                        long providedRevision = Long.parseLong(daoProxy.getData().get("__revision").toString());//there is always...
                                        long currentRevision = Long.parseLong(value.toString());//there is always...

                                        // For complex entities the providedRevision is the sum of __revision from more than one tables.
                                        // For such the check needs to be part of the query:
                                        // i.e. ... where (?/*COND:<__revision><>*/ < 0 or (t1.__revision + t2.__revision) = ?/*COND:<__revision><>*/)
                                        // For simple entities (=row) we can omit this check and rely on the following logic to handle stale data.
                                        if (providedRevision >= 0 && providedRevision < currentRevision)
                                            throw new ConcurrentModificationException("Stale entity. Re-fetch and try again.");

                                        rs.updateObject("__revision", currentRevision + 1);
                                        daoProxy.getData().put("__revision$new", currentRevision + 1);
                                    }
                                    else {
                                        daoProxy.getData().put("__revision$new", 0L);
                                    }
                                }
                                else if (daoProxy.getData().containsKey(fieldName))
                                {
                                    value = daoProxy.getData().get(fieldName);

                                    if (isUpdate && entityFields.get(column.getEntityField()).isIdentifier()) {
                                        log.warn("Skipping " + columnName
                                                + " because it is an identifier column.");
                                        continue;//next column
                                    }
                                    else {
                                        log.debug("Setting " + columnName + " = '" + value + "'");
                                    }

                                    if (column.getMethod() != null)
                                    {
                                        if (value != null) {
                                            log.debug("Executing:"+ column.getMethod());
                                            rs.updateObject(columnName, column.getMethod().invoke(value));
                                        }
                                        else {
                                            rs.updateNull(columnName);
                                        }
                                    }
                                    else {
                                        rs.updateObject(columnName, value);
                                    }
                                }
                                else if (column.getMethod() != null
                                        && (column.getMethod().getModifiers() & Modifier.STATIC) > 0)
                                {
                                    log.debug("Executing:"+ column.getMethod());
                                    rs.updateObject(columnName, column.getMethod().invoke(null));
                                }
                            }
                            catch (IllegalAccessException | InvocationTargetException e) {
                                throw new DaoRuntimeException(column.getMethod() + ": " + e.toString());
                            }
                            affectedColumns++;
                        }

                        isUpdate = (isUpdate && affectedColumns > 0);
                        entityUpdated.set(isUpdate);

                        if (affectedColumns > 0) {
                            if (!isUpdate)
                                rs.insertRow();
                            else
                                rs.updateRow();

                            rs.first();
                        }

                        lastEntityId.remove();

                        if (!rs.isFirst()) {
                            log.warn("######################################################################");
                            log.warn("The JDBC driver does not support TYPE_SCROLL_SENSITIVE ResultSet !!!");
                            log.warn("OUTPUT columns are not processed !!!!");
                            log.warn("THIS IS ONLY FOR THE TESTS WITH H2DB or HSQLDB !!!!!");
                            log.warn("######################################################################");

                            PreparedStatement preparedStatement = preparedStatementCreator.getPreparedStatement();
                            ResultSet rs_ = preparedStatement.getGeneratedKeys();
                            if (rs_.next()) {
                                int newId = rs_.getInt(1);
                                log.warn("NEW ID:"+newId);
                                daoProxy.getData().put("id$new",newId);
                            }
                            rs_.close();
                        }
                        else
                        {
                            //Handle output column value. Usually this is an auto-generated value (i.e. GUID or One-up)
                            for (PersistQuery.Column column : persistQuery.getResultColumns()) {
                                if (column.getInOut() == PersistQuery.Column.InOut.OUTPUT) {
                                    String columnName = column.getColumnName();
                                    String value = rs.getString(columnName);

                                    if (column.getEntityField() != null) {
                                        EntityProperty entityProperty = entityFields.get(column.getEntityField());

                                        log.debug("Set field [" + entityProperty.getName() + "] of type ("
                                                + entityProperty.getDataType().getSimpleName() + ") " +
                                                "= output column [" + column.getColumnName() + "] = " + value);

                                        //when the transaction is committed, the key will be renamed
                                        daoProxy.getData().put(entityProperty.getName() + "$new", value);
                                        if (entityProperty.getDataType() == Integer.class
                                                || entityProperty.getDataType() == int.class) {
                                            log.debug("Re-setting field value");
                                            daoProxy.getData().put(entityProperty.getName() + "$new",
                                                    rs.getInt(column.getColumnName()));
                                        } else if (entityProperty.getDataType() == Long.class
                                                || entityProperty.getDataType() == long.class) {
                                            log.debug("Re-setting field value");
                                            daoProxy.getData().put(entityProperty.getName() + "$new",
                                                    rs.getLong(column.getColumnName()));
                                        }

                                        //besides auto-generated PK values, this is another possible output
                                        if (entityProperty.getName().equals("__revision"))
                                            continue;
                                    }

                                    log.debug("Set lastEntityId = " + value);
                                    lastEntityId.set(value);
                                }
                            }
                        }
                        return isUpdate;
                    }
                };

                //execute persist
                jdbcOperations.query(preparedStatementCreator,resultSetExtractor);
            }

            //Preserve the current lastEntityId to re-set after persisting Array properties and nested Entities
            String lastEntityId_Orig = lastEntityId.get();
            boolean entityUpdated_Orig = entityUpdated.get();

            //Create AdditionalProperties from all (parent=this) entity PKs
            //Useful for nested entities that require the parent PKs (e.g. log records)
            //(getLastEntityId is useful only when the PK is single (i.e. one-up PK) that is why we have this.)
            List<AdditionalProperty> parentIdentifiers = new ArrayList<>();
            for (EntityProperty field : entityFields.values()) {
                if (field.isIdentifier()) {
                    if ( daoProxy.getData().get(field.getName()+"$new") != null ) {
                        parentIdentifiers.add(new AdditionalProperty("__parent$" + field.getName(),
                                daoProxy.getData().get(field.getName()+"$new")));
                    }
                    else if ( daoProxy.getData().get(field.getName()) != null ) {
                        parentIdentifiers.add(new AdditionalProperty("__parent$" + field.getName(),
                                daoProxy.getData().get(field.getName())));
                    }
                }
            }
            AdditionalProperty[] additionalProperties = Utils.toArray(parentIdentifiers, AdditionalProperty.class);

            //Array properties and nested Entities
            for (EntityProperty field : entityFields.values())
            {
                //For each NEW item from an array property, try to INSERT it
                if (field.isArray()) {
                    List<Object> array = new ArrayList<>();

                    if (daoProxy.getData().get(field.getName() + "$new") instanceof List)
                    {
                        List items = (List) daoProxy.getData().get(field.getName() + "$new");
                        for (Object item : items)
                        {
                            if (field.getFactory() != null && !field.immutable())
                                field.getFactory().insert((Entity) item, additionalProperties);
                            else
                                array.add(item);

                            //reset lastEntityId since the item insert might change lastEntityId
                            lastEntityId.set(lastEntityId_Orig);

                            //set to modified
                            entityUpdated.set(true);
                        }
                    }

                    //Can get and persist arrays in XML format from and into a single table column.
                    //TODO: persist primitive array
                    if (array.size() > 0)
                        log.error("TODO persist: " + field.getName() + "=" + array);
                }
                //Will try to persist a nested Entity for which there is annotated factory
                else if (field.getFactory() != null && !field.immutable()
                        && daoProxy.getData().get(field.getName()) != null)
                {
                    switch (operation) {
                        case INSERT:
                        case INSERT_MERGE:
                        case INSERT_UNIQUE:
                            field.getFactory().insert((Entity) daoProxy.getData().get(field.getName()),
                                    additionalProperties);
                            break;
                        case UPDATE:
                            field.getFactory().update((Entity) daoProxy.getData().get(field.getName()),
                                    additionalProperties);
                            break;
                    }
                }

                //reset lastEntityId since nested objects persistence might have changed lastEntityId
                lastEntityId.set(lastEntityId_Orig);

                if (!entityUpdated.get())//if the nested entity was not updated, reset, otherwise keep the new value
                    entityUpdated.set(entityUpdated_Orig);
            }

            return entityUpdated.get();

        }//end data synchronized

    }//end persist

    private interface PreparedStatementCreatorCustom extends PreparedStatementCreator {
        PreparedStatement getPreparedStatement();
    }
}

