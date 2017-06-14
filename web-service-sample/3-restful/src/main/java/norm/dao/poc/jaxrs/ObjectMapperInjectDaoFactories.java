package norm.dao.poc.jaxrs;

import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import norm.dao.poc.dao_model.department.DepartmentFactory;
import norm.dao.poc.dao_model.employee.EmployeeSmallFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Providers;
import java.io.IOException;

//See ApiConfig
//Help the ObjectMapper with custom entity constructors requiring an entity factory argument
//A way to avoid the usage of FactoriesRegistryBean
public class ObjectMapperInjectDaoFactories implements ContainerRequestFilter {
    private final Logger log = LoggerFactory.getLogger(ObjectMapperInjectDaoFactories.class);

    @Autowired
    private EmployeeSmallFactory employeeFactorySmall;
    @Autowired
    private DepartmentFactory departmentFactory;

    @Context
    private Providers providers;

    private boolean injected = false;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (injected) return;

        ObjectMapper mapper = providers.getContextResolver(ObjectMapper.class, MediaType.APPLICATION_JSON_TYPE).getContext(null);
        final InjectableValues.Std injectableValues = new InjectableValues.Std();

        log.warn("Setting Injectable Value:"+employeeFactorySmall);
        injectableValues.addValue(EmployeeSmallFactory.class, employeeFactorySmall);

        log.warn("Setting Injectable Value:"+departmentFactory);
        injectableValues.addValue(DepartmentFactory.class, departmentFactory);

        mapper.setInjectableValues(injectableValues);

        injected = true;
    }
}
