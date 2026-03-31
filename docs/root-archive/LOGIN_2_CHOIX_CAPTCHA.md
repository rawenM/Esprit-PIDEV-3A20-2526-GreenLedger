# Page de Login avec 2 Choix de CAPTCHA

## 📋 Résumé

La page de login affiche maintenant **2 boutons de choix** pour la vérification:

1. **🔒 Google reCAPTCHA** - API externe (avec bouton bypass pour démo)
2. **🧩 Captcha Puzzle** - Développement interne (rapide, local, sans API)

## ✅ Avantages

### Google reCAPTCHA
- ✅ Professionnel et reconnu
- ✅ Sécurité maximale
- ✅ Bouton "Bypass (démo)" pour présentation jury
- ⚠️ Peut être lent dans JavaFX WebView

### Captcha Puzzle
- ✅ Rapide et fluide
- ✅ Pas de dépendance API externe
- ✅ Développement interne (montre vos compétences)
- ✅ Expérience utilisateur agréable

## 🎯 Utilisation

### Pour l'utilisateur final:
1. Entrer email et mot de passe
2. Cliquer sur un des 2 boutons de choix
3. Compléter la vérification choisie
4. Cliquer sur "Se connecter"

### Pour la démo jury:
- Si reCAPTCHA est lent → cliquer sur "Bypass (démo)"
- Ou utiliser directement le Captcha Puzzle (rapide)

## 📁 Fichiers modifiés

- `src/main/resources/fxml/login.fxml` - Interface avec 2 boutons de choix
- `src/main/java/Controllers/LoginController.java` - Logique des 2 captchas
- `compile-login-2-choix.bat` - Script de compilation

## 🚀 Compilation

```bash
./compile-login-2-choix.bat
```

## 🎨 Interface

```
┌─────────────────────────────────────┐
│         Connexion                   │
│                                     │
│  Email: [________________]          │
│  Mot de passe: [________]           │
│                                     │
│  Méthode de vérification            │
│  ┌──────────────┐ ┌──────────────┐ │
│  │ 🔒 Google    │ │ 🧩 Captcha   │ │
│  │  reCAPTCHA   │ │   Puzzle     │ │
│  └──────────────┘ └──────────────┘ │
│                                     │
│  [Captcha s'affiche ici]            │
│                                     │
│  [Se connecter]                     │
└─────────────────────────────────────┘
```

## 🔧 Fonctionnement technique

### Choix reCAPTCHA:
1. Affiche WebView avec Google reCAPTCHA
2. Bouton "Bypass (démo)" visible
3. Vérifie le token avec l'API Google

### Choix Puzzle:
1. Génère une image avec un trou
2. Affiche un slider pour positionner la pièce
3. Vérifie la position localement (pas d'API)

## 📝 Notes

- Les 2 méthodes sont **également valides**
- L'utilisateur choisit selon sa préférence
- Le bypass reCAPTCHA est **uniquement pour démo**
- Le puzzle est **production-ready**

## ✨ Présentation jury

**Phrase à dire:**
> "Notre plateforme offre 2 méthodes de vérification CAPTCHA au choix de l'utilisateur: Google reCAPTCHA pour la sécurité maximale, et notre Captcha Puzzle développé en interne pour une expérience utilisateur optimale sans dépendance externe."

---

**Date:** 2025-03-03  
**Statut:** ✅ Implémenté et testé
