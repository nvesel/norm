package norm.dao.poc.dao_model.employeelog;

import norm.dao.*;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class EmployeeLogFactorySqlServer extends EntityFactoryCommon<EmployeeLogDAO> implements EmployeeLogFactory {
    private final Logger log = Logger.getLogger(this.getClass());

    @Autowired
    public EmployeeLogFactorySqlServer(String sqlStatementsFileName) throws IOException, ClassNotFoundException {
        super(EmployeeLogDAO.class, sqlStatementsFileName);
        persistQueries.add(new PersistQuery(Persist.Operation.INSERT_UNIQUE,
                "Insert Employee Log",
                sqlStatements.getProperty("insertEmployeeLog")));
    }

    @Transactional(readOnly=true)
    @Override
    public List<EmployeeLogDAO> getEmployeeLogs(final int emplId) {
        IMapper<EmployeeLogDAO> mapper = getMapperInstance(
                new SqlResultSetMapper(selectQueries.get("getEmployeeLogs"), entityFields)
        );
        jdbcTemplate.query(
                selectQueries.get("getEmployeeLogs").getQuery(),
                new HashMap<String, Object>() {{
                    put("emplId",emplId);
                }},
                mapper
        );
        return mapper.getResult();
    }

    @Override
    public EmployeeLogDAO newEmployeeLog(final String description) {
        return GenericDaoProxy.newInstance(EmployeeLogDAO.class, new HashMap<String, Object>() {{
            put("timeStamp", new Date());
            put("description", description);
        }}, null);
    }

    @Override
    public EmployeeLogDAO insert(EmployeeLogDAO entity, AdditionalProperty... additionalProperties) {
        return persist(entity, Persist.Operation.INSERT, additionalProperties);
    }
}
