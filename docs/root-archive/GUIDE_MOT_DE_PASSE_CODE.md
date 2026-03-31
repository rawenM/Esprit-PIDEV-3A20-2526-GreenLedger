# 🔐 GUIDE: Mot de Passe Oublié avec Code de Vérification

## 📋 Vue d'ensemble

Système de réinitialisation de mot de passe en 2 étapes avec code de vérification par email.

**Flux utilisateur**:
1. Utilisateur entre son email
2. Reçoit un code à 6 chiffres par email
3. Entre le code + nouveau mot de passe
4. Mot de passe réinitialisé

---

## ✅ Fichiers créés

1. **Interface**: `src/main/resources/fxml/forgot_password.fxml`
2. **Contrôleur**: `src/main/java/Controllers/ForgotPasswordController.java`
3. **Script**: `compile-forgot-password.bat`
4. **Guide**: `GUIDE_MOT_DE_PASSE_CODE.md` (ce fichier)

---

## 🎨 Interface utilisateur

### Étape 1: Demander le code

```
┌─────────────────────────────────────────────────────────┐
│              MOT DE PASSE OUBLIÉ                         │
│   Entrez votre email pour recevoir un code              │
│─────────────────────────────────────────────────────────│
│                                                          │
│  Étape 1: Demander le code                              │
│                                                          │
│  Email: [_____________________________________]          │
│                                                          │
│            [     Envoyer le code     ]                  │
│                                                          │
│              ← Retour à la connexion                    │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### Étape 2: Vérifier le code et changer le mot de passe

```
┌─────────────────────────────────────────────────────────┐
│              MOT DE PASSE OUBLIÉ                         │
│   Un code à 6 chiffres a été envoyé à user@email.com   │
│─────────────────────────────────────────────────────────│
│                                                          │
│  Étape 2: Vérifier le code et changer le mot de passe  │
│                                                          │
│  Email: [user@email.com] (lecture seule)                │
│                                                          │
│  Code de vérification (6 chiffres)                      │
│  Expire dans: 09:45                                     │
│  [______]  [Renvoyer le code]                           │
│                                                          │
│  Nouveau mot de passe:                                  │
│  [_____________________________________]                 │
│  • Minimum 8 caractères                                 │
│  • Au moins une majuscule                               │
│  • Au moins un chiffre                                  │
│                                                          │
│  Confirmer le mot de passe:                             │
│  [_____________________________________]                 │
│                                                          │
│        [  Réinitialiser le mot de passe  ]              │
│                                                          │
│              ← Retour à la connexion                    │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

---

## 📧 Email envoyé

L'utilisateur reçoit un email HTML professionnel:

```
┌─────────────────────────────────────────────────────────┐
│                                                          │
│              🔐 Code de Vérification                     │
│                                                          │
│─────────────────────────────────────────────────────────│
│                                                          │
│  Bonjour Jean Dupont,                                   │
│                                                          │
│  Vous avez demandé à réinitialiser votre mot de passe   │
│  sur GreenLedger.                                        │
│                                                          │
│  Voici votre code de vérification:                      │
│                                                          │
│  ┌─────────────────────────────────────────────────┐   │
│  │                                                  │   │
│  │              1 2 3 4 5 6                        │   │
│  │                                                  │   │
│  └─────────────────────────────────────────────────┘   │
│                                                          │
│  ⚠️ Important:                                           │
│  • Ce code expire dans 10 minutes                       │
│  • Ne partagez jamais ce code avec personne             │
│  • Si vous n'avez pas demandé cette réinitialisation,   │
│    ignorez cet email                                    │
│                                                          │
│  Entrez ce code sur la page de réinitialisation pour    │
│  continuer.                                              │
│                                                          │
│  Cordialement,                                           │
│  L'équipe GreenLedger                                    │
│                                                          │
│─────────────────────────────────────────────────────────│
│  © 2025 GreenLedger - Plateforme de Financement         │
│  Cet email a été envoyé automatiquement                 │
└─────────────────────────────────────────────────────────┘
```

---

## 🔧 Caractéristiques techniques

### Génération du code

```java
private String generateVerificationCode() {
    int code = 100000 + random.nextInt(900000); // 100000 à 999999
    return String.valueOf(code);
}
```

- **Format**: 6 chiffres (100000 à 999999)
- **Aléatoire**: SecureRandom pour sécurité
- **Unique**: Nouveau code à chaque demande

### Expiration

- **Durée**: 10 minutes
- **Compte à rebours**: Affichage en temps réel (MM:SS)
- **Couleur**: 
  - Vert: > 3 minutes
  - Orange: 1-3 minutes
  - Rouge: < 1 minute

### Validation

**Email**:
- ✅ Non vide
- ✅ Contient @ et .
- ✅ Existe dans la base de données

**Code**:
- ✅ 6 chiffres exactement
- ✅ Correspond au code généré
- ✅ Non expiré

**Mot de passe**:
- ✅ Minimum 8 caractères
- ✅ Au moins une majuscule
- ✅ Au moins un chiffre
- ✅ Confirmation identique

---

## 🚀 Installation

### Étape 1: Compilation

```bash
compile-forgot-password.bat
```

### Étape 2: Vérifier les fichiers

- ✅ `forgot_password.fxml` dans `src/main/resources/fxml/`
- ✅ `ForgotPasswordController.java` dans `src/main/java/Controllers/`
- ✅ Fichiers compilés dans `target/classes/`

### Étape 3: Lancer l'application

```bash
run.bat
```

### Étape 4: Tester

1. Cliquer sur "Mot de passe oublié ?" sur la page de connexion
2. Entrer un email valide
3. Vérifier la réception de l'email
4. Entrer le code reçu
5. Entrer un nouveau mot de passe
6. Confirmer
7. ✅ Mot de passe réinitialisé!

---

## 🧪 Tests

### Test 1: Flux complet

1. Page de connexion → "Mot de passe oublié ?"
2. Entrer email: `test@example.com`
3. Cliquer "Envoyer le code"
4. ✅ Message: "Code envoyé! Vérifiez votre boîte email."
5. Vérifier l'email reçu
6. Copier le code à 6 chiffres
7. Entrer le code
8. Entrer nouveau mot de passe: `Test1234`
9. Confirmer: `Test1234`
10. Cliquer "Réinitialiser le mot de passe"
11. ✅ Message: "Mot de passe réinitialisé avec succès!"
12. Redirection automatique vers la connexion
13. Se connecter avec le nouveau mot de passe
14. ✅ Connexion réussie!

### Test 2: Code expiré

1. Demander un code
2. Attendre 10 minutes
3. Essayer d'utiliser le code
4. ❌ Message: "Le code a expiré. Veuillez demander un nouveau code."
5. Cliquer "Renvoyer le code"
6. ✅ Nouveau code envoyé

### Test 3: Code incorrect

1. Demander un code
2. Entrer un code incorrect: `999999`
3. ❌ Message: "Code incorrect. Veuillez réessayer."
4. Entrer le bon code
5. ✅ Validation réussie

### Test 4: Mot de passe faible

1. Demander un code
2. Entrer le code correct
3. Entrer mot de passe faible: `test`
4. ❌ Message: "Le mot de passe doit contenir au moins 8 caractères"
5. Entrer mot de passe sans majuscule: `test1234`
6. ❌ Message: "Le mot de passe doit contenir au moins une majuscule"
7. Entrer mot de passe valide: `Test1234`
8. ✅ Validation réussie

---

## 📊 Comparaison: Ancien vs Nouveau système

### Ancien système (avec lien)

```
1. Utilisateur entre email
2. Reçoit email avec lien
3. Clique sur le lien
4. Entre nouveau mot de passe
```

**Problèmes**:
- ❌ Lien peut être intercepté
- ❌ Lien peut être partagé
- ❌ Pas de contrôle sur l'expiration
- ❌ Nécessite clic sur lien externe

### Nouveau système (avec code)

```
1. Utilisateur entre email
2. Reçoit email avec code à 6 chiffres
3. Entre le code dans l'application
4. Entre nouveau mot de passe
```

**Avantages**:
- ✅ Code court et facile à copier
- ✅ Expiration visible (compte à rebours)
- ✅ Pas de lien externe
- ✅ Plus sécurisé (code à usage unique)
- ✅ Possibilité de renvoyer le code
- ✅ Meilleure UX (tout dans l'application)

---

## 🔒 Sécurité

### Mesures de sécurité implémentées

1. **Code aléatoire**: SecureRandom pour génération
2. **Expiration**: 10 minutes maximum
3. **Usage unique**: Code invalidé après utilisation
4. **Validation stricte**: Email, code, mot de passe
5. **Compte à rebours**: Utilisateur voit le temps restant
6. **Email sécurisé**: Gmail API avec OAuth2
7. **Hashage**: Mot de passe hashé avec BCrypt

### Bonnes pratiques

- ✅ Ne jamais afficher le code dans les logs en production
- ✅ Limiter le nombre de tentatives (à implémenter)
- ✅ Envoyer notification si réinitialisation non demandée
- ✅ Invalider tous les tokens/codes après changement
- ✅ Forcer déconnexion de toutes les sessions

---

## 🎓 Pour la présentation au jury

### Démonstration (2 minutes)

1. **Montrer la page de connexion** (10 sec)
   - Cliquer sur "Mot de passe oublié ?"

2. **Étape 1: Demander le code** (30 sec)
   - Entrer un email
   - Cliquer "Envoyer le code"
   - Montrer le message de succès
   - Montrer l'email reçu avec le code

3. **Étape 2: Réinitialiser** (1 min)
   - Montrer le compte à rebours
   - Entrer le code
   - Entrer nouveau mot de passe
   - Montrer la validation
   - Cliquer "Réinitialiser"
   - Montrer le message de succès
   - Redirection automatique

4. **Tester la connexion** (20 sec)
   - Se connecter avec le nouveau mot de passe
   - ✅ Connexion réussie!

### Points clés à mentionner

- ✅ Code à 6 chiffres (facile à copier)
- ✅ Expiration 10 minutes (sécurité)
- ✅ Compte à rebours visuel (UX)
- ✅ Email HTML professionnel
- ✅ Validation complète
- ✅ Possibilité de renvoyer le code

---

## 📝 Résumé

**Avant**: Système avec lien de réinitialisation

**Après**: Système avec code de vérification à 6 chiffres

**Avantages**:
- ✅ Plus sécurisé
- ✅ Meilleure UX
- ✅ Pas de lien externe
- ✅ Expiration visible
- ✅ Code facile à copier

**Fichiers créés**: 4
- `forgot_password.fxml` - Interface
- `ForgotPasswordController.java` - Logique
- `compile-forgot-password.bat` - Compilation
- `GUIDE_MOT_DE_PASSE_CODE.md` - Ce guide

**Résultat**: Système moderne et sécurisé de réinitialisation de mot de passe avec code de vérification par email.

---

**Date**: 28 Février 2026  
**Projet**: GreenLedger  
**Auteur**: Ibrahim Imajid

**Votre système de mot de passe oublié est maintenant moderne et sécurisé! 🔐**
