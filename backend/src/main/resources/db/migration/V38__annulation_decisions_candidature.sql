-- Permet d'annuler la dernière décision prise sur une candidature (retour au
-- statut précédent) et de reculer explicitement vers une étape antérieure du
-- parcours, à tout moment — pas seulement juste après l'action. Un seul
-- niveau conservé (pas d'historique complet) : suffisant pour « annuler ma
-- dernière action » et pour un recul explicite, qui ne dépend d'aucun
-- historique.
ALTER TABLE candidatures ADD COLUMN statut_precedent VARCHAR(50);
ALTER TABLE tests_academiques ADD COLUMN note_obtenue_precedente REAL;
