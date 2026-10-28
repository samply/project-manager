-- The email lists of the roles store addresses in lower case (the login, the configuration and the configuration page
-- may write them differently); entries that become equal are kept once (lowest id)
UPDATE samply.project_manager_admin_user SET email = LOWER(TRIM(email));
DELETE FROM samply.project_manager_admin_user a USING samply.project_manager_admin_user b
WHERE a.email = b.email AND a.id > b.id;

UPDATE samply.central_approval_committee_user SET email = LOWER(TRIM(email));
DELETE FROM samply.central_approval_committee_user a USING samply.central_approval_committee_user b
WHERE a.email = b.email AND a.id > b.id;

UPDATE samply.bridgehead_admin_user SET email = LOWER(TRIM(email));
DELETE FROM samply.bridgehead_admin_user a USING samply.bridgehead_admin_user b
WHERE a.email = b.email AND a.bridgehead = b.bridgehead AND a.id > b.id;
