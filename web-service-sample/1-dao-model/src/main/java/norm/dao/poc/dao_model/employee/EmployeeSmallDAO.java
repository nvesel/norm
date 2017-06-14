package norm.dao.poc.dao_model.employee;

import norm.dao.annotations.EntityCommonName;
import norm.dao.poc.dao_model.department.DepartmentDAO;

//provides a small view
@EntityCommonName("employee")
public interface EmployeeSmallDAO extends EmployeeDAO {

    String getName();
    void setName(String name);

    String getSsn();
    void setSsn(String ssn);

    DepartmentDAO getDepartment();
    void setDepartment(DepartmentDAO department);

    String getTitle();
    void setTitle(String title);
}
