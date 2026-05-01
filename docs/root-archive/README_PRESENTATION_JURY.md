# 🍃 GREENLEDGER - PRÉSENTATION JURY

## 🚀 DÉMARRAGE RAPIDE

### Option 1: Menu Interactif (RECOMMANDÉ)
```bash
COMMANDES_PRESENTATION.bat
```
Ce menu vous permet de:
- Lancer l'application
- Ouvrir tous les guides
- Vérifier la checklist
- Mettre à jour les ressources

### Option 2: Lancement Direct
```bash
run.bat
```

---

## 📚 DOCUMENTS ESSENTIELS

### 🎯 Pour la Présentation
1. **LANCER_PRESENTATION.txt** - Guide ultra-rapide (COMMENCEZ ICI)
2. **GUIDE_PRESENTATION_JURY.md** - Guide complet détaillé
3. **PHRASES_JURY_EXACTES.txt** - Phrases exactes à dire + réponses aux questions
4. **RESUME_VISUEL_PROJET.txt** - Résumé visuel avec statistiques

### 📋 Pour la Préparation
- **RETOUCHES_FINALES_COMPLETEES.md** - Toutes les fonctionnalités
- **CHECKLIST_PRESENTATION.txt** - Checklist rapide

---

## 🎬 SCÉNARIO DE DÉMONSTRATION (5 MINUTES)

| Temps | Étape | Action |
|-------|-------|--------|
| 30s | 1. Connexion | Montrer reCAPTCHA "Je ne suis pas un robot" |
| 45s | 2. Dashboard | Statistiques en temps réel + fraude |
| 90s | 3. Fraude IA ⭐ | Cliquer 📊 "Détails" - Montrer les 10 indicateurs |
| 60s | 4. Chart.js | Cliquer "📊 Statistiques" - 4 graphiques interactifs |
| 90s | 5. Mot de passe | Code à 6 chiffres + timer + email |
| 30s | 6. Gestion | Filtres + actions (✓ ⛔ 🗑 📊) |

---

## 🔌 5 APIS INTÉGRÉES

1. **Gmail API** ⭐⭐⭐⭐⭐ - OAuth2, envoi sécurisé
2. **Google reCAPTCHA** ⭐⭐⭐⭐⭐ - Protection anti-bot
3. **Chart.js** ⭐⭐⭐⭐⭐ - 4 graphiques interactifs
4. **Détection Fraude IA** ⭐⭐⭐⭐⭐ - 10 indicateurs analysés
5. **MySQL** ⭐⭐⭐⭐⭐ - Base de données relationnelle

---

## 🎤 3 PHRASES CLÉS POUR LE JURY

### 1. Ouverture
> "J'ai développé GreenLedger avec un focus sur la sécurité et l'expérience utilisateur moderne. J'ai intégré 5 APIs dont un système de détection de fraude par intelligence artificielle."

### 2. Point Fort (Détection de Fraude)
> "Le système analyse automatiquement 10 indicateurs lors de chaque inscription: emails jetables, VPN, patterns suspects, vitesse de saisie anormale, etc. Le score final détermine si on bloque, vérifie, ou accepte l'utilisateur."

### 3. Conclusion
> "Cette gestion utilisateur combine sécurité moderne avec OAuth2 et reCAPTCHA, intelligence artificielle pour la détection de fraude, et visualisation professionnelle avec Chart.js. Le tout avec une architecture MVC propre."

---

## ✅ CHECKLIST AVANT PRÉSENTATION

### Technique
- [ ] MySQL démarré
- [ ] Navigateurs fermés (éviter conflit WebView)
- [ ] Application testée avec `run.bat`
- [ ] Email de test accessible

### Données
- [ ] Au moins 5 utilisateurs créés
- [ ] Au moins 2 avec fraude détectée (score > 75)
- [ ] Différents statuts (Actif, En Attente, Bloqué)
- [ ] Différents types (Investisseur, Porteur, Admin)

### Préparation
- [ ] Lu `GUIDE_PRESENTATION_JURY.md`
- [ ] Lu `PHRASES_JURY_EXACTES.txt`
- [ ] Testé le scénario de démonstration
- [ ] Préparé les réponses aux questions

---

## 🎯 POINTS FORTS À METTRE EN AVANT

### 1. Détection de Fraude IA ⭐⭐⭐⭐⭐ (LE PLUS IMPRESSIONNANT)
- 10 indicateurs analysés en temps réel
- Score automatique (0-100)
- Recommandations intelligentes
- Unique et innovant

### 2. Sécurité Moderne ⭐⭐⭐⭐⭐
- Gmail API avec OAuth2 (pas de SMTP basique)
- Google reCAPTCHA v2
- Codes temporaires avec expiration
- Bcrypt pour les mots de passe

### 3. Visualisation Professionnelle ⭐⭐⭐⭐⭐
- 4 graphiques Chart.js interactifs
- Données en temps réel depuis MySQL
- Interface moderne et intuitive

### 4. Expérience Utilisateur ⭐⭐⭐⭐⭐
- Code à 6 chiffres comme Google/Facebook
- Timer visuel avec compte à rebours
- Feedback en temps réel
- Navigation fluide

### 5. Architecture Propre ⭐⭐⭐⭐⭐
- MVC bien structuré
- Code modulaire et maintenable
- Séparation des responsabilités
- Bonnes pratiques Java

---

## 💬 RÉPONSES AUX QUESTIONS FRÉQUENTES

### "Pourquoi Gmail API et pas SMTP?"
> "Gmail API utilise OAuth2, ce qui est beaucoup plus sécurisé que SMTP qui nécessite un mot de passe en clair. De plus, Gmail API offre de meilleures garanties de délivrabilité et est utilisé par les applications professionnelles."

### "Comment fonctionne la détection de fraude?"
> "Le système analyse 10 indicateurs en temps réel. Chaque indicateur a un poids: email jetable +30 points, VPN +25 points, etc. Le score final détermine le niveau de risque. Si > 75, on recommande de bloquer. Entre 50-75, vérification manuelle. < 50, utilisateur sûr. Tout est automatique."

### "Pourquoi Chart.js?"
> "Chart.js est léger, gratuit, et offre des graphiques interactifs professionnels. Il est utilisé par des millions de sites web et s'intègre facilement avec JavaFX via WebView."

### "Le code à 6 chiffres est-il sécurisé?"
> "Oui: généré avec SecureRandom (cryptographiquement sûr), expire après 10 minutes, stocké uniquement en mémoire, un seul essai possible. C'est le même système utilisé par Google et Facebook."

---

## 🛠️ TECHNOLOGIES UTILISÉES

### Frontend
- JavaFX avec FXML
- CSS pour le styling
- WebView pour Chart.js et reCAPTCHA

### Backend
- Java 17
- Architecture MVC
- JDBC pour MySQL

### Base de Données
- MySQL 8.0
- Tables: user, fraud_detection_results

### APIs & Bibliothèques
- Gmail API (com.google.api-client)
- Google reCAPTCHA v2
- Chart.js 4.4.0
- Bcrypt pour le hashing
- JavaMail API

### Outils
- Maven pour les dépendances
- Git pour le versioning

---

## 📊 STATISTIQUES DU PROJET

- **Lignes de code:** 3000+ lignes
- **Fichiers créés:** 50+ fichiers
- **APIs intégrées:** 5 APIs modernes
- **Temps de développement:** [Votre estimation]
- **Fonctionnalités:** 5 fonctionnalités majeures

---

## 🚨 DÉPANNAGE RAPIDE

### L'application ne démarre pas
1. Vérifier que MySQL est démarré
2. Vérifier le fichier `.env`
3. Relancer avec `run.bat`

### reCAPTCHA ne s'affiche pas
1. Fermer tous les navigateurs
2. Relancer l'application
3. Vérifier `config.properties`

### Les graphiques ne s'affichent pas
1. Exécuter `update-resources.bat`
2. Vérifier que `user-statistics.html` existe dans `target/classes/charts/`
3. Relancer l'application

### Email non reçu
1. Vérifier le fichier `.env`
2. Vérifier que Gmail API est configuré
3. Vérifier les logs de l'application

---

## 📁 STRUCTURE DU PROJET

```
GreenLedger/
├── src/main/java/
│   ├── Controllers/          # Contrôleurs JavaFX
│   │   ├── AdminUsersController.java
│   │   ├── LoginController.java
│   │   ├── ForgotPasswordController.java
│   │   └── UserStatisticsController.java
│   ├── Models/               # Entités
│   │   ├── User.java
│   │   └── FraudDetectionResult.java
│   ├── Services/             # Services métier
│   │   ├── UserServiceImpl.java
│   │   └── FraudDetectionService.java
│   ├── DAO/                  # Accès aux données
│   │   ├── UserDAOImpl.java
│   │   └── FraudDetectionDAOImpl.java
│   └── Utils/                # Utilitaires
│       ├── GmailApiService.java
│       ├── CaptchaService.java
│       └── ChartDataService.java
├── src/main/resources/
│   ├── fxml/                 # Interfaces FXML
│   │   ├── admin_users.fxml
│   │   ├── login.fxml
│   │   ├── forgot_password.fxml
│   │   └── user_statistics.fxml
│   ├── charts/               # Graphiques HTML
│   │   └── user-statistics.html
│   ├── css/                  # Styles
│   └── email-templates/      # Templates emails
├── .env                      # Configuration Gmail API
├── config.properties         # Configuration reCAPTCHA
└── run.bat                   # Lancement application
```

---

## 🎓 POUR ALLER PLUS LOIN

### Améliorations Possibles
- Authentification à deux facteurs (2FA)
- Export des statistiques en PDF
- Notifications en temps réel
- API REST pour mobile
- Dashboard temps réel avec WebSocket

### Apprentissages
- Intégration d'APIs tierces
- Sécurité des applications
- Détection de fraude
- Visualisation de données
- Architecture MVC

---

## 📞 SUPPORT

### Documents à Consulter
1. `LANCER_PRESENTATION.txt` - Démarrage rapide
2. `GUIDE_PRESENTATION_JURY.md` - Guide complet
3. `PHRASES_JURY_EXACTES.txt` - Phrases et réponses
4. `RETOUCHES_FINALES_COMPLETEES.md` - Fonctionnalités

### En Cas de Problème
- Vérifier MySQL
- Vérifier `.env`
- Relancer `run.bat`
- Consulter les logs

---

## ✨ CONCLUSION

Votre application GreenLedger est prête pour la présentation au jury. Vous avez:

✅ 5 APIs modernes intégrées
✅ Système de détection de fraude unique
✅ Sécurité de niveau professionnel
✅ Interface moderne et intuitive
✅ Code propre et bien structuré

**Points forts:**
- Détection de fraude IA (10 indicateurs)
- Gmail API avec OAuth2
- Chart.js interactif
- Code à 6 chiffres moderne
- Architecture MVC propre

**Vous êtes prêt à impressionner le jury! 🚀**

---

## 🍀 BONNE CHANCE!

Lisez les documents, testez l'application, et présentez avec confiance. Vous avez créé quelque chose d'impressionnant!

---

*Dernière mise à jour: [Date actuelle]*
*Version: 1.0*
*Auteur: [Votre nom]*
