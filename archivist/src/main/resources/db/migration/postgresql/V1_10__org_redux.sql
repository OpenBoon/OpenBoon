DROP INDEX permission_name_and_type_uniq_idx;
CREATE UNIQUE INDEX permission_unique_idx ON permission (pk_organization, str_name, str_type);

DROP INDEX jblob_uidx;
CREATE UNIQUE INDEX jblob_uidx ON jblob (pk_organization, str_app, str_feature, str_name);

DROP INDEX folder_unique_siblings_idx;
CREATE UNIQUE INDEX folder_unique_siblings_idx ON folder(pk_organization, pk_parent, str_name);

CREATE INDEX folder_pk_organization_idx ON folder (pk_organization);
CREATE INDEX folder_trash_pk_organization_idx ON folder_trash (pk_organization);
CREATE INDEX users_pk_organization_idx ON users (pk_organization);
CREATE INDEX shared_link_pk_organization_idx ON shared_link (pk_organization);
CREATE INDEX request_link_pk_organization_idx ON request (pk_organization);
CREATE INDEX jblob_pk_organization_idx ON jblob (pk_organization);
CREATE INDEX taxonomy_pk_organization_idx ON taxonomy (pk_organization);
CREATE INDEX dyhi_pk_organization_idx ON dyhi (pk_organization);


