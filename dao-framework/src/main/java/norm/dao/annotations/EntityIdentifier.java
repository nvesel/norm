package norm.dao.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//Signifies whether a field is an Entity Identifier.
//If more than one field is annotated the compound identity will be like this: id1.id2
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface EntityIdentifier {
    //When an entity identity is comprised from multiple fields, provide the position.
    int position() default 0;
}
