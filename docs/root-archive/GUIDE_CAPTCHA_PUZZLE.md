# 🧩 GUIDE: CAPTCHA Puzzle (Slider)

## 📝 Description

Le CAPTCHA Puzzle est une méthode de vérification interactive où l'utilisateur doit glisser une pièce de puzzle pour compléter une image. C'est plus convivial qu'un CAPTCHA mathématique et plus engageant visuellement.

---

## 🎯 Fonctionnalités

### Génération automatique
- ✅ Image de fond avec motif coloré aléatoire
- ✅ Pièce de puzzle avec encoches
- ✅ Position aléatoire du trou
- ✅ Extraction de la pièce avec transparence

### Interaction utilisateur
- ✅ Glisser-déposer (drag & drop)
- ✅ Limitation du mouvement horizontal
- ✅ Animation de validation
- ✅ Feedback visuel immédiat

### Vérification
- ✅ Tolérance de 5 pixels
- ✅ Vérification côté serveur
- ✅ Session ID unique
- ✅ Bouton de rafraîchissement

---

## 💻 Implémentation

### 1. Service Backend

**Fichier**: `src/main/java/Utils/PuzzleCaptchaService.java`

```java
// Génération du puzzle
PuzzleCaptchaResult puzzle = service.generatePuzzle();

// Récupération des données
String backgroundBase64 = puzzle.getBackgroundImageBase64();
String puzzleBase64 = puzzle.getPuzzlePieceBase64();
int correctPosition = puzzle.getCorrectPosition();

// Vérification de la position
boolean isValid = service.verifyPosition(userPosition, correctPosition);
```

### 2. Contrôleur JavaFX

**Fichier**: `src/main/java/Controllers/PuzzleCaptchaController.java`

```java
// Initialisation
@FXML
public void initialize() {
    setupDragHandlers();
    generateNewPuzzle();
}

// Callbacks
controller.setOnSuccess(() -> {
    System.out.println("CAPTCHA validé!");
    // Autoriser la connexion
});

controller.setOnFailure(() -> {
    System.out.println("CAPTCHA échoué!");
    // Afficher une erreur
});
```

### 3. Interface FXML

**Fichier**: `src/main/resources/fxml/puzzle_captcha.fxml`

```xml
<VBox>
    <Pane fx:id="captchaContainer">
        <ImageView fx:id="backgroundImageView"/>
        <ImageView fx:id="puzzlePieceImageView"/>
    </Pane>
    <Label fx:id="statusLabel"/>
    <Button fx:id="refreshButton"/>
</VBox>
```

---

## 🔄 Flux de fonctionnement

```
1. Génération du puzzle
   ├─ Créer image de fond (300x150)
   ├─ Choisir position aléatoire (50-250 pixels)
   ├─ Créer forme de puzzle avec encoches
   ├─ Extraire la pièce
   └─ Créer l'image avec le trou

2. Affichage
   ├─ Charger l'image de fond
   ├─ Charger la pièce du puzzle
   └─ Positionner la pièce à gauche

3. Interaction utilisateur
   ├─ Utilisateur clique sur la pièce
   ├─ Utilisateur glisse horizontalement
   └─ Utilisateur relâche

4. Vérification
   ├─ Calculer la position finale
   ├─ Comparer avec la position correcte
   ├─ Tolérance de ±5 pixels
   └─ Retourner succès ou échec

5. Feedback
   ├─ Si succès: Animation + message vert
   └─ Si échec: Retour position initiale + message rouge
```

---

## 🎨 Personnalisation

### Modifier la taille du puzzle

```java
private static final int IMAGE_WIDTH = 300;  // Largeur de l'image
private static final int IMAGE_HEIGHT = 150; // Hauteur de l'image
private static final int PUZZLE_SIZE = 50;   // Taille de la pièce
```

### Modifier la tolérance

```java
private static final int TOLERANCE = 5; // Tolérance en pixels
```

### Modifier les couleurs

```java
// Dans createBackgroundImage()
GradientPaint gradient = new GradientPaint(
    0, 0, new Color(100, 150, 200),      // Couleur de départ
    IMAGE_WIDTH, IMAGE_HEIGHT, new Color(150, 200, 250) // Couleur de fin
);
```

---

## 🧪 Tests

### Test manuel

```bash
# Compiler
mvn compile

# Exécuter le test
java -cp target/classes tools.TestPuzzleCaptcha
```

### Résultat attendu

```
=== TEST DU CAPTCHA PUZZLE ===

TEST 1: Génération du puzzle
----------------------------------------
✓ Puzzle généré avec succès
  Position correcte: 156 pixels
  Session ID: xY9kL2mP4nQ8rT
  Image de fond: iVBORw0KGgoAAAANSUhEUgAAASwAAACSCAYAAAD...
  Pièce du puzzle: iVBORw0KGgoAAAANSUhEUgAAAEYAAABGCAYAAAB...

TEST 2: Vérification de position correcte
----------------------------------------
[PuzzleCaptcha] Vérification:
  Position utilisateur: 156
  Position correcte: 156
  Différence: 0 pixels
  Résultat: VALIDE
Résultat: ✓ VALIDE

TEST 3: Vérification de position proche (+3 pixels)
----------------------------------------
[PuzzleCaptcha] Vérification:
  Position utilisateur: 159
  Position correcte: 156
  Différence: 3 pixels
  Résultat: VALIDE
Résultat: ✓ VALIDE

TEST 4: Vérification de position incorrecte (+20 pixels)
----------------------------------------
[PuzzleCaptcha] Vérification:
  Position utilisateur: 176
  Position correcte: 156
  Différence: 20 pixels
  Résultat: INVALIDE
Résultat: ✗ INVALIDE

TEST 5: Génération de 5 puzzles différents
----------------------------------------
Puzzle 1: Position = 123 pixels
Puzzle 2: Position = 187 pixels
Puzzle 3: Position = 95 pixels
Puzzle 4: Position = 214 pixels
Puzzle 5: Position = 142 pixels

=== FIN DES TESTS ===
```

---

## 🔗 Intégration dans la page de connexion

### Option 1: Remplacer le CAPTCHA mathématique

```java
// Dans LoginController.java
private PuzzleCaptchaController puzzleCaptchaController;

@FXML
public void initialize() {
    // Charger le CAPTCHA puzzle
    FXMLLoader loader = new FXMLLoader(
        getClass().getResource("/fxml/puzzle_captcha.fxml")
    );
    Parent puzzleCaptcha = loader.load();
    puzzleCaptchaController = loader.getController();
    
    // Ajouter au container
    captchaContainer.getChildren().add(puzzleCaptcha);
    
    // Définir les callbacks
    puzzleCaptchaController.setOnSuccess(() -> {
        captchaVerified = true;
    });
}
```

### Option 2: Ajouter comme option supplémentaire

```xml
<!-- Dans login.fxml -->
<VBox spacing="10">
    <Label text="Choisissez votre méthode de vérification:"/>
    
    <RadioButton text="Équation mathématique" selected="true"/>
    <RadioButton text="Puzzle slider"/>
    <RadioButton text="Google reCAPTCHA"/>
    
    <!-- Container qui change selon la sélection -->
    <StackPane fx:id="captchaContainer"/>
</VBox>
```

---

## ✅ Avantages

### Par rapport au CAPTCHA mathématique
- ✅ Plus visuel et engageant
- ✅ Pas de calcul mental requis
- ✅ Accessible aux personnes ayant des difficultés en mathématiques
- ✅ Plus difficile à automatiser pour les bots

### Par rapport au reCAPTCHA
- ✅ Pas de dépendance externe (API Google)
- ✅ Fonctionne hors ligne
- ✅ Pas de tracking utilisateur
- ✅ Contrôle total sur l'apparence

### Général
- ✅ Expérience utilisateur ludique
- ✅ Feedback visuel immédiat
- ✅ Tolérance configurable
- ✅ Génération aléatoire infinie

---

## ⚠️ Limitations

### Sécurité
- ⚠️ Moins sécurisé que reCAPTCHA v3
- ⚠️ Peut être contourné par vision par ordinateur avancée
- ⚠️ Nécessite une vérification côté serveur

### Accessibilité
- ⚠️ Nécessite l'utilisation de la souris
- ⚠️ Peut être difficile sur mobile/tactile
- ⚠️ Pas adapté aux lecteurs d'écran

### Performance
- ⚠️ Génération d'image côté serveur
- ⚠️ Transfert de données Base64 (images)

---

## 🔒 Recommandations de sécurité

### 1. Vérification côté serveur
```java
// TOUJOURS vérifier côté serveur
boolean isValid = captchaService.verifyPosition(
    userPosition, 
    storedCorrectPosition
);
```

### 2. Session unique
```java
// Stocker la position correcte dans la session
session.setAttribute("captcha_position", correctPosition);
session.setAttribute("captcha_session_id", sessionId);
```

### 3. Expiration
```java
// Expirer le CAPTCHA après 5 minutes
session.setAttribute("captcha_expiry", 
    LocalDateTime.now().plusMinutes(5)
);
```

### 4. Limite de tentatives
```java
// Limiter à 3 tentatives
int attempts = (int) session.getAttribute("captcha_attempts");
if (attempts >= 3) {
    // Bloquer temporairement
    // Ou générer un nouveau puzzle
}
```

---

## 📊 Comparaison des méthodes CAPTCHA

| Critère | Équation Math | Puzzle Slider | reCAPTCHA |
|---------|---------------|---------------|-----------|
| **Sécurité** | Faible | Moyenne | Élevée |
| **UX** | Moyenne | Excellente | Bonne |
| **Accessibilité** | Bonne | Moyenne | Bonne |
| **Offline** | ✅ | ✅ | ❌ |
| **Mobile** | ✅ | ⚠️ | ✅ |
| **Visuel** | ❌ | ✅ | ⚠️ |
| **Dépendance** | Aucune | Aucune | Google |

---

## 🎓 Pour la présentation au jury

### Points à mentionner

1. **Innovation**: CAPTCHA puzzle interactif et visuel
2. **Expérience utilisateur**: Plus engageant qu'une équation
3. **Technologie**: Génération d'image avec Java AWT/Swing
4. **Sécurité**: Vérification côté serveur avec tolérance

### Démonstration

1. Montrer la génération aléatoire (plusieurs puzzles)
2. Démontrer le glisser-déposer
3. Montrer la validation (succès et échec)
4. Expliquer la tolérance de 5 pixels

---

## 📁 Fichiers créés

```
src/main/java/Utils/PuzzleCaptchaService.java
src/main/java/Controllers/PuzzleCaptchaController.java
src/main/java/tools/TestPuzzleCaptcha.java
src/main/resources/fxml/puzzle_captcha.fxml
GUIDE_CAPTCHA_PUZZLE.md
```

---

## 🚀 Prochaines étapes

1. ✅ Tester la génération du puzzle
2. ✅ Intégrer dans la page de connexion
3. ✅ Ajouter la vérification côté serveur
4. ✅ Tester sur différentes résolutions
5. ✅ Ajouter des animations
6. ✅ Documenter pour le jury

---

**Date**: 28 Février 2026  
**Projet**: GreenLedger  
**Auteur**: Ibrahim Imajid
