package norm.dao.poc.jaxrs.error_handling;

import org.glassfish.jersey.server.validation.ValidationConfig;
import org.glassfish.jersey.server.validation.internal.InjectingConstraintValidatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.MessageInterpolator;
import javax.validation.ParameterNameProvider;
import javax.validation.Validation;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.ContextResolver;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Custom configuration of validation. This configuration defines custom:
 * <ul>
 *     <li>ConstraintValidationFactory - so that validators are able to inject Jersey providers/resources.</li>
 *     <li>CustomMessageInterpolator - In case we want to modify messages before returned.</li>
 *     <li>ParameterNameProvider - if method input parameters are invalid, this class returns actual parameter names
 *     instead of the default ones ({@code arg0, arg1, ..})</li>
 * </ul>
 */
public class ValidationConfigurationContextResolver implements ContextResolver<ValidationConfig> {
    private final Logger log = LoggerFactory.getLogger(ValidationConfigurationContextResolver.class);

    @Context
    private ResourceContext resourceContext;

    @Override
    public ValidationConfig getContext(final Class<?> type) {
        return new ValidationConfig()
                .constraintValidatorFactory(resourceContext.getResource(InjectingConstraintValidatorFactory.class))
                .messageInterpolator(new CustomMessageInterpolator(Validation.byDefaultProvider().configure().getDefaultMessageInterpolator()))
                .parameterNameProvider(new CustomParameterNameProvider());
    }

    private class CustomParameterNameProvider implements ParameterNameProvider {
        private final ParameterNameProvider nameProvider;

        public CustomParameterNameProvider() {
            nameProvider = Validation.byDefaultProvider().configure().getDefaultParameterNameProvider();
        }

        @Override
        public List<String> getParameterNames(final Constructor<?> constructor) {
            return nameProvider.getParameterNames(constructor);
        }

        @Override
        //Here we can customize argument names
        public List<String> getParameterNames(final Method method) {
            if ("getEmployeeById".equals(method.getName())) {
                return Collections.singletonList("{emplId}");
            }
            return nameProvider.getParameterNames(method);
        }
    }

    private class CustomMessageInterpolator implements MessageInterpolator {
        private final MessageInterpolator defaultInterpolator;

        public CustomMessageInterpolator(MessageInterpolator interpolator) {
            this.defaultInterpolator = interpolator;
        }

        @Override
        //We can customize error messages
        public String interpolate(String message, Context context) {
            return defaultInterpolator.interpolate(message, context); // customize the message here
        }

        @Override
        public String interpolate(String message, Context context, Locale locale) {
            return defaultInterpolator.interpolate(message, context, locale); // customize the message here
        }
    }
}