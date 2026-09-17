-- Les anciennes notes étaient saisies sur 20. La plateforme conserve désormais
-- la valeur de référence sur 100 et affiche son équivalent sur 20.
UPDATE public.note_module
SET note = note * 5
WHERE note >= 0 AND note <= 20;

-- Les relevés délivrés sont figés : leurs valeurs doivent rester cohérentes avec
-- le nouveau barème et continuer à être lisibles en /100 et /20 dans leur PDF.
UPDATE public.releve_ligne
SET moyenne = moyenne * 5
WHERE moyenne IS NOT NULL AND moyenne >= 0 AND moyenne <= 20;

UPDATE public.releve_notes
SET moyenne_generale = moyenne_generale * 5
WHERE moyenne_generale IS NOT NULL AND moyenne_generale >= 0 AND moyenne_generale <= 20;
