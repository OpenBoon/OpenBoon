DELETE FROM folder WHERE str_name LIKE '%★%' AND pk_parent != 0;
DELETE FROM folder WHERE str_name LIKE '%★%' AND pk_parent = 0;
