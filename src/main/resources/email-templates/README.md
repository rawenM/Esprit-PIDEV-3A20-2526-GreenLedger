# Templates d'emails

Ce dossier contient les templates HTML pour les emails transactionnels.

## Structure

Les templates sont actuellement intégrés dans `GmailApiService.java`.

Pour personnaliser les templates, modifiez les méthodes suivantes :

- `buildWelcomeEmailHtml()` - Email de bienvenue
- `buildVerificationEmailHtml()` - Email de vérification
- `buildResetPasswordEmailHtml()` - Email de réinitialisation
- `buildAccountApprovedEmailHtml()` - Email d'approbation
- `buildAccountRejectedEmailHtml()` - Email de rejet
- `buildAccountBlockedEmailHtml()` - Email de blocage
- `buildAccountUnblockedEmailHtml()` - Email de déblocage

## Personnalisation

### Couleurs

Les couleurs sont définies dans chaque méthode de template :

```java
// Vert pour succès
"<h2 style='color: #2ecc71;'>Bienvenue ! 🌱</h2>"

// Bleu pour information
"<h2 style='color: #3498db;'>Vérifiez votre compte</h2>"

// Rouge pour erreur/alerte
"<h2 style='color: #e74c3c;'>Réinitialisation de mot de passe</h2>"
```

### Logo

Pour ajouter un logo, modifiez les templates dans `GmailApiService.java` :

```java
"<div style='text-align: center; margin-bottom: 20px;'>" +
"<img src='https://votre-domaine.com/logo.png' alt='GreenLedger' style='height: 50px;'/>" +
"</div>" +
```

### Footer

Modifiez le footer dans chaque template :

```java
"<p style='font-size: 12px; color: #999; margin-top: 20px;'>" +
"© 2024 GreenLedger - Tous droits réservés<br>" +
"<a href='https://greenledger.com'>greenledger.com</a>" +
"</p>"
```

## Prévisualisation

Pour prévisualiser les templates :

1. Exécutez `TestGmailApi.java`
2. Modifiez l'adresse email de test par la vôtre
3. Vérifiez votre boîte de réception

## Exemples

Les fichiers `example-*.html` dans ce dossier sont des exemples de templates que vous pouvez utiliser comme référence.
