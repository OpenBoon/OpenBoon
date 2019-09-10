
DROP TABLE pipeline_acl;

-- Don't need this user.
DELETE FROM users WHERE str_username='monitor';

DELETE FROM permission WHERE str_authority='zorroa::share';
DELETE FROM permission WHERE str_authority='zorroa::monitor';

--- zorroa::read, zorroa::write, zorroa::export, zorroa::view-all-folders get renamed.
UPDATE permission SET str_type='assets', str_name='write-all', str_authority='assets::write-all' WHERE str_authority='zorroa::write';
UPDATE permission SET str_type='assets', str_name='read-all', str_authority='assets::read-all' WHERE str_authority='zorroa::read';
UPDATE permission SET str_type='assets', str_name='export-all', str_authority='assets::export-all' WHERE str_authority='zorroa::export';
UPDATE permission SET str_type='folders', str_name='read-all', str_authority='folders::read-all' WHERE str_authority='zorroa::view-all-folders';

--- Add delete-all to control deletes.
INSERT INTO permission(
        pk_organization,
        pk_permission,
        str_name,
        str_description,
        str_type,
        bool_immutable,
        bool_user_assignable,
        bool_obj_assignable,
        str_authority)
    SELECT
        organization.pk_organization,
        uuid_generate_v4(),
        'delete-all',
        'User can delete all assets',
        'asset',
        TRUE,
        TRUE,
        FALSE,
        'assets::delete-all'
    FROM organization WHERE pk_organization!='00000000-9998-8888-7777-666666666666' ON CONFLICT DO NOTHING;
