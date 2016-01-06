/**
 *
 */

DROP INDEX permission_str_name_idx;

UPDATE permission SET str_name='superuser' WHERE str_name='group::superuser';
UPDATE permission SET str_name='manager' WHERE str_name='group::manager';
UPDATE permission SET str_name='user' WHERE str_name='group::user';
UPDATE permission SET str_name='export' WHERE str_name='group::export';
UPDATE permission SET str_name='admin' WHERE str_name='user::admin';

CREATE UNIQUE INDEX permission_name_and_type_uniq_idx ON permission (str_name, str_type);
