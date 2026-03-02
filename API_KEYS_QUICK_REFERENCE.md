# API Keys Configuration - Quick Reference

## The Fix in One Sentence
✅ Created `DotEnvLoader` to load environment variables from `.env`, fixed `ApiConfig` to check them first, and fixed `OpenAiAssistantService` to use the correct OpenAI endpoint format.

---

## Quick Status Check

### At Application Startup, You Should See:
```
[DotEnvLoader] Loaded .env from working directory: ...
[API CONFIG] Configuration loaded successfully
...
[API CONFIG] OpenAI API Key Present: true
[API CONFIG] OpenAI Enabled: true
[API CONFIG] Twilio SID Present: true
[API CONFIG] Twilio TOKEN Present: true
```

**All TRUE?** → Configuration is working ✅  
**Any FALSE?** → Check `.env` file contents

---

## Key Files Changed

### 1. **DotEnvLoader.java** (NEW)
- **What**: Loads `.env` file automatically
- **When**: At app startup (called from MainFX.start())
- **Where**: `src/main/java/Utils/DotEnvLoader.java`
- **Why**: Environment variables weren't being read

### 2. **ApiConfig.java** (FIXED)
- **What**: 
  - Fixed `getOpenAiBaseUrl()` to check env vars first
  - Added error logging for missing keys
  - Updated diagnostic output
- **Where**: `src/main/java/Utils/ApiConfig.java`
- **Why**: Was checking system properties before env vars

### 3. **OpenAiAssistantService.java** (FIXED)
- **What**:
  - Now uses `ApiConfig` instead of loading properties
  - Fixed endpoint from `/responses` → `/chat/completions`
  - Fixed payload format to match OpenAI API
  - Better error handling in response parsing
- **Where**: `src/main/java/Services/OpenAiAssistantService.java`
- **Why**: Was using wrong endpoint and format

### 4. **MainFX.java** (UPDATED)
- **What**: Added `DotEnvLoader.load()` and `ApiConfig.printConfiguration()`
- **Where**: `src/main/java/org/GreenLedger/MainFX.java`
- **Why**: Need to load config at app startup

---

## Configuration Fallback Chain

### For Any API Key (e.g., OPENAI_API_KEY):
```
1. Check environment variable: System.getenv("OPENAI_API_KEY")
2. Check system property: System.getProperty("openai.api.key")
3. Check properties file: api-config.properties
4. Return empty string if not found
```

**This means:**
- Environment variables have highest priority
- Properties file is backup
- System properties can override both

---

## API Configuration Methods

### OpenAI
```java
ApiConfig.isOpenAiEnabled()        // true/false
ApiConfig.getOpenAiApiKey()        // Your API key
ApiConfig.getOpenAiBaseUrl()       // https://api.openai.com/v1
ApiConfig.getOpenAiConnectTimeout()
ApiConfig.getOpenAiReadTimeout()
```

### Twilio
```java
ApiConfig.isTwilioEnabled()        // true/false
ApiConfig.getTwilioAccountSid()    // AC...
ApiConfig.getTwilioAuthToken()     // Token
ApiConfig.getTwilioFromNumber()    // +1...
```

### Diagnostics
```java
ApiConfig.printConfiguration()     // Prints all config to console
```

---

## Environment Variables Expected

### In `.env` file:
```
OPENAI_API_KEY=sk-proj-...
OPENAI_API_URL=https://api.openai.com/v1
TWILIO_ACCOUNT_SID=AC...
TWILIO_AUTH_TOKEN=...
TWILIO_FROM_NUMBER=+...
```

### In `api-config.properties`:
```
openai.api.key=sk-proj-...
openai.api.url=https://api.openai.com/v1
twilio.account.sid=AC...
twilio.auth.token=...
twilio.from.number=+...
```

---

## How to Verify It's Working

### 1. Check Console at Startup
```
✅ Look for: [DotEnvLoader] Loaded .env
✅ Look for: OpenAI API Key Present: true
✅ Look for: Twilio TOKEN Present: true
```

### 2. Test OpenAI
```java
OpenAiAssistantService service = new OpenAiAssistantService();
String response = service.ask("Hello");
// Should return response from ChatGPT, not error message
```

### 3. Test Twilio
```java
SmsService.sendProjectSubmittedSms("+216XXXXXXXX", 123);
// Check console for: [SMS] Envoyé. SID=...
```

---

## Common Issues & Fixes

| Issue | Check | Fix |
|-------|-------|-----|
| OpenAI disabled message | Startup logs | Verify `.env` is in project root |
| SMS not sending | Console logs | Check phone number format |
| API keys not found | `ApiConfig.printConfiguration()` | Verify `.env` file permissions |
| Wrong endpoint error | OpenAI service logs | Already fixed, should work now |

---

## DotEnvLoader Features

```java
// Automatic loading at startup
DotEnvLoader.load();

// Get environment variables with fallback
String key = DotEnvLoader.getEnv("OPENAI_API_KEY", "default-value");

// Works with System.getenv() too
String key = System.getenv("OPENAI_API_KEY");
```

**Priority:**
1. Loaded variables from `.env`
2. System environment variables
3. Default value if provided

---

## OpenAI Request/Response Format

### OLD (BROKEN) ❌
```json
{
  "model": "gpt-4.1-mini",
  "input": "Your message here"
}
```

### NEW (FIXED) ✅
```json
{
  "model": "gpt-3.5-turbo",
  "messages": [
    {
      "role": "user",
      "content": "Your message here"
    }
  ],
  "max_tokens": 500,
  "temperature": 0.7
}
```

### Response Format
```json
{
  "choices": [
    {
      "message": {
        "content": "Assistant response here"
      }
    }
  ]
}
```

---

## Rebuild & Test

```bash
# Clean and rebuild
mvn clean install

# Run the application
# Check console for startup messages
# Verify "[DotEnvLoader] Loaded .env" appears
# Check "[API CONFIG] OpenAI API Key Present: true"

# Test features
# 1. Try OpenAI assistant
# 2. Submit a project to trigger SMS
# 3. Check both work without errors
```

---

## Summary of Changes

| Component | Old | New | Impact |
|-----------|-----|-----|--------|
| Config loading | Properties only | Env vars → Properties → System | ✅ Flexible |
| OpenAI endpoint | `/responses` | `/chat/completions` | ✅ Correct API |
| Request format | `input` param | `messages` array | ✅ Compatible |
| Config source | Duplicated in service | Centralized in ApiConfig | ✅ DRY |
| Startup logging | Silent | Full diagnostics | ✅ Debugging |
| Error handling | Minimal | Detailed warnings | ✅ Better UX |

**Result:** API keys now properly read and services working correctly! ✅

