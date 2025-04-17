package com.example.nimbus.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherResponse(
    val location: Location,
    val current: Current,
    val forecast: Forecast
)

@JsonClass(generateAdapter = true)
data class Location(
    val name: String,
    val region: String,
    val country: String,
    val lat: Double,
    val lon: Double,
    @Json(name = "tz_id") val timezoneId: String,
    @Json(name = "localtime_epoch") val localtimeEpoch: Long,
    val localtime: String
)

@JsonClass(generateAdapter = true)
data class Current(
    @Json(name = "last_updated_epoch") val lastUpdatedEpoch: Long,
    @Json(name = "last_updated") val lastUpdated: String,
    @Json(name = "temp_c") val tempC: Double,
    @Json(name = "temp_f") val tempF: Double,
    @Json(name = "is_day") val isDay: Int,
    val condition: Condition,
    @Json(name = "wind_mph") val windMph: Double,
    @Json(name = "wind_kph") val windKph: Double,
    @Json(name = "wind_degree") val windDegree: Int,
    @Json(name = "wind_dir") val windDir: String,
    @Json(name = "pressure_mb") val pressureMb: Double,
    @Json(name = "pressure_in") val pressureIn: Double,
    @Json(name = "precip_mm") val precipMm: Double,
    @Json(name = "precip_in") val precipIn: Double,
    val humidity: Int,
    val cloud: Int,
    @Json(name = "feelslike_c") val feelslikeC: Double,
    @Json(name = "feelslike_f") val feelslikeF: Double,
    @Json(name = "vis_km") val visKm: Double,
    @Json(name = "vis_miles") val visMiles: Double,
    val uv: Double,
    @Json(name = "gust_mph") val gustMph: Double,
    @Json(name = "gust_kph") val gustKph: Double,
    @Json(name = "air_quality") val airQuality: AirQuality
)

@JsonClass(generateAdapter = true)
data class Condition(
    val text: String,
    val icon: String,
    val code: Int
)

@JsonClass(generateAdapter = true)
data class AirQuality(
    val co: Double,
    val no2: Double,
    val o3: Double,
    val so2: Double,
    @Json(name = "pm2_5") val pm25: Double,
    val pm10: Double,
    @Json(name = "us-epa-index") val usEpaIndex: Int,
    @Json(name = "gb-defra-index") val gbDefraIndex: Int
)

@JsonClass(generateAdapter = true)
data class Forecast(
    val forecastday: List<ForecastDay>
)

@JsonClass(generateAdapter = true)
data class ForecastDay(
    val date: String,
    @Json(name = "date_epoch") val dateEpoch: Long,
    val day: Day,
    val astro: Astro,
    val hour: List<Hour>
)

@JsonClass(generateAdapter = true)
data class Day(
    @Json(name = "maxtemp_c") val maxtempC: Double,
    @Json(name = "maxtemp_f") val maxtempF: Double,
    @Json(name = "mintemp_c") val mintempC: Double,
    @Json(name = "mintemp_f") val mintempF: Double,
    @Json(name = "avgtemp_c") val avgtempC: Double,
    @Json(name = "avgtemp_f") val avgtempF: Double,
    @Json(name = "maxwind_mph") val maxwindMph: Double,
    @Json(name = "maxwind_kph") val maxwindKph: Double,
    @Json(name = "totalprecip_mm") val totalprecipMm: Double,
    @Json(name = "totalprecip_in") val totalprecipIn: Double,
    @Json(name = "totalsnow_cm") val totalsnowCm: Double,
    @Json(name = "avgvis_km") val avgvisKm: Double,
    @Json(name = "avgvis_miles") val avgvisMiles: Double,
    @Json(name = "avghumidity") val avghumidity: Double,
    @Json(name = "daily_will_it_rain") val dailyWillItRain: Int,
    @Json(name = "daily_chance_of_rain") val dailyChanceOfRain: Int,
    @Json(name = "daily_will_it_snow") val dailyWillItSnow: Int,
    @Json(name = "daily_chance_of_snow") val dailyChanceOfSnow: Int,
    val condition: Condition,
    val uv: Double
)

@JsonClass(generateAdapter = true)
data class Astro(
    val sunrise: String,
    val sunset: String,
    val moonrise: String,
    val moonset: String,
    @Json(name = "moon_phase") val moonPhase: String,
    @Json(name = "moon_illumination") val moonIllumination: String,
    @Json(name = "is_moon_up") val isMoonUp: Int,
    @Json(name = "is_sun_up") val isSunUp: Int
)

@JsonClass(generateAdapter = true)
data class Hour(
    @Json(name = "time_epoch") val timeEpoch: Long,
    val time: String,
    @Json(name = "temp_c") val tempC: Double,
    @Json(name = "temp_f") val tempF: Double,
    @Json(name = "is_day") val isDay: Int,
    val condition: Condition,
    @Json(name = "wind_mph") val windMph: Double,
    @Json(name = "wind_kph") val windKph: Double,
    @Json(name = "wind_degree") val windDegree: Int,
    @Json(name = "wind_dir") val windDir: String,
    @Json(name = "pressure_mb") val pressureMb: Double,
    @Json(name = "pressure_in") val pressureIn: Double,
    @Json(name = "precip_mm") val precipMm: Double,
    @Json(name = "precip_in") val precipIn: Double,
    val humidity: Int,
    val cloud: Int,
    @Json(name = "feelslike_c") val feelslikeC: Double,
    @Json(name = "feelslike_f") val feelslikeF: Double,
    @Json(name = "windchill_c") val windchillC: Double,
    @Json(name = "windchill_f") val windchillF: Double,
    @Json(name = "heatindex_c") val heatindexC: Double,
    @Json(name = "heatindex_f") val heatindexF: Double,
    @Json(name = "dewpoint_c") val dewpointC: Double,
    @Json(name = "dewpoint_f") val dewpointF: Double,
    @Json(name = "will_it_rain") val willItRain: Int,
    @Json(name = "chance_of_rain") val chanceOfRain: Int,
    @Json(name = "will_it_snow") val willItSnow: Int,
    @Json(name = "chance_of_snow") val chanceOfSnow: Int,
    @Json(name = "vis_km") val visKm: Double,
    @Json(name = "vis_miles") val visMiles: Double,
    @Json(name = "gust_mph") val gustMph: Double,
    @Json(name = "gust_kph") val gustKph: Double,
    val uv: Double
)