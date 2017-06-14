package norm.dao.poc.dao_model.department;

import norm.dao.annotations.EntityCommonName;
import norm.dao.poc.dao_model.employee.EmployeeSmallDAO;

import java.util.List;

@EntityCommonName("department")
public interface DepartmentFullDAO extends DepartmentDAO {
    List<EmployeeSmallDAO> getEmployees();
}
