package norm.dao.poc.service.dto_model.employee;

import com.fasterxml.jackson.annotation.*;
import norm.dao.FactoriesRegistryBean;
import norm.dao.poc.dao_model.employee.EmployeeDAO;
import norm.dao.poc.dao_model.employee.EmployeeSmallDAO;
import norm.dao.poc.dao_model.employee.EmployeeSmallFactory;
import norm.dao.poc.service.dto_model.ObjectFactory;
import norm.dao.poc.service.dto_model.department.Department;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.ResourceSupport;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;

//An example of how to use the DAO

@JsonPropertyOrder({ "pk", "name", "ssn", "department", "links" })
@XmlRootElement
@XmlType(
        propOrder = { "pk", "name", "ssn", "department" }
//        ,factoryClass = ObjectFactory.class, factoryMethod = "newEmployeeSmall"
)
public class EmployeeSmall extends ResourceSupport implements Serializable {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private EmployeeSmallDAO employeeSmallDAO;

    //We need to define the fields in order for the validation framework not to throw an exception immediately if a property is not set. For example PATCH and entity.
    //This way the Validation is using the fields instead of the getters
    private int pk = EmployeeDAO.id;
    @NotNull @Pattern(regexp = "[\\w ]+")
    private String name = EmployeeDAO.name; //by setting a bogus value, we trick the @NotNull validation when the property if not supplied with the input...
    @NotNull @Pattern(regexp = "[0-9\\-]+")
    private String ssn = EmployeeDAO.ssn; //...It works well for not-required properties or when doing PATCH
    @Valid
    private Department department;
    private String title = EmployeeDAO.title;

    //See ObjectMapperInjectDaoFactories
    @JsonCreator
    public EmployeeSmall(@JacksonInject final EmployeeSmallFactory employeeFactorySmall) {
        this.employeeSmallDAO = employeeFactorySmall.blank();
    }

    public EmployeeSmall(EmployeeSmallDAO employeeSmallDAO) {
        this.employeeSmallDAO = employeeSmallDAO;
    }

    //This could be the default constructor if we don't use ObjectMapperInjectDaoFactories
    //We can omit it if we enable factoryClass = ObjectFactory.class, factoryMethod = "newEmployeeSmall"
    public EmployeeSmall() {
        this.employeeSmallDAO = FactoriesRegistryBean.getEntityFactory(EmployeeSmallFactory.class).blank();
    }

    @JsonProperty(value = "pk", required = false)
    public int getPk() {
        return employeeSmallDAO.getId();
    }

    public void setPk(int pk) {
        this.pk = pk;
        employeeSmallDAO.setId(pk);
    }

    //@JsonProperty(value = "name", required = true) @NotNull @Pattern(regexp = "[\\w ]+")
    public String getName() {
        return employeeSmallDAO.getName();
    }

    public void setName(String name) {
        this.name = name;
        employeeSmallDAO.setName(name);
    }

    @JsonProperty(value = "ssn", required = true)// @NotNull @Pattern(regexp = "[0-9]+")
    public String getSsn() {
        return employeeSmallDAO.getSsn();
    }

    public void setSsn(String ssn) {
        this.ssn = ssn;
        employeeSmallDAO.setSsn(ssn);
    }

    //@JsonProperty(value = "department", required = true) @Valid
    public Department getDepartment() {
        return new Department(employeeSmallDAO.getDepartment());
    }

    public void setDepartment(Department department) {
        this.department = department;
        employeeSmallDAO.setDepartment(department.__getDepartmentDAO());
    }

    @JsonProperty(value = "title", required = false)
    public String getTitle() {
        return employeeSmallDAO.getTitle();
    }

    public void setTitle(String title) {
        this.title = title;
        employeeSmallDAO.setTitle(title);
    }

    @JsonIgnore
    public EmployeeSmallDAO __getEmployeeSmallDAO() {
            return employeeSmallDAO;
    }

    @Override
    public String toString() {
        return String.valueOf(__getEmployeeSmallDAO());
    }
}
