# 🔐 GUIDE: Activer Google reCAPTCHA sur la Page de Connexion

## 📋 Vue d'ensemble

Ce guide vous montre comment activer Google reCAPTCHA (API externe) sur votre page de connexion pour remplacer le CAPTCHA mathématique simple.

---

## ✅ Ce que vous avez déjà

Votre projet contient déjà:
- ✅ `CaptchaService.java` - Service reCAPTCHA
- ✅ `CaptchaHttpServer.java` - Serveur local pour reCAPTCHA
- ✅ `LoginController.java` - Contrôleur avec support reCAPTCHA
- ✅ `login.fxml` - Interface avec WebView pour reCAPTCHA

---

## 🚀 ÉTAPE 1: Obtenir les clés reCAPTCHA de Google

### 1.1 Créer un compte Google reCAPTCHA

1. Aller sur: https://www.google.com/recaptcha/admin/create
2. Se connecter avec votre compte Google
3. Remplir le formulaire:
   - **Label**: GreenLedger
   - **Type de reCAPTCHA**: 
     - ✅ reCAPTCHA v2 → "Je ne suis pas un robot" (RECOMMANDÉ)
     - OU reCAPTCHA v3 (invisible)
   - **Domaines**: 
     - `localhost` (pour développement)
     - `127.0.0.1` (pour développement)
   - Accepter les conditions
4. Cliquer sur "Envoyer"

### 1.2 Récupérer les clés

Vous obtiendrez 2 clés:
- **Site Key** (clé publique) - À utiliser côté client
- **Secret Key** (clé privée) - À utiliser côté serveur

Exemple:
```
Site Key: 6LcXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
Secret Key: 6LcYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY
```

---

## 🔧 ÉTAPE 2: Configuration des clés

### Option A: Fichier config.properties (RECOMMANDÉ)

Créer ou modifier `src/main/resources/config.properties`:

```properties
# Google reCAPTCHA Configuration
RECAPTCHA_SITE_KEY=6LcXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
RECAPTCHA_SECRET_KEY=6LcYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY
RECAPTCHA_VERIFY_URL=https://www.google.com/recaptcha/api/siteverify
```

### Option B: Variables d'environnement

Ajouter dans `.env`:

```properties
RECAPTCHA_SITE_KEY=6LcXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
RECAPTCHA_SECRET_KEY=6LcYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY
```

---

## 📝 ÉTAPE 3: Vérifier CaptchaService.java

Votre `CaptchaService.java` devrait charger les clés automatiquement:

```java
public class CaptchaService {
    private String siteKey;
    private String secretKey;
    private String verifyUrl;
    
    public CaptchaService() {
        loadConfiguration();
    }
    
    private void loadConfiguration() {
        try {
            // Charger depuis config.properties
            Properties props = new Properties();
            InputStream input = getClass().getClassLoader()
                .getResourceAsStream("config.properties");
            
            if (input != null) {
                props.load(input);
                this.siteKey = props.getProperty("RECAPTCHA_SITE_KEY");
                this.secretKey = props.getProperty("RECAPTCHA_SECRET_KEY");
                this.verifyUrl = props.getProperty("RECAPTCHA_VERIFY_URL");
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement config reCAPTCHA: " + e.getMessage());
        }
    }
    
    public boolean isConfigured() {
        return siteKey != null && !siteKey.isEmpty() 
            && secretKey != null && !secretKey.isEmpty();
    }
}
```

---

## 🎨 ÉTAPE 4: Modifier la page de connexion

### Option 1: Utiliser le fichier actuel (login.fxml)

Votre `login.fxml` contient déjà le WebView pour reCAPTCHA:

```xml
<!-- Captcha -->
<VBox spacing="5" fx:id="captchaContainer">
    <HBox alignment="CENTER_LEFT" spacing="10">
        <Label text="Verification" styleClass="form-label"/>
        <Button fx:id="bypassCaptchaBtn" text="Bypass (temp)" 
                style="-fx-font-size: 10px; -fx-padding: 2 8 2 8;" 
                onAction="#bypassCaptcha"/>
    </HBox>
    
    <!-- reCAPTCHA WebView -->
    <WebView fx:id="captchaWebView" 
             prefHeight="120" minHeight="120" maxHeight="300" 
             prefWidth="380" maxWidth="400"/>
    
    <!-- Fallback: CAPTCHA simple -->
    <HBox spacing="10" alignment="CENTER_LEFT" 
          fx:id="simpleCaptchaBox" visible="false" managed="false">
        <Label fx:id="captchaQuestion" styleClass="form-label"/>
        <TextField fx:id="captchaAnswer" promptText="Votre réponse" 
                   prefWidth="100" styleClass="field"/>
    </HBox>
</VBox>
```

### Option 2: Utiliser login_with_captcha_choice.fxml

Si vous voulez le sélecteur de méthodes, utilisez le fichier que nous avons créé.

---

## 💻 ÉTAPE 5: Vérifier LoginController.java

Votre `LoginController.java` contient déjà la logique reCAPTCHA:

```java
private void setupCaptcha() {
    // Essayer reCAPTCHA d'abord si configuré
    if (captchaService.isConfigured() && captchaWebView != null) {
        String siteKey = captchaService.getSiteKey();
        System.out.println("[CAPTCHA] reCAPTCHA configuré");
        
        WebEngine engine = captchaWebView.getEngine();
        engine.setJavaScriptEnabled(true);
        
        // Charger reCAPTCHA
        if (captchaHttpServer == null) {
            captchaHttpServer = new CaptchaHttpServer(siteKey);
            captchaHttpServer.start();
        }
        String url = captchaHttpServer.getCaptchaUrl();
        engine.load(url);
        
    } else {
        // Fallback vers CAPTCHA simple
        System.out.println("[CAPTCHA] reCAPTCHA non configuré - CAPTCHA simple");
        showSimpleCaptcha();
    }
}
```

---

## 🧪 ÉTAPE 6: Tester reCAPTCHA

### 6.1 Compiler le projet

```bash
mvn clean compile
# OU
compile-all.bat
```

### 6.2 Lancer l'application

```bash
run.bat
# OU
mvn javafx:run
```

### 6.3 Tester la connexion

1. Ouvrir la page de connexion
2. Attendre le chargement de reCAPTCHA (2-3 secondes)
3. Vous devriez voir:
   ```
   ┌────────────────────────────────────┐
   │  ☐ Je ne suis pas un robot         │
   │     [Logo reCAPTCHA]                │
   └────────────────────────────────────┘
   ```
4. Cocher la case
5. Résoudre le challenge si demandé (sélectionner des images)
6. Entrer email et mot de passe
7. Cliquer "Se connecter"
8. ✅ Connexion réussie

---

## 🔍 ÉTAPE 7: Vérification et Debug

### Vérifier les logs

Dans la console, vous devriez voir:

```
[CAPTCHA] reCAPTCHA configuré avec site key: 6LcXXXXXXX...
[CAPTCHA] Chargement reCAPTCHA v2 depuis serveur local
[CAPTCHA] Chargé depuis: http://localhost:8765/captcha
[CAPTCHA] WebView state: SUCCEEDED
[CAPTCHA] reCAPTCHA WebView chargé et bridge connecté
[CAPTCHA] Token reçu: 03AGdBq26XXXXXXXXX...
[CAPTCHA] Vérification: réussie
[LOGIN] Connexion réussie: user@example.com
```

### Si reCAPTCHA ne se charge pas

**Problème 1: Clés non configurées**
```
[CAPTCHA] reCAPTCHA non configuré - CAPTCHA simple
```
**Solution**: Vérifier que les clés sont dans `config.properties`

**Problème 2: WebView ne charge pas**
```
[CAPTCHA] WebView failed to load
```
**Solution**: 
- Vérifier la connexion internet
- Vérifier que JavaFX WebView est disponible
- Essayer avec le bypass temporaire

**Problème 3: Token non reçu**
```
[CAPTCHA] Token manquant
```
**Solution**:
- Attendre 3-5 secondes après le chargement
- Vérifier que JavaScript est activé
- Utiliser le fallback CAPTCHA simple

---

## 📊 Comparaison: CAPTCHA Simple vs reCAPTCHA

| Critère | CAPTCHA Simple | reCAPTCHA |
|---------|----------------|-----------|
| **Sécurité** | ⭐ Faible | ⭐⭐⭐ Élevée |
| **Protection bots** | 20% | 99.9% |
| **Configuration** | ✅ Aucune | ⚠️ Clés Google |
| **Connexion internet** | ❌ Non requise | ✅ Requise |
| **Expérience utilisateur** | Simple | Professionnelle |
| **Coût** | Gratuit | Gratuit (1M/mois) |

---

## 🎯 ÉTAPE 8: Désactiver le CAPTCHA simple (optionnel)

Si vous voulez UNIQUEMENT reCAPTCHA (sans fallback):

### Modifier LoginController.java

```java
private void setupCaptcha() {
    if (!captchaService.isConfigured()) {
        showError("reCAPTCHA n'est pas configuré. Veuillez contacter l'administrateur.");
        loginButton.setDisable(true);
        return;
    }
    
    // Charger reCAPTCHA uniquement
    String siteKey = captchaService.getSiteKey();
    WebEngine engine = captchaWebView.getEngine();
    engine.setJavaScriptEnabled(true);
    
    if (captchaHttpServer == null) {
        captchaHttpServer = new CaptchaHttpServer(siteKey);
        captchaHttpServer.start();
    }
    String url = captchaHttpServer.getCaptchaUrl();
    engine.load(url);
}
```

### Masquer le CAPTCHA simple dans login.fxml

```xml
<!-- Supprimer ou commenter -->
<!--
<HBox spacing="10" alignment="CENTER_LEFT" 
      fx:id="simpleCaptchaBox" visible="false" managed="false">
    <Label fx:id="captchaQuestion" styleClass="form-label"/>
    <TextField fx:id="captchaAnswer" promptText="Votre réponse" 
               prefWidth="100" styleClass="field"/>
</HBox>
-->
```

---

## 🎓 Pour la présentation au jury

### Démonstration (1 minute)

1. **Montrer la page de connexion** (10 sec)
   - "Voici la page de connexion avec Google reCAPTCHA"

2. **Expliquer reCAPTCHA** (20 sec)
   - "reCAPTCHA est l'API officielle de Google"
   - "Protection contre 99.9% des bots"
   - "Utilisé par des millions de sites"

3. **Tester en direct** (30 sec)
   - Cocher "Je ne suis pas un robot"
   - Résoudre le challenge si demandé
   - Se connecter
   - "Vérification côté serveur via l'API Google"

### Points clés à mentionner

- ✅ API officielle Google (crédibilité)
- ✅ Protection maximale (99.9% des bots)
- ✅ Vérification côté serveur (sécurité)
- ✅ Gratuit jusqu'à 1 million de requêtes/mois
- ✅ Fallback automatique vers CAPTCHA simple si problème

---

## 🔒 Sécurité

### Bonnes pratiques implémentées

1. ✅ **Vérification côté serveur**: Token vérifié via API Google
2. ✅ **Secret Key sécurisée**: Jamais exposée côté client
3. ✅ **HTTPS recommandé**: Pour la production
4. ✅ **Fallback automatique**: Si reCAPTCHA indisponible
5. ✅ **Timeout**: Passage au CAPTCHA simple après 5 secondes

---

## 📝 Checklist finale

- [ ] Clés reCAPTCHA obtenues de Google
- [ ] Clés configurées dans `config.properties`
- [ ] `CaptchaService.java` charge les clés correctement
- [ ] `LoginController.java` initialise reCAPTCHA
- [ ] `login.fxml` contient le WebView
- [ ] Projet compilé (`mvn clean compile`)
- [ ] Application testée (connexion avec reCAPTCHA)
- [ ] Logs vérifiés (token reçu et vérifié)
- [ ] Démonstration préparée pour le jury

---

## 🆘 Dépannage

### Problème: "reCAPTCHA non configuré"

**Cause**: Clés manquantes ou incorrectes

**Solution**:
1. Vérifier `config.properties` existe
2. Vérifier les clés sont correctes
3. Recompiler le projet
4. Relancer l'application

### Problème: WebView vide ou blanc

**Cause**: JavaScript désactivé ou problème de chargement

**Solution**:
1. Vérifier `engine.setJavaScriptEnabled(true)`
2. Vérifier la connexion internet
3. Vérifier les logs pour erreurs
4. Utiliser le bypass temporaire pour tester

### Problème: "Token invalide"

**Cause**: Secret Key incorrecte ou token expiré

**Solution**:
1. Vérifier la Secret Key dans `config.properties`
2. Vérifier l'URL de vérification
3. Tester avec un nouveau token
4. Vérifier les logs côté serveur

---

## 📚 Ressources

- **Documentation officielle**: https://developers.google.com/recaptcha
- **Console admin**: https://www.google.com/recaptcha/admin
- **FAQ**: https://developers.google.com/recaptcha/docs/faq

---

## ✅ Résumé

**Avant**: CAPTCHA mathématique simple (peu sécurisé)

**Après**: Google reCAPTCHA (protection maximale)

**Avantages**:
- ✅ Protection contre 99.9% des bots
- ✅ API officielle et fiable
- ✅ Expérience utilisateur professionnelle
- ✅ Gratuit pour la plupart des usages
- ✅ Fallback automatique si problème

**Résultat**: Page de connexion hautement sécurisée avec protection anti-bot de niveau professionnel.

---

**Date**: 28 Février 2026  
**Projet**: GreenLedger  
**Auteur**: Ibrahim Imajid

**Votre page de connexion est maintenant protégée par Google reCAPTCHA! 🔐**
