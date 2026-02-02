package com.example.primeiroapp2.presentation.data

import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// =================================================================
// 1. API MOBILIBUS
// =================================================================

data class TimetableResponse(val timetable: TimetableDetails?)
data class TimetableDetails(val trips: List<TripData>?)
data class TripData(
    val tripId: Long,       // ID da Viagem que o ViewModel precisa caçar
    val tripDesc: String,
    val directionId: Int
)

data class BusVehicle(
    val prefixo: String,
    @SerializedName("lat", alternate = ["latitude"]) val lat: Double,
    @SerializedName("lng", alternate = ["longitude", "lon"]) val lng: Double,
    val delay: Int? = null,
    @SerializedName("vehicleId") val vehicleId: String? = null
)

// =================================================================
// 2.  GOOGLE DIRECTIONS API
// =================================================================

data class GoogleDirectionsResponse(val routes: List<Route>?)
data class Route(val legs: List<Leg>?)
data class Leg(
    val distance: Distance?,
    val duration: Duration?,
    @SerializedName("duration_in_traffic") val durationInTraffic: Duration?
)
data class Distance(
    val text: String, // Ex: "6.0 km"
    val value: Int    // Distância em metros
)
data class Duration(
    val text: String, // Ex: "10 mins"
    val value: Int    // Duração em segundos
)

// =================================================================
// 3. INTERFACES RETROFIT
// =================================================================

interface MobilibusApiService {
    @GET("timetable?v=2")
    suspend fun buscarTabelaHoraria(
        @Query("project_id") projectId: String = "317",
        @Query("route_id") routeId: String
    ): TimetableResponse

    @GET("vehicles?v=1")
    suspend fun buscarVeiculoPorTripId(
        @Query("project_id") projectId: String = "317",
        @Query("trip_id") tripId: String,
        @Header("User-Agent") agent: String = "Dart/3.9 (dart:io)",
        @Header("x-mob-app_name") appName: String = "SOU",
        @Header("x-mob-project-id") headerProjeto: String = "317"
    ): List<BusVehicle>
}

interface GoogleMapsApiService {
    @GET("directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("mode") mode: String = "driving",
        @Query("traffic_model") trafficModel: String? = null,
        @Query("departure_time") departureTime: String? = null,
        @Query("key") apiKey: String
    ): GoogleDirectionsResponse
}

// =================================================================
// 4. REPOSITÓRIO (CAÇA, ROTA E URL)
// =================================================================

object BusRepository {

    // --- CONSTANTES DE API ---
    private const val BASE_API_MOBILIBUS = "https://ss7u5urlxs.singularcdn.net.br/api/"
    private const val GOOGLE_API_BASE_URL = "https://maps.googleapis.com/maps/api/"


    private const val GOOGLE_API_KEY = "CHAVE_AQUI"

    // --- OBJETOS DE API ---
    val apiMobilibus: MobilibusApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_API_MOBILIBUS)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MobilibusApiService::class.java)
    }

    val apiGoogle: GoogleMapsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(GOOGLE_API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleMapsApiService::class.java)
    }

    /**
     * Objeto de dados simples para retorno da rota.
     */
    data class RouteResult(
        val distanceText: String,
        val durationText: String,
        val durationSeconds: Int
    )

    // =================================================================
    // FUNÇÕES DE BUSCA MOBILIBUS (CAÇA EM CASCATA)
    // =================================================================

    suspend fun buscarVeiculoEmCascata(linhaId: String): BusVehicle? = withContext(Dispatchers.IO) {

        val response = try {
            apiMobilibus.buscarTabelaHoraria(routeId = linhaId)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }

        val trips = response.timetable?.trips ?: emptyList()

        for (trip in trips) {
            val tripIdString = trip.tripId.toString()

            val veiculos = try {
                apiMobilibus.buscarVeiculoPorTripId(tripId = tripIdString)
            } catch (e: Exception) {
                continue
            }

            if (veiculos.isNotEmpty()) {
                return@withContext veiculos[0]
            }
        }
        return@withContext null
    }

    suspend fun buscarVeiculos(linhaId: String): List<BusVehicle> = withContext(Dispatchers.IO) {
        val veiculo = buscarVeiculoEmCascata(linhaId)
        return@withContext if (veiculo != null) listOf(veiculo) else emptyList()
    }

    // =================================================================
    // FUNÇÃO GOOGLE DIRECTIONS (ROTEAMENTO REAL COM FALLBACK)
    // =================================================================

    suspend fun buscarRotaETempo(origLat: Double, origLng: Double, destLat: Double, destLng: Double): RouteResult? = withContext(Dispatchers.IO) {
        val origin = "$origLat,$origLng"
        val destination = "$destLat,$destLng"

        // PASSO 1: Tentar com tráfego (ETA mais precisa)
        try {
            val responseTraffic = apiGoogle.getDirections(
                origin = origin,
                destination = destination,
                trafficModel = "best_guess",
                departureTime = (System.currentTimeMillis() / 1000).toString(),
                apiKey = GOOGLE_API_KEY
            )

            val legTraffic = responseTraffic.routes?.firstOrNull()?.legs?.firstOrNull()

            if (legTraffic?.durationInTraffic != null && legTraffic.distance != null) {
                return@withContext RouteResult(
                    distanceText = legTraffic.distance.text,
                    durationText = legTraffic.durationInTraffic.text,
                    durationSeconds = legTraffic.durationInTraffic.value
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // PASSO 2: Fallback sem tráfego (tempo normal)
        try {
            val responseNormal = apiGoogle.getDirections(
                origin = origin,
                destination = destination,
                trafficModel = null,
                departureTime = null,
                apiKey = GOOGLE_API_KEY
            )

            val legNormal = responseNormal.routes?.firstOrNull()?.legs?.firstOrNull()

            if (legNormal?.duration != null && legNormal.distance != null) {
                return@withContext RouteResult(
                    distanceText = legNormal.distance.text,
                    durationText = legNormal.duration.text,
                    durationSeconds = legNormal.duration.value
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // PASSO 3: Falha total
        return@withContext null
    }

    // =================================================================
    // FUNÇÃO PARA GERAR URL DO GOOGLE MAPS (DESTINO: O ÔNIBUS)
    // =================================================================

    /**
     * Gera um URL de Intent do Google Maps para localizar o ônibus (destino).
     * O Ônibus está em (origLat, origLng).
     */
    fun buildGoogleMapsUrl(origLat: Double, origLng: Double, destLat: Double, destLng: Double): String {

        //  as coordenadas do ônibus (origLat, origLng) como o PONTO DE INTERESSE (q)
        val busLocation = URLEncoder.encode("$origLat,$origLng", StandardCharsets.UTF_8.toString())

        return "geo:0,0?q=$busLocation"
    }
}