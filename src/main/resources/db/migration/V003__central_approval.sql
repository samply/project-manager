-- The project state APPROVAL is renamed CENTRAL_APPROVAL (the local approval of the sites follows it)
UPDATE samply.project SET state = 'CENTRAL_APPROVAL' WHERE state = 'APPROVAL';
