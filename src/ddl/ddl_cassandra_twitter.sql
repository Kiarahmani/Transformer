CREATE TABLE USERS (
	username VARCHAR(255),
	password VARCHAR(255),
	PRIMARY KEY (username)
);



CREATE TABLE TIMELINE (
	username VARCHAR(255),
	ttime INT,
	tweet_id INT,
	PRIMARY KEY (username, ttime)
);


