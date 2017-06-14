package norm.dao.poc.dao_model.employee;

import norm.dao.EntityFactory;

import java.util.List;

public interface EmployeeSmallFactory extends EntityFactory<EmployeeSmallDAO>
{
    List<EmployeeSmallDAO> getEmployeeSmallAll();
}
