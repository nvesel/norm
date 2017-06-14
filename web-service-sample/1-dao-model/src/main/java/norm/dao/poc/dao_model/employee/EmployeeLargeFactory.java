package norm.dao.poc.dao_model.employee;

import norm.dao.EntityFactory;

import java.util.List;

public interface EmployeeLargeFactory extends EntityFactory<EmployeeLargeDAO>
{
    EmployeeLargeDAO getEmployeeLargeById(int emplId);

    List<EmployeeLargeDAO> getEmployeeLargeWithPreFetch();

    EmployeeLargeDAO getEmployeeLargeWithSomePrefetchedData(int emplId);

    EmployeeSmallDAO getEmployeeManager(int emplId);

    EmployeeLargeDAO transformEmployeeSmall(final EmployeeSmallDAO employeeSmall);
}
