


CREATE TABLE user_profiles (
  uid INT NOT NULL,
  name VARCHAR(255) DEFAULT NULL,
  email VARCHAR(255) DEFAULT NULL,
  partitionid INT DEFAULT NULL,
  partitionid2 INT DEFAULT NULL,
  followers INT DEFAULT NULL,
  PRIMARY KEY (uid)
);


CREATE TABLE followers (
  f1 INT NOT NULL REFERENCES user_profiles (uid),
  f2 INT NOT NULL REFERENCES user_profiles (uid),
  PRIMARY KEY (f1,f2)
);

CREATE TABLE follows (
  f1 INT NOT NULL REFERENCES user_profiles (uid),
  f2 INT NOT NULL REFERENCES user_profiles (uid),
  PRIMARY KEY (f1,f2)
);


CREATE TABLE tweets (
  id INT NOT NULL,
  uid INT NOT NULL REFERENCES user_profiles (uid),
  text VARCHAR(140) NOT NULL,
  createdate VARCHAR DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE added_tweets (
  id INT NOT NULL ,
  uid INT NOT NULL REFERENCES user_profiles (uid),
  text VARCHAR(140) NOT NULL,
  createdate VARCHAR DEFAULT NULL,
  PRIMARY KEY (id)
);
