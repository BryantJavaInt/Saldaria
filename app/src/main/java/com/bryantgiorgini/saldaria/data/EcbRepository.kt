package com.bryantgiorgini.saldaria.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import java.math.BigDecimal
import java.math.RoundingMode

class EcbRepository(private val apiService: EcbApiService? = null) {
    
    private val service: EcbApiService by lazy {
        apiService ?: run {
            val json = Json { ignoreUnknownKeys = true }
            val retrofit = Retrofit.Builder()
                .baseUrl("https://data-api.ecb.europa.eu/")
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
            retrofit.create(EcbApiService::class.java)
        }
    }

    suspend fun getLatestRate(): Result<BigDecimal> {
        return try {
            val response = service.getLatestInterestRate()
            
            // Navegamos por el JSON: dataSets[0].series["0:0:0:0:0:0:0"].observations["0"][0]
            val dataSet = response.dataSets.firstOrNull() 
                ?: throw IllegalStateException("Respuesta del BCE sin dataSets")
            val series = dataSet.series.values.firstOrNull() 
                ?: throw IllegalStateException("Respuesta del BCE sin series")
            // Tomamos la observación más reciente (índice más alto, asumiendo lastNObservations=1, será "0")
            val observations = series.observations.values.firstOrNull()
                ?: throw IllegalStateException("Respuesta del BCE sin observations")
            val rateDouble = observations.firstOrNull()
                ?: throw IllegalStateException("Respuesta del BCE sin valor de tipo")
                
            val rate = BigDecimal.valueOf(rateDouble).setScale(2, RoundingMode.HALF_UP)
            Result.success(rate)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
