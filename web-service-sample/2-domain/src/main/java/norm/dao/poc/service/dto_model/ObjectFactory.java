package norm.dao.poc.service.dto_model;

import norm.dao.FactoriesRegistryBean;
import norm.dao.poc.dao_model.department.DepartmentFactory;
import norm.dao.poc.dao_model.employee.EmployeeSmallFactory;
import norm.dao.poc.service.dto_model.department.Department;
import norm.dao.poc.service.dto_model.employee.EmployeeSmall;

//Customize JAXB deserialization. (Just for the demo)
public class ObjectFactory {
    public static EmployeeSmall newEmployeeSmall() {
        return new EmployeeSmall(FactoriesRegistryBean.getEntityFactory(EmployeeSmallFactory.class).blank());
    }

    public static Department newDepartment() {
        return new Department(FactoriesRegistryBean.getEntityFactory(DepartmentFactory.class).blank());
    }
}
