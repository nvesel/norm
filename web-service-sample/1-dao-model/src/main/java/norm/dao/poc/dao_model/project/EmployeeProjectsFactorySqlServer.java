package norm.dao.poc.dao_model.project;

import norm.dao.*;
import norm.dao.poc.dao_model.employee.EmployeeDAO;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EmployeeProjectsFactorySqlServer
        extends EntityFactoryCommon<EmployeeDAO>
        implements EmployeeProjectsFactory {
    private final Logger log = Logger.getLogger(this.getClass());

    @Autowired
    public EmployeeProjectsFactorySqlServer(String sqlStatementsFileName) throws IOException, ClassNotFoundException {
        super(EmployeeDAO.class, sqlStatementsFileName);

        entityFields = new HashMap<>();
        Map<String, EntityProperty> entityFieldsEmployeeDAO = Utils.getEntityFields(EmployeeDAO.class);
        //we don't want to include all since arrays and foreign fields will be attempted to be saved.
        entityFields.put("id",entityFieldsEmployeeDAO.get("id"));

        log.info(this+" sqlStatements:"+sqlStatements);

        persistQueries.add(new PersistQuery(
                //we don't really need to notify the caller if the record already exists, that is why we will quietly merge
                Persist.Operation.INSERT_MERGE,
                "Insert into EmployeeProjects",
                sqlStatements.getProperty("assignEmployee")));
    }

    @Transactional(readOnly=false)
    @Override
    public EmployeeDAO insert(EmployeeDAO entity, AdditionalProperty... additionalProperties) {
        return persist(entity, Persist.Operation.INSERT, additionalProperties);
    }
}
