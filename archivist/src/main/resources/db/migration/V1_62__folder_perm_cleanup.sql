
DELETE FROM folder WHERE str_name='Imports' AND pk_parent=0;
DELETE FROM folder WHERE str_name='Exports' AND pk_parent=0;
DELETE FROM folder WHERE str_name='Date' AND pk_parent=0;
DELETE FROM folder WHERE str_name='Folders' AND pk_parent=0;

DELETE FROM permission WHERE str_name='user' AND str_type='group';
DELETE FROM permission WHERE str_name='export' AND str_type='group';
DELETE FROM permission WHERE str_name='server' AND str_type='internal';

UPDATE permission SET str_name='administrator' WHERE str_name='superuser';
UPDATE permission SET str_description='Can manage users and permissions' WHERE str_name='manager' and str_type='group';

INSERT INTO permission (str_name, str_description, str_type, bool_immutable, bool_user_assignable, bool_obj_assignable)
  VALUES ('developer', 'Can manage and create pipelines', 'group', true, true, true);
