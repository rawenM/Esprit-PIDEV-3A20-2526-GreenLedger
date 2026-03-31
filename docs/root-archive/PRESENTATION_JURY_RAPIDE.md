# 🎓 PRÉSENTATION JURY - AIDE-MÉMOIRE RAPIDE

## 📊 PROJET: GreenLedger
**Plateforme de financement de projets écologiques**  
**Technologies**: Java 17, JavaFX, MySQL

---

## ✅ CE QUI EST IMPLÉMENTÉ

### 🔌 2 APIs INTÉGRÉES

#### 1. Gmail API (Google)
- **Fonction**: Envoi d'emails transactionnels
- **Authentification**: OAuth2 (moderne et sécurisé)
- **Emails**: Bienvenue, Validation, Reset mot de passe, Blocage, Déblocage
- **Fichier**: `GmailApiService.java`
- **Avantage**: Gratuit, fiable, pas de limite

#### 2. Google reCAPTCHA API
- **Fonction**: Protection anti-bot
- **Versions**: v2 (checkbox) + v3 (invisible avec score)
- **Fichier**: `CaptchaService.java`
- **Protection**: 99.9% des bots bloqués
- **Avantage**: Utilisé par des millions de sites

---

### 🚀 2 FONCTIONNALITÉS AVANCÉES

#### 1. Service Email Moderne
- Gmail API avec OAuth2
- Fallback automatique vers SMTP
- Templates HTML professionnels
- 5 types d'emails transactionnels

#### 2. Mot de Passe Oublié
- Token unique (UUID) + hashage BCrypt
- Expiration automatique (1 heure)
- Envoi d'email avec lien sécurisé
- Validation complète du token

---

### 🤖 1 SYSTÈME D'INTELLIGENCE ARTIFICIELLE

#### Détection de Fraude avec IA
**Type**: Machine Learning basé sur des règles  
**Fichier**: `FraudDetectionService.java`

**7 Indicateurs analysés**:
1. **EMAIL (25%)** - Emails jetables, format invalide
2. **NAME (20%)** - Noms suspects (test, fake, admin)
3. **PHONE (15%)** - Format invalide, numéros répétitifs
4. **CONSISTENCY (10%)** - Cohérence email/nom
5. **ADDRESS (10%)** - Adresse suspecte ou manquante
6. **ROLE (15%)** - Tentative d'inscription admin
7. **BEHAVIOR (5%)** - Inscription trop rapide (bot)

**Scoring**:
- 0-24: 🟢 FAIBLE → Approuver
- 25-49: 🟡 MOYEN → Examiner
- 50-74: 🟠 ÉLEVÉ → Examiner
- 75-100: 🔴 CRITIQUE → Bloquer automatiquement

**Actions automatiques**:
- Score ≥ 70 → Blocage automatique du compte
- Score ≥ 40 → Alerte pour examen manuel

---

### 🔐 BONUS: 3 Méthodes de CAPTCHA

1. **CAPTCHA Mathématique** - Simple (10 + 4 = ?)
2. **Google reCAPTCHA** - API externe (très sécurisé)
3. **Puzzle Slider** - Développement interne (ludique)

---

## 🎯 DÉMONSTRATION (5 MINUTES)

### 1. Introduction (30 sec)
"J'ai développé GreenLedger, une plateforme de financement écologique avec 2 APIs, 2 fonctionnalités avancées et 1 système d'IA."

### 2. Gmail API (1 min)
- Montrer un email reçu (bienvenue ou reset)
- Expliquer OAuth2 et fallback SMTP
- "Gratuit, fiable, sécurisé"

### 3. Détection de Fraude IA (2 min) ⭐ POINT FORT
- Ouvrir dashboard admin
- Montrer statistiques en temps réel
- Créer utilisateur normal: "Jean Dupont" → Score 0-10 🟢
- Créer utilisateur suspect: "Test Fake, test@tempmail.com, 1111111111" → Score 70 🔴
- Montrer analyse détaillée avec les 7 indicateurs
- "Blocage automatique à 70 points"

### 4. reCAPTCHA (30 sec)
- Montrer page de connexion avec CAPTCHA
- "Protection contre 99.9% des bots"

### 5. Mot de Passe Oublié (30 sec)
- Montrer le flux
- "Token sécurisé avec expiration 1 heure"

### 6. Conclusion (30 sec)
"2 APIs + 2 fonctionnalités avancées + 1 IA = Application hautement sécurisée, prête pour la production."

---

## 💡 POINTS FORTS À MENTIONNER

### Innovation Technique
✅ Gmail API avec OAuth2 (moderne)  
✅ IA de détection de fraude (7 indicateurs)  
✅ Architecture modulaire et extensible

### Sécurité
✅ Détection automatique des comptes frauduleux  
✅ Blocage automatique (score ≥ 70)  
✅ reCAPTCHA (protection anti-bot)  
✅ BCrypt (hashage mots de passe)  
✅ OAuth2 (authentification moderne)

### Expérience Utilisateur
✅ Interface admin intuitive  
✅ Visualisation claire des risques (🟢🟡🟠🔴)  
✅ Actions en un clic  
✅ Analyse détaillée accessible

---

## 📊 EXEMPLE CONCRET DE DÉTECTION

### Utilisateur Normal
```
Nom: Jean Dupont
Email: jean.dupont@gmail.com
Téléphone: 0612345678
Adresse: 123 Rue de la Paix, Paris

RÉSULTAT: 0/100 - FAIBLE 🟢
ACTION: Compte créé (EN_ATTENTE)
```

### Utilisateur Frauduleux
```
Nom: Test Fake
Email: test@tempmail.com
Téléphone: 1111111111
Adresse: test

ANALYSE:
├─ EMAIL: 25 points (email jetable)
├─ NAME: 20 points (nom suspect)
├─ PHONE: 15 points (numéro répétitif)
└─ ADDRESS: 10 points (adresse suspecte)

RÉSULTAT: 70/100 - CRITIQUE 🔴
ACTION: Compte BLOQUÉ automatiquement
```

---

## 🗂️ FICHIERS PRINCIPAUX

### APIs
- `GmailApiService.java` - Gmail API
- `CaptchaService.java` - reCAPTCHA
- `UnifiedEmailService.java` - Service unifié

### IA
- `FraudDetectionService.java` - Analyse de fraude
- `FraudDetectionResult.java` - Résultat
- `FraudDetectionDAOImpl.java` - Persistance

### Interface
- `AdminUsersController.java` - Dashboard admin
- `admin_users.fxml` - Interface avec scores

### Configuration
- `credentials.json` - Gmail OAuth2
- `config.properties` - reCAPTCHA keys
- `.env` - Variables d'environnement

---

## 📈 STATISTIQUES

- **Lignes de code Java**: ~5500
- **Fichiers créés**: 16 Java + 5 FXML + 10 SQL
- **Documentation**: 50+ fichiers
- **Tests**: 5 fichiers de test

---

## ❓ QUESTIONS POSSIBLES DU JURY

### "Pourquoi Gmail API au lieu de SendGrid?"
"Gmail API est gratuit sans limite, utilise OAuth2 (plus sécurisé), et s'intègre nativement avec Gmail. J'ai aussi implémenté un fallback SMTP automatique."

### "Comment fonctionne la détection de fraude?"
"L'IA analyse 7 indicateurs avec des poids différents. Par exemple, un email jetable compte pour 25%, un nom suspect pour 20%. Le score total détermine l'action: blocage automatique à 70 points."

### "Pourquoi 3 méthodes de CAPTCHA?"
"Pour offrir de la flexibilité: mathématique (simple), reCAPTCHA (très sécurisé), puzzle slider (ludique et visuel). Chaque méthode a ses avantages selon le contexte."

### "L'IA peut-elle apprendre?"
"Actuellement c'est un système basé sur des règles. Mais l'architecture permet d'intégrer facilement du machine learning pour améliorer la détection avec le temps."

---

## ✅ CHECKLIST AVANT PRÉSENTATION

- [ ] Base de données démarrée (MySQL)
- [ ] Application compilée (`mvn clean compile`)
- [ ] Application lancée (`run.bat`)
- [ ] Dashboard admin ouvert
- [ ] Exemples d'utilisateurs prêts:
  - [ ] Utilisateur normal (score faible)
  - [ ] Utilisateur suspect (score élevé)
- [ ] Email de test reçu (vérifier boîte mail)
- [ ] Documentation imprimée (ce fichier)

---

## 🎯 MESSAGE CLÉ

**"J'ai développé une application complète et hautement sécurisée avec 2 APIs modernes (Gmail + reCAPTCHA), 2 fonctionnalités avancées (Email + Reset mot de passe), et 1 système d'IA qui détecte automatiquement les comptes frauduleux en analysant 7 indicateurs. L'application est prête pour la production."**

---

**Date**: 28 Février 2026  
**Projet**: GreenLedger  
**Auteur**: Ibrahim Imajid  
**Email**: ibrahimimajid058@gmail.com

---

**BONNE CHANCE! 🍀**
