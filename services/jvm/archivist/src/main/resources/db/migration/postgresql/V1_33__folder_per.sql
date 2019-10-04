

INSERT INTO permission(pk_organization, pk_permission, str_name, str_description, str_type, bool_immutable, bool_user_assignable, bool_obj_assignable, str_authority) VALUES
  ('00000000-9998-8888-7777-666666666666', 'AB0740C6-B306-4FCF-0246-BB797D58DBDB', 'view-all-folders', 'User can view all folders', 'zorroa', TRUE, TRUE, FALSE, 'zorroa::view-all-folders');

--- Add a serial col to make dropping the ACL entries easier.
ALTER TABLE folder_acl ADD COLUMN entry_id SERIAL NOT NULL;

--- A recursive query to walk the Users area and drop zorroa::everyone read
WITH recursive children(pk_folder, pk_parent, str_name) AS (
    SELECT pk_folder, pk_parent, str_name from folder WHERE str_name='Users'
UNION
    SELECT o.pk_folder, o.pk_parent, o.str_name
    FROM folder o, children f
    WHERE o.pk_parent = f.pk_folder
)
DELETE FROM folder_acl WHERE entry_id IN (
SELECT folder_acl.entry_id FROM children, folder_acl, permission
WHERE children.pk_folder = folder_acl.pk_folder AND folder_acl.pk_permission = permission.pk_permission
AND permission.str_authority='zorroa::everyone' AND children.str_name !='Users');

--- Drop the entry_id since we no longer need it.
ALTER TABLE folder_acl DROP COLUMN entry_id;
