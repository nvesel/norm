package norm.dao;

import norm.dao.model.PersonDAO;
import norm.dao.model.PersonFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)//Spring Test Context Framework
@ContextConfiguration(locations = {"classpath:spring-test-context.xml"})
public class TestFetch
{
    @Autowired
    private PersonFactory personFactory;

    @Test
    public void testGetAllPersons()
    {
        List<PersonDAO> allPersons = personFactory.getAllPersons();
        PersonDAO person = allPersons.get(1);//pick an arbitrary item from the list

        assertThat(person.__revision(), is(0L));
        assertThat(person.__identity(), is("person[2]"));
        assertThat(person.getId(), is(2));
        assertThat(person.getName(), is("Person Two"));
    }

    @Test
    public void testGetPersonById()
    {
        PersonDAO person = personFactory.getPersonById(5);//existing personId

        assertThat(person.__revision(), is(0L));
        assertThat(person.__identity(), is("person[5]"));
        assertThat(person.getId(), is(5));
        assertThat(person.getName(), is("Person Five"));
    }

    @Test
    public void testGetPersonByInvalidId()
    {
        PersonDAO person = personFactory.getPersonById(1111);//invalid personId
        assertThat(person, is(nullValue()));
    }

    @Test
    public void testGetFullPersonById()
    {
        PersonDAO person = personFactory.getFullPersonById(6);

        assertThat(person.__revision(), is(0L));
        assertThat(person.__identity(), is("person[6]"));
        assertThat(person.getId(), is(6));
        assertThat(person.getName(), is("Person Six"));
        //the nested object:
        assertThat(person.getParent().getId(), is(2)); //the parent id
        assertThat(person.getParent().getName(), is("Person Two")); //the parent name
    }
}
