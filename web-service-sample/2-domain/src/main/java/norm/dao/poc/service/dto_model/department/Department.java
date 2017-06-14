package norm.dao.poc.service.dto_model.department;

import com.fasterxml.jackson.annotation.*;
import norm.dao.FactoriesRegistryBean;
import norm.dao.poc.dao_model.department.DepartmentDAO;
import norm.dao.poc.dao_model.department.DepartmentFactory;
import norm.dao.poc.service.dto_model.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.ResourceSupport;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;

@JsonPropertyOrder({ "pk", "name", "links" })
@XmlType(
        propOrder = { "pk", "name" }
//        ,factoryClass = ObjectFactory.class, factoryMethod = "newDepartment"
)
public class Department extends ResourceSupport implements Serializable {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private DepartmentDAO departmentDAO;

    @NotNull //This will enforce that pk is always provided
    private int pk;
    @NotNull @Pattern(regexp = "[\\w ]+")
    private String name = DepartmentDAO.name;

    //See ObjectMapperInjectDaoFactories
    @JsonCreator
    public Department(@JacksonInject final DepartmentFactory departmentFactory) {
        this.departmentDAO = departmentFactory.blank();
    }

    public Department(DepartmentDAO departmentDAO) {
        this.departmentDAO = departmentDAO;
    }

    //This could be the default constructor if we don't use ObjectMapperInjectDaoFactories
    //We can omit it if we enable factoryClass = ObjectFactory.class, factoryMethod = "newDepartment"
    public Department() {
        this.departmentDAO = FactoriesRegistryBean.getEntityFactory(DepartmentFactory.class).blank();
    }

    @JsonProperty(value = "pk", required = true)
    public int getPk() {
        return departmentDAO.getId();
    }

    public void setPk(int pk) {
        this.pk = pk;
        departmentDAO.setId(pk);
    }

    @JsonProperty(value = "name", required = false)
    public String getName() {
        return departmentDAO.getName();
    }

    public void setName(String name) {
        this.name = name;
        departmentDAO.setName(name);
    }

    @JsonIgnore
    public DepartmentDAO __getDepartmentDAO() {
        return departmentDAO;
    }
}
