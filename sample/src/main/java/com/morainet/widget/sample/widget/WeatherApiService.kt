package com.morainet.widget.sample.widget

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Open-Meteo 免费天气 API 客户端。
 *
 * Open-Meteo 无需 API Key，适合开发与示例使用。
 * 文档: https://open-meteo.com/en/docs
 */
object WeatherApiService {

    private const val BASE_URL = "https://api.open-meteo.com/v1/forecast"
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 10_000

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 请求当前天气数据。
     *
     * @param latitude  纬度
     * @param longitude 经度
     * @return API 原始 JSON 响应
     * @throws IOException 网络错误
     */
    suspend fun fetchCurrentWeather(
        latitude: Double = 31.23,
        longitude: Double = 121.47,
    ): WeatherApiResponse {
        val url = URL(
            "$BASE_URL" +
                "?latitude=$latitude" +
                "&longitude=$longitude" +
                "&current=temperature_2m,weather_code,wind_speed_10m" +
                "&timezone=auto",
        )
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            requestMethod = "GET"
        }

        return connection.inputStream.use { input ->
            val body = BufferedReader(InputStreamReader(input)).readText()
            val root = json.parseToJsonElement(body).jsonObject
            val current = root["current"]!!.jsonObject

            WeatherApiResponse(
                temperature = current["temperature_2m"]!!.jsonPrimitive.content.toDoubleOrNull() ?: 0.0,
                weatherCode = current["weather_code"]!!.jsonPrimitive.int,
                windSpeed = current["wind_speed_10m"]?.jsonPrimitive?.content?.toDoubleOrNull(),
            )
        }.also {
            connection.disconnect()
        }
    }
}

/**
 * Open-Meteo API 返回的当前天气数据。
 */
data class WeatherApiResponse(
    val temperature: Double,
    val weatherCode: Int,
    val windSpeed: Double?,
)
