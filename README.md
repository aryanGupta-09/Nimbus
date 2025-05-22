# Nimbus - Modern Weather App

![Nimbus Logo](assets/cloud_15593838.webp)

## Overview

Nimbus is a feature-rich weather application that delivers accurate forecasts with a modern, intuitive interface. It combines traditional weather data with advanced AI-powered sky analysis, multiple location tracking, device sensor integration, and customizable UI options to provide a complete and personalized weather experience.

## Features

### 🌤️ Weather Dashboard
- Current weather conditions with detailed metrics
- Hourly and daily forecasts (up to 7 days)
- Feels-like temperature, humidity, wind, and pressure data
- Air quality information with pollutant details (PM2.5, PM10, CO, NO2, SO2, O3)
- UV index and visibility metrics
- Historical weather data comparison

### 📍 Location Management
- Automatic detection of current location
- Save and manage multiple locations
- Quick switching between saved locations

### 🔍 Sky Analysis
- Camera integration for sky photos
- AI-powered cloud pattern recognition using Google's Gemini AI
- Weather prediction based on visual analysis
- Cloud type identification and atmospheric phenomena detection
- Gallery integration to analyze existing photos

### 📱 Device Sensors
- Barometric pressure readings from device sensors
- Integration of sensor data with weather forecasts
- Enhanced local weather accuracy

### ⚙️ Background Updates
- Periodic weather data refresh using WorkManager
- Offline data caching for when you're without internet
- Battery-efficient background operations

### 🎨 UI/UX
- Modern Material 3 design
- Dark and light theme support
- Responsive and intuitive interface
- Beautiful weather animations and icons
- Pull-to-refresh for instant weather updates
- Temperature unit switching (°C/°F)

## Technical Details

### Architecture
- MVVM (Model-View-ViewModel) architecture
- Repository pattern for data management
- Clean separation of concerns

### Technologies
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **API Integration**: Retrofit with Moshi for JSON parsing
- **Background Processing**: WorkManager
- **Local Storage**: Room Database
- **Dependency Injection**: Manual DI pattern
- **Image Loading**: Coil
- **Animations**: Lottie
- **AI Integration**: Google Gemini API
- **Location Services**: Google Play Services Location

## Getting Started

### Prerequisites
- Android Studio Arctic Fox (2021.3.1) or newer
- Min SDK 24 (Android 7.0)
- Target SDK 35
- Kotlin 1.8.10 or newer

### Setup
1. Clone the repository:
   ```
   git clone https://github.com/yourusername/nimbus.git
   ```

2. Create an `apikey.properties` file in the root directory with the following content:
   ```
   WEATHER_API_KEY="your_weather_api_key"
   GEMINI_API_KEY="your_gemini_api_key"
   ```

3. Get your API keys:
   - Register at [WeatherAPI](https://www.weatherapi.com/) for the WEATHER_API_KEY
   - Get your Gemini API key from [Google AI Studio](https://ai.google.dev/)

4. Build and run the app using Android Studio

## Permissions

Nimbus requires the following permissions:
- **Internet**: For fetching weather data
- **Location**: For detecting your current position
- **Camera**: For sky analysis feature
- **Storage**: For saving sky photos
- **Network State**: For checking connectivity

## Troubleshooting

- **Location not updating?** Check that location permissions are granted and GPS is enabled
- **Sky analysis not working?** Verify your Gemini API key is correctly set in `apikey.properties`
- **Missing sensor data?** Not all devices have barometric pressure or humidity sensors
- **Weather not refreshing?** Check network connectivity and background refresh settings

## Acknowledgments

- Weather data provided by [WeatherAPI](https://www.weatherapi.com/)
- Cloud analysis powered by [Google Gemini AI](https://ai.google.dev/)
- Icons and animations from various open-source libraries

---

*Nimbus - Weather at your fingertips: Accurate, Beautiful, Intelligent*
