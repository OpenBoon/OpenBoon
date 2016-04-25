/**
 * The user::admin perm was not marked as immutable.
 */
UPDATE user_permission SET bool_immutable=1 WHERE pk_permission=(SELECT pk_permission FROM permission WHERE str_name='admin' AND str_type='user');
