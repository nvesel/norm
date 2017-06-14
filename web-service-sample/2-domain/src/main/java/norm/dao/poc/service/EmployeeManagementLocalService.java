package norm.dao.poc.service;

import norm.dao.poc.dao_model.employee.*;
import norm.dao.poc.service.dto_model.employee.EmployeeSmall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;

@Service("employeeManagementLocalService")
@PropertySource("classpath:service.properties")
public class EmployeeManagementLocalService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private EmployeeSmallFactory employeeFactorySmall;
    @Autowired
    private EmployeeLargeFactory employeeFactoryLarge;

    @Value("${msg.employee.does.not.exist}")
    private String employeeDoesNotExist;

    @Transactional(readOnly=true)
    @ValidateOnExecution
    @NotNull
    public EmployeeSmall getEmployeeSmall(@DecimalMin(value = "0") int emplId) {
        log.info("getEmployeeSmall:"+emplId);

        //We can do this if we need to.
        EmployeeSmallDAO employeeSmallDAO = employeeFactoryLarge.getEmployeeLargeById(emplId);

        if (employeeSmallDAO == null) {
            throw new ServiceException(
                    "msg.employee.does.not.exist",
                    employeeDoesNotExist,
                    "emplId",
                    emplId
            );
        }

        //Convert the DAO to the expected EmployeeSmall object
        return new EmployeeSmall(employeeSmallDAO);
    }

    @Transactional(readOnly=false)
    @ValidateOnExecution
    public int addEmployee(@Valid EmployeeSmall employeeSmall) {
        log.info("addEmployee:"+employeeSmall);

        //Convert the input EmployeeSmall into DAO in order to persist it.
        EmployeeSmallDAO employeeSmallDAO = employeeSmall.__getEmployeeSmallDAO();
        employeeSmallDAO = employeeFactorySmall.insert(employeeSmallDAO);

        return employeeSmallDAO.getId();
    }

    @Transactional(readOnly=false)
    @ValidateOnExecution
    public EmployeeSmall updateEmployee(int emplId, @Valid EmployeeSmall employeeSmall) {
        log.info("updateEmployee:"+employeeSmall);
        employeeSmall.setPk(emplId);
        employeeFactorySmall.update(employeeSmall.__getEmployeeSmallDAO());
        return employeeSmall;
    }
}
