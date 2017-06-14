package norm.dao.model;

import norm.dao.EntityFactory;

import java.util.List;

public interface PersonFactory extends EntityFactory<PersonDAO> {
    List<PersonDAO> getAllPersons();
    PersonDAO getPersonById(int personId);
    PersonDAO getFullPersonById(int personId);
}
