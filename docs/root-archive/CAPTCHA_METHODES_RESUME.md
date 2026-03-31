# 🔐 RÉSUMÉ: Méthodes de CAPTCHA Implémentées

## 📊 VUE D'ENSEMBLE

**Nombre de méthodes**: 3  
**Objectif**: Protection multi-niveaux contre les bots et attaques automatisées

---

## 1️⃣ CAPTCHA MATHÉMATIQUE (Simple)

### Description
Équation mathématique simple que l'utilisateur doit résoudre.

### Exemple
```
Combien fait 10 + 4 ?
Réponse: [____]
```

### Implémentation
- **Fichier**: `LoginController.java`
- **Génération**: Aléatoire (addition de 2 nombres)
- **Vérification**: Côté client et serveur

### Avantages
- ✅ Très simple à implémenter
- ✅ Pas de dépendance externe
- ✅ Fonctionne offline
- ✅ Léger (pas d'image)

### Inconvénients
- ❌ Facile à automatiser
- ❌ Peu sécurisé
- ❌ Pas visuel

---

## 2️⃣ GOOGLE reCAPTCHA (API Externe)

### Description
API officielle de Google pour la protection anti-bot avec analyse comportementale.

### Versions supportées

#### reCAPTCHA v2 (Checkbox)
```
☐ Je ne suis pas un robot
```
- Challenge visible
- Clic requis
- Challenges supplémentaires si suspect

#### reCAPTCHA v3 (Invisible)
```
Analyse en arrière-plan
Score: 0.0 (bot) à 1.0 (humain)
```
- Pas d'interaction utilisateur
- Score de confiance
- Seuil recommandé: 0.5

### Implémentation
- **Fichier**: `CaptchaService.java`
- **API**: `https://www.google.com/recaptcha/api/siteverify`
- **Authentification**: Site Key + Secret Key

### Configuration
```properties
RECAPTCHA_SITE_KEY=votre_site_key
RECAPTCHA_SECRET_KEY=votre_secret_key
```

### Code
```java
public boolean verifyToken(String token) {
    // Appel API Google
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(verifyUrl))
        .POST(HttpRequest.BodyPublishers.ofString(form))
        .build();
    
    // Vérification du score (v3)
    if (json.has("score")) {
        double score = json.get("score").getAsDouble();
        return score >= 0.5;
    }
    
    return success;
}
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

## 3️⃣ CAPTCHA PUZZLE (Slider) - Développement Interne

### Description
CAPTCHA visuel interactif où l'utilisateur glisse une pièce de puzzle pour compléter une image.

### Exemple visuel
```
┌─────────────────────────────────────┐
│  [Image de fond avec trou]          │
│                                     │
│         ┌──┐                        │
│         │  │  ← Pièce à glisser     │
│         └──┘                        │
└─────────────────────────────────────┘

Glissez la pièce pour compléter l'image →
```

### Implémentation
- **Service**: `PuzzleCaptchaService.java`
- **Contrôleur**: `PuzzleCaptchaController.java`
- **Interface**: `puzzle_captcha.fxml`
- **Technologie**: Java AWT/Swing + JavaFX

### Fonctionnement

#### 1. Génération
```java
public PuzzleCaptchaResult generatePuzzle() {
    // 1. Créer image de fond (300x150)
    BufferedImage backgroundImage = createBackgroundImage();
    
    // 2. Choisir position aléatoire
    int puzzleX = random.nextInt(IMAGE_WIDTH - PUZZLE_SIZE * 2);
    
    // 3. Créer forme de puzzle avec encoches
    Shape puzzleShape = createPuzzleShape(puzzleX, puzzleY);
    
    // 4. Extraire la pièce
    BufferedImage puzzlePiece = extractPuzzlePiece(...);
    
    // 5. Créer image avec trou
    BufferedImage backgroundWithHole = createBackgroundWithHole(...);
    
    return new PuzzleCaptchaResult(...);
}
```

#### 2. Interaction
```java
// Glisser-déposer
puzzlePieceImageView.setOnMousePressed(this::handleMousePressed);
puzzlePieceImageView.setOnMouseDragged(this::handleMouseDragged);
puzzlePieceImageView.setOnMouseReleased(this::handleMouseReleased);
```

#### 3. Vérification
```java
public boolean verifyPosition(int userPosition, int correctPosition) {
    int difference = Math.abs(userPosition - correctPosition);
    return difference <= TOLERANCE; // ±5 pixels
}
```

### Caractéristiques
- **Taille image**: 300x150 pixels
- **Taille pièce**: 50x50 pixels
- **Tolérance**: ±5 pixels
- **Génération**: Aléatoire (position 50-250 pixels)

### Avantages
- ✅ Expérience utilisateur ludique
- ✅ Très visuel et engageant
- ✅ Pas de dépendance externe
- ✅ Fonctionne offline
- ✅ Pas de tracking
- ✅ Contrôle total
- ✅ Accessible (pas de calcul)

### Inconvénients
- ❌ Moins sécurisé que reCAPTCHA
- ❌ Nécessite la souris
- ❌ Peut être difficile sur mobile
- ❌ Génération d'image (performance)

---

## 📊 COMPARAISON DES 3 MÉTHODES

| Critère | Mathématique | Puzzle Slider | reCAPTCHA |
|---------|--------------|---------------|-----------|
| **Sécurité** | ⭐ Faible | ⭐⭐ Moyenne | ⭐⭐⭐ Élevée |
| **UX** | ⭐⭐ Moyenne | ⭐⭐⭐ Excellente | ⭐⭐ Bonne |
| **Visuel** | ❌ Non | ✅ Oui | ⚠️ Moyen |
| **Offline** | ✅ Oui | ✅ Oui | ❌ Non |
| **Mobile** | ✅ Facile | ⚠️ Moyen | ✅ Facile |
| **Accessibilité** | ✅ Bonne | ⚠️ Moyenne | ✅ Bonne |
| **Dépendance** | ❌ Aucune | ❌ Aucune | ⚠️ Google |
| **Performance** | ✅ Rapide | ⚠️ Moyen | ✅ Rapide |
| **Tracking** | ✅ Non | ✅ Non | ❌ Oui |
| **Coût** | ✅ Gratuit | ✅ Gratuit | ✅ Gratuit* |

*Gratuit jusqu'à 1M requêtes/mois

---

## 🎯 RECOMMANDATIONS D'UTILISATION

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

### Approche hybride (RECOMMANDÉ)
```
1. Première tentative: reCAPTCHA v3 (invisible)
   └─ Si score < 0.5 → Afficher Puzzle Slider

2. Deuxième tentative: Puzzle Slider
   └─ Si échec → Afficher reCAPTCHA v2 (checkbox)

3. Troisième tentative: reCAPTCHA v2
   └─ Si échec → Bloquer temporairement
```

---

## 💻 INTÉGRATION DANS LE PROJET

### Fichiers créés

#### CAPTCHA Mathématique
```
LoginController.java (méthode generateMathCaptcha)
```

#### reCAPTCHA
```
src/main/java/Utils/CaptchaService.java
src/main/resources/config.properties
```

#### Puzzle Slider
```
src/main/java/Utils/PuzzleCaptchaService.java
src/main/java/Controllers/PuzzleCaptchaController.java
src/main/resources/fxml/puzzle_captcha.fxml
src/main/java/tools/TestPuzzleCaptcha.java
```

### Documentation
```
GUIDE_CAPTCHA_PUZZLE.md
CAPTCHA_METHODES_RESUME.md (ce fichier)
```

---

## 🧪 TESTS

### Test du Puzzle Slider
```bash
mvn compile
java -cp target/classes tools.TestPuzzleCaptcha
```

### Résultat attendu
```
=== TEST DU CAPTCHA PUZZLE ===

TEST 1: Génération du puzzle
✓ Puzzle généré avec succès
  Position correcte: 156 pixels

TEST 2: Vérification de position correcte
✓ VALIDE

TEST 3: Vérification de position proche (+3 pixels)
✓ VALIDE

TEST 4: Vérification de position incorrecte (+20 pixels)
✗ INVALIDE
```

---

## 🎓 POUR LA PRÉSENTATION AU JURY

### Points à mentionner

1. **Diversité**: 3 méthodes de CAPTCHA différentes
2. **Sécurité**: Protection multi-niveaux
3. **Innovation**: Puzzle Slider développé en interne
4. **Expérience**: Choix adapté à chaque situation

### Démonstration suggérée (2 min)

1. **CAPTCHA Mathématique** (20 sec)
   - Montrer l'équation simple
   - Expliquer: simple mais peu sécurisé

2. **Puzzle Slider** (1 min)
   - Montrer la génération aléatoire
   - Démontrer le glisser-déposer
   - Montrer succès et échec
   - Expliquer: ludique et visuel

3. **reCAPTCHA** (40 sec)
   - Montrer v2 (checkbox)
   - Expliquer v3 (invisible avec score)
   - Expliquer: le plus sécurisé

---

## 📈 STATISTIQUES

### Lignes de code
- **CAPTCHA Mathématique**: ~50 lignes
- **reCAPTCHA**: ~150 lignes
- **Puzzle Slider**: ~400 lignes
- **Total**: ~600 lignes

### Fichiers créés
- **Code Java**: 4 fichiers
- **FXML**: 1 fichier
- **Tests**: 1 fichier
- **Documentation**: 2 fichiers

---

## 🔒 SÉCURITÉ

### Bonnes pratiques implémentées

1. **Vérification côté serveur** (toutes les méthodes)
2. **Session unique** (Puzzle + reCAPTCHA)
3. **Expiration** (tokens avec durée limitée)
4. **Limite de tentatives** (3 essais maximum)
5. **Logging** (toutes les tentatives enregistrées)

---

## 📝 RÉSUMÉ POUR LE JURY

**Méthodes implémentées**: 3
1. CAPTCHA Mathématique (simple)
2. Google reCAPTCHA (API externe, très sécurisé)
3. Puzzle Slider (développement interne, ludique)

**Technologies**:
- Java AWT/Swing (génération d'images)
- JavaFX (interface interactive)
- Google reCAPTCHA API
- HTTP Client (vérification serveur)

**Résultat**:
Protection complète et flexible avec 3 niveaux de sécurité adaptés à différents besoins.

---

**Date**: 28 Février 2026  
**Projet**: GreenLedger  
**Auteur**: Ibrahim Imajid
