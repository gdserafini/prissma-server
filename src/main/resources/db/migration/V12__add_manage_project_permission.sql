-- Fecha o IDOR de GET/PATCH/DELETE /projects/{id}: essas rotas nao tinham
-- checagem nenhuma, entao qualquer usuario autenticado lia, editava e apagava
-- qualquer obra por id.
--
-- A autorizacao passa a usar o mesmo ProjectPermissionService que ja governa
-- membros e permissoes customizadas por obra, e para isso falta uma permissao
-- que represente "gerenciar a obra em si". As demais (MANAGE_STAGES,
-- MANAGE_TASKS...) cobrem o conteudo; nenhuma cobria o cadastro da obra.
--
-- Aditiva: so amplia a CHECK constraint. Nenhuma linha existente e afetada, e
-- obra sem linhas em project_role_permissions continua caindo nos defaults do
-- ProjectRole (OWNER e ENGINEER recebem MANAGE_PROJECT).
ALTER TABLE project_role_permissions
    DROP CONSTRAINT project_role_permissions_permission_check;

ALTER TABLE project_role_permissions
    ADD CONSTRAINT project_role_permissions_permission_check
    CHECK (permission IN ('VIEW_PROJECT', 'MANAGE_PROJECT', 'MANAGE_MEMBERS',
                          'MANAGE_BUDGET', 'MANAGE_STAGES', 'MANAGE_TEAMS',
                          'MANAGE_TASKS', 'MANAGE_ATTACHMENTS'));
