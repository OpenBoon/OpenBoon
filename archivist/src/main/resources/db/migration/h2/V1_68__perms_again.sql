
INSERT INTO permission (str_name, str_description, str_type, bool_immutable, bool_user_assignable, bool_obj_assignable)
  VALUES ('share', 'Can share restricted assets with any groups or user.', 'group', true, true, false);

INSERT INTO user_permission (pk_permission, pk_user, bool_immutable) VALUES (
  (SELECT pk_permission FROM permission WHERE str_name='share' AND str_type='group'),
  (SELECT pk_user FROM user WHERE str_username='admin'),
  0
);
