CREATE TABLE users (

    id SERIAL PRIMARY KEY,

    name VARCHAR(64) NOT NULL,

    -- REAL should be enough, right?
    latitude REAL NOT NULL,

    longitude REAL NOT NULL -- TODO ask our team lead
);