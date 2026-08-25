# Base de données — migrations

Depuis le 15/08/2026, le schéma est piloté par **Flyway**, plus par Hibernate.

`spring.jpa.hibernate.ddl-auto` est passé de `update` à `validate` : Hibernate ne
modifie plus la base, il vérifie seulement que les entités correspondent aux tables
et refuse de démarrer sinon.

## Pourquoi ce changement

`ddl-auto=update` sait ajouter une colonne, mais ni en supprimer une, ni en renommer.
Le renommage `Cohorte` → `Promotion` lors de la fusion d'août a donc laissé dans
`seance` une colonne `date` NOT NULL orpheline, que plus aucun code ne renseignait :
toute création de séance échouait, et le backend refusait de démarrer. Le diagnostic
a pris plus de temps que la correction, parce que rien dans le code ne disait que la
base avait divergé.

## Ce que tu dois faire, une seule fois

### Si ta base date d'avant le 15/08/2026

Applique d'abord le rattrapage, sinon Flyway posera une baseline sur un schéma
incomplet et Hibernate refusera de démarrer :

```bash
docker exec demo-postgres pg_dump -U admin -d myappdb > sauvegarde.sql   # filet
docker exec -i demo-postgres psql -U admin -d myappdb < backend/db/2026-08-14_fusion-promotion.sql
docker compose up -d --build backend
```

Au démarrage, Flyway enregistre ton schéma existant comme version 1 sans rejouer
`V1__baseline.sql` (`baseline-on-migrate=true`).

### Si tu repars d'une base vide

Rien à faire. Flyway joue `V1__baseline.sql` et crée les 50 tables, puis les
`DataInitializer` insèrent les données de démonstration.

```bash
docker compose down -v && docker compose up -d --build
```

## Ajouter un changement de schéma

1. Créer `src/main/resources/db/migration/V2__ce_que_ca_fait.sql`
   (puis `V3__`, `V4__`… — la numérotation ne se réutilise jamais).
2. Y écrire le SQL : `ALTER TABLE`, `CREATE INDEX`, etc.
3. Modifier l'entité JPA en conséquence.
4. Redémarrer : Flyway applique le script, Hibernate valide le résultat.

**Ne jamais modifier un script déjà appliqué.** Flyway stocke une empreinte de chaque
fichier joué dans `flyway_schema_history` et refuse de démarrer si elle change. Pour
corriger une migration partie trop vite, on en écrit une nouvelle.

## Vérifier l'état

```bash
docker exec demo-postgres psql -U admin -d myappdb -c \
  "SELECT version, description, type, success FROM flyway_schema_history ORDER BY installed_rank;"
```

## En cas de blocage

| Symptôme | Cause probable | Ce qu'il faut faire |
|---|---|---|
| `Schema-validation: missing table [x]` | Entité sans migration correspondante | Écrire le `V…__.sql` qui crée la table |
| `Schema-validation: missing column [x]` | Champ ajouté sans migration | Écrire le `ALTER TABLE` correspondant |
| `Migration checksum mismatch` | Un script déjà appliqué a été modifié | Restaurer le fichier d'origine ; corriger via un nouveau script |
| `Found non-empty schema without schema history table` | Base ancienne, baseline non posée | Vérifier `spring.flyway.baseline-on-migrate=true` |

En dernier recours, en développement uniquement, on repart d'une base neuve :
`docker compose down -v && docker compose up -d --build`. Les données de démonstration
sont regénérées ; **ne jamais faire ça sur des données réelles.**

## Contenu de démonstration (hors Flyway)

`seed-demo-formateur.sql` n'est **pas** une migration : c'est du contenu (séances,
notes, présences, avis) pour que chaque section de la sidebar formateur ait un
exemple concret à montrer, plutôt que des écrans vides. Volontairement en dehors de
`db/migration/` — le mélanger à Flyway empêcherait de le rejouer après une remise à
zéro des données.

```bash
docker exec -i demo-postgres psql -U admin -d myappdb < backend/db/seed-demo-formateur.sql
```

Réexécutable sans dupliquer (`ON CONFLICT` / `WHERE NOT EXISTS`). Si une base neuve
(`docker compose down -v`) doit retrouver ces exemples, relancer ce script après.
