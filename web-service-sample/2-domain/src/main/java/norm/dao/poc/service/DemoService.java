package norm.dao.poc.service;

import norm.dao.poc.dao_model.department.DepartmentFactory;
import norm.dao.poc.dao_model.employee.EmployeeSmallFactory;
import norm.dao.poc.service.dto_model.department.Department;
import norm.dao.poc.service.dto_model.employee.EmployeeSmall;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.transaction.annotation.Transactional;

@PropertySource("classpath:service.properties")
public class DemoService {
    private static final Logger log = Logger.getLogger(DemoService.class);

    @Autowired
    private static EmployeeSmallFactory employeeFactorySmall;
    @Autowired
    private static DepartmentFactory departmentFactory;

    //Load test
    static public void main(String[] args) throws Exception
    {
        ApplicationContext ctx = new ClassPathXmlApplicationContext(new String[]{"spring-service-context.xml"});
        DemoService demoService = ctx.getBean(DemoService.class);
        employeeFactorySmall = ctx.getBean(EmployeeSmallFactory.class);
        departmentFactory = ctx.getBean(DepartmentFactory.class);

        // Starts 5 threads every 30 milliseconds
        // Each thread creates a new employee and not new persons (personal info is merged into one Person).
        // Then it spawns a new thread that modifies the title.
        // This way we load test and check whether there are issues due to concurrency.
        // It was tested successfully with -Xmx32m

        demoService.go();

        /**
         * Verify results:

         select count(*) from [dao].[Employee] e where e.start_date is null -- must be 50000
         select count(*) from [dao].[Person] p where p.SSN like '123-123-123%' -- must be 5

         -- Nothing should be returned by the following:
         select * from [dao].[Employee] e
         left join [dao].[Person] p
         on p.id = e.person_id
         where e.start_date is null -- the new records don't have start data
         and (p.id is null
         or p.name != 'Name '+cast(e.department_id as varchar(255))
         or e.title != 'Title '+cast(e.department_id as varchar(255))+', origTid:'+cast(e.department_id as varchar(255))+', emplId:'+cast(e.id as varchar(255)))
         **/
    }

    private void go() throws Exception
    {
        ConcurrentInsert r1 = new ConcurrentInsert(1, this);
        ConcurrentInsert r2 = new ConcurrentInsert(2, this);
        ConcurrentInsert r3 = new ConcurrentInsert(3, this);
        ConcurrentInsert r4 = new ConcurrentInsert(4, this);
        ConcurrentInsert r5 = new ConcurrentInsert(5, this);

        int count = 0;
        while (count < 10000) {
            (new Thread(r1)).start();
            (new Thread(r2)).start();
            (new Thread(r3)).start();
            (new Thread(r4)).start();
            (new Thread(r5)).start();

            count++;

            if (count <= 1)
                Thread.sleep(1000);//The first time there are not records to be merged (and locked) in the process, so duplicated Persons are possible
            else
                Thread.sleep(30);
        }
    }

    static class ConcurrentInsert implements Runnable {
        private int threadId;
        private DemoService demoService;

        protected ConcurrentInsert(int threadId, DemoService demoService) {
            this.threadId = threadId;
            this.demoService = demoService;
        }

        public void run() {
            Department department = new Department(demoService.departmentFactory);
            department.setPk(threadId);

            EmployeeSmall employeeSmall = new EmployeeSmall(demoService.employeeFactorySmall);
            employeeSmall.setDepartment(department);
            employeeSmall.setName("Name "+ threadId);
            employeeSmall.setSsn("123-123-123"+ threadId);
            employeeSmall.setTitle("Title "+ threadId);

            log.info("Before:"+employeeSmall);
            employeeSmall = demoService.addEmployee(employeeSmall, threadId);
            log.info("After Insert:"+employeeSmall);

            (new Thread(new ConcurrentUpdate(threadId, demoService, employeeSmall))).start();
        }
    }

    @Transactional(readOnly=false)
    public EmployeeSmall addEmployee(EmployeeSmall employeeSmall, int threadId) {
        try {
            employeeFactorySmall.insert(employeeSmall.__getEmployeeSmallDAO());

            //Initiate an update before it is even committed
            //(new Thread(new ConcurrentUpdate(threadId*10, this, employeeSmall))).start(); //This will fail because transaction synchronisation for this one is not active (Spring internals)

        } catch (Throwable e) {
            System.exit(1);
        }
        return employeeSmall;
    }

    static class ConcurrentUpdate implements Runnable {
        private int threadId;
        private DemoService demoService;
        private EmployeeSmall employeeSmall;

        protected ConcurrentUpdate(int threadId, DemoService demoService, EmployeeSmall employeeSmall) {
            this.threadId = threadId;
            this.demoService = demoService;
            this.employeeSmall = employeeSmall;
        }

        public void run() {
            demoService.updateEmployee(employeeSmall, threadId);
            log.info("After Update:"+employeeSmall);
        }

    }

    @Transactional(readOnly=false)
    public void updateEmployee(EmployeeSmall employeeSmall, int threadId) {
        try {
            employeeSmall.setTitle(employeeSmall.getTitle()+", origTid:"+threadId+", emplId:"+employeeSmall.getPk());
            employeeFactorySmall.update(employeeSmall.__getEmployeeSmallDAO());
        } catch (Throwable e) {
            System.exit(1);
        }
    }


}
