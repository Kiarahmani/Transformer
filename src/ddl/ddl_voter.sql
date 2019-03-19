CREATE TABLE CONTESTANTS(
  contestant_number SMALLINT     NOT NULL, 
  contestant_name   VARCHAR(50) NOT NULL, 
  PRIMARY KEY(contestant_number)
);

CREATE TABLE AREA_CODE_STATE(
  area_code SMALLINT   NOT NULL, 
  state     VARCHAR(2) NOT NULL, 
  PRIMARY KEY(area_code)
);

CREATE TABLE VOTES(
  vote_id            BIGINT     NOT NULL, 
  phone_number       BIGINT     NOT NULL, 
  state              VARCHAR(2) NOT NULL, 
  contestant_number  SMALLINT    NOT NULL REFERENCES CONTESTANTS (contestant_number), 
  created            BIGINT   NOT NULL,
  PRIMARY KEY(vote_id)
);