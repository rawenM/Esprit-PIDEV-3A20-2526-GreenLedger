# Environment Variables Setup Guide

## Overview
This project requires several API keys and configuration values to function properly. All sensitive information should be stored in environment variables.

## Setup Instructions

### Option 1: Using .env file (Recommended for local development)

1. Copy the example file:
   ```bash
   cp .env.example .env
   ```

2. Edit the `.env` file and replace all placeholder values with your actual credentials:

### Option 2: IntelliJ IDEA Run Configuration

1. Open IntelliJ IDEA
2. Go to Run → Edit Configurations
3. Select "GreenLedger" configuration (or create a new one)
4. Add environment variables in the "Environment variables" field
5. Fill in the values listed below

### Option 3: System Environment Variables

Set the following environment variables in your operating system.

## Required Environment Variables

### API Keys

- **CARBON_API_KEY**: Your Carbon API key for carbon footprint calculations
  - Get it from: [Carbon Interface](https://www.carboninterface.com/)

- **OPENWEATHERMAP_API_KEY**: Your OpenWeatherMap API key
  - Get it from: [OpenWeatherMap API](https://openweathermap.org/api)

- **PDFREST_API_KEY**: Your PDFRest API key for PDF processing
  - Get it from: [PDFRest](https://pdfrest.com/)

- **ADOBE_CLIENT_ID**: Adobe PDF Services client ID
  - Get it from: [Adobe Developer Console](https://developer.adobe.com/)

- **ADOBE_CLIENT_SECRET**: Adobe PDF Services client secret
  - Get it from: [Adobe Developer Console](https://developer.adobe.com/)

- **ADOBE_ORG_ID**: Your Adobe organization ID
  - Format: `YOUR_ORG_ID@AdobeOrg`

- **ADOBE_ACCESS_TOKEN**: Adobe access token (optional, can be generated at runtime)

### reCAPTCHA Configuration

- **RECAPTCHA_SITE_KEY**: Your reCAPTCHA v2 site key
  - Get it from: [Google reCAPTCHA Admin](https://www.google.com/recaptcha/admin)

- **RECAPTCHA_SECRET_KEY**: Your reCAPTCHA v2 secret key
  - Get it from: [Google reCAPTCHA Admin](https://www.google.com/recaptcha/admin)

### Email (SMTP) Configuration

- **SMTP_HOST**: SMTP server hostname (e.g., `smtp.gmail.com`)
- **SMTP_PORT**: SMTP server port (usually `587` for TLS)
- **SMTP_USERNAME**: Your email address
- **SMTP_PASSWORD**: Your email app password
  - For Gmail: [Create an App Password](https://support.google.com/accounts/answer/185833)
- **SMTP_FROM**: Sender name and email (e.g., `GreenLedger Team <your_email@gmail.com>`)
- **SMTP_AUTH**: Set to `true`
- **SMTP_STARTTLS**: Set to `true`

### Other Configuration

- **APP_RESET_URL_PREFIX**: Password reset URL prefix (default: `http://127.0.0.1:8088/reset?token=`)
- **OPENAI_API_URL**: OpenAI API URL (default: `https://api.openai.com/v1`)

## Security Notes

⚠️ **IMPORTANT**: 
- Never commit your `.env` file to version control
- Never commit API keys or passwords to git
- The `.env` file is already in `.gitignore` to prevent accidental commits
- Use `.env.example` as a template only - it contains placeholders, not real credentials

## Troubleshooting

If you encounter authentication errors:
1. Verify all API keys are correctly set
2. Check that API keys are active and have not expired
3. Ensure you have sufficient quota/credits for paid APIs
4. For Adobe services, you may need to refresh the access token

## Support

For questions about obtaining API keys, refer to each service's documentation:
- [Carbon Interface Documentation](https://docs.carboninterface.com/)
- [OpenWeatherMap Documentation](https://openweathermap.org/guide)
- [Adobe PDF Services Documentation](https://developer.adobe.com/document-services/docs/overview/pdf-services-api/)
- [Google reCAPTCHA Documentation](https://developers.google.com/recaptcha/intro)
