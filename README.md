# Claud — Projet Cloud (Vue.js + Spring Boot + PostgreSQL + Firebase)

Ce dépôt contient l'application full-stack "Claud" :
- Frontend : Vue.js 3 (dossier `web`)
- Backend : Spring Boot (dossier `Backend`)
- Base de données : PostgreSQL (`base.sql`)
- Firebase : fichier de configuration `firebase-service-account.json` (situé dans `Backend/src/main/resources`)

Important : le repository contient la configuration Firebase par conception. Assurez-vous d'invalider/rotater la clé si vous la mettez en public.

## Préparation locale (commande à exécuter une fois)
Ouvrez un terminal à la racine du projet (`d:\Web\Cloud`) puis exécutez :

```bash
# initialiser git (si pas déjà fait)
git init
# ajouter tous les fichiers
git add .
# valider
git commit -m "Initial import du projet Claud (Frontend + Backend + DB + Firebase config)"
# ajouter la télécommande (remote)
git remote add origin https://github.com/lita25/Claud.git
# pousser sur la branche main (si la branche principale s'appelle main)
git push -u origin main
```

> Si votre branche principale s'appelle `master`, remplacez `main` par `master` dans la commande `git push`.

## Notes de sécurité
- Le fichier `Backend/src/main/resources/firebase-service-account.json` est inclus dans le dépôt. Cela contient des identifiants sensibles. Si vous publiez ce repo, **changez/rotâtes la clé de service dans la console Firebase** immédiatement.
- Recommandation production : stocker les secrets en variables d'environnement ou dans un secret manager (GitHub Secrets, Azure Key Vault, etc.) et **ne pas** garder les clés en clair dans le repo public.

## Structure du projet
- `Backend/` — Spring Boot application
- `web/` — Vue.js frontend
- `base.sql` — Script d'initialisation de la DB
- `Project_Tracking.csv` — suivi d'avancement
- `generate_pdf.html` — helper pour exporter captures en PDF

## Après le push
1. Vérifiez le dépôt sur GitHub pour vous assurer que tous les fichiers sont présents.
2. Si le repo est public : invalidez les clés Firebase et créez-en de nouvelles.
3. Ajoutez un `.github/workflows` si vous voulez CI/CD (optionnel).

---
Si vous voulez, j'ajoute une action GitHub CI de base (build backend + lint frontend) et/ou j'exécute les commandes git pour vous (requiert que vous me donniez un accès ou exécutez les scripts fournis).