GREEN LEDGER - GUIDE DE DEMARRAGE
==================================

PREREQUIS
=========

1. Java 17 ou superieur installe
   > java --version

2. MySQL 8.0 ou superieur demarr
   > mysql -u root -p

3. Les dependances JavaFX (normalement dans pom.xml)

ETAPES DE CONFIGURATION
=======================

1. CREATION DE LA BASE DE DONNEES

   a) Se connecter a MySQL:
      > mysql -u root -p

   b) Creer la base de donnees:
      > CREATE DATABASE green_ledger;
      > USE green_ledger;

   c) Importer le schema SQL:
      > SOURCE database_schema_green_wallet.sql;

2. VERIFIER LA CONNEXION

   Le fichier DataBase/MyConnection.java contient les parametres:
   - URL: jdbc:mysql://localhost:3306/green_ledger
   - USER: root
   - PASSWORD: (a configurer)

   Modifier si necessaire pour votre configuration locale.

3. COMPILER LE PROJET

   a) Compiler avec javac:
      > cd "C:\Users\Lenovo\Desktop\Pi_Dev"
      > javac -cp "src/main/java" -d target/classes -encoding UTF-8 ^
          src/main/java/org/GreenLedger/*.java ^
          src/main/java/Controllers/*.java ^
          src/main/java/Services/*.java ^
          src/main/java/dao/*.java ^
          src/main/java/Models/*.java ^
          src/main/java/DataBase/*.java ^
          src/main/java/Utils/*.java

   b) Ou utiliser le script run.bat:
      > run.bat

4. LANCER L'APPLICATION

   a) Methode 1 - Ligne de commande:
      > java -cp "target/classes;src/main/java" org.GreenLedger.MainFX

   b) Methode 2 - Script batch:
      > run.bat

   c) Methode 3 - IDE (Eclipse, IntelliJ, etc.):
      - Importer le projet
      - Configurer JavaFX
      - Lancer MainFX.java

STRUCTURE DU PROJET
===================

src/main/java/
  ├── Controllers/        (Interface utilisateur)
  ├── Services/           (Logique metier)
  ├── dao/                (Acces aux donnees)
  ├── Models/             (Modeles de donnees)
  ├── DataBase/           (Gestion connexion)
  ├── Utils/              (Utilitaires)
  └── org/GreenLedger/    (Point d'entree)

src/main/resources/
  ├── fxml/               (Interfaces FXML)
  ├── css/                (Styles CSS)
  ├── themes/             (Themes applicatifs)
  └── images/             (Images)

CORRECTIONS APPORTEES (15/02/2026)
==================================

1. Imports org.example.* corriges vers packages directs
2. Encodage UTF-8 applique a tous les fichiers Java
3. Tous les emojis supprimes et remplaces par marqueurs texte
4. Import Utils.ResetHttpServer ajoute dans MainFX.java
5. Signature de methode start() corrigee dans MainFX.java
6. Code duplique supprime de MainFX.java
7. UserServiceImpl.java completement recreé avec bon encodage

VERIFICATION POST-COMPILATION
=============================

Apres compilation, verifier:
  ✓ target/classes/ contient les fichiers .class compiles
  ✓ Pas de message d'erreur lors de la compilation
  ✓ L'application démarre sans erreur Java
  ✓ La connexion a la BD fonctionne
  ✓ L'interface utilisateur s'affiche

FICHIERS GENERES
================

- run.bat: Script pour compiler et lancer l'application
- RAPPORT_FINAL.txt: Rapport complet des corrections
- CORRECTIONS_APPLIQUEES.txt: Détail des corrections

SUPPORT
=======

En cas de probleme:
  1. Verifier que MySQL est demarr
  2. Verifier les parametres de connexion dans MyConnection.java
  3. Verifier que les fichiers FXML sont dans le classpath
  4. Verifier que l'encodage des fichiers Java est bien UTF-8

La compilation devrait reussir sans erreur.
L'application devrait maintenant fonctionner correctement.

==============================================

