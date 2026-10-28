-- Email addresses of the central approval committee, filled at login (like project_manager_admin_user)
CREATE TABLE samply.central_approval_committee_user
(
    id    SERIAL NOT NULL PRIMARY KEY,
    email TEXT   NOT NULL
);
