
INSERT INTO user_permission (pk_permission, pk_user, bool_immutable) VALUES (
  (SELECT pk_permission FROM permission WHERE str_name='developer' AND str_type='group'),
  (SELECT pk_user FROM user WHERE str_username='admin'),
  0
);
