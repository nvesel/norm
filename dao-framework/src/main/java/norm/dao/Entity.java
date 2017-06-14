package norm.dao;

import norm.dao.annotations.EntityAccessorHelper;

public interface Entity {
    //Each entity provides the following service properties and methods:

    String __identity = null;
    long __revision = -1;

    //An unique entity identifier per entity type. Useful when there is no single PK.
    @EntityAccessorHelper(fieldName = "__identity", action = "get")
    String __identity();

    //Change Tracking version. Upon persist, if provided(!!optional!!),
    // it will be compared with the DB before persist operation, if not, the record will be updated without a check.
    @EntityAccessorHelper(fieldName = "__revision", action = "get")
    long __revision();
    @EntityAccessorHelper(fieldName = "__revision", action = "set")
    void __revision(long __revision);
}
