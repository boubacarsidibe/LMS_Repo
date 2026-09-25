ALTER TABLE entretiens ADD COLUMN IF NOT EXISTS groupe_entretien_id UUID;
UPDATE entretiens SET groupe_entretien_id = md5(id::text || ':' || clock_timestamp()::text)::uuid WHERE groupe_entretien_id IS NULL;
ALTER TABLE entretiens ALTER COLUMN groupe_entretien_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_entretiens_groupe ON entretiens (groupe_entretien_id);
