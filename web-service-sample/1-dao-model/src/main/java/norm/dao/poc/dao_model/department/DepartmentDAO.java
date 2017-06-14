package norm.dao.poc.dao_model.department;

import norm.dao.Entity;
import norm.dao.annotations.EntityCommonName;
import norm.dao.annotations.EntityIdentifier;
import norm.dao.annotations.LazyFetched;
import norm.dao.poc.dao_model.employee.EmployeeSmallDAO;

import java.util.List;

@EntityCommonName("department")
public interface DepartmentDAO extends Entity
{
    @EntityIdentifier
    int id = -1;

    String name = "Software Development";

    @LazyFetched
    List<EmployeeSmallDAO> employees = null;

    int getId();
    void setId(int id);

    String getName();
    void setName(String name);
}
