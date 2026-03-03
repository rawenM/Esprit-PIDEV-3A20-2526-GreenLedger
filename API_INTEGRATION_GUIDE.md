# 🌍 Carbon & Air Quality API Integration - Setup Guide

## ✅ Integration Complete!

Your Green Ledger carbon management system now integrates with:
1. **Carbon Interface API** - Real-time carbon emission calculations
2. **OpenWeatherMap Air Pollution API** - Real-time air quality data

---

## 📋 What Was Implemented

### New Files Created:
1. **Configuration:**
   - `src/main/resources/api-config.properties` - API settings
   - `src/main/java/Utils/ApiConfig.java` - Configuration loader

2. **API Services:**
   - `src/main/java/Services/ExternalCarbonApiService.java` - Carbon Interface API client
   - `src/main/java/Services/AirQualityService.java` - OpenWeatherMap API client

3. **Data Models (DTOs):**
   - `src/main/java/Models/dto/external/CarbonEstimateResponse.java`
   - `src/main/java/Models/dto/external/CarbonAttributes.java`
   - `src/main/java/Models/dto/external/AirPollutionResponse.java`
   - `src/main/java/Models/dto/external/AirQualityData.java`
   - `src/main/java/Models/dto/external/MainAirQuality.java`
   - `src/main/java/Models/dto/external/Components.java`

### Modified Files:
1. **`pom.xml`** - Added Apache HttpClient 5 and Gson dependencies
2. **`src/main/java/Models/Projet.java`** - Added location fields (latitude, longitude, activityType)
3. **`src/main/java/Services/CarbonReportService.java`** - Added enrichment method
4. **`src/main/java/Controllers/CarbonAuditController.java`** - Integrated API calls

---

## 🔑 Step 1: Get Your FREE API Keys

### Carbon Interface API:
1. Go to: https://www.carboninterface.com/
2. Click **"Sign Up"** (free account)
3. Verify your email
4. Navigate to **Dashboard** → **API Keys**
5. Copy your API key (looks like: `sk_live_abc123xyz456...`)

**Free Tier:** 200 requests/month

### OpenWeatherMap API:
1. Go to: https://openweathermap.org/api
2. Click **"Sign Up"** (free account)
3. Verify your email
4. Navigate to **Profile** → **My API Keys**
5. Copy your API key (looks like: `a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6`)

**Free Tier:** 1,000 calls/day

---

## ⚙️ Step 2: Configure Environment Variables

### Windows (PowerShell):
```powershell
# Set for current session
$env:CARBON_API_KEY = "your_carbon_interface_key_here"
$env:OPENWEATHERMAP_API_KEY = "your_openweathermap_key_here"

# Set permanently (System-wide)
[System.Environment]::SetEnvironmentVariable("CARBON_API_KEY", "your_key_here", "User")
[System.Environment]::SetEnvironmentVariable("OPENWEATHERMAP_API_KEY", "your_key_here", "User")
```

### Windows (Command Prompt):
```cmd
setx CARBON_API_KEY "your_carbon_interface_key_here"
setx OPENWEATHERMAP_API_KEY "your_openweathermap_key_here"
```

### Linux/Mac:
```bash
# Add to ~/.bashrc or ~/.zshrc
export CARBON_API_KEY="your_carbon_interface_key_here"
export OPENWEATHERMAP_API_KEY="your_openweathermap_key_here"

# Reload shell
source ~/.bashrc
```

**⚠️ IMPORTANT:** After setting environment variables, **restart your IDE** (IntelliJ, Eclipse, VS Code) or terminal.

---

## 🏗️ Step 3: Build the Project

```bash
# Compile with Maven
mvn clean compile

# Or with the batch file
.\run.bat
```

---

## 🚀 Step 4: Test the Integration

### Test 1: Verify Configuration
Run this in a Java test or main method:
```java
Utils.ApiConfig.printConfiguration();
```

Expected output:
```
[API CONFIG] ========== API Configuration ==========
[API CONFIG] Carbon API Enabled: true
[API CONFIG] Carbon API Key Present: true
[API CONFIG] Weather API Enabled: true
[API CONFIG] Weather API Key Present: true
...
```

### Test 2: Test API Connections
```java
// In CarbonReportService
CarbonReportService service = new CarbonReportService();
boolean apisWorking = service.testExternalApis();
System.out.println("APIs Working: " + apisWorking);
```

### Test 3: Create an Evaluation with External Data

1. **Launch the application**
2. **Navigate to Carbon Audit module** (gestionCarbone.fxml)
3. **Create or select a project:**
   - Make sure project has `latitude`, `longitude`, and `activityType` set
   - Example: lat=48.8566, lon=2.3522 (Paris), activityType="electricity"
4. **Add an evaluation** with criteria
5. **Check the AI Insights area** - should display:
   - Carbon Interface API data (estimated emissions)
   - OpenWeatherMap data (air quality index, pollutants)

---

## 📊 How It Works

### Workflow:
1. User creates/evaluates a project in `CarbonAuditController`
2. `ajouterEvaluation()` is called
3. System calls `carbonReportService.enrichWithExternalData()`
4. **External APIs are called:**
   - Carbon Interface API → estimates emissions based on activity type
   - OpenWeatherMap API → gets air quality for project location
5. **Results are displayed** in the UI (txtAIInsights TextArea)
6. **Graceful degradation:** If APIs fail, the evaluation continues without external data

### API Call Examples:

**Carbon Interface - Electricity:**
```
POST https://www.carboninterface.com/api/v1/estimates
{
  "type": "electricity",
  "electricity_unit": "kwh",
  "electricity_value": 1000,
  "country": "us"
}
```

**OpenWeatherMap - Air Quality:**
```
GET https://api.openweathermap.org/data/2.5/air_pollution?lat=48.8566&lon=2.3522&appid=YOUR_KEY
```

---

## 🔧 Configuration Options

Edit `src/main/resources/api-config.properties`:

```properties
# Enable/Disable APIs
api.carbon.enabled=true
api.weather.enabled=true

# Timeouts (milliseconds)
api.connection.timeout=5000
api.read.timeout=10000

# Graceful degradation (continue if APIs fail)
api.graceful.degradation=true
```

---

## 🧪 Testing Without API Keys (Mock Data)

If you don't have API keys yet, the system will use **mock data**:

```java
// In ExternalCarbonApiService
CarbonEstimateResponse mockData = carbonApiService.getMockEstimate("electricity", 1000);

// In AirQualityService
AirPollutionResponse mockAir = airQualityService.getMockAirQuality(48.8566, 2.3522);
```

This allows development/testing without real API accounts.

---

## 🐛 Troubleshooting

### Problem: "API key not configured"
**Solution:** Ensure environment variables are set and IDE is restarted
```powershell
# Check if variables are set
echo $env:CARBON_API_KEY
echo $env:OPENWEATHERMAP_API_KEY
```

### Problem: "Connection timeout"
**Solution:** Increase timeout in `api-config.properties`
```properties
api.connection.timeout=10000
api.read.timeout=20000
```

### Problem: "Invalid API key"
**Solution:** 
- Verify key is copied correctly (no spaces/newlines)
- Check API dashboard - key might not be activated yet (wait 5-10 minutes)

### Problem: "No location data"
**Solution:** Set project location in `Projet` model:
```java
projet.setLatitude(48.8566);
projet.setLongitude(2.3522);
projet.setActivityType("electricity");
```

---

## 📈 API Usage Limits

### Free Tier Limits:
| API | Free Limit | Paid Plans |
|-----|------------|------------|
| **Carbon Interface** | 200 requests/month | From $9/month |
| **OpenWeatherMap** | 1,000 calls/day | From $40/month |

### Monitor Usage:
- **Carbon Interface:** Dashboard → Usage
- **OpenWeatherMap:** Profile → Statistics

---

## 🎯 Next Steps

### Optional Enhancements:
1. **Add location form fields** in UI to set project lat/lon
2. **Display API data in dedicated cards** (not just txtAIInsights)
3. **Cache API responses** to reduce calls
4. **Add retry logic** with exponential backoff
5. **Store enriched data in database** for historical tracking
6. **Add more activity types** (fuel, shipping, flights)

### Database Extension (Optional):
```sql
-- Add columns to projet table
ALTER TABLE projet ADD COLUMN latitude DOUBLE;
ALTER TABLE projet ADD COLUMN longitude DOUBLE;
ALTER TABLE projet ADD COLUMN activity_type VARCHAR(100);
```

---

## 📚 API Documentation Links

- **Carbon Interface:** https://docs.carboninterface.com
- **OpenWeatherMap Air Pollution:** https://openweathermap.org/api/air-pollution

---

## ✅ Verification Checklist

- [ ] API keys obtained from both platforms
- [ ] Environment variables set (CARBON_API_KEY, OPENWEATHERMAP_API_KEY)
- [ ] IDE/Terminal restarted after setting env vars
- [ ] Project compiles without errors (`mvn clean compile`)
- [ ] Configuration prints correctly (`ApiConfig.printConfiguration()`)
- [ ] Test API connections successful
- [ ] Created evaluation shows external data in UI
- [ ] Graceful degradation works (invalid key = continues without data)

---

## 🎉 Integration Complete!

Your carbon management system now has real-time external data integration. The system will automatically enrich carbon reports with:
- ✅ Carbon emission estimates from Carbon Interface
- ✅ Air quality data from OpenWeatherMap
- ✅ Graceful degradation if APIs fail
- ✅ Mock data for development without API keys

**Questions? Check console logs for detailed API call information:**
```
[API CONFIG] Configuration loaded successfully
[CARBON ENRICHMENT] Starting enrichment for project: ...
[CARBON API] Success: 200
[AIR QUALITY API] Success: 200
```
