---
name: education-design-expert
description: Concevoir ou améliorer les écrans du LMS éducatif INPG en préservant son identité visuelle, ses composants réutilisables et des parcours simples pour candidats, étudiants, formateurs et équipes d’admission.
metadata:
  short-description: Design UX/UI pour plateforme éducative
---

# Expert design éducation

Utiliser ce skill pour créer ou améliorer une page, un formulaire, un tableau de bord,
une navigation, un bouton ou un parcours du LMS INPG.

## Résultat attendu

Produire une interface claire, accessible et cohérente avec le design existant. Ajouter
les actions utiles, sans surcharger l’écran ni réinventer un composant déjà présent.
Chaque action doit répondre à un besoin métier : apprendre, déposer, suivre, corriger,
planifier, décider ou administrer.

## Avant de modifier une page

1. Lire `CLAUDE.md` et inspecter les écrans voisins du même espace.
2. Chercher d’abord un composant dans `frontend/src/app/shared/` et une action API dans
   `frontend/src/app/core/`.
3. Identifier le rôle concerné et l’état vide, le chargement, le succès et l’erreur.
4. Préserver la structure et le vocabulaire déjà utilisés sur les autres pages.

## Règles de composition

- Une page présente un objectif principal, un titre, un court contexte et une action
  principale visible. Les actions secondaires restent discrètes.
- Réutiliser les cartes, tableaux, modales, notifications et boutons existants. Ne pas
  créer plusieurs variantes pour une même action.
- Dans les tableaux, garder les actions près de la ligne concernée ; placer les filtres
  et l’export au-dessus de la liste.
- Pour les formulaires, grouper les champs par étape métier, indiquer les champs requis,
  prévenir la perte de données et afficher les erreurs près du champ.
- Ne jamais ajouter un bouton sans destination, permission et retour visible. Une action
  non disponible doit expliquer pourquoi ou être masquée selon le contexte.

## Charte INPG

Respecter les jetons Tailwind et les règles de `CLAUDE.md`. Un titre de page utilise
`font-display text-display`; une carte utilise
`bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm`; un
bouton principal utilise `bg-secondary text-on-secondary rounded-lg font-label-md text-label-md`.
Ne pas introduire de couleurs codées en dur ni modifier globalement le design sans une
demande explicite.

## Vérification

Après toute modification frontend, lancer :

```bash
cd frontend && npm test -- --watch=false
cd frontend && npm run build
```

Signaler les parcours vérifiés et les limites liées à une API ou à une décision métier
manquante.
