INSERT INTO storage.buckets (id, name, public)
VALUES
    ('audios', 'audios', TRUE),
    ('imagens', 'imagens', TRUE)
ON CONFLICT (id) DO UPDATE
SET public = EXCLUDED.public;
