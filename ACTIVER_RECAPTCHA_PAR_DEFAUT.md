# ✅ ACTIVER GOOGLE reCAPTCHA PAR DÉFAUT

## 🎯 OBJECTIF

Utiliser le Google reCAPTCHA ("Je ne suis pas un robot") au lieu de l'équation mathématique simple.

## ✅ MODIFICATIONS APPLIQUÉES

### 1. Fichier FXML (Interface)
**Fichier**: `src/main/resources/fxml/login_with_captcha_choice.fxml`

**Changement**: Déplacer `selected="true"` du CAPTCHA mathématique vers reCAPTCHA

```xml
<!-- AVANT -->
<RadioButton fx:id="mathCaptchaRadio" text="Équation" 
             selected="true"/>  ← Équation par défaut
<RadioButton fx:id="recaptchaRadio" text="reCAPTCHA"/>

<!-- APRÈS -->
<RadioButton fx:id="mathCaptchaRadio" text="Équation"/>
<RadioButton fx:id="recaptchaRadio" text="reCAPTCHA" 
             selected="true"/>  ← reCAPTCHA par défaut
```

### 2. Fichier Java (Contrôleur)
**Fichier**: `src/main/java/Controllers/LoginWithCaptchaChoiceController.java`

**Changement**: Initialiser avec reCAPTCHA au lieu de CAPTCHA mathématique

```java
// AVANT
// Initialiser avec CAPTCHA mathématique par défaut
setupMathCaptcha();

// APRÈS
// Initialiser avec Google reCAPTCHA par défaut (plus professionnel)
recaptchaRadio.setSelected(true);
switchCaptchaMethod(null);
```

## 🔑 VÉRIFIER VOS CLÉS reCAPTCHA

Vos clés sont déjà configurées dans `src/main/resources/config.properties`:

```properties
captcha.site.key=6LdGL3ssAAAAAN07BLFPjU-qHkztb9YJ3GhRfU-z
captcha.secret=6LdGL3ssAAAAALlX9Qt7jkMDZXYEZX5HZmHz1x70
```

**IMPORTANT**: Si ces clés ne fonctionnent pas, obtenez vos propres clés sur:
https://www.google.com/recaptcha/admin/create

## 🚀 COMPILATION MANUELLE

Si le script de compilation échoue, copiez manuellement les fichiers:

### Étape 1: Copier le FXML
```bash
copy src\main\resources\fxml\login_with_captcha_choice.fxml target\classes\fxml\
```

### Étape 2: Compiler le contrôleur
Le contrôleur a déjà été modifié. Si vous avez des erreurs de compilation, utilisez l'ancienne version compilée qui fonctionne.

## 📋 ALTERNATIVE SIMPLE

Si la compilation pose problème, vous pouvez simplement:

1. **Lancer l'application** avec la version actuelle
2. **Sur la page de login**, sélectionner manuellement "reCAPTCHA" au lieu d'"Équation"
3. Le reCAPTCHA Google apparaîtra avec la case "Je ne suis pas un robot"

## 🎯 RÉSULTAT ATTENDU

Quand vous lancez l'application:

### AVANT:
```
┌─────────────────────────────┐
│ Méthode:                    │
│ ⦿ Équation  ○ reCAPTCHA  ○ Puzzle │
│                             │
│ Combien fait 5 + 3 ?        │
│ [_____]                     │
└─────────────────────────────┘
```

### APRÈS:
```
┌─────────────────────────────┐
│ Méthode:                    │
│ ○ Équation  ⦿ reCAPTCHA  ○ Puzzle │
│                             │
│ ☑ Je ne suis pas un robot   │
│                             │
└─────────────────────────────┘
```

## 🔧 SI VOUS VOULEZ UTILISER UNIQUEMENT reCAPTCHA

Si vous voulez **supprimer les autres options** et garder seulement reCAPTCHA:

1. Ouvrir `src/main/resources/fxml/login_with_captcha_choice.fxml`
2. Supprimer ou commenter les lignes des radio buttons "Équation" et "Puzzle"
3. Garder seulement le radio button reCAPTCHA

## 📝 NOTES

- Les 3 méthodes CAPTCHA restent disponibles (Équation, reCAPTCHA, Puzzle)
- L'utilisateur peut toujours changer de méthode
- reCAPTCHA est maintenant sélectionné par défaut
- Plus professionnel et reconnu par les utilisateurs

## ✅ AVANTAGES DU reCAPTCHA

- ✅ Reconnu mondialement ("Je ne suis pas un robot")
- ✅ Plus difficile à contourner pour les bots
- ✅ Interface familière pour les utilisateurs
- ✅ Maintenu par Google (toujours à jour)
- ✅ Analyse comportementale avancée

---

**Pour tester**: Lancez `run.bat` et vérifiez que reCAPTCHA est sélectionné par défaut!
