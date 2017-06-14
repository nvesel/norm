package norm.dao.poc.dao_model.employee;

import norm.dao.IMapper;
import norm.dao.SqlResultSetMapper;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class EmployeeSmallFactorySqlServer
        extends EmployeeFactorySqlServer<EmployeeSmallDAO>
        implements EmployeeSmallFactory {
    private final Logger log = Logger.getLogger(this.getClass());

    @Autowired
    public EmployeeSmallFactorySqlServer(String sqlStatementsFileName) throws IOException, ClassNotFoundException {
        super(EmployeeSmallDAO.class, sqlStatementsFileName);
    }

    @Transactional(readOnly=true)
    @Override
    public List<EmployeeSmallDAO> getEmployeeSmallAll() {
        IMapper<EmployeeSmallDAO> mapper = getMapperInstance(
                new SqlResultSetMapper(selectQueries.get("getEmployeeSmall"), entityFields)
        );

        jdbcTemplate.query(
                selectQueries.get("getEmployeeSmall").getQuery(),
                new HashMap<>(),
                mapper
        );

        return mapper.getResult();
    }
}

