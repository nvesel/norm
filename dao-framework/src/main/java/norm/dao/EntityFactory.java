package norm.dao;

import java.util.Map;

public interface EntityFactory<T extends Entity> {
    T blank();
    IMapper<T> getMapperInstance(SqlResultSetMapper sqlResultSetMapper);
    Map<String, EntityProperty> getEntityFields();

    //Persistence methods.
    //additionalProperties are key-value objects that supply additional contextual parameters to an entity.
    // For example customer context
    T insert(T entity, AdditionalProperty... additionalProperties);
    T merge(T entity, AdditionalProperty... additionalProperties);
    T update(T entity, AdditionalProperty... additionalProperties);
    void delete(T entity, AdditionalProperty... additionalProperties);
}
