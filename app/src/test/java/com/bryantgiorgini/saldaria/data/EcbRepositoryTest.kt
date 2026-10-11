package com.bryantgiorgini.saldaria.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class EcbRepositoryTest {

    @Test
    fun `getLatestRate parsea correctamente el JSON simulado`() = runBlocking {
        // Simulamos la respuesta de la API basándonos en el JSON real del BCE
        val mockResponse = EcbResponse(
            dataSets = listOf(
                DataSet(
                    series = mapOf(
                        "0:0:0:0:0:0:0" to SeriesData(
                            observations = mapOf(
                                "0" to listOf(2.65, 0.0, 0.0, null, null)
                            )
                        )
                    )
                )
            )
        )

        val mockService = object : EcbApiService {
            override suspend fun getLatestInterestRate(): EcbResponse {
                return mockResponse
            }
        }

        val repository = EcbRepository(mockService)
        val result = repository.getLatestRate()

        assertTrue(result.isSuccess)
        val rate = result.getOrNull()!!
        
        // Verificamos que parsea el 2.65 y lo convierte a BigDecimal con escala 2
        assertEquals(BigDecimal("2.65").setScale(2, RoundingMode.HALF_UP), rate)
    }

    @Test
    fun `getLatestRate maneja errores adecuadamente`() = runBlocking {
        val mockService = object : EcbApiService {
            override suspend fun getLatestInterestRate(): EcbResponse {
                throw RuntimeException("Error de red simulado")
            }
        }

        val repository = EcbRepository(mockService)
        val result = repository.getLatestRate()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeException)
        assertEquals("Error de red simulado", result.exceptionOrNull()?.message)
    }
}
