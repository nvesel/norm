package norm.dao.poc.dao_model.employee;

import norm.dao.annotations.EntityCommonName;
import norm.dao.poc.dao_model.employeelog.EmployeeLogDAO;
import norm.dao.poc.dao_model.project.ProjectDAO;
import java.util.Date;
import java.util.List;

@EntityCommonName("employee")
public interface EmployeeLargeDAO extends EmployeeSmallDAO {
    Date getStartDate();
    void setStartDate(Date startDate);

    EmployeeSmallDAO getManager();
    void setManager(EmployeeSmallDAO managerId);

    List<ProjectDAO> getEmployeeProjects();

    List<EmployeeLogDAO> getEmployeeLogs();
}
