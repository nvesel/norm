package norm.dao.annotations;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface  ForeignFactory {

    //For a property that contains a nested entity - which is the factory
    Class value();

    //Instruct the Persist procedure not to persist a nested entity. Makes the nested entity immutable.
    boolean immutable() default true;

}
