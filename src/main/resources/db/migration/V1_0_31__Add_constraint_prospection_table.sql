
ALTER TABLE prospection_entries ALTER COLUMN seller_id DROP NOT NULL;

-- 2. Colonnes de la saisie manuelle (NULL = ligne en mode officiel).
ALTER TABLE prospection_entries ADD COLUMN IF NOT EXISTS garage_name VARCHAR(150);
ALTER TABLE prospection_entries ADD COLUMN IF NOT EXISTS phone_number VARCHAR(30);


ALTER TABLE prospection_entries
DROP CONSTRAINT IF EXISTS ck_prospection_consent;


DO $$
BEGIN

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_prospection_seller_or_manual'
    ) THEN
ALTER TABLE prospection_entries ADD CONSTRAINT ck_prospection_seller_or_manual CHECK
    (
    seller_id IS NOT NULL
        OR (garage_name IS NOT NULL AND length(trim(garage_name)) >= 2)
    );
END IF;

    -- Exclusivité stricte : une ligne officielle ne porte aucun champ manuel,
    -- une ligne manuelle ne référence pas de vendeur officiel.
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_prospection_manual_fields'
    ) THEN
ALTER TABLE prospection_entries ADD CONSTRAINT ck_prospection_manual_fields CHECK
    (
    (seller_id IS NOT NULL AND garage_name IS NULL AND phone_number IS NULL)
        OR
    (seller_id IS NULL)
    );
END IF;

    -- Téléphone facultatif, mais format plausible s'il est renseigné
    -- (chiffres, +, espaces, tirets, points — 6 à 30 caractères).
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_prospection_phone_format'
    ) THEN
ALTER TABLE prospection_entries ADD CONSTRAINT ck_prospection_phone_format CHECK
    (phone_number IS NULL OR phone_number ~ '^[0-9+][0-9+ .-]{5,29}$');
END IF;
END
$$;
