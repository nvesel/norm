package norm.dao.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EntityAccessorHelper {

    //In some cases the accessor method does not obey the standard.
    // then we use this to annotate which field and what action

    String fieldName();
    String action();
}
