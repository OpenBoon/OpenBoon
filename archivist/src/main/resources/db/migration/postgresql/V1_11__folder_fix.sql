UPDATE zorroa.folder SET json_search='{}',bool_recursive='f' WHERE str_name='Library';
UPDATE zorroa.folder set bool_recursive='f',json_search=NULL WHERE str_name='Users';