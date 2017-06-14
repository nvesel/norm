package norm.dao;

import norm.dao.exceptions.DaoRuntimeException;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class EntityFactoryCommon<T extends Entity> implements EntityFactory<T> {
    private final Logger log = Logger.getLogger(EntityFactoryCommon.class);

    @Autowired
    protected NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    protected AfterTransactionExecutor afterTransactionExecutor;

    protected Class<T> entityClass;

    protected Properties sqlStatements;
    protected Map<String, EntityProperty> entityFields = new HashMap<>();
    protected Map<String, SelectQuery> selectQueries = new HashMap<>();
    protected List<PersistQuery> persistQueries = new ArrayList<>();

    protected CustomDaoInvocationHandler customDaoInvocationHandler = null;

    public EntityFactoryCommon(Class<T> entityClass, String sqlStatementsFileName)
            throws IOException, ClassNotFoundException {
        log.info("new EntityFactoryCommon<"+entityClass.getSimpleName()+">");

        this.entityClass = entityClass;

        entityFields = Utils.getEntityFields(entityClass);

        log.debug("sqlStatementsFileName="+sqlStatementsFileName);
        if (!StringUtils.isEmpty(sqlStatementsFileName)) {
            log.info("Parsing "+sqlStatementsFileName);
            sqlStatements = Utils.loadXmlProperties(sqlStatementsFileName);
            selectQueries = Utils.getSelectQueriesFromProperties(sqlStatements);
        }

        log.info(this+" sqlStatements:"+sqlStatements);
    }

    @Override
    public Map<String, EntityProperty> getEntityFields() {
        return entityFields;
    }

    @Override
    public T blank() {
        return GenericDaoProxy.newInstance(entityClass, new HashMap<>(), customDaoInvocationHandler);
    }

    @Override
    public IMapper<T> getMapperInstance(SqlResultSetMapper sqlResultSetMapper) {
        return new Mapper<>(entityClass, sqlResultSetMapper, customDaoInvocationHandler);
    }

    private class Mapper<MT extends T> implements IMapper<MT> {
        private Class<MT> type;
        private final List<MT> result = new ArrayList<>();
        private final Map<String, Integer> resultMap = new HashMap<>();//identity to index in the result list

        SqlResultSetMapper sqlResultSetMapper;
        CustomDaoInvocationHandler customDaoInvocationHandler = null;

        Mapper(Class<MT> type, SqlResultSetMapper sqlResultSetMapper,
               CustomDaoInvocationHandler customDaoInvocationHandler) {
            this.type = type;
            this.sqlResultSetMapper = sqlResultSetMapper;
            this.customDaoInvocationHandler = customDaoInvocationHandler;
        }

        @Override
        public MT mapRow(ResultSet rs, int rowNum) throws SQLException
        {
            Map<String, Object> entityData = sqlResultSetMapper.mapRow(rs, rowNum);

            MT entity = GenericDaoProxy.newInstance(type, entityData, customDaoInvocationHandler);

            //Not each row from the result set contains a new entity. It is possible that from one query we build a
            // complex entity with nested entities. That is why we do this:
            if (entity.__identity() != null && resultMap.containsKey(entity.__identity()))
            {//update existing one
                result.set(resultMap.get(entity.__identity()), entity);
            }
            else {//insert a new one
                result.add(entity);
                resultMap.put(entity.__identity(), result.size()-1);
            }

            return entity;
        }

        @Override
        public Map<String, Integer> getResultMap() {
            return resultMap;
        }

        @Override
        public List<MT> getResult() {
            return result;
        }

        @Override
        public MT getSingleResult() {
            if (result.size() == 0)
                return null;
            else if (result.size() > 1)
                log.warn("getSingleResult was invoked but the result contains more than one item. Use getResult() instead.");

            return result.get(0);
        }
    }

    public List<T> getListResult(String queryAlias, HashMap<String, Object> params) {
        IMapper<T> mapper = getMapperInstance(
                new SqlResultSetMapper(selectQueries.get(queryAlias), entityFields)
        );

        jdbcTemplate.query(
                selectQueries.get(queryAlias).getQuery(),
                params,
                mapper
        );

        return mapper.getResult();
    }

    public T getSingleResult(String queryAlias, HashMap<String, Object> params) {
        IMapper<T> mapper = getMapperInstance(
                new SqlResultSetMapper(selectQueries.get(queryAlias), entityFields)
        );

        jdbcTemplate.query(
                selectQueries.get(queryAlias).getQuery(),
                params,
                mapper
        );

        return mapper.getSingleResult();
    }

    @Transactional
    protected T persist(T entity, Persist.Operation persistOperation, AdditionalProperty... additionalProperties) {
        final GenericDaoProxy<T> dao = GenericDaoProxy.getDaoFromProxiedEntity(entity);

        for (AdditionalProperty additionalProperty : additionalProperties) {
            if (dao.getData().containsKey(additionalProperty.getKey()))
                throw new DaoRuntimeException("Contextual Argument '"
                        + additionalProperty.getKey()+"' already exists in entity "+entityClass);
            dao.getData().put(additionalProperty.getKey(), additionalProperty.getValue());
        }

        afterTransactionExecutor.schedule(dao, new AfterTransactionRunnable() {
            public void run() {
                //Cleanup generated fields (like one-up ids)
                Map<String, Object> data = dao.getData();
                for (String fieldName : (new HashSet<>(data.keySet()))) {
                    if (fieldName.endsWith("$new")) {
                        data.put(fieldName.replaceAll("\\$new$", ""), data.remove(fieldName));
                    }
                    else if (fieldName.startsWith("__parent$")) {
                        data.remove(fieldName);
                    }
                }
            }
        }, null);

        boolean entityUpdated = Persist.persist(dao, entityFields, persistQueries, jdbcTemplate.getJdbcOperations(), persistOperation);

        if (entityUpdated) {//It might be useful for cache management
            String entityIdentity = dao.getIdentity();
        }

        for (AdditionalProperty additionalProperty : additionalProperties) {
            dao.getData().remove(additionalProperty.getKey());
        }

        return entity;
    }

    @Override
    public T insert(T entity, AdditionalProperty... additionalProperties) {
        throw new UnsupportedOperationException("Insert is not implemented");
    }

    @Override
    public T merge(T entity, AdditionalProperty... additionalProperties) {
        throw new UnsupportedOperationException("Merge is not implemented");
    }

    @Override
    public T update(T entity, AdditionalProperty... additionalProperties) {
        throw new UnsupportedOperationException("Update is not implemented");
    }

    @Override
    public void delete(T entity, AdditionalProperty... additionalProperties) {
        throw new UnsupportedOperationException("Delete is not implemented");
    }

}

