CREATE TABLE ipblocks (
  ipb_id BIGINT NOT NULL,
  ipb_address VARCHAR(1024) NOT NULL,
  ipb_user BIGINT NOT NULL,
  ipb_by BIGINT NOT NULL,
  ipb_by_text VARCHAR(255) NOT NULL,
  ipb_reason VARCHAR(1024) NOT NULL,
  ipb_timestamp VARCHAR(14) NOT NULL,
  ipb_auto BIGINT NOT NULL,
  ipb_anon_only BIGINT NOT NULL,
  ipb_create_account BIGINT NOT NULL ,
  ipb_enable_autoblock BIGINT NOT NULL ,
  ipb_expiry VARCHAR(14) NOT NULL,
  ipb_range_start VARCHAR(1024) NOT NULL,
  ipb_range_end VARCHAR(1024) NOT NULL,
  ipb_deleted BIGINT NOT NULL ,
  ipb_block_email BIGINT NOT NULL ,
  ipb_allow_usertalk BIGINT NOT NULL ,
  PRIMARY KEY (ipb_id)
);


CREATE TABLE useracct (
  user_id BIGINT NOT NULL,
  user_name VARCHAR(255) NOT NULL,
  user_real_name VARCHAR(255) NOT NULL,
  user_password VARCHAR(1024) NOT NULL,
  user_newpassword VARCHAR(1024) NOT NULL,
  user_newpass_time VARCHAR(14) DEFAULT NULL,
  user_email VARCHAR(1024) NOT NULL,
  user_options VARCHAR(1024) NOT NULL,
  user_touched VARCHAR(14) NOT NULL,
  user_token VARCHAR(32) NOT NULL,
  user_email_authenticated VARCHAR(14) DEFAULT NULL,
  user_email_token VARCHAR(32) DEFAULT NULL,
  user_email_token_expires VARCHAR(14) DEFAULT NULL,
  user_registration VARCHAR(14) DEFAULT NULL,
  user_editcount BIGINT DEFAULT NULL,
  PRIMARY KEY (user_id)
);

CREATE TABLE logging (
  log_id BIGINT NOT NULL,
  log_type VARCHAR(32) NOT NULL,
  log_action VARCHAR(32) NOT NULL,
  log_timestamp VARCHAR(14) NOT NULL,
  log_user BIGINT NOT NULL,
  log_namespace BIGINT NOT NULL,
  log_title VARCHAR(255) NOT NULL,
  log_comment VARCHAR(255) NOT NULL,
  log_params VARCHAR(1024) NOT NULL,
  log_deleted BIGINT NOT NULL,
  log_user_text VARCHAR(255) NOT NULL,
  log_page BIGINT DEFAULT NULL,
  PRIMARY KEY (log_id)
);


CREATE TABLE page (
  page_id BIGINT NOT NULL,
  page_namespace BIGINT NOT NULL,
  page_title VARCHAR(255) NOT NULL,
  page_restrictions VARCHAR(1024) NOT NULL,
  page_counter BIGINT NOT NULL,
  page_is_redirect BIGINT NOT NULL,
  page_is_new BIGINT NOT NULL,
  page_random BIGINT NOT NULL,
  page_touched VARCHAR(14) NOT NULL,
  page_latest BIGINT NOT NULL,
  page_len BIGINT NOT NULL,
  PRIMARY KEY (page_id)
);



CREATE TABLE page_backup (
  page_id BIGINT NOT NULL,
  page_namespace BIGINT NOT NULL,
  page_title VARCHAR(255) NOT NULL,
  page_restrictions VARCHAR(1024) NOT NULL,
  page_counter BIGINT NOT NULL,
  page_is_redirect BIGINT NOT NULL,
  page_is_new BIGINT NOT NULL,
  page_random BIGINT NOT NULL,
  page_touched VARCHAR(14) NOT NULL,
  page_latest BIGINT NOT NULL,
  page_len BIGINT NOT NULL,
  PRIMARY KEY (page_id)
);



CREATE TABLE page_restrictions (
  pr_page BIGINT NOT NULL,
  pr_type VARCHAR(60) NOT NULL,
  pr_level VARCHAR(60) NOT NULL,
  pr_cascade BIGINT NOT NULL,
  pr_user BIGINT DEFAULT NULL,
  pr_expiry VARCHAR(14) DEFAULT NULL,
  pr_id BIGINT NOT NULL,
  PRIMARY KEY (pr_id)
);


CREATE TABLE recentchanges (
  rc_id BIGINT NOT NULL,
  rc_timestamp VARCHAR(14) NOT NULL,
  rc_cur_time VARCHAR(14) NOT NULL,
  rc_user BIGINT NOT NULL,
  rc_user_text VARCHAR(255) NOT NULL,
  rc_namespace BIGINT NOT NULL,
  rc_title VARCHAR(255) NOT NULL,
  rc_comment VARCHAR(255) NOT NULL,
  rc_minor BIGINT NOT NULL,
  rc_bot BIGINT NOT NULL,
  rc_new BIGINT NOT NULL,
  rc_cur_id BIGINT NOT NULL,
  rc_this_oldid BIGINT NOT NULL,
  rc_last_oldid BIGINT NOT NULL,
  rc_type BIGINT NOT NULL,
  rc_moved_to_ns BIGINT NOT NULL,
  rc_moved_to_title VARCHAR(255) NOT NULL,
  rc_patrolled BIGINT NOT NULL,
  rc_ip VARCHAR(40) NOT NULL,
  rc_old_len BIGINT DEFAULT NULL,
  rc_new_len BIGINT DEFAULT NULL,
  rc_deleted BIGINT NOT NULL,
  rc_logid BIGINT NOT NULL,
  rc_log_type VARCHAR(255) DEFAULT NULL,
  rc_log_action VARCHAR(255) DEFAULT NULL,
  rc_params VARCHAR(1024),
  PRIMARY KEY (rc_id)
);


CREATE TABLE revision (
  rev_id BIGINT NOT NULL,
  rev_page BIGINT NOT NULL,
  rev_text_id BIGINT NOT NULL,
  rev_comment VARCHAR(1024) NOT NULL,
  rev_user BIGINT NOT NULL,
  rev_user_text VARCHAR(255) NOT NULL,
  rev_timestamp VARCHAR(14) NOT NULL,
  rev_minor_edit BIGINT NOT NULL,
  rev_deleted BIGINT NOT NULL,
  rev_len BIGINT,
  rev_pcarenct_id BIGINT,
  PRIMARY KEY (rev_id)
);


CREATE TABLE text (
  old_id BIGINT NOT NULL,
  old_text VARCHAR(1000) NOT NULL,
  old_flags VARCHAR(1024) NOT NULL,
  old_page BIGINT DEFAULT NULL,
  PRIMARY KEY (old_id)
);

CREATE TABLE user_groups (
  ug_user BIGINT NOT NULL,
  ug_group VARCHAR(16) NOT NULL,
  PRIMARY KEY (ug_user,ug_group)
);

CREATE TABLE value_backup (
  table_name VARCHAR(255),
  maxid BIGINT,
  PRIMARY KEY (table_name,maxid)
);

CREATE TABLE watchlist (
  wl_user BIGINT NOT NULL,
  wl_namespace BIGINT NOT NULL,
  wl_title VARCHAR(255) NOT NULL,
  wl_notificationtimestamp VARCHAR(14) DEFAULT NULL,
  PRIMARY KEY (wl_user,wl_namespace,wl_title)
);