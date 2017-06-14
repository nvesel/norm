package norm.dao.poc.dao_model.project;

import norm.dao.*;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

public class ProjectFactorySqlServer extends EntityFactoryCommon<ProjectDAO> implements ProjectFactory {
    private final Logger log = Logger.getLogger(this.getClass());

    @Autowired
    public ProjectFactorySqlServer(String sqlStatementsFileName) throws IOException, ClassNotFoundException {
        super(ProjectDAO.class, sqlStatementsFileName);

        customDaoInvocationHandler = new CustomDaoInvocationHandler() {
            public Object invoke(GenericDaoProxy dao, AbstractDaoProxy.MethodAttributes method, Object proxy, Method m, final Object[] args) throws UnsupportedOperationException, InvocationTargetException, IllegalAccessException {
                switch (method.getMethodName()) {
                    case "getEmployees":
                        //not needed for the demo - TODO
                        break;
                }
                return dao.invoke_default(proxy, m, args);
            }
        };

        log.info(this+" sqlStatements:"+sqlStatements);

        persistQueries.add(new PersistQuery(
                Persist.Operation.INSERT_MERGE,
                "Merge Project",
                sqlStatements.getProperty("mergeProject")));
    }

    @Transactional(readOnly=true)
    @Override
    public List<ProjectDAO> getProjectsByEmplId(final int emplId) {
        IMapper<ProjectDAO> mapper = getMapperInstance(
                new SqlResultSetMapper(selectQueries.get("getProjectsByEmplId"), entityFields)
        );
        jdbcTemplate.query(
                selectQueries.get("getProjectsByEmplId").getQuery(),
                new HashMap<String, Object>() {{
                    put("emplId",emplId);
                }},
                mapper
        );
        return mapper.getResult();
    }

    @Transactional(readOnly=true)
    @Override
    public ProjectDAO getProjectById(final int projId) {
        IMapper<ProjectDAO> mapper = getMapperInstance(
                new SqlResultSetMapper(selectQueries.get("getProjectById"), entityFields)
        );

        jdbcTemplate.query(
                selectQueries.get("getProjectById").getQuery(),
                new HashMap<String, Object>() {{
                    put("projId", projId);
                }},
                mapper
        );

        return mapper.getSingleResult();
    }

    ///////////////////////////////////////////
    //Persistence
    ///////////////////////////////////////////
    private ProjectDAO persist(ProjectDAO entity, Persist.Operation persistOperation) {
        final GenericDaoProxy<ProjectDAO> dao = GenericDaoProxy.getDaoFromProxiedEntity(entity);

        //optional - only if we care about the entity after persistance.
        afterTransactionExecutor.schedule(
                dao,
                new AfterTransactionRunnable() { public void run() {dao.getData().remove("employees$new");}},
                null);

        return super.persist(entity, persistOperation);
    }

    @Transactional(readOnly=false)
    @Override
    public ProjectDAO insert(ProjectDAO entity, AdditionalProperty... additionalProperties) {
        return persist(entity, Persist.Operation.INSERT);
    }

    @Transactional(readOnly=false)
    @Override
    public ProjectDAO update(ProjectDAO entity, AdditionalProperty... additionalProperties) {
        //Note, that we (can) use insert merge instead of update
        return persist(entity, Persist.Operation.INSERT_MERGE);
    }

}


