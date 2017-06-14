package norm.dao.poc.jaxrs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import norm.dao.poc.jaxrs.error_handling.*;
import norm.dao.poc.jaxrs.resource.EmployeeManagementRemoteService;
import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;
import org.glassfish.jersey.server.validation.ValidationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

public class ApiConfig extends ResourceConfig {

    public ApiConfig() {

        packages(EmployeeManagementRemoteService.class.getPackage().getName()).

        property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true).
        property(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true).

        register(RequestContextFilter.class).

//        register(EmployeeSmallReader.class).

        //Help the ObjectMapper with custom entity constructors requiring an entity factory argument
        register(ObjectMapperInjectDaoFactories.class).

        //Jackson
        //register(JacksonFeature.class).//NO!NO! use JacksonJaxbJsonProvider instead. A nasty bug JERSEY-2722
        register(JacksonJaxbJsonProvider.class).
        register(CustomJacksonObjectMapperProvider.class).
        //Validation
        register(ValidationFeature.class).
        register(ValidationConfigurationContextResolver.class).
        //Exception Mappers
        register(JsonMappingExceptionMapper.class).
        register(UnsupportedMediaTypeMapper.class).
        register(ValidationExceptionMapper.class).
        register(ServiceExceptionMapper.class).
        register(UnhandledExceptionMapper.class).
        //Debug Log. DO NOT USE ON PRODUCTION!!!
        register(new LoggingFilter(java.util.logging.Logger.getLogger(LoggingFilter.class.getName()), true));//TODO: Do not use on production
    }

    @Provider
    public static class CustomJacksonObjectMapperProvider implements ContextResolver<ObjectMapper> {
        private static final Logger log = LoggerFactory.getLogger(CustomJacksonObjectMapperProvider.class);

        final ObjectMapper defaultObjectMapper;

        public CustomJacksonObjectMapperProvider() {
            defaultObjectMapper = new ObjectMapper();

            //mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            //mapper.enable(SerializationFeature.INDENT_OUTPUT);
            //Serialize Only Fields that meet a Custom Criteria: http://www.baeldung.com/jackson-serialize-field-custom-criteria

            //mapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.PUBLIC_ONLY);
            //mapper.setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.PUBLIC_ONLY);
            //mapper.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.PUBLIC_ONLY);
            //mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.NONE);

            //mapper.enableDefaultTyping();

            log.debug("Custom ObjectMapper created");
        }

        @Override
        public ObjectMapper getContext(Class<?> entityClass) {
            log.warn("Get JSON ObjectMapper for "+entityClass);
            return defaultObjectMapper;
        }
    }
}
