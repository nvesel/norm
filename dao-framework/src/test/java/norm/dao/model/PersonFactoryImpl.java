package norm.dao.model;

import norm.dao.AdditionalProperty;
import norm.dao.EntityFactoryCommon;
import norm.dao.Persist;
import norm.dao.PersistQuery;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class PersonFactoryImpl extends EntityFactoryCommon<PersonDAO> implements PersonFactory {

    public PersonFactoryImpl(String sqlStatementsFileName) throws IOException, ClassNotFoundException {
        super(PersonDAO.class, sqlStatementsFileName);

        //Specify persist queries:
        persistQueries.add(new PersistQuery(Persist.Operation.UPDATE,
                "Update a person",
                sqlStatements.getProperty("updatePerson")));
        persistQueries.add(new PersistQuery(Persist.Operation.INSERT_MERGE,
                "Merge insert a person",
                sqlStatements.getProperty("mergeInsertPerson")));
        persistQueries.add(new PersistQuery(Persist.Operation.INSERT_UNIQUE,
                "Insert a new person",
                sqlStatements.getProperty("newPerson")));

    }

    @Transactional(readOnly=true)
    @Override
    public List<PersonDAO> getAllPersons() {
        return super.getListResult("getAllPersons", new HashMap<>());
    }

    @Transactional(readOnly=true)
    @Override
    public PersonDAO getPersonById(int personId) {
        return super.getSingleResult("getPersonById", new HashMap<String, Object>() {{
            put("pid", personId);
        }});
    }

    @Transactional(readOnly=true)
    @Override
    public PersonDAO getFullPersonById(int personId) {
        return super.getSingleResult("getFullPersonById", new HashMap<String, Object>() {{
            put("pid", personId);
        }});
    }

    @Override
    public PersonDAO update(PersonDAO entity, AdditionalProperty... additionalProperties) {
        return super.persist(entity, Persist.Operation.UPDATE, additionalProperties);
    }

    @Override
    public PersonDAO merge(PersonDAO entity, AdditionalProperty... additionalProperties) {
        return super.persist(entity, Persist.Operation.INSERT_MERGE, additionalProperties);
    }

    @Override
    public PersonDAO insert(PersonDAO entity, AdditionalProperty... additionalProperties) {
        return super.persist(entity, Persist.Operation.INSERT_UNIQUE, additionalProperties);
    }

}
