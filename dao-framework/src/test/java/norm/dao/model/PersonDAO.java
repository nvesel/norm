package norm.dao.model;

import norm.dao.Entity;
import norm.dao.annotations.EntityCommonName;
import norm.dao.annotations.EntityIdentifier;
import norm.dao.annotations.ForeignFactory;
import norm.dao.annotations.LazyFetched;

@EntityCommonName("person")
public interface PersonDAO extends Entity
{
    @EntityIdentifier
    int id = -1;

    String name = "John Smith";

    @ForeignFactory(
            value = PersonFactory.class,
            immutable = true //makes the nested parent object immutable (the default). (Can set a new one but cannot modify the current one)
    )
    @LazyFetched
    PersonDAO parent = null;

    int getId();
    void setId(int id);

    String getName();
    void setName(String name);

    PersonDAO getParent();
    void setParent(PersonDAO parent);
}
