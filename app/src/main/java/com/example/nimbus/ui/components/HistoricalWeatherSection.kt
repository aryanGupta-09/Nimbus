package com.example.nimbus.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.nimbus.data.model.WeatherResponse
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HistoricalWeatherSection(
    historicalData: List<WeatherResponse>,
    isLoading: Boolean,
    onRetry: () -> Unit,
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
                text = "Historical Weather",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (historicalData.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No historical data available",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                historicalData.take(7).forEach { weatherData ->
                    val forecastDay = weatherData.forecast.forecastday.firstOrNull()
                    if (forecastDay != null) {
                        HistoricalDayItem(weatherData, isCelsius)
                        
                        if (weatherData != historicalData.take(7).last()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.surface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoricalDayItem(
    weatherData: WeatherResponse,
    isCelsius: Boolean
) {
    val forecastDay = weatherData.forecast.forecastday.firstOrNull()
    if (forecastDay != null) {
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
}