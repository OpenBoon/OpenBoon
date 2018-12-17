
ALTER TABLE analyst ADD COLUMN int_free_disk INTEGER NOT NULL DEFAULT 1024;

INSERT INTO permission(pk_organization, pk_permission, str_name, str_description, str_type, bool_immutable, bool_user_assignable, bool_obj_assignable, str_authority) VALUES
('00000000-9998-8888-7777-666666666666', '00000000-FC08-CCCC-CCCC-A183F42C9FA1', 'superadmin', 'Super admin can manage non-organization data', 'zorroa', TRUE, TRUE, FALSE, 'zorroa::superadmin');

INSERT INTO user_permission(pk_permission, pk_user, bool_immutable)
  VALUES ('00000000-FC08-CCCC-CCCC-A183F42C9FA1', '00000000-7B0B-480E-8C36-F06F04AED2F1', TRUE)
