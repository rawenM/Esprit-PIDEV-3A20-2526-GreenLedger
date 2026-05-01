# 🔐 GUIDE: Page de Connexion avec 3 Méthodes CAPTCHA

## 📋 Vue d'ensemble

Nouvelle page de connexion permettant à l'utilisateur de choisir parmi 3 méthodes de vérification CAPTCHA:

1. **CAPTCHA Mathématique** - Équation simple (par défaut)
2. **Google reCAPTCHA** - API externe (très sécurisé)
3. **Puzzle Slider** - Glisser-déposer (ludique)

---

## 📁 Fichiers créés

### Interface
- `src/main/resources/fxml/login_with_captcha_choice.fxml` - Interface avec sélecteur

### Contrôleur
- `src/main/java/Controllers/LoginWithCaptchaChoiceController.java` - Logique des 3 CAPTCHA

---

## 🎨 Interface utilisateur

### Structure

```
┌─────────────────────────────────────────────────────────┐
│                      CONNEXION                           │
│           Plateforme de Financement Participatif        │
│─────────────────────────────────────────────────────────│
│                                                          │
│  Email: [_____________________________________]          │
│                                                          │
│  Mot de passe: [_____________________________________]   │
│                                                          │
│  ☐ Se souvenir de moi                                   │
│                                                          │
│  Vérification:                    [Bypass (temp)]       │
│  Méthode: ⦿ Équation  ○ reCAPTCHA  ○ Puzzle            │
│                                                          │
│  ┌─────────────────────────────────────────────────┐   │
│  │  [CAPTCHA sélectionné s'affiche ici]            │   │
│  └─────────────────────────────────────────────────┘   │
│                                                          │
│            [        Se connecter        ]               │
│                                                          │
│     Mot de passe oublié ?  |  Créer un compte          │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

---

## 🔢 Méthode 1: CAPTCHA Mathématique

### Affichage
```
Méthode: ⦿ Équation  ○ reCAPTCHA  ○ Puzzle

Combien fait 7 + 3 ?  [____]
```

### Fonctionnement
1. Équation générée aléatoirement (addition de 2 nombres 1-10)
2. Utilisateur entre la réponse
3. Vérification côté client
4. Si incorrect, nouvelle équation générée

### Avantages
- ✅ Très simple
- ✅ Pas de dépendance externe
- ✅ Fonctionne offline
- ✅ Rapide

### Inconvénients
- ❌ Peu sécurisé (facile à automatiser)
- ❌ Pas visuel

---

## 🔐 Méthode 2: Google reCAPTCHA

### Affichage
```
Méthode: ○ Équation  ⦿ reCAPTCHA  ○ Puzzle

┌────────────────────────────────────┐
│  ☐ Je ne suis pas un robot         │
│     [Logo reCAPTCHA]                │
└────────────────────────────────────┘
```

### Fonctionnement
1. Chargement du widget reCAPTCHA dans WebView
2. Utilisateur coche la case (v2) ou analyse invisible (v3)
3. Token généré et envoyé au serveur
4. Vérification via API Google

### Configuration requise
```properties
# config.properties
RECAPTCHA_SITE_KEY=votre_site_key
RECAPTCHA_SECRET_KEY=votre_secret_key
```

### Avantages
- ✅ Très sécurisé (99.9% des bots bloqués)
- ✅ Utilisé par des millions de sites
- ✅ Score de confiance (v3)
- ✅ Gratuit (1M requêtes/mois)

### Inconvénients
- ❌ Dépendance externe (Google)
- ❌ Nécessite connexion internet
- ❌ Tracking utilisateur

---

## 🧩 Méthode 3: Puzzle Slider

### Affichage
```
Méthode: ○ Équation  ○ reCAPTCHA  ⦿ Puzzle

Glissez la pièce pour compléter l'image

┌────────────────────────────────────┐
│  [Image de fond avec trou]          │
└────────────────────────────────────┘

Position: [━━━━━━━━━━━━━━━━━━━━━━━━] [Vérifier]

Pièce: [🧩]

[🔄 Nouveau puzzle]
```

### Fonctionnement
1. Génération d'une image aléatoire avec motif
2. Extraction d'une pièce de puzzle
3. Utilisateur déplace le slider pour positionner la pièce
4. Vérification de la position (tolérance ±5 pixels)
5. Feedback visuel (✅ Correct! / ❌ Incorrect)

### Caractéristiques
- **Taille image**: 300x150 pixels
- **Taille pièce**: 50x50 pixels
- **Tolérance**: ±5 pixels
- **Position aléatoire**: 50-250 pixels

### Avantages
- ✅ Expérience ludique et engageante
- ✅ Très visuel
- ✅ Pas de dépendance externe
- ✅ Fonctionne offline
- ✅ Pas de tracking
- ✅ Contrôle total

### Inconvénients
- ❌ Moins sécurisé que reCAPTCHA
- ❌ Nécessite la souris
- ❌ Génération d'image (performance)

---

## 💻 Utilisation dans le code

### Changer le fichier FXML de connexion

#### Option 1: Remplacer le fichier actuel
```bash
# Sauvegarder l'ancien
copy src\main\resources\fxml\login.fxml src\main\resources\fxml\login_old.fxml

# Remplacer par le nouveau
copy src\main\resources\fxml\login_with_captcha_choice.fxml src\main\resources\fxml\login.fxml
```

#### Option 2: Utiliser le nouveau fichier directement
```java
// Dans MainFX.java ou autre point d'entrée
FXMLLoader loader = new FXMLLoader(
    getClass().getResource("/fxml/login_with_captcha_choice.fxml")
);
```

### Mettre à jour le contrôleur dans le FXML
```xml
<!-- Dans login_with_captcha_choice.fxml -->
<BorderPane xmlns:fx="http://javafx.com/fxml"
            fx:controller="Controllers.LoginWithCaptchaChoiceController"
            ...>
```

---

## 🔧 Compilation

### Script de compilation
```bash
# Compiler le nouveau contrôleur
javac -encoding UTF-8 -cp "%CP%" -d target/classes ^
    src/main/java/Controllers/LoginWithCaptchaChoiceController.java
```

### Ou utiliser Maven
```bash
mvn clean compile
```

---

## 🧪 Tests

### Test 1: CAPTCHA Mathématique
1. Lancer l'application
2. Sélectionner "Équation"
3. Résoudre l'équation
4. Cliquer "Se connecter"
5. ✅ Connexion réussie

### Test 2: reCAPTCHA
1. Sélectionner "reCAPTCHA"
2. Attendre le chargement du widget
3. Cocher "Je ne suis pas un robot"
4. Résoudre le challenge si demandé
5. Cliquer "Se connecter"
6. ✅ Connexion réussie

### Test 3: Puzzle Slider
1. Sélectionner "Puzzle"
2. Observer l'image avec le trou
3. Déplacer le slider pour positionner la pièce
4. Cliquer "Vérifier"
5. Si correct: ✅ Correct!
6. Si incorrect: ❌ Position incorrecte, réessayez
7. Cliquer "Se connecter"
8. ✅ Connexion réussie

---

## 📊 Comparaison des 3 méthodes

| Critère | Mathématique | Puzzle | reCAPTCHA |
|---------|--------------|--------|-----------|
| **Sécurité** | ⭐ Faible | ⭐⭐ Moyenne | ⭐⭐⭐ Élevée |
| **UX** | ⭐⭐ Moyenne | ⭐⭐⭐ Excellente | ⭐⭐ Bonne |
| **Visuel** | ❌ Non | ✅ Oui | ⚠️ Moyen |
| **Offline** | ✅ Oui | ✅ Oui | ❌ Non |
| **Dépendance** | ❌ Aucune | ❌ Aucune | ⚠️ Google |
| **Performance** | ✅ Rapide | ⚠️ Moyen | ✅ Rapide |

---

## 🎯 Recommandations

### Pour la production
**Recommandé**: reCAPTCHA v3 (invisible)
- Meilleure sécurité
- Pas d'interaction utilisateur
- Score de confiance

### Pour l'expérience utilisateur
**Recommandé**: Puzzle Slider
- Ludique et engageant
- Pas de tracking
- Contrôle total

### Pour la simplicité
**Recommandé**: CAPTCHA Mathématique
- Très simple
- Pas de dépendance
- Léger

### Approche hybride (OPTIMAL)
```
1. Première tentative: reCAPTCHA v3 (invisible)
   └─ Si score < 0.5 → Afficher Puzzle Slider

2. Deuxième tentative: Puzzle Slider
   └─ Si échec → Afficher reCAPTCHA v2 (checkbox)

3. Troisième tentative: reCAPTCHA v2
   └─ Si échec → Bloquer temporairement
```

---

## 🎓 Pour la présentation au jury

### Points à mentionner

1. **Flexibilité**: 3 méthodes au choix de l'utilisateur
2. **Innovation**: Puzzle développé en interne
3. **Sécurité**: reCAPTCHA pour protection maximale
4. **UX**: Choix adapté aux préférences utilisateur

### Démonstration (2 min)

1. **Montrer les 3 méthodes** (30 sec)
   - Cliquer sur chaque radio button
   - Montrer l'affichage de chaque CAPTCHA

2. **Tester le Puzzle** (1 min)
   - Générer un puzzle
   - Déplacer le slider
   - Vérifier (succès et échec)
   - Régénérer un nouveau puzzle

3. **Expliquer les avantages** (30 sec)
   - Mathématique: simple et rapide
   - reCAPTCHA: très sécurisé
   - Puzzle: ludique et visuel

---

## 🔒 Sécurité

### Vérifications implémentées

1. **Validation côté client** (toutes les méthodes)
2. **Vérification côté serveur** (reCAPTCHA)
3. **Régénération automatique** (Mathématique, Puzzle)
4. **Tolérance limitée** (Puzzle: ±5 pixels)
5. **Bypass temporaire** (développement uniquement)

---

## 📝 Résumé

**Fichiers créés**: 2
- `login_with_captcha_choice.fxml` - Interface
- `LoginWithCaptchaChoiceController.java` - Contrôleur

**Méthodes CAPTCHA**: 3
- Mathématique (simple)
- reCAPTCHA (sécurisé)
- Puzzle (ludique)

**Résultat**: Page de connexion flexible avec choix de méthode de vérification adaptée aux besoins de l'utilisateur.

---

**Date**: 28 Février 2026  
**Projet**: GreenLedger  
**Auteur**: Ibrahim Imajid
