-- The role lists are filled at login on every authenticated request; parallel requests right after a login could both
-- find no entry and both insert one (check-then-insert). Duplicates are removed (lowest id kept) and the database now
-- refuses them; the backend inserts with ON CONFLICT DO NOTHING.
DELETE FROM samply.project_manager_admin_user a USING samply.project_manager_admin_user b
WHERE a.email = b.email AND a.id > b.id;
CREATE UNIQUE INDEX project_manager_admin_user_email_key ON samply.project_manager_admin_user (email);

DELETE FROM samply.central_approval_committee_user a USING samply.central_approval_committee_user b
WHERE a.email = b.email AND a.id > b.id;
CREATE UNIQUE INDEX central_approval_committee_user_email_key ON samply.central_approval_committee_user (email);

DELETE FROM samply.bridgehead_admin_user a USING samply.bridgehead_admin_user b
WHERE a.email = b.email AND a.bridgehead = b.bridgehead AND a.id > b.id;
CREATE UNIQUE INDEX bridgehead_admin_user_email_bridgehead_key ON samply.bridgehead_admin_user (email, bridgehead);
