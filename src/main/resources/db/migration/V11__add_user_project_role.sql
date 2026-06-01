-- RF: permitir adicionar um cliente (papel USER) que apenas acompanha a obra.
-- USER passa a ser um papel válido dentro da obra, com permissão somente de
-- visualização (VIEW_PROJECT) por padrão. Atualiza as CHECK constraints das
-- duas tabelas que restringem role_in_project.

ALTER TABLE construction_project_members
    DROP CONSTRAINT construction_project_members_role_in_project_check;
ALTER TABLE construction_project_members
    ADD CONSTRAINT construction_project_members_role_in_project_check
    CHECK (role_in_project IN ('OWNER', 'ARCHITECT', 'ENGINEER', 'FOREMAN', 'USER'));

ALTER TABLE project_role_permissions
    DROP CONSTRAINT project_role_permissions_role_in_project_check;
ALTER TABLE project_role_permissions
    ADD CONSTRAINT project_role_permissions_role_in_project_check
    CHECK (role_in_project IN ('OWNER', 'ARCHITECT', 'ENGINEER', 'FOREMAN', 'USER'));
