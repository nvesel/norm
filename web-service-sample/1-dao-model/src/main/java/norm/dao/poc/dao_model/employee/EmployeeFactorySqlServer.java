package norm.dao.poc.dao_model.employee;

import norm.dao.*;
import norm.dao.poc.dao_model.employeelog.EmployeeLogDAO;
import norm.dao.poc.dao_model.employeelog.EmployeeLogFactory;
import norm.dao.poc.dao_model.project.ProjectFactory;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

abstract public class EmployeeFactorySqlServer<T extends EmployeeDAO>
        extends EntityFactoryCommon<T>
        implements EntityFactory<T> {
    private final Logger log = Logger.getLogger(this.getClass());

    @Autowired
    ProjectFactory projectFactory;
    @Autowired
    EmployeeLogFactory employeeLogFactory;

    @Autowired
    public EmployeeFactorySqlServer(Class<T> entityClass, String sqlStatementsFileName)
            throws IOException, ClassNotFoundException {
        super(entityClass, sqlStatementsFileName);

        //Persistence Queries are executed in order of insertion!
        //Insert Queries
        persistQueries.add(new PersistQuery(Persist.Operation.INSERT_MERGE,
                "Merge Person",
                sqlStatements.getProperty("mergePersonByEmplIdNameSsn")));
        persistQueries.add(new PersistQuery(Persist.Operation.INSERT_UNIQUE,
                "Insert Employee",
                sqlStatements.getProperty("insertEmployee")));
        persistQueries.add(new PersistQuery(Persist.Operation.INSERT_MERGE,
                "Update Revision",
                sqlStatements.getProperty("getRevision")));
        //Update queries
        persistQueries.add(new PersistQuery(Persist.Operation.UPDATE,
                "Persist Person",
                sqlStatements.getProperty("updatePersonByEmplId")));
        persistQueries.add(new PersistQuery(Persist.Operation.UPDATE,
                "Persist Employee",
                sqlStatements.getProperty("updateEmployeeById")));
        log.debug("persistQueries:"+persistQueries);
    }

    ///////////////////////////////////////////
    //Persistence
    ///////////////////////////////////////////
    public T persist(T entity, Persist.Operation persistOperation) {
        final GenericDaoProxy dao = GenericDaoProxy.getDaoFromProxiedEntity(entity);

        //optional - only if you care about the entity after persistance.
        afterTransactionExecutor.schedule(dao, new AfterTransactionRunnable() {
            public void run() {
                    List<EmployeeLogDAO> employeeLogs$news =
                            (List<EmployeeLogDAO>) dao.getData().remove("employeeLogs$new");
                    if (dao.getData().get("employeeLogs") instanceof List && employeeLogs$news != null)
                        ((List<EmployeeLogDAO>)dao.getData().get("employeeLogs")).addAll(employeeLogs$news);
                }
            }, null);

        return super.persist(entity, persistOperation);
    }

    @Transactional(readOnly=false)
    @Override
    public T insert(T entity, AdditionalProperty... additionalProperties) {
        return persist(entity, Persist.Operation.INSERT);
    }

    @Transactional(readOnly=false)
    @Override
    public T update(T entity, AdditionalProperty... additionalProperties) {
        return persist(entity, Persist.Operation.UPDATE);
    }

}

