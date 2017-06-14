package norm.dao;

import norm.dao.model.PersonDAO;
import norm.dao.model.PersonFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:spring-test-context.xml"})
@Transactional
public class TestInsert
{
    @Autowired
    private PersonFactory personFactory;

    private int newId = -1;

    @Rollback(false)
    @Test
    public void testEntityInsert()
    {
        //Get a blank person entity
        PersonDAO person = personFactory.blank();
        //Populate
        person.setName("A New Person");
        //Add an EXISTING parent person
        PersonDAO parent = personFactory.blank();
        parent.setId(5);
        person.setParent(parent);

        //persist
        personFactory.merge(person);
        System.out.println("Person:"+person);

        newId = person.getId();

        //-------------------------------------------------------------------------------------

        //dissect
        GenericDaoProxy<PersonDAO> dao = GenericDaoProxy.getDaoFromProxiedEntity(person);

        assertThat(dao.getCommonName(), is("person"));
        assertThat(person.__identity(), is(dao.getIdentity()));

        //verify that modified fields are detected correctly and not cleared before transaction commit
        Set<String> modifiedFields = dao.getModifiedFields();
        assertThat(modifiedFields.size(),is(2));
        assertThat(modifiedFields.contains("name"),is(true));
        assertThat(modifiedFields.contains("parent"),is(true));

        assertThat(person.__revision(), is(-1L));//the default value (not yet updated to the new value (1))
        assertThat(dao.getData().get("__revision$new"), is(0L));//verify the new entity revision is 0 (after merge)

        //verify raw data
        assertThat(dao.getData().get("id$new"), is(newId));
        assertThat(dao.getData().get("name"), is("A New Person"));
    }

    @AfterTransaction
    public void verifyFinalDatabaseState()
    {
        //verify the final state of the DB after the persist transaction was committed
        //Re-fetch the person we merged
        PersonDAO person = personFactory.getFullPersonById(newId);

        assertThat(person.__revision(), is(0L));
        assertThat(person.getId(), is(newId));
        assertThat(person.__identity(), is("person["+newId+"]"));
        assertThat(person.getName(), is("A New Person"));

        //verify that the parent was indeed updated and the name was not changed (due to immutability):
        assertThat(person.getParent(), is(instanceOf(PersonDAO.class)));
        assertThat(person.getParent(), is(notNullValue()));
        assertThat(person.getParent().getId(), is(notNullValue()));
        assertThat(person.getParent().getId(), is(5)); //the parent id
        assertThat(person.getParent().getName(), is(notNullValue()));
        assertThat(person.getParent().getName(), is("Person Five")); //the parent name

    }
}
