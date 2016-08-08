ALTER TABLE permission ADD bool_user_assignable BOOLEAN NOT NULL DEFAULT 1;
ALTER TABLE permission ADD bool_obj_assignable BOOLEAN NOT NULL DEFAULT 1;

UPDATE permission SET bool_user_assignable=0,bool_obj_assignable=0 WHERE str_type='internal';
UPDATE permission SET bool_user_assignable=0,bool_obj_assignable=1 WHERE str_type='user';
