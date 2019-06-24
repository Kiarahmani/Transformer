CREATE TABLE ACCOUNTS (
    custid      BIGINT      NOT NULL,
    name        VARCHAR(64) NOT NULL,
    PRIMARY KEY (custid)
);
CREATE INDEX IDX_ACCOUNTS_NAME ON ACCOUNTS (name);    

CREATE TABLE SAVINGS (
    custid      BIGINT      NOT NULL,
    bal         BIGINT       NOT NULL,
    PRIMARY KEY (custid),
    FOREIGN KEY (custid) REFERENCES ACCOUNTS (custid)
);

CREATE TABLE CHECKING (
    custid      BIGINT      NOT NULL,
    bal         BIGINT       NOT NULL,
    CONSTRAINT pk_checking PRIMARY KEY (custid),
    FOREIGN KEY (custid) REFERENCES ACCOUNTS (custid)
);