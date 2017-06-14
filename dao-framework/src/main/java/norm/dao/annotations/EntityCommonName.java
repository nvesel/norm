package norm.dao.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//Entities can be scoped and different class names will be used to describe the scopes.
//Via this annotation we assign a common entity name for all of them. Used along with the entity identifier to create a GUID
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EntityCommonName {
    String value();
}
