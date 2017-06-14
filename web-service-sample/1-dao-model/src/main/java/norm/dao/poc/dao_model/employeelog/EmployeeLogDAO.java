package norm.dao.poc.dao_model.employeelog;

import norm.dao.Entity;
import norm.dao.annotations.EntityCommonName;
import norm.dao.annotations.EntityIdentifier;

import java.util.Date;

@EntityCommonName("employeeLog")
public interface EmployeeLogDAO extends Entity
{
    @EntityIdentifier(position = 0)
    int emplId = -1;

    @EntityIdentifier(position = 1)
    Date timeStamp = new Date(System.currentTimeMillis());

    String description = "Test";

    Date getTimeStamp();

    String getDescription();
}
