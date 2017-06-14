CREATE TABLE person (
    __created_ts datetime DEFAULT NOW() NOT NULL, -- __created_ts in conjunction with __revision signifies real version
    __revision bigint DEFAULT 0 NOT NULL, -- __revision column is expected to be bigint/long
    id int NOT NULL IDENTITY PRIMARY KEY,
    name varchar(50) NOT NULL,
    parent_person int NULL,
    last_mod_ts datetime NULL,
    FOREIGN KEY(id) REFERENCES person(id)
);
