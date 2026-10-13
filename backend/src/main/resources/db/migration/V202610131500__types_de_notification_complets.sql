-- La contrainte d'origine (V1) n'autorisait que 11 types de modèles d'e-mails : LISTE_ATTENTE, RAPPEL_DOSSIER_INCOMPLET et
-- RAPPEL_PAIEMENT existaient dans l'énumération sans pouvoir être enregistrés (le serveur affiche le modèle par défaut
-- tant qu'il n'est pas personnalisé, puis échoue à l'enregistrer). DEMANDE_COMPLEMENT s'y ajoute avec l'action
-- « demander un complément » de l'admission.
ALTER TABLE public.notification_templates DROP CONSTRAINT IF EXISTS notification_templates_type_check;
ALTER TABLE public.notification_templates ADD CONSTRAINT notification_templates_type_check CHECK (type IN (
    'VALIDATION_DOSSIER', 'REJET_DOSSIER', 'PAIEMENT_SUCCES', 'CONVOCATION_TEST', 'CONVOCATION_ENTRETIEN',
    'ADMISSION_TEST', 'ADMISSION_ENTRETIEN', 'ADMISSION_FINALE', 'LISTE_ATTENTE', 'NON_ADMISSION',
    'DOCUMENT_CONFORME', 'DOCUMENT_NON_CONFORME', 'DEMANDE_COMPLEMENT', 'RAPPEL_DOSSIER_INCOMPLET', 'RAPPEL_PAIEMENT'));
