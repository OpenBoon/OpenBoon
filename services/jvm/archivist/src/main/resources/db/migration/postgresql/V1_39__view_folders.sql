DROP EXTENSION IF EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

--- Add the view-all-folders perm to all orgs
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
        'view-all-folders',
        'User can view all folders',
        'zorroa',
        TRUE,
        TRUE,
        FALSE,
        'zorroa::view-all-folders'
    FROM organization WHERE pk_organization!='00000000-9998-8888-7777-666666666666' ON CONFLICT DO NOTHING;
