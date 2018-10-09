

CREATE TABLE COUNTRY (
    CO_ID        BIGINT NOT NULL,
    CO_NAME      VARCHAR(64) NOT NULL,
    CO_CODE_2    VARCHAR(2) NOT NULL,
    PRIMARY KEY (CO_ID)
);

CREATE TABLE CUSTOMER (
    C_ID           BIGINT NOT NULL,
    C_ID_STR       VARCHAR(64) UNIQUE NOT NULL,
    C_BASE_AP_ID   BIGINT REFERENCES AIRPORT (AP_ID),
    C_BALANCE      FLOAT NOT NULL,
    C_SATTR00      VARCHAR(32),
    PRIMARY KEY (C_ID)
);




