# IronLog — app de muscu Android (Java)

Tracker d'entraînement natif Android : gestion des séances, log des charges/répétitions,
chrono de repos automatique, et quelques outils malins (calcul de plaques, 1RM estimé,
échauffement, suggestion de progression).

## Fonctionnalités
- **6 séances** pré-remplies (ton split PPL) + tes charges de départ.
- **Log par série** : poids + reps, avec boutons rapides **−2,5 / +2,5 kg**.
- **Chrono de repos auto** : démarre quand tu valides une série, bip + vibration à la fin,
  −15/+15, pause, passer. L'écran reste allumé pendant la séance.
- **« Dernière fois » + suggestion** : rappelle ta perf précédente et propose la charge
  suivante (+2,5 kg quand tu as atteint le haut de ta fourchette de reps).
- **1RM estimé** (formule d'Epley) à chaque série validée.
- **Calcul de plaques** : entre un poids cible, l'app te dit quoi mettre de chaque côté
  (réglable selon le poids de ta barre).
- **Générateur d'échauffement** sur le 1er exo (séries de montée en charge).
- **Historique** des séances avec volume total + compteur « séances cette semaine ».
- Données stockées **localement** sur le téléphone (SharedPreferences).

## Construire l'APK

### Option A — sans rien installer (GitHub Actions)
1. Crée un dépôt GitHub et pousse ce dossier dessus.
2. L'onglet **Actions** lance le workflow `Build APK` automatiquement (ou clique
   « Run workflow »).
3. Une fois fini, télécharge l'artefact **IronLog-debug-apk** : c'est ton `.apk`.
4. Sur ton téléphone, autorise « sources inconnues » et installe-le.

### Option B — Android Studio
1. Ouvre le dossier dans Android Studio (laisse-le synchroniser Gradle).
2. Menu **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
3. L'APK est dans `app/build/outputs/apk/debug/`.

## Notes
- Versions : AGP 8.5.2 / Gradle 8.7 / compileSdk 34 / minSdk 24 / Java 17.
- C'est un build **debug** (installable pour usage perso). Pour un APK release signé,
  il faut générer une clé de signature.
- Le code a été écrit sans pouvoir être compilé sur place : si le premier build
  remonte une erreur mineure, elle sera facile à corriger (souvent une version de
  dépendance).
