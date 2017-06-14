package norm.dao.poc.dao_model.employeelog;

import norm.dao.EntityFactory;

import java.util.List;

public interface EmployeeLogFactory extends EntityFactory<EmployeeLogDAO> {
    List<EmployeeLogDAO> getEmployeeLogs(int emplId);
    EmployeeLogDAO newEmployeeLog(final String description);
}
