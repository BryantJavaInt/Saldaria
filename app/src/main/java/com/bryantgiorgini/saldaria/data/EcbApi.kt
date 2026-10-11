package com.bryantgiorgini.saldaria.data

import kotlinx.serialization.Serializable
import retrofit2.http.GET

/** Respuesta de la API del BCE */
@Serializable
data class EcbResponse(
    val dataSets: List<DataSet>
)

@Serializable
data class DataSet(
    val series: Map<String, SeriesData>
)

@Serializable
data class SeriesData(
    val observations: Map<String, List<Double?>>
)

interface EcbApiService {
    @GET("service/data/FM/B.U2.EUR.4F.KR.MRR_FR.LEV?lastNObservations=1&format=jsondata")
    suspend fun getLatestInterestRate(): EcbResponse
}
