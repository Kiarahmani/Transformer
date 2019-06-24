CREATE TABLE SUBSCRIBER (
   s_id INT,
   sub_nbr VARCHAR(15),
   bit_1 INT,
   bit_2 INT,
   bit_3 INT,
   bit_4 INT,
   bit_5 INT,
   bit_6 INT,
   bit_7 INT,
   bit_8 INT,
   bit_9 INT,
   bit_10 INT,
   hex_1 INT,
   hex_2 INT,
   hex_3 INT,
   hex_4 INT,
   hex_5 INT,
   hex_6 INT,
   hex_7 INT,
   hex_8 INT,
   hex_9 INT,
   hex_10 INT,
   byte2_1 INT,
   byte2_2 INT,
   byte2_3 INT,
   byte2_4 INT,
   byte2_5 INT,
   byte2_6 INT,
   byte2_7 INT,
   byte2_8 INT,
   byte2_9 INT,
   byte2_10 INT,
   msc_location INT,
   vlr_location INT,
   PRIMARY KEY (s_id)
);


CREATE TABLE ACCESS_INFO (
   s_id INTEGER NOT NULL,
   ai_type INT NOT NULL,
   data1 SMALLINT,
   data2 SMALLINT,
   data3 VARCHAR(3),
   data4 VARCHAR(5),
   PRIMARY KEY(s_id, ai_type),
   FOREIGN KEY (s_id) REFERENCES SUBSCRIBER (s_id)
);


CREATE TABLE SPECIAL_FACILITY (
   s_id INTEGER NOT NULL,
   sf_type INT NOT NULL,
   is_active INT NOT NULL,
   error_cntrl SMALLINT,
   data_a SMALLINT,
   data_b VARCHAR(5),
   PRIMARY KEY (s_id, sf_type),
   FOREIGN KEY (s_id) REFERENCES SUBSCRIBER (s_id)
);


CREATE TABLE CALL_FORWARDING (
   s_id INTEGER NOT NULL,
   sf_type INT NOT NULL,
   start_time INT NOT NULL,
   end_time INT,
   numberx VARCHAR(15),
   PRIMARY KEY (s_id, sf_type, start_time),
   FOREIGN KEY (s_id, sf_type) REFERENCES SPECIAL_FACILITY(s_id, sf_type)
);
