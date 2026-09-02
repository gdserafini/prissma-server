-- Escopa as obras por workspace. Ordem estrita:
--   0. trava: obra órfã (sem membro OWNER) aborta a migração — decisão manual
--   1. coluna aditiva + índice
--   2. backfill: workspace primário por dono de obra + memberships + UPDATE
--   3. NOT NULL
--   4. título único POR TENANT (era global)

-- 0. Trava: nenhuma obra pode ficar sem dono no backfill. Falha alto e lista.
DO $$
DECLARE
    orphans TEXT;
BEGIN
    SELECT string_agg(p.id || ' (' || p.title || ')', ', ' ORDER BY p.id)
      INTO orphans
      FROM construction_projects p
     WHERE NOT EXISTS (
               SELECT 1 FROM construction_project_members m
                WHERE m.construction_project_id = p.id
                  AND m.role_in_project = 'OWNER');
    IF orphans IS NOT NULL THEN
        RAISE EXCEPTION 'Backfill abortado: obras sem membro OWNER precisam de decisao manual: %', orphans;
    END IF;
END $$;

-- 1. Coluna aditiva.
ALTER TABLE construction_projects ADD COLUMN workspace_id BIGINT REFERENCES workspaces(id);
CREATE INDEX idx_construction_projects_workspace ON construction_projects (workspace_id);

-- 2a. Dono de cada obra = membro OWNER mais antigo (joined_at, id).
--     Cria o workspace primário de cada dono distinto, se ainda não existir.
INSERT INTO workspaces (owner_id, name, is_primary)
SELECT DISTINCT o.user_id, 'Obras de ' || u.name, TRUE
  FROM (SELECT DISTINCT ON (m.construction_project_id)
               m.construction_project_id, m.user_id
          FROM construction_project_members m
         WHERE m.role_in_project = 'OWNER'
         ORDER BY m.construction_project_id, m.joined_at, m.id) o
  JOIN users u ON u.id = o.user_id
 WHERE NOT EXISTS (SELECT 1 FROM workspaces w
                    WHERE w.owner_id = o.user_id
                      AND w.is_primary
                      AND w.deleted_at IS NULL);

-- 2b. O dono também ganha linha de membership OWNER (fonte de verdade de
--     "é dono" continua sendo workspaces.owner_id).
INSERT INTO workspace_members (workspace_id, user_id, role, accepted_at)
SELECT w.id, w.owner_id, 'OWNER', NOW()
  FROM workspaces w
 WHERE w.deleted_at IS NULL
   AND NOT EXISTS (SELECT 1 FROM workspace_members wm
                    WHERE wm.workspace_id = w.id
                      AND wm.user_id = w.owner_id
                      AND wm.deleted_at IS NULL);

-- 2c. Cada obra vai para o workspace primário do seu dono.
UPDATE construction_projects p
   SET workspace_id = w.id
  FROM (SELECT DISTINCT ON (m.construction_project_id)
               m.construction_project_id, m.user_id
          FROM construction_project_members m
         WHERE m.role_in_project = 'OWNER'
         ORDER BY m.construction_project_id, m.joined_at, m.id) o
  JOIN workspaces w ON w.owner_id = o.user_id
                   AND w.is_primary
                   AND w.deleted_at IS NULL
 WHERE p.id = o.construction_project_id
   AND p.workspace_id IS NULL;

-- 2d. Demais membros de cada obra ganham membership no workspace dela:
--     role_in_project USER -> CLIENT; qualquer outro -> MEMBER.
--     Agregado por (workspace, usuário) para quem participa de várias obras
--     (MEMBER vence CLIENT).
INSERT INTO workspace_members (workspace_id, user_id, role, accepted_at)
SELECT p.workspace_id,
       m.user_id,
       CASE WHEN bool_or(m.role_in_project <> 'USER') THEN 'MEMBER' ELSE 'CLIENT' END,
       NOW()
  FROM construction_project_members m
  JOIN construction_projects p ON p.id = m.construction_project_id
 GROUP BY p.workspace_id, m.user_id
HAVING NOT EXISTS (SELECT 1 FROM workspace_members wm
                    WHERE wm.workspace_id = p.workspace_id
                      AND wm.user_id = m.user_id
                      AND wm.deleted_at IS NULL);

-- 3. Trava: a partir daqui toda obra tem tenant.
ALTER TABLE construction_projects ALTER COLUMN workspace_id SET NOT NULL;

-- 4. Título único por tenant, não global (duas construtoras podem ter obras
--    homônimas). O nome vem do UNIQUE inline da V2, gerado pelo Postgres.
ALTER TABLE construction_projects DROP CONSTRAINT IF EXISTS construction_projects_title_key;

-- Verificação: se o nome divergiu (banco não criado pela V2), falha alto em
-- vez de deixar a unicidade global viva silenciosamente.
DO $$
DECLARE
    leftover TEXT;
BEGIN
    SELECT c.conname INTO leftover
      FROM pg_constraint c
     WHERE c.conrelid = 'construction_projects'::regclass
       AND c.contype = 'u'
       AND c.conkey = (SELECT ARRAY[a.attnum]
                         FROM pg_attribute a
                        WHERE a.attrelid = c.conrelid
                          AND a.attname = 'title');
    IF leftover IS NOT NULL THEN
        RAISE EXCEPTION 'UNIQUE global de title ainda existe com outro nome: % — dropar manualmente', leftover;
    END IF;
END $$;

ALTER TABLE construction_projects ADD CONSTRAINT uq_construction_projects_ws_title
    UNIQUE (workspace_id, title);
