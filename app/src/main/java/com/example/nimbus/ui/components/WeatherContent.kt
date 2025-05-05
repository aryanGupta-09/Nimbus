package com.example.nimbus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.nimbus.data.model.AirQuality
import com.example.nimbus.data.model.Astro
import com.example.nimbus.data.model.Current
import com.example.nimbus.data.model.ForecastDay
import com.example.nimbus.data.model.WeatherResponse
import com.example.nimbus.data.model.local.DeviceSensorData
import com.example.nimbus.ui.screens.HistoricalWeatherState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.LottieConstants
import java.time.LocalTime
import java.time.LocalDateTime

@Composable
fun WeatherContent(
    weatherData: WeatherResponse,
    locationName: String? = null,
    fullLocationDisplay: String? = null,
    historicalWeatherState: HistoricalWeatherState = HistoricalWeatherState.Loading,
    onRetryHistorical: () -> Unit = {},
    isCelsius: Boolean,
    devicePressure: Float? = null,
    hasBarometer: Boolean = false,
    sensorData: DeviceSensorData = DeviceSensorData(),
    hasSensors: Boolean = false,
    isCurrentLocation: Boolean = false
) {
    val scrollState = rememberScrollState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Location and current weather header
            if (weatherData.current != null) {
                if (fullLocationDisplay != null) {
                    // Use the full location display if provided
                    CurrentWeatherHeaderWithFullLocation(
                        fullLocationDisplay = fullLocationDisplay,
                        current = weatherData.current,
                        isCelsius = isCelsius,
                        localtime = weatherData.location.localtime,
                        sunrise = weatherData.forecast.forecastday.first().astro.sunrise,
                        sunset = weatherData.forecast.forecastday.first().astro.sunset
                    )
                } else {
                    // Otherwise use the old method
                    CurrentWeatherHeader(
                        locationName = locationName ?: weatherData.location.name,
                        country = weatherData.location.country,
                        current = weatherData.current,
                        isCelsius = isCelsius,
                        localtime = weatherData.location.localtime,
                        sunrise = weatherData.forecast.forecastday.first().astro.sunrise,
                        sunset = weatherData.forecast.forecastday.first().astro.sunset
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Weather details cards
                WeatherDetailsSection(weatherData, devicePressure, hasBarometer)
                
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Device Sensors Card - placed just above forecast section
            if (hasSensors && isCurrentLocation) {
                DeviceSensorsCard(sensorData = sensorData, hasSensors = hasSensors)
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Forecast for the next days
            ForecastSection(weatherData.forecast.forecastday, isCelsius)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Historical weather section
            when (historicalWeatherState) {
                is HistoricalWeatherState.Loading -> {
                    HistoricalWeatherSection(
                        historicalData = emptyList(),
                        isLoading = true,
                        onRetry = onRetryHistorical,
                        isCelsius = isCelsius
                    )
                }
                is HistoricalWeatherState.Success -> {
                    HistoricalWeatherSection(
                        historicalData = historicalWeatherState.data,
                        isLoading = false,
                        onRetry = onRetryHistorical,
                        isCelsius = isCelsius
                    )
                }
                is HistoricalWeatherState.Error -> {
                    HistoricalWeatherSection(
                        historicalData = emptyList(),
                        isLoading = false,
                        onRetry = onRetryHistorical,
                        isCelsius = isCelsius
                    )
                }
            }
        }
    }
}

@Composable
fun CurrentWeatherHeader(
    locationName: String,
    country: String,
    current: Current,
    isCelsius: Boolean,
    localtime: String,
    sunrise: String,
    sunset: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$locationName, $country",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Determine current time using API-provided localtime
            val now = runCatching { LocalDateTime.parse(localtime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")).toLocalTime() }.getOrNull() ?: LocalTime.now()
            val fmt = DateTimeFormatter.ofPattern("h:mm a")
            val sunsetTime = runCatching { LocalTime.parse(sunset, fmt) }.getOrNull() ?: now
            val sunriseTime = runCatching { LocalTime.parse(sunrise, fmt) }.getOrNull() ?: now
            val condText = current.condition.text
            // Animation logic based on condition and time
            when {
                condText.contains("thunder", ignoreCase = true) -> {
                    val comp = rememberLottieComposition(LottieCompositionSpec.Url("https://lottie.host/7ecb5799-5531-4387-aca7-3b33a5ff8727/SoKzuvPsWL.lottie")).value
                    comp?.let { LottieAnimation(it, iterations = LottieConstants.IterateForever, isPlaying = true, modifier = Modifier.size(96.dp)) }
                }
                condText.contains("partly cloudy", ignoreCase = true) -> {
                    // Partly cloudy: rely on API-provided isDay flag for correct day/night
                    val dayUrl = "https://lottie.host/1cdcfda5-6e5e-47bb-8e1e-bfec6e3d0ae0/Kq0aAfLIqC.lottie"
                    val nightUrl = "https://lottie.host/7b298f06-75ca-42e1-bdbb-7ffd3f1b1f16/elwa52RDsI.lottie"
                    val url = if (current.isDay == 1) dayUrl else nightUrl
                    val comp = rememberLottieComposition(LottieCompositionSpec.Url(url)).value
                    comp?.let { LottieAnimation(it, iterations = LottieConstants.IterateForever, isPlaying = true, modifier = Modifier.size(96.dp)) }
                }
                condText.contains("sunny", ignoreCase = true) -> {
                    val url = "https://lottie.host/8f9a2f9f-1be3-4a11-bc52-2f870e06485c/copScDooXi.lottie"
                    val comp = rememberLottieComposition(LottieCompositionSpec.Url(url)).value
                    comp?.let { LottieAnimation(it, iterations = LottieConstants.IterateForever, isPlaying = true, modifier = Modifier.size(96.dp)) }
                }
                condText.contains("clear", ignoreCase = true) -> {
                    // Use day animation only between sunrise and sunset, else night
                    val dayClearUrl = "https://lottie.host/8f9a2f9f-1be3-4a11-bc52-2f870e06485c/copScDooXi.lottie"
                    val nightClearUrl = "https://lottie.host/64c86eeb-ec84-4fb9-97e4-ea27fa2fe8ee/nHVSLj6Bhe.lottie"
                    val url = if (now.isAfter(sunriseTime) && now.isBefore(sunsetTime)) dayClearUrl else nightClearUrl
                    val comp = rememberLottieComposition(LottieCompositionSpec.Url(url)).value
                    comp?.let { LottieAnimation(it, iterations = LottieConstants.IterateForever, isPlaying = true, modifier = Modifier.size(96.dp)) }
                }
                condText.contains("overcast", ignoreCase = true) -> {
                    val comp = rememberLottieComposition(LottieCompositionSpec.Url("https://lottie.host/310ae1aa-03ff-4e88-9dd8-76c6fdc6a96d/5Zosi3mtEw.lottie")).value
                    comp?.let { LottieAnimation(it, iterations = LottieConstants.IterateForever, isPlaying = true, modifier = Modifier.size(96.dp)) }
                }
                condText.contains("mist", ignoreCase = true) -> {
                    // Mist animation: day vs night based on sunrise and sunset times
                    val dayMistUrl = "https://lottie.host/310ae1aa-03ff-4e88-9dd8-76c6fdc6a96d/5Zosi3mtEw.lottie"
                    val nightMistUrl = "https://lottie.host/bf10ee8d-182b-4e2a-a308-1030655c0238/ITiX7Ns5ca.lottie"
                    // Use now in between sunrise/sunset for day
                    val isDaytime = now.isAfter(sunriseTime) && now.isBefore(sunsetTime)
                    val url = if (isDaytime) dayMistUrl else nightMistUrl
                    val comp = rememberLottieComposition(LottieCompositionSpec.Url(url)).value
                    comp?.let { LottieAnimation(it, iterations = LottieConstants.IterateForever, isPlaying = true, modifier = Modifier.size(96.dp)) }
                }
                condText.contains("patchy rain nearby", ignoreCase = true) -> {
                    // Patchy rain animation: day vs night
                    val dayUrl = "https://lottie.host/15ac356a-6872-4832-b2f9-c4861a4ea9ab/5EYIWzbm59.lottie"
                    val nightUrl = "https://lottie.host/951f760f-10e7-42f8-bc0d-1cdbdd89f694/SxvvKI0c3X.lottie"
                    val url = if (now.isAfter(sunriseTime) && now.isBefore(sunsetTime)) dayUrl else nightUrl
                    val comp = rememberLottieComposition(LottieCompositionSpec.Url(url)).value
                    comp?.let { LottieAnimation(it, iterations = LottieConstants.IterateForever, isPlaying = true, modifier = Modifier.size(96.dp)) }
                }
                else -> {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data("https:${current.condition.icon}").crossfade(true).build(),
                        contentDescription = current.condition.text,
                        modifier = Modifier.size(64.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Temperature
            Text(
                text = "${if (isCelsius) current.tempC.toInt() else current.tempF.toInt()}°${if (isCelsius) "C" else "F"}",
                modifier = Modifier.align(Alignment.CenterVertically),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        Text(
            text = current.condition.text,
            style = MaterialTheme.typography.bodyLarge
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Feels like ${if (isCelsius) current.feelslikeC.toInt() else current.feelslikeF.toInt()}°${if (isCelsius) "C" else "F"}",
            style = MaterialTheme.typography.bodyMedium
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "Last updated: ${current.lastUpdated}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun CurrentWeatherHeaderWithFullLocation(
    fullLocationDisplay: String,
    current: Current,
    isCelsius: Boolean,
    localtime: String,
    sunrise: String,
    sunset: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = fullLocationDisplay,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Determine current time using API-provided localtime
            val now = runCatching { LocalDateTime.parse(localtime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")).toLocalTime() }.getOrNull() ?: LocalTime.now()
            val fmt = DateTimeFormatter.ofPattern("h:mm a")
            val sunsetTime = runCatching { LocalTime.parse(sunset, fmt) }.getOrNull() ?: now
            val sunriseTime = runCatching { LocalTime.parse(sunrise, fmt) }.getOrNull() ?: now
            val condText = current.condition.text
            // Animation logic based on condition and time
            when {
                condText.contains("thunder", ignoreCase = true) -> {
                    val comp = rememberLottieComposition(LottieCompositionSpec.Url("https://lottie.host/7ecb5799-5531-4387-aca7-3b33a5ff8727/SoKzuvPsWL.lottie")).value
                    comp?.let { LottieAnimation(it, iterations = LottieConstants.IterateForever, isPlaying = true, modifier = Modifier.size(96.dp)) }
                }
                condText.contains("partly cloudy", ignoreCase = true) -> {
                    // Partly cloudy: rely on API-provided isDay flag for correct day/night
                    val dayUrl = "https://lottie.host/1cdcfda5-6e5e-47bb-8e1e-bfec6e3d0ae0/Kq0aAfLIqC.lottie"
                    val nightUrl = "https://lottie.host/7b298f06-75ca-42e1-bdbb-7ffd3f1b1f16/elwa52RDsI.lottie"
                    val url = if (current.isDay == 1) dayUrl else nightUrl
                    val comp = rememberLottieComposition(LottieCompositionSpec.Url(url)).value
                    comp?.let { LottieAnimation(it, iterations = LottieConstants.IterateForever, isPlaying = true, modifier = Modifier.size(96.dp)) }
                }
                condText.contains("sunny", ignoreCase = true) -> {
                    val url = "https://lottie.host/8f9a2f9f-1be3-4a11-bc52-2f870e06485c/copScDooXi.lottie"
                    val comp = rememberLottieComposition(LottieCompositionSpec.Url(url)).value
                    comp?.let { LottieAnimation(it, iterations = LottieConstants.IterateForever, isPlaying = true, modifier = Modifier.size(96.dp)) }
                }
                condText.contains("clear", ignoreCase = true) -> {
                    val dayClearUrl = "https://lottie.host/8f9a2f9f-1be3-4a11-bc52-2f870e06485c/copScDooXi.lottie"
                    val nightClearUrl = "https://lottie.host/64c86eeb-ec84-4fb9-97e4-ea27fa2fe8ee/nHVSLj6Bhe.lottie"
                    val url = if (now.isAfter(sunriseTime) && now.isBefore(sunsetTime)) dayClearUrl else nightClearUrl
                    val comp = rememberLottieComposition(LottieCompositionSpec.Url(url)).value
                    comp?.let { LottieAnimation(it, iterations = LottieConstants.IterateForever, isPlaying = true, modifier = Modifier.size(96.dp)) }
                }
                condText.contains("overcast", ignoreCase = true) -> {
                    val comp = rememberLottieComposition(LottieCompositionSpec.Url("https://lottie.host/310ae1aa-03ff-4e88-9dd8-76c6fdc6a96d/5Zosi3mtEw.lottie")).value
                    comp?.let { LottieAnimation(it, iterations = LottieConstants.IterateForever, isPlaying = true, modifier = Modifier.size(96.dp)) }
                }
                condText.contains("mist", ignoreCase = true) -> {
                    // Mist animation: day vs night based on sunrise and sunset times
                    val dayMistUrl = "https://lottie.host/310ae1aa-03ff-4e88-9dd8-76c6fdc6a96d/5Zosi3mtEw.lottie"
                    val nightMistUrl = "https://lottie.host/bf10ee8d-182b-4e2a-a308-1030655c0238/ITiX7Ns5ca.lottie"
                    // Use now in between sunrise/sunset for day
                    val isDaytime = now.isAfter(sunriseTime) && now.isBefore(sunsetTime)
                    val url = if (isDaytime) dayMistUrl else nightMistUrl
                    val comp = rememberLottieComposition(LottieCompositionSpec.Url(url)).value
                    comp?.let { LottieAnimation(it, iterations = LottieConstants.IterateForever, isPlaying = true, modifier = Modifier.size(96.dp)) }
                }
                condText.contains("patchy rain nearby", ignoreCase = true) -> {
                    // Patchy rain animation: day vs night
                    val dayUrl = "https://lottie.host/15ac356a-6872-4832-b2f9-c4861a4ea9ab/5EYIWzbm59.lottie"
                    val nightUrl = "https://lottie.host/951f760f-10e7-42f8-bc0d-1cdbdd89f694/SxvvKI0c3X.lottie"
                    val url = if (now.isAfter(sunriseTime) && now.isBefore(sunsetTime)) dayUrl else nightUrl
                    val comp = rememberLottieComposition(LottieCompositionSpec.Url(url)).value
                    comp?.let { LottieAnimation(it, iterations = LottieConstants.IterateForever, isPlaying = true, modifier = Modifier.size(96.dp)) }
                }
                else -> {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data("https:${current.condition.icon}").crossfade(true).build(),
                        contentDescription = current.condition.text,
                        modifier = Modifier.size(64.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Temperature
            Text(
                text = "${if (isCelsius) current.tempC.toInt() else current.tempF.toInt()}°${if (isCelsius) "C" else "F"}",
                modifier = Modifier.align(Alignment.CenterVertically),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        Text(
            text = current.condition.text,
            style = MaterialTheme.typography.bodyLarge
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Feels like ${if (isCelsius) current.feelslikeC.toInt() else current.feelslikeF.toInt()}°${if (isCelsius) "C" else "F"}",
            style = MaterialTheme.typography.bodyMedium
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "Last updated: ${current.lastUpdated}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun WeatherDetailsSection(
    weatherData: WeatherResponse,
    devicePressure: Float? = null,
    hasBarometer: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Astro info (sunrise, sunset, moon phase)
        AstroInfoCard(weatherData.forecast.forecastday.first().astro)
        
        // Only show current weather details if current data is available
        if (weatherData.current != null) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Removed BarometerCard as requested
            
            // Air Quality
            AirQualityCard(weatherData.current.airQuality)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Other weather details
            WeatherDetailsCard(weatherData.current)
        }
    }
}

@Composable
fun AstroInfoCard(astro: Astro) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Sun & Moon",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Sunrise
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sunrise",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = astro.sunrise,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Sunset
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sunset",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = astro.sunset,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surface)
            Spacer(modifier = Modifier.height(16.dp))
            
            // Moon info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Moon phase
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Moon Phase",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = astro.moonPhase,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
                
                // Moon illumination
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Illumination",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${astro.moonIllumination}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AirQualityCard(airQuality: AirQuality) {
    val airQualityIndex = airQuality.usEpaIndex
    val airQualityText = when (airQualityIndex) {
        1 -> "Good"
        2 -> "Moderate"
        3 -> "Unhealthy for sensitive groups"
        4 -> "Unhealthy"
        5 -> "Very Unhealthy"
        6 -> "Hazardous"
        else -> "Unknown"
    }
    
    val airQualityColor = when (airQualityIndex) {
        1 -> Color(0xFF4CAF50) // Green
        2 -> Color(0xFFFFEB3B) // Yellow
        3 -> Color(0xFFFF9800) // Orange
        4 -> Color(0xFFE53935) // Red
        5 -> Color(0xFF9C27B0) // Purple
        6 -> Color(0xFF7E0023) // Dark Red
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Air Quality",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Air Quality Index
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "US EPA Index:",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(airQualityColor)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = airQualityText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Air pollutants
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AirPollutantItem(name = "PM2.5", value = "${airQuality.pm25.toInt()} µg/m³")
                AirPollutantItem(name = "PM10", value = "${airQuality.pm10.toInt()} µg/m³")
                AirPollutantItem(name = "O3", value = "${airQuality.o3.toInt()} µg/m³")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AirPollutantItem(name = "NO2", value = "${airQuality.no2.toInt()} µg/m³")
                AirPollutantItem(name = "SO2", value = "${airQuality.so2.toInt()} µg/m³")
                AirPollutantItem(name = "CO", value = "${airQuality.co.toInt()} µg/m³")
            }
        }
    }
}

@Composable
fun AirPollutantItem(name: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun WeatherDetailsCard(current: Current) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Weather Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // First row of details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WeatherDetailItem(
                    label = "Humidity",
                    value = "${current.humidity}%"
                )
                
                WeatherDetailItem(
                    label = "Pressure",
                    value = "${current.pressureMb.toInt()} mb"
                )
                
                WeatherDetailItem(
                    label = "UV Index",
                    value = "${current.uv}"
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Second row of details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WeatherDetailItem(
                    label = "Wind",
                    value = "${current.windKph} km/h"
                )
                
                WeatherDetailItem(
                    label = "Precip",
                    value = "${current.precipMm} mm"
                )
                
                WeatherDetailItem(
                    label = "Visibility",
                    value = "${current.visKm} km"
                )
            }
        }
    }
}

@Composable
fun WeatherDetailItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ForecastSection(
    forecastDays: List<ForecastDay>,
    isCelsius: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "3-Day Forecast",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            forecastDays.take(3).forEach { day -> 
                ForecastDayItem(day, isCelsius)
                
                if (day != forecastDays.take(3).last()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.surface
                    )
                }
            }
        }
    }
}

@Composable
fun ForecastDayItem(
    forecastDay: ForecastDay,
    isCelsius: Boolean
) {
    val date = try {
        val localDate = LocalDate.parse(forecastDay.date)
        val formatter = DateTimeFormatter.ofPattern("EEE, MMM d")
        localDate.format(formatter)
    } catch (e: Exception) {
        forecastDay.date
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Day
        Text(
            text = date,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        
        // Condition icon
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data("https:${forecastDay.day.condition.icon}")
                .crossfade(true)
                .build(),
            contentDescription = forecastDay.day.condition.text,
            modifier = Modifier.size(40.dp),
            contentScale = ContentScale.Fit
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Condition text
        Text(
            text = forecastDay.day.condition.text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        
        // Min/Max temp
        Text(
            text = "${if (isCelsius) forecastDay.day.mintempC.toInt() else forecastDay.day.mintempF.toInt()}° / ${if (isCelsius) forecastDay.day.maxtempC.toInt() else forecastDay.day.maxtempF.toInt()}°",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}