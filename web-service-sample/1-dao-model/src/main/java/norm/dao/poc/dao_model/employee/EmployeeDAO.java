package norm.dao.poc.dao_model.employee;

import norm.dao.Entity;
import norm.dao.annotations.*;
import norm.dao.poc.dao_model.department.DepartmentDAO;
import norm.dao.poc.dao_model.department.DepartmentFactory;
import norm.dao.poc.dao_model.employeelog.EmployeeLogDAO;
import norm.dao.poc.dao_model.employeelog.EmployeeLogFactory;
import norm.dao.poc.dao_model.project.ProjectDAO;
import norm.dao.poc.dao_model.project.ProjectFactory;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

@EntityCommonName("employee")
public interface EmployeeDAO extends Entity
{
    @EntityIdentifier
    int id = -1;

    String name = "Fake Person"; //Valid looking values. Optional but recommended.
    // Used in the service layer in order for the data validation to pass when "Patching" an entity.
    // Useful with mock-ups

    String ssn = "123-12-1234";

    @ForeignFactory(
            value = DepartmentFactory.class,
            immutable = true //makes department nested entity immutable (the default). Can set a new one but cannot modify the current one.
    )
    DepartmentDAO department = null;

    Date startDate = new Date(System.currentTimeMillis());

    Date endDate = new Date(System.currentTimeMillis());

    String title = "Developer";

    @ForeignFactory(EmployeeSmallFactory.class)
    @LazyFetched
    EmployeeSmallDAO manager = null;

    @ForeignFactory(ProjectFactory.class)
    @LazyFetched
    List<ProjectDAO> employeeProjects = new ArrayList<>(0);

    @ForeignFactory(value = EmployeeLogFactory.class, immutable = false)//we need to be mutable since we add new logs here.
    @LazyFetched
    List<EmployeeLogDAO> employeeLogs = new ArrayList<>(0);

    ///////////////////////////////////////////
    // Minimum Methods Set.
    // It should always include the identity accessor(s)!!!
    ///////////////////////////////////////////
    int getId();
    void setId(int id);

    @EntityAccessorHelper(fieldName = "employeeLogs", action = "add")
    void addLog(EmployeeLogDAO employeeLog);
}
