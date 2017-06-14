package norm.dao.poc.dao_model.project;

import norm.dao.*;
import norm.dao.annotations.*;
import norm.dao.poc.dao_model.employee.EmployeeSmallDAO;

import java.util.List;

@EntityCommonName("project")
public interface ProjectDAO extends Entity
{
    @EntityIdentifier
    int id = -1;

    String name = "Project Manhattan";

    @ForeignFactory(value = EmployeeProjectsFactory.class, immutable = false)
    @LazyFetched
    List<EmployeeSmallDAO> employees = null;

    int getId();

    String getName();

    @EntityAccessorHelper(fieldName = "employees", action = "add")
    void assignEmployee(EmployeeSmallDAO employee);
}
