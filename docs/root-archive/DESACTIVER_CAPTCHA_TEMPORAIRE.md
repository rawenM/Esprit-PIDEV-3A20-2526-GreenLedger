# 🔓 DÉSACTIVER LE CAPTCHA TEMPORAIREMENT

## 🎯 SOLUTION RAPIDE

Pour désactiver le CAPTCHA et te connecter facilement pendant les tests:

### Méthode 1: Utiliser le Bouton Bypass (RECOMMANDÉ)

1. Lance l'application: `run.bat`
2. Sur la page de login, cherche le bouton **"Bypass (temp)"**
3. Clique dessus AVANT de te connecter
4. Entre ton email et mot de passe
5. Clique sur "Se connecter"

Le CAPTCHA sera ignoré pour cette connexion.

---

### Méthode 2: Modifier le Code (Pour Tests Prolongés)

Si tu veux désactiver complètement le CAPTCHA:

#### Étape 1: Modifier LoginController.java

Trouve cette ligne (vers ligne 73):
```java
private void setupCaptcha() {
    captchaBypassed = false;
```

Change en:
```java
private void setupCaptcha() {
    captchaBypassed = true;  // ← Change false en true
```

#### Étape 2: Recompiler

Option A - Avec ton IDE:
- Ouvre le projet dans IntelliJ/Eclipse/NetBeans
- Rebuild le projet

Option B - Sans IDE:
- Supprime le fichier compilé:
  ```
  del target\classes\Controllers\LoginController.class
  ```
- L'application utilisera l'ancienne version (sans audit log)

---

## ⚠️ IMPORTANT

### Pour la Présentation au Jury

**RÉACTIVE LE CAPTCHA** avant la présentation:
1. Remets `captchaBypassed = false;`
2. Recompile
3. Teste que le reCAPTCHA fonctionne

Le CAPTCHA est une **fonctionnalité avancée** à montrer au jury!

---

## 🔍 COMPRENDRE LE CAPTCHA

### Ce Que Tu As Actuellement

Ton application a **Google reCAPTCHA v2** (le "Je ne suis pas un robot" avec images):
- ✅ Intégré et fonctionnel
- ✅ Clés configurées dans `config.properties`
- ✅ Serveur local pour charger le CAPTCHA
- ✅ Fallback vers CAPTCHA mathématique si problème

### Les 3 Types de CAPTCHA Disponibles

1. **Google reCAPTCHA** (actuel)
   - Case "Je ne suis pas un robot"
   - Sélection d'images si nécessaire
   - Le plus professionnel

2. **CAPTCHA Mathématique** (fallback)
   - Question simple: "Combien fait 8 + 6 ?"
   - S'active si reCAPTCHA ne charge pas

3. **CAPTCHA Puzzle** (disponible mais non utilisé)
   - Glisser une pièce de puzzle
   - Dans `PuzzleCaptchaService.java`

---

## 🚀 APRÈS LES TESTS

Une fois tes tests terminés:

1. **Réactive le CAPTCHA**:
   ```java
   captchaBypassed = false;
   ```

2. **Teste que ça marche**:
   - Lance l'application
   - Le reCAPTCHA doit apparaître
   - Coche "Je ne suis pas un robot"
   - Connecte-toi

3. **Pour le jury**, explique:
   - "J'ai intégré Google reCAPTCHA pour la sécurité"
   - "Il y a un fallback vers CAPTCHA mathématique"
   - "C'est une API externe qui protège contre les bots"

---

## 📊 STATUT ACTUEL

- ✅ reCAPTCHA configuré et fonctionnel
- ✅ Clés Google valides
- ✅ Serveur local opérationnel
- ✅ Fallback mathématique disponible
- ⚠️ Bypass activé pour tests (à désactiver avant présentation)

---

**Date**: 2025-03-02  
**Pour**: Tests et Développement  
**Rappel**: Réactiver avant la présentation jury!
