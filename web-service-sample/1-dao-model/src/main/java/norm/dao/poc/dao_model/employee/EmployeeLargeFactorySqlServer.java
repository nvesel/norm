package norm.dao.poc.dao_model.employee;

import norm.dao.*;
import norm.dao.poc.dao_model.employeelog.EmployeeLogDAO;
import norm.dao.poc.dao_model.project.ProjectDAO;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EmployeeLargeFactorySqlServer extends EmployeeFactorySqlServer<EmployeeLargeDAO> implements EmployeeLargeFactory {
    private final Logger log = Logger.getLogger(this.getClass());

    @Autowired
    public EmployeeLargeFactorySqlServer(String sqlStatementsFileName) throws IOException, ClassNotFoundException {
        super(EmployeeLargeDAO.class, sqlStatementsFileName);

        customDaoInvocationHandler = new CustomDaoInvocationHandler() {
            @Override
            public Object invoke(GenericDaoProxy dao, AbstractDaoProxy.MethodAttributes method, Object proxy, Method m, Object[] args) throws UnsupportedOperationException, InvocationTargetException, IllegalAccessException {
                synchronized (dao.getData()) {
                    switch (method.getMethodName()) {
                        case "getEmployeeLogs":
                            if (!dao.getData().containsKey("employeeLogs")) {
                                log.info("Lazy fetch employeeLogs");
                                dao.getData().put("employeeLogs",
                                        employeeLogFactory.getEmployeeLogs((Integer) dao.getData().get("id")));
                            }
                            return new ArrayList() {{
                                addAll((List) dao.getData().get("employeeLogs"));
                                addAll((dao.getData().containsKey("employeeLogs$new")) ?
                                        (List) dao.getData().get("employeeLogs$new") :
                                        new ArrayList<>());
                            }};
                        case "getEmployeeProjects":
                            if (!dao.getData().containsKey("employeeProjects")) {
                                log.info("Lazy fetch employeeProjects");
                                dao.getData().put("employeeProjects",
                                        projectFactory.getProjectsByEmplId((Integer) dao.getData().get("id")));
                            }
                            return dao.getData().get("employeeProjects");
                        case "getManager":
                            if (!dao.getData().containsKey("manager")) {
                                log.info("Lazy fetch manager");
                                dao.getData().put("manager", getEmployeeManager((Integer) dao.getData().get("id")));
                            }
                            return dao.getData().get("manager");
                    }
                }
                return dao.invoke_default(proxy, m, args);
            }
        };
    }

    @Transactional(readOnly=true)
    @Override
    public EmployeeLargeDAO getEmployeeLargeById(final int emplId) {
        log.debug("getEmployeeLargeById:"+emplId);

        IMapper<EmployeeLargeDAO> mapper = getMapperInstance(
                new SqlResultSetMapper(selectQueries.get("getEmployeeLargeById"), entityFields)
        );

        jdbcTemplate.query(
                selectQueries.get("getEmployeeLargeById").getQuery(),
                new HashMap<String, Object>() {{
                    put("emplId", emplId);
                }},
                mapper
        );

        return mapper.getSingleResult();
    }

    @Transactional(readOnly=true)
    @Override
    public EmployeeLargeDAO getEmployeeLargeWithSomePrefetchedData(final int emplId) {
        log.debug("getEmployeeLargeWithSomePrefetchedData:"+emplId);

        IMapper<EmployeeLargeDAO> mapper = getMapperInstance(new SqlResultSetMapper(selectQueries.get("getEmployeeAndManagerAtOnce"), entityFields));

        jdbcTemplate.query(
                selectQueries.get("getEmployeeAndManagerAtOnce").getQuery(),
                new HashMap<String, Object>() {{
                    put("emplId", emplId);
                }},
                mapper
        );

        return mapper.getSingleResult();
    }

    //When we know we are producing the full scope of an entity we can decide to make the method generic
    //Which properties are fetched can be further elaborated based on the generic type (scope)
    @Transactional(readOnly=true)
    @Override
    public List<EmployeeLargeDAO> getEmployeeLargeWithPreFetch() {
        IMapper<EmployeeLargeDAO> mapperSelf = getMapperInstance(new SqlResultSetMapper(selectQueries.get("getEmployeeLarge"), entityFields));
        jdbcTemplate.query(
                selectQueries.get("getEmployeeLarge").getQuery(),
                new HashMap<>(),
                mapperSelf
        );

        //Although we know the query will not produce the full employee entity, we declare it like that in order to get the "manager" property after that
        IMapper<EmployeeLargeDAO> mapperManagers = getMapperInstance(new SqlResultSetMapper(selectQueries.get("getEmployeesManagerBulk"), entityFields));
        jdbcTemplate.query(
                selectQueries.get("getEmployeesManagerBulk").getQuery(),
                new HashMap<>(),
                mapperManagers
        );

        //similar to the above, but the projects are in arrays
        IMapper<EmployeeLargeDAO> mapperProjects = getMapperInstance(new SqlResultSetMapper(selectQueries.get("getEmployeesProjectsBulk"), entityFields));
        jdbcTemplate.query(
                selectQueries.get("getEmployeesProjectsBulk").getQuery(),
                new HashMap<>(),
                mapperProjects
        );

        //yet again for the logs
        IMapper<EmployeeLargeDAO> mapperLogs = getMapperInstance(new SqlResultSetMapper(selectQueries.get("getEmployeeLogsBulk"), entityFields));
        jdbcTemplate.query(
                selectQueries.get("getEmployeeLogsBulk").getQuery(),
                new HashMap<>(),
                mapperLogs
        );

        List<EmployeeLargeDAO> result = mapperSelf.getResult();

        //MERGE all pre-fetched properties into the main list/result
        for (String identity : mapperSelf.getResultMap().keySet()) {
            DaoProxy dao = GenericDaoProxy.getDaoFromProxiedEntity(result.get(mapperSelf.getResultMap().get(identity)));

            //Manager
            EmployeeSmallDAO manager = null;
            if (mapperManagers.getResultMap().containsKey(identity))
                manager = mapperManagers.getResult().get(mapperManagers.getResultMap().get(identity)).getManager();
            dao.getData().put("manager", manager);

            //Projects
            List<ProjectDAO> employeeProjects = new ArrayList<>();
            if (mapperProjects.getResultMap().containsKey(identity))
                employeeProjects = mapperProjects.getResult().get(mapperProjects.getResultMap().get(identity)).getEmployeeProjects();
            dao.getData().put("employeeProjects", employeeProjects);

            //Logs
            List<EmployeeLogDAO> employeeLogs = new ArrayList<>();
            if (mapperLogs.getResultMap().containsKey(identity))
                employeeLogs = mapperLogs.getResult().get(mapperLogs.getResultMap().get(identity)).getEmployeeLogs();
            dao.getData().put("employeeLogs", employeeLogs);
        }

        return result;
    }

    @Transactional(readOnly=true)
    @Override
    public EmployeeSmallDAO getEmployeeManager(final int emplId) {
        IMapper<EmployeeLargeDAO> mapper = getMapperInstance(new SqlResultSetMapper(selectQueries.get("getEmployeeManager"), entityFields));

        jdbcTemplate.query(
                selectQueries.get("getEmployeeManager").getQuery(),
                new HashMap<String, Object>() {{
                    put("emplId", emplId);
                }},
                mapper
        );

        return mapper.getSingleResult().getManager();
    }

    @Transactional(readOnly=true)
    @Override
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public EmployeeLargeDAO transformEmployeeSmall(final EmployeeSmallDAO employeeSmall) {
        synchronized (employeeSmall) {//in order to prevent concurrent transformations of the same entity
            if (employeeSmall instanceof EmployeeLargeDAO)
                return (EmployeeLargeDAO) employeeSmall;

            //EmployeeSmall is not EmployeeLarge so we have to replenish the data and re-wrap in EmployeeLarge
            return  GenericDaoProxy.getDaoFromProxiedEntity(employeeSmall).transform(getEmployeeLargeById(employeeSmall.getId()), entityClass);
        }
    }

}

