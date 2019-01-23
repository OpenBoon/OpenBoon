
---
--- Add a monitor user
---
INSERT INTO users(pk_user, pk_organization, str_username, str_password, str_email, str_firstname, str_lastname, bool_enabled, str_source, pk_permission, pk_folder, bool_reset_pass, str_reset_pass_token, time_reset_pass, json_settings, hmac_key) values
  ('D5DEEB56-0990-4AD8-906C-9569C05D4282', '00000000-9998-8888-7777-666666666666', 'monitor', '$2a$10$KVQTdOAG0m6yX1y4pbcZCepGzkd7rc6xdrOBBNbvCrxoBH4p1yj4K', 'monitor@zorroa.com', 'Ana', 'Monitor', TRUE, 'local', '00000000-FC08-4E4A-AA7A-A183F42C9FA5', '00000000-2395-4E71-9E4C-DACCEEF6AD51', TRUE, NULL, 0, '{}', 'd84090d2842ca87269ed763d5af20fa17cdc4df106a31da6ea7f1f3e4aa5a629');


INSERT INTO permission(pk_organization, pk_permission, str_name, str_description, str_type, bool_immutable, bool_user_assignable, bool_obj_assignable, str_authority) VALUES
  ('00000000-9998-8888-7777-666666666666', 'AB0740C6-B306-4FCF-9512-BB797D58DBDB', 'monitor', 'Monitor user can access metric endpoints', 'zorroa', TRUE, TRUE, FALSE, 'zorroa::monitor');

INSERT INTO user_permission(pk_permission, pk_user, bool_immutable) VALUES
  ('AB0740C6-B306-4FCF-9512-BB797D58DBDB', 'D5DEEB56-0990-4AD8-906C-9569C05D4282', TRUE);
