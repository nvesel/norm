package norm.dao;

import norm.dao.model.PersonDAO;
import norm.dao.model.PersonFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:spring-test-context.xml"})
@Transactional
public class TestUpdate
{
    @Autowired
    private PersonFactory personFactory;

    private PersonDAO personUnderTest;

    private long originalRevision = -1;

    @BeforeTransaction
    public void verifyInitialDatabaseState() {
        //Verify the initial state before a transaction is started
        //Fetch the person we are going to update
        PersonDAO person = personFactory.getPersonById(6);//arbitrary personId
        assertThat(person.__identity(), is("person[6]"));
        assertThat(person.getId(), is(6));
        assertThat(person.getName(), is("Person Six"));

        //revision might have been changed during other tests
        originalRevision = person.__revision();
    }

    @Before
    public void setUpTestDataWithinTransaction() {
        // set up test data within the transaction
    }

    @Rollback(false)
    @Test
    public void testEntityUpdate()
    {
        //Get a blank person entity
        personUnderTest = personFactory.blank();
        //Populate
        personUnderTest.setId(6);//specify which person
        personUnderTest.setName("Person Six Updated");//update person name

        //persist
        personFactory.update(personUnderTest);

        //-------------------------------------------------------------------------------------

        //dissect
        GenericDaoProxy<PersonDAO> dao = GenericDaoProxy.getDaoFromProxiedEntity(personUnderTest);

        assertThat(dao.getCommonName(), is("person"));
        assertThat(personUnderTest.__identity(), is(dao.getIdentity()));

        //verify that modified fields are detected correctly and not cleared before transaction commit
        Set<String> modifiedFields = dao.getModifiedFields();
        assertThat(modifiedFields.size(),is(2));
        assertThat(modifiedFields.contains("id"),is(true));
        assertThat(modifiedFields.contains("name"),is(true));

        assertThat(personUnderTest.__revision(), is(-1L));//the default value (not yet updated to the new value (1))
        assertThat(dao.getData().get("__revision$new"), is(originalRevision+1));//verify the new entity revision is 1 (after update)
        //after the transaction is committed __revision$new will be renamed to __revision. See verifyFinalDatabaseState()

        //verify raw data
        assertThat(dao.getData().get("name"), is("Person Six Updated"));
        assertThat(dao.getData().get("id"), is(6));
    }

    @After
    public void tearDownWithinTransaction() {
        // "tear down" logic within the transaction
    }

    @AfterTransaction
    public void verifyFinalDatabaseState()
    {
        //Verify the state of the original personUnderTest object after the transaction was committed
        //without re-fetching from the DB
        assertThat(personUnderTest.__revision(), is(originalRevision+1));//note this, the revision was bumped
        assertThat(personUnderTest.__identity(), is("person[6]"));
        assertThat(personUnderTest.getId(), is(6));
        assertThat(personUnderTest.getName(), is("Person Six Updated"));

        //verify the final state of the DB after the persist transaction was committed
        //Re-fetch the person we updated
        PersonDAO person = personFactory.getPersonById(6);//arbitrary personId
        assertThat(person.__revision(), is(originalRevision+1));
        assertThat(person.__identity(), is("person[6]"));
        assertThat(person.getId(), is(6));
        assertThat(person.getName(), is("Person Six Updated"));
    }
}
