# ROYYA WATCH - Mock Data & Simulation Specification

**Status:** Living Document  
**Version:** 1.0  
**Context:** Specification of deterministic mock repositories, realistic aquaculture telemetry seeds, and simulation scenarios for offline testing and developer validation.

---

## 1. Seed Ponds

```json
[
  {
    "id": "pond_01",
    "name": "Pond 01 (ఉత్తర చెరువు)",
    "location": "Bhimavaram - Block A",
    "deviceId": "esp32_relay_01",
    "connectivity": "ONLINE",
    "healthStatus": "HEALTHY",
    "healthScore": 92,
    "lastUpdated": "2026-11-10T00:15:00Z"
  },
  {
    "id": "pond_02",
    "name": "Pond 02 (దక్షిణ చెరువు)",
    "location": "Bhimavaram - Block B",
    "deviceId": "esp32_relay_02",
    "connectivity": "ONLINE",
    "healthStatus": "ATTENTION",
    "healthScore": 72,
    "lastUpdated": "2026-11-10T00:12:30Z"
  },
  {
    "id": "pond_03",
    "name": "Pond 03 (తూర్పు చెరువు)",
    "location": "Bhimavaram - Block C",
    "deviceId": "esp32_relay_03",
    "connectivity": "OFFLINE",
    "healthStatus": "CRITICAL",
    "healthScore": 45,
    "lastUpdated": "2026-11-09T22:30:00Z"
  }
]
```

---

## 2. Telemetry Presets & Hysteresis Thresholds

The mock repository adheres to the verified aquaculture control logic documented in the web frontend:

| Metric | Normal Range | Warning / Hysteresis Band | Critical Threshold |
|---|---|---|---|
| **Dissolved Oxygen (DO)** | `> 6.0 mg/L` (Aerator OFF in AUTO) | `5.0 – 6.0 mg/L` (Hysteresis Buffer: Preserves current relay state) | `< 5.0 mg/L` (Critical: Triggers Aerator ON in AUTO) |
| **Water Temperature** | `26.0°C – 32.0°C` | `24.0°C – 25.9°C` or `32.1°C – 34.0°C` | `< 24.0°C` or `> 34.0°C` |
| **Turbidity** | `1.5 – 3.0 NTU / V` | `3.1 – 4.0 NTU / V` | `> 4.0 NTU / V` |
| **pH** | `7.5 – 8.5` | `7.0 – 7.4` or `8.6 – 9.0` | `< 7.0` or `> 9.0` |
| **TDS** | `600 – 900 ppm` | `901 – 1200 ppm` | `> 1200 ppm` |

---

## 3. The 10 Developer Simulation Scenarios

The `DeveloperSimulationScreen` allows instant toggling between these 10 scenarios:

1. **Scenario 1: Optimal Healthy State**
   - DO: `6.8 mg/L` (Healthy, Aerator: `OFF`)
   - Temp: `28.4°C`, Turbidity: `2.1V`, pH: `8.0`
   - Active Alerts: None
2. **Scenario 2: Warning Monitoring Band (Hysteresis Buffer)**
   - DO: `5.5 mg/L`
   - Aerator: Preserves previous state (`ON` if it was on, `OFF` if it was off)
   - Status: `ATTENTION`
3. **Scenario 3: Low DO Triggering Auto Aeration**
   - DO: `4.2 mg/L`
   - Aerator: Automatically commands `ON`
   - Active Alert: `LOW_DISSOLVED_OXYGEN` (Warning)
4. **Scenario 4: Critical Emergency Oxygen Drop**
   - DO: `2.5 mg/L`
   - Aerator: `ON`
   - Active Alert: `LOW_DISSOLVED_OXYGEN` (Critical full-screen alarm + Telugu voice)
5. **Scenario 5: Recovered DO (Auto Stop)**
   - DO: `6.4 mg/L`
   - Aerator: Automatically transitions `OFF`
   - Alert: Auto-resolves previous low DO alert
6. **Scenario 6: Aerator Motor Failure**
   - Mode: `AUTO` or `MANUAL`, commanded state: `ON`, but physical feedback indicates `OFF`
   - Active Alert: `AERATOR_FAILURE` (Critical)
7. **Scenario 7: ESP32 Hardware Disconnection / Power Outage**
   - Connectivity: `OFFLINE`
   - Readings: Cached with last update timestamp > 1 hour ago
   - Controls: Manual relay controls disabled with offline badge
8. **Scenario 8: Missing Sensor Telemetry**
   - DO and Temp present, but pH and TDS report `null` (displayed as `Unknown`, never `0.0`)
9. **Scenario 9: Audio Retrieval Failure**
   - Audio download fails -> Player gracefully displays error badge and presents localized text
10. **Scenario 10: Duplicate & Delayed FCM Alert Delivery**
    - Repeated delivery of same `alert_id` -> Idempotent handling prevents duplicate cards in history
