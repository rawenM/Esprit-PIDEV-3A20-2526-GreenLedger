# Air Quality API Integration - Setup Guide

## Overview
Your Green Wallet map now displays **REAL** pollution data from OpenWeatherMap API for 70+ global zones!

## How It Works

### 🌍 **70+ Global Zones**
- North America: 9 zones (USA, Canada, Mexico, Caribbean)
- South America: 5 zones (Brazil, Argentina, Chile, Colombia, etc.)
- Europe: 14 zones (UK, France, Germany, Italy, Eastern Europe, etc.)  
- Africa: 9 zones (North, West, East, Southern Africa)
- Middle East: 4 zones (Iraq, Saudi Arabia, Iran, Turkey)
- Asia: 15 zones (China, India, Japan, Korea, Southeast Asia, Russia)
- Oceania: 3 zones (Australia, New Zealand, Pacific)

### 📊 **Data Fetching**
- Each zone fetches REAL air quality from its center point
- Converts OpenWeatherMap AQI (1-5) to US EPA AQI (0-300+)
- Uses PM2.5 concentration for more accurate calculations

### ⚡ **Smart Caching (1 hour)**
- Minimizes API usage - data refreshes every hour
- If 10 users view the map in 1 hour = only 70 API calls (not 700!)
- Graceful fallback to static data if API unavailable

### 🔑 **Free Tier Limits**
- **1,000 calls/day** = ~14 map loads per day
- **60 calls/minute** = instant loading
- **100% free** with free account

## Setup Instructions

### 1. Get Your Free API Key

1. Go to https://openweathermap.org/api
2. Click "Sign Up" (free account)
3. Verify your email
4. Go to https://home.openweathermap.org/api_keys
5. Copy your API key (looks like: `abc123def456ghi789jkl012mno345pq`)

### 2. Configure the API Key

**Option A: Environment Variable (Recommended)**
```bash
# Windows PowerShell
$env:OPENWEATHERMAP_API_KEY = "your_api_key_here"

# Windows CMD
set OPENWEATHERMAP_API_KEY=your_api_key_here

# Linux/Mac
export OPENWEATHERMAP_API_KEY=your_api_key_here
```

**Option B: Properties File**
Edit `src/main/resources/api-config.properties`:
```properties
openweathermap.api.key=your_api_key_here
```

### 3. Build & Run

```bash
mvn clean compile
mvn javafx:run
```

### 4. Verify It Works

When you open Green Wallet, check the console output:
```
[AIR QUALITY API] Service initialized successfully
[GREEN WALLET] Initializing map with REAL air quality data...
[GREEN WALLET] API Status: ENABLED
[AIR QUALITY API] GET https://api.openweathermap.org/data/2.5/air_pollution?lat=54.00&lon=-110.00&appid=***
[AIR QUALITY API] Success: 200
[AIR QUALITY] Fetched real data for 54.00,-110.00: OWM=2 → EPA=75
```

## How API Usage is Minimized

### 🔄 **1-Hour Cache**
```
First load:     70 API calls (fetch all zones)
2nd load (30 min later): 0 API calls (all cached)
3rd load (2 hours later): 70 API calls (cache expired, refresh)
```

### 📉 **Daily Usage Example**
| Scenario | Daily Loads | API Calls | Status |
|----------|-------------|-----------|--------|
| 1 user | 5 loads | 70 calls | ✅ Safe |
| 5 users | 10 loads total | 140 calls | ✅ Safe |
| 10 users | 20 loads total | 280 calls | ✅ Safe |
| Heavy usage | 14 loads | 980 calls | ✅ Near limit |
| Too many | 15 loads | 1050 calls | ⚠️ Over limit |

### 🎯 **Optimization Tips**

**If you hit the free tier limit:**

1. **Increase cache duration** (line 44 in GreenWalletController.java):
   ```java
   private static final long CACHE_DURATION_MS = 7200000; // 2 hours instead of 1
   ```

2. **Reduce zone count** (only fetch major cities):
   ```java
   // Comment out less important zones in setupMapWebView()
   ```

3. **Lazy loading** (only fetch on map interaction):
   ```java
   // Fetch data only when user clicks a zone
   ```

4. **Upgrade to paid tier** ($40/month for 100,000 calls)

## Fallback Behavior

If API is unavailable (no key, rate limit, network error):
- Map still loads with static sample data
- No errors shown to user
- Console logs explain what happened
- Cache prevents repeated failures

## API Response Example

```json
{
  "coord": {"lon": -110.0, "lat": 54.0},
  "list": [{
    "main": {"aqi": 2},
    "components": {
      "co": 230.31,
      "no": 0.01,
      "no2": 2.98,
      "o3": 68.66,
      "so2": 0.64,
      "pm2_5": 12.5,
      "pm10": 18.2,
      "nh3": 0.11
    },
    "dt": 1709145600
  }]
}
```

## AQI Conversion

OpenWeatherMap uses 1-5 scale:
- 1 = Good (converts to 0-50 EPA)
- 2 = Fair (converts to 51-100 EPA)
- 3 = Moderate (converts to 101-150 EPA)
- 4 = Poor (converts to 151-200 EPA)
- 5 = Very Poor (converts to 201-300 EPA)

We refine this using PM2.5 concentration for accuracy.

## Color Scheme

| AQI Range | Color | Label |
|-----------|-------|-------|
| 0-50 | 🟢 Green | Bonne Qualité |
| 51-100 | 🟡 Yellow | Modéré |
| 101-150 | 🟠 Orange | Mauvais |
| 151-200 | 🔴 Red | Très Mauvais |
| 201-300 | 🔴 Dark Red | Dangereux |
| 301+ | ⚫ Maroon | Très Dangereux |

## Troubleshooting

### "API not configured" error
**Solution:** Set the `OPENWEATHERMAP_API_KEY` environment variable or add it to api-config.properties

### "Service disabled" message
**Solution:** Make sure `api.weather.enabled=true` in api-config.properties

### API returns 401 Unauthorized
**Solution:** Check your API key is correct and activated (can take 1-2 hours after signup)

### API returns 429 Too Many Requests
**Solution:** You hit the rate limit. Wait 1 hour or increase cache duration

### Map shows same data after changing API key
**Solution:** Restart the application to clear the cache

## Code Files Modified

1. **GreenWalletController.java** (lines 40-720)
   - Added `airQualityCache` and `CachedAirQuality` class
   - Added `fetchRealAirQuality()` method
   - Added `convertOwmAqiToUsEpa()` method
   - Updated all 70+ zones to fetch real data
   - Updated map comments to say "REAL-TIME"

2. **api-config.properties**
   - Added comments about OpenWeatherMap API setup

3. **AirQualityService.java** (already existed)
   - No changes needed - already functional!

## Need Help?

- OpenWeatherMap Docs: https://openweathermap.org/api/air-pollution
- API Dashboard: https://home.openweathermap.org/
- Support: https://openweathermap.org/faq

---

🎉 **Enjoy your real-time global pollution map!**
