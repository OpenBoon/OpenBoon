
INSERT INTO permission (str_name, str_description, str_type, bool_immutable, bool_user_assignable, bool_obj_assignable)
  VALUES ('readAll', 'User can read all data', 'group', true, true, false);

INSERT INTO permission (str_name, str_description, str_type, bool_immutable, bool_user_assignable, bool_obj_assignable)
  VALUES ('writeAll', 'User can write all data', 'group', true, true, false);


ALTER TABLE permission ADD COLUMN str_authority VARCHAR(128);
UPDATE permission SET str_authority = str_type || '::' || str_name;
ALTER TABLE permission ALTER COLUMN str_authority SET NOT NULL;

