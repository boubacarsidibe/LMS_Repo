ALTER TABLE tests_academiques ADD COLUMN IF NOT EXISTS groupe_epreuve_id UUID;
UPDATE tests_academiques
SET groupe_epreuve_id = md5(id::text || ':' || clock_timestamp()::text)::uuid
WHERE groupe_epreuve_id IS NULL;
ALTER TABLE tests_academiques ALTER COLUMN groupe_epreuve_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_tests_academiques_groupe_epreuve
    ON tests_academiques (groupe_epreuve_id);
