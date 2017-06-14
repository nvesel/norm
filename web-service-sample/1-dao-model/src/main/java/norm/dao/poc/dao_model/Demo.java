package norm.dao.poc.dao_model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import norm.dao.exceptions.DaoRuntimeException;
import norm.dao.poc.dao_model.department.DepartmentDAO;
import norm.dao.poc.dao_model.department.DepartmentFactory;
import norm.dao.poc.dao_model.employee.*;
import norm.dao.poc.dao_model.employeelog.EmployeeLogDAO;
import norm.dao.poc.dao_model.employeelog.EmployeeLogFactory;
import norm.dao.poc.dao_model.project.ProjectDAO;
import norm.dao.poc.dao_model.project.ProjectFactory;
import org.apache.commons.lang3.Validate;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

public class Demo {
    private static final Logger log = Logger.getLogger(Demo.class);

    @Autowired
    private EmployeeSmallFactory employeeFactorySmall;
    @Autowired
    private EmployeeLargeFactory employeeFactoryLarge;
    @Autowired
    private EmployeeLogFactory employeeLogFactory;
    @Autowired
    private ProjectFactory projectFactory;
    @Autowired
    private DepartmentFactory departmentFactory;

    //Jackson JSON serializer
    private static final ObjectMapper mapper = new ObjectMapper();

    static public void main(String[] args) throws Exception {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        //mapper.enable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);

        ApplicationContext ctx = new ClassPathXmlApplicationContext(new String[] {"spring-dao-context.xml"});

        //Start the main thread
        final Demo demo = ctx.getBean(Demo.class);

        System.out.println("Init complete.\n");

        String useage = "First review web-service-sample\\1-dao-model\\src\\main\\java\\norm\\dao\\poc\\dao_model\\Demo.java\n" +
                "Usage:\n"
                + "help - to display this\n"
                + "exit - to Exit\n"
                + "1 - for PATCH demonstration\n"
                + "2 - for another PATCH demonstration\n"
                + "3 - Create two new Employees with same Personal info\n"
                + "4 - A \"Huge\" list of SmallEmployeeDAOs\n"
                + "5 - Entity Scope Transformations\n"
                + "6 - Lazy fetch fields\n"
                + "7 - Pre-fetch Lazy fetched fields\n"
                + "8 - Set NULL\n"
                + "9 - Concurrency\n"
                + "10 - Atomic operations\n"
                + "11 - Multi-level nesting with single query\n"
                + "12 - Are two entities equal\n";
        System.out.print(useage);

        boolean stop_this = false;
        while (!stop_this) {
            InputStreamReader input = new InputStreamReader(System.in);
            BufferedReader reader = new BufferedReader(input);
            String line = reader.readLine();
            line = line.trim();
            if ("gc".equals(line))
                System.gc();
            else if ("help".equals(line)) {
                System.out.print(useage);
            }
            else if ("exit".equals(line)) {
                stop_this = true;
            }
            else {
                line = line.trim();
                String[] commands = line.split(", ?");
                for (String command : commands) {
                    if (command.length() == 0) continue;
                    System.out.println("Executing:"+command);

                    switch (command) {
                        case "1":
                            demo.patch_person_info();
                            System.out.println("After commit: "+lastEmployeeLargeDAO);
                            break;
                        case "2":
                            demo.move_employee_to_another_department(4, 1);
                            break;
                        case "3":
                            demo.create_new_employeeS();
                            System.out.println("After commit: "+lastEmployeeLargeDAO);
                            break;
                        case "4":
                            demo.display_all_employees();
                            break;
                        case "5":
                            demo.get_employee_manager_and_modify_title(2);
                            System.out.println("After commit: "+lastEmployeeSmallDAO);
                            break;
                        case "6":
                            demo.lazy_fetched_field(2);
                            break;
                        case "7":
                            demo.preload_efficiently_lazy_fetched_fields();
                            break;
                        case "8":
                            demo.make_employee_bossless(2);
                            break;
                        case "9":
                            System.out.println("Start two concurrent threads manipulating the same entities and watch one fail.");
                            Runnable r = new Runnable() {
                                @Override
                                public void run() {
                                    demo.multi_entity_atomic_operation(false);
                                }
                            };
                            Thread t1 = new Thread(r, "thr1");
                            Thread t2 = new Thread(r, "thr2");
                            t1.start();
                            t2.start();
                            t1.join();
                            t2.join();
                            break;
                        case "10":
                            System.out.println("Throw an exception and rollback all changes");
                            try {
                                demo.multi_entity_atomic_operation(true/*trigger an exception*/);
                            } catch (DaoRuntimeException ignore) {}
                            break;
                        case "11":
                            demo.multilevel_nesting_with_single_query();
                            break;
                        case "12":
                            demo.are_two_instances_equal();
                            break;
                        default:
                            System.out.println("Unknown command:"+command);
                    }
                }
            }
            try {Thread.sleep(10);} catch (InterruptedException ignore) {}
        }
    }

    private static EmployeeSmallDAO lastEmployeeSmallDAO;
    private static EmployeeLargeDAO lastEmployeeLargeDAO;

    @Transactional
    public void patch_person_info() {
        System.out.println("--patch_person_info--------------------------------------------------------------------");
        System.out.println("PATCH Update a Person's Name. Patching does not require the full Entity");

        EmployeeLargeDAO employee = employeeFactoryLarge.blank();
        employee.setId(1);
        employee.setName("New Name");
        employeeFactoryLarge.update(employee);

        lastEmployeeLargeDAO = employee;

        //select * from [dao].[Person] where id = 1
    }

    @Transactional
    public void create_new_employeeS() {
        System.out.println("--create_new_employee-------------------------------------------------------------------");
        System.out.println("Create a new employee, then create a new one but with same personal info. " +
                "The result should be 2 new records in table Employee and 1 new record in Person");

        EmployeeLargeDAO employee1 = employeeFactoryLarge.blank();
        employee1.setName("New Guy");
        employee1.setSsn("123-123-1234");

        DepartmentDAO department = departmentFactory.blank();
        department.setId(1);
        //try without setting a department. It will fail because department_id is not nullable.
        employee1.setDepartment(department);

        employeeFactoryLarge.insert(employee1);

        log.info("employee1="+employee1);

        //New employee but same person as the previous one
        //It will not create a new Person, but it will create a new employee
        EmployeeLargeDAO employee2 = employeeFactoryLarge.blank();
        employee2.setName("New Guy");
        employee2.setSsn("123-123-1234");

        department = departmentFactory.blank();
        department.setId(2);
        employee2.setDepartment(department);

        employeeFactoryLarge.insert(employee2);

        lastEmployeeLargeDAO = employee2;

        //select * from [dao].[Employee] where person_id = 7
        //select * from [dao].[Person] where id = 7
    }

    @Transactional
    public void move_employee_to_another_department(int emplId, int newDepId) {
        System.out.println("--move_employee_to_another_department----------------------------------------------------");
        System.out.println("Another PATCH Update example. Move an Employee into a different department.");

        EmployeeLargeDAO employee = employeeFactoryLarge.blank();
        employee.setId(emplId);

        DepartmentDAO department = departmentFactory.blank();
        department.setId(newDepId);

        //Demonstration - The name will not be persisted since property "department" is annotated as immutable!
        department.setName("Blah Blah");//

        employee.setDepartment(department);
        //We did not edit the existing department (it is immutable). We replaced the current department with a new one.

        //employee.__revision(0); //Optional. Supply if concurrent edits are expected. Provided during GET.

        employeeFactoryLarge.update(employee);

        lastEmployeeLargeDAO = employee;

        //select * from [dao].[Employee] where id = 4
    }

    @Transactional(readOnly=true)
    public void display_all_employees() {
        System.out.println("--display_all_employees------------------------------------------------------------------");
        System.out.println("Create a (huge) list of \"Small\" Employee entities.");

        List<EmployeeSmallDAO> employees = employeeFactorySmall.getEmployeeSmallAll();
        for (int i = 0; i < employees.size(); i++) {
            log.info("getEmployeeSmallAll["+i+"]"+employees.get(i));
        }

        lastEmployeeSmallDAO = employees.get(0);
    }

    @Transactional
    public void get_employee_manager_and_modify_title(int emplId) {
        System.out.println("--get_employee_manager_and_modify_title--------------------------------------------------");
        System.out.println("A demonstration how we get a \"Small\" Employee entity, " +
                "then seamlessly transform it to \"Large\" Employee and persist the changes.");

        //Get employee Manager (which is a SmallEmployeeDAO)
        EmployeeSmallDAO manager = employeeFactoryLarge.getEmployeeManager(emplId);
        manager.setTitle("New TITLE!");

        //Convert to EmployeeLarge to see more details.
        //Note: uncommitted changes are not touched!!
        EmployeeLargeDAO employeeLarge = employeeFactoryLarge.transformEmployeeSmall(manager);
        log.info("employeeLarge:" + employeeLarge);

        Validate.isTrue(employeeLarge.getTitle().equals("New TITLE!"));//The change is not overwritten by the transformation.

        employeeLarge.setTitle("New TITLE 2!");//Now overwrite

        //Convert back to EmployeeSmall to demonstrate that the underlying data is not touched and truncated.
        //It still contains the "Large" attributes, but EmployeeSmall interface allows the developer
        // to see only what is defined there
        EmployeeSmallDAO employeeSmall = employeeLarge;
        log.info("employeeSmall:" + employeeSmall);

        Validate.isTrue(employeeSmall.getTitle().equals("New TITLE 2!"));

        //Persist the changes (Manager's title is now "New TITLE 2!")
        employeeFactorySmall.update(employeeSmall);

        lastEmployeeSmallDAO = employeeSmall;

        //select * from [dao].[Employee] where id = 2
    }

    @Transactional(readOnly=true)
    public void lazy_fetched_field(int emplId) {
        System.out.println("--lazy_fetched_field----------------------------------------------------------------------");
        System.out.println("Lazy fetch data demo. By serializing an object we ensure that all lazy" +
                " fetched fields are fetched. JSON serialization will trigger loading of all lazy fetched fields.");

        EmployeeLargeDAO employeeLarge = employeeFactoryLarge.getEmployeeLargeById(emplId);
        //the JSON serialization will trigger loading of all lazy fetched fields
        try {
            log.info("employeeLarge before Lazy fetched fields:" + employeeLarge);
            log.info("employeeLarge Lazily populated:" + mapper.writeValueAsString(employeeLarge));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        lastEmployeeLargeDAO = employeeLarge;
    }

    @Transactional(readOnly=true)
    public void preload_efficiently_lazy_fetched_fields() {
        System.out.println("--preload_efficiently_lazy_fetched_fields-------------------------------------------------");
        System.out.println("When a developer knows that lazy fetched fields will be accessed," +
                " and the \"finder\" produces a list of entities, it is much better to pre-fetch those lazy" +
                " fetched fields in bulk instead of one by one. There shouldn't be info logs stating 'Lazy fetch...'");

        List<EmployeeLargeDAO> EMPLOYEES = employeeFactoryLarge.getEmployeeLargeWithPreFetch();

        log.info(EMPLOYEES);

        //A very good test whether the factory has fetched all entity fields. If it hasn't an
        // UnsupportedOperationException will the thrown.
        try {
            log.warn("Pre-fetched EmployeeLarge[]:" + mapper.writeValueAsString(EMPLOYEES));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        lastEmployeeLargeDAO = EMPLOYEES.get(0);
    }

    @Transactional() // this is what makes the following operations atomic
    public void multi_entity_atomic_operation(final boolean triggerAnError) {
        System.out.println("--multi_entity_atomic_operation(triggerAnError:"+triggerAnError+")-----------------------");
        System.out.println("Multiple operations on different entities all under the same transaction" +
                " that will be rolled back upon exception");

        final EmployeeSmallDAO employeeSmall = employeeFactoryLarge.getEmployeeLargeById(2);
        //Modify some fields
        employeeSmall.setName("Modified Name by "+Thread.currentThread().getName());
        employeeSmall.setTitle("Modified Title by "+Thread.currentThread().getName());

        //Assign employee to a project and log the event.
        final ProjectDAO project = projectFactory.getProjectById(2);
        project.assignEmployee(employeeSmall);

        //We must log the change
        final EmployeeLogDAO employeeLog =
                employeeLogFactory.newEmployeeLog("Employee added to project by "+Thread.currentThread().getName());
        employeeSmall.addLog(employeeLog);

        log.info("EmployeeSmall Before persist:"+employeeSmall);
        log.info("Project Before persist:"+project);

        employeeFactorySmall.update(employeeSmall);
        projectFactory.update(project);

        log.info("EmployeeSmall After persist before commit:"+employeeSmall);
        log.info("Project After persist before commit:"+project);

        try {
            //If an error occur on any of the steps, everything will be rolled back to where it was
            if (triggerAnError) {
                //(...the Spring Framework’s transaction infrastructure code only marks
                // a transaction for rollback in the case of runtime, unchecked exceptions...)
                throw new DaoRuntimeException("Rollback...");
            }
            lastEmployeeSmallDAO = employeeSmall;
        }
        finally {
            // Show the entity after a rollback (should be same as the one before "persist")
            //Also demonstrates that foreign threads are blocked until the transaction is done (commit or rollback)
            (new Thread("TransDone") {
                public void run() {
                    log.info("EmployeeSmall After "+((triggerAnError)?"error":"successful persist")+":" + employeeSmall);
                }
            }).start();
        }
    }

    @Transactional()
    public void make_employee_bossless(int emplId) {
        System.out.println("--make_employee_bossless-----------------------------------------------------------------");
        System.out.println("Demonstrates how can set NULLs");

        //fetch
        EmployeeLargeDAO employeeLarge = employeeFactoryLarge.blank();
        employeeLarge.setId(emplId);
        //set null
        employeeLarge.setManager(null);//CEO? or Anarchy?
        //persist
        employeeFactoryLarge.update(employeeLarge);

        lastEmployeeLargeDAO = employeeLarge;

        //select * from [dao].[Employee] where id = 2
    }

    @Transactional
    public void multilevel_nesting_with_single_query() {
        System.out.println("--multilevel_nesting_with_single_query---------------------------------------------------");
        System.out.println("Multi-level nested entities fetched with a single query.");

        EmployeeLargeDAO employeeLarge = employeeFactoryLarge.getEmployeeLargeWithSomePrefetchedData(2);

        log.debug("employeeLarge:"+employeeLarge);

        lastEmployeeLargeDAO = employeeLarge;
    }

    @Transactional
    public void are_two_instances_equal() {
        System.out.println("--are_two_instances_equal---------------------------------------------------------------");
        System.out.println("Check whether two different instances of an Employee are same." +
                " The framework checks the underlying data hashmap");

        EmployeeLargeDAO employee1 = employeeFactoryLarge.getEmployeeLargeWithSomePrefetchedData(2);
        EmployeeLargeDAO employee2 = employeeFactoryLarge.getEmployeeLargeWithSomePrefetchedData(2);

        log.info("employee1:"+employee1);
        log.info("employee2:"+employee2);

        log.info("ARE THEY EQUAL:"+employee1.equals(employee2));//true

        EmployeeLargeDAO employee3 = employeeFactoryLarge.getEmployeeLargeById(2);

        log.info("employee2:"+employee2);
        log.info("employee3:"+employee3);

        log.info("ARE THEY EQUAL:"+employee2.equals(employee3));//false

        log.info("ARE THEY THE SAME ENTITY:"+employee2.__identity().equals(employee3.__identity()));//true
    }
}

