# norm
DAO Layer and ORM-ing without a JPA/JDO framework

There is no doubt (in my mind) that DAO design pattern is beneficial for mid- to large-sized enterprise projects.
Benefits include:
    Separation of expertise and responsibilities
    Interaction via contracts (interfaces) allows parallel development on both sides – business logic and data access/persistence
    Replacing of underlying storage engine does not affect DAO consumers
    Replacing of underlying persistence framework does not affect DAO consumers
    Reusability across projects
    Changes on the storage side should not imply changes on the application side (and vs versa)

Although DAO layer can be implemented with the help of JPA/JDO framework, I believe that this is not necessary.
Modern ORM frameworks offer tremendously convenient tools for accessing of persistent data. Unfortunately, in their strive to be universally usable they introduce thick abstraction, lack of fine control (or at least lack of understanding how to control...) and respectively lowered performance. The abstraction between Application Model and Relational DB Model allows for non-optimal models on either side. In many cases corners are cut or in others the models are overly complex. More often than not, developers would lack expertise on either side. It also makes troubleshooting of performance issues much harder. In many situations the presentation of the data does not need to match the storage model. (Or it does not make sense in one of the worlds)

With this project I’m trying to keep aspects of JPA that are important and avoid some of the typical ORM framework pitfalls.

* Leave the DB model to the DBAs. The RDBMS model should not be modifiable by application developers. Although this seems convenient, at the end it might(will) bite back.
* Supports PATCH-ing of entities. The DAO does not require the full entity supplied. Only fields that have been set will be persisted.
* The actual mapping between relation and object models is defined by the DAO developer. Mapping to entity fields is configured directly in the query. This allows a database developer to "see" the mapping while creating the query.
* Optimized querying. One single query execution can produce an array of entities containing nested entities.
* Complex persistence. Supports merge. If desired a nested entity can be persisted in one transaction. One persist call can trigger more than one update.
* Although (L1 and L2) caching can be implemented, this is left to the consumer. Presumably the production environment is multi-node load balanced application layer with a clustered storage. Caching on a single node is not optimal and synchronization between nodes is imperfect - that is why caching is left to the client. In a RESTful WS environment a reversed proxy is more appropriate.
* Concurrency during persistence is handled on the storage side. Important in a distributed application environment.
* There is no protection against concurrent reads (no L1 cache). Each “get” produces a new instance.
* Caching of read only entities. Some objects such as applications settings that are stored in the DB are either read only or rarely changed. Such can be loaded once and cached by the DAO layer.
* The DAO layer is thread safe. A particular DAO instance is locked only during a persistence operation.
* Transaction handling is done by utilizing Spring AOP.
* The DAO factories produces POJOs defined via Java interfaces.
* The DAO factories could be implemented for different DB engines or mocks and switched via configuration.
* All SQL code is stored in XML configuration files - one per entity type/storage engine.
* DAO Entities can be scoped. A sub-set of the full entity. In some cases it is expensive to produce a large entity every time. That is why entities with sub-set of the full entity properties can be defined.
* The DAO layer provides easy transformers from one scope to another.
* It offers lazy fetched entity fields although in a RESTful environment this should be used carefully.
* The DAO layer is portable. It can be used in any environment from a standalone app to a JEE app.
* Data encryption and decryption could be handled here.
