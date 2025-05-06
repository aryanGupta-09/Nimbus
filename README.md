# Nimbus - Modern Weather App

![Nimbus Logo](assets/cloud_15593838.webp)

## Overview

Nimbus is a comprehensive weather application for Android that provides accurate, real-time weather data with a beautiful, intuitive UI built with Jetpack Compose. The app offers detailed weather forecasts, location tracking, sky analysis, and sensor integration for a complete weather experience.

## Features

### 🌤️ Weather Dashboard
- Current weather conditions with detailed metrics
- Hourly and daily forecasts
- Feels-like temperature, humidity, wind, and pressure data
- Air quality information
- UV index and visibility metrics

### 📍 Location Management
- Automatic detection of current location
- Save and manage multiple locations
- Quick switching between saved locations

### 🔍 Sky Analysis
- Camera integration for sky photos
- AI-powered cloud pattern recognition using Google's Gemini AI
- Weather prediction based on visual analysis

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

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Weather data provided by [WeatherAPI](https://www.weatherapi.com/)
- Cloud analysis powered by [Google Gemini AI](https://ai.google.dev/)
- Icons and animations from various open-source libraries

---

*Nimbus - Weather at your fingertips*