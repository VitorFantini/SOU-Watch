package com.example.primeiroapp2.presentation.data

import com.example.primeiroapp2.presentation.BusSchedule
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.TimeUnit

// Reconstructed from the installed SouWatch 1.4 APK.
data class TimetableResponse(val timetable: TimetableDetails?)
data class TimetableDetails(val trips: List<TripData>?, val directions: List<DirectionData>?, val exceptions: List<JsonElement>?)
data class TripData(val tripId: Long, val tripDesc: String, val directionId: Int, val seq: Int?)
data class DirectionData(val directionId: Int, val services: List<ServiceData>)
data class ServiceData(val days: List<Boolean>, val start: String?, val end: String?, val departures: List<DepartureData>)
data class DepartureData(val dep: String, val seq: Int)
data class ApiStop(val stopId: Long, val name: String, val lat: Double, @SerializedName(value="lng", alternate=["lon"]) val lng: Double)
data class TripDetails(val stops: List<ApiStop>, val shape: String?)
data class BusVehicle(
    val prefixo: String? = null,
    @SerializedName(value="lat", alternate=["latitude"]) val lat: Double,
    @SerializedName(value="lng", alternate=["longitude", "lon"]) val lng: Double,
    val delay: Int? = null, val vehicleId: String? = null,
    val positionTime: String? = null, val startTime: String? = null
)
data class ViagemConfirmada(val tripId: Long, val horario: String, val detalhes: TripDetails, val data: LocalDate)
enum class EstadoHorario { LOCALIZADO, SEM_VEICULO, GPS_ANTIGO, INCONCLUSIVO }
data class ChecagemHorario(val estado: EstadoHorario, val veiculo: BusVehicle? = null, val mensagem: String = "") {
    fun estadoEm(agora: Instant): EstadoHorario = if(estado==EstadoHorario.LOCALIZADO && (veiculo==null || !RastreamentoCorreto.gpsValido(veiculo,agora))) EstadoHorario.GPS_ANTIGO else estado
}
interface MobilibusApiService {
    @GET("timetable?v=2") suspend fun buscarTabelaHoraria(@Query("project_id") projectId: String = "317", @Query("route_id") routeId: String): TimetableResponse
    @GET("trip-details?v=2") suspend fun buscarDetalhes(@Query("project_id") projectId: String = "317", @Query("trip_id") tripId: String): TripDetails
    @GET("vehicles?v=1") suspend fun buscarVeiculoPorTripId(@Query("project_id") projectId: String = "317", @Query("trip_id") tripId: String): List<BusVehicle>
}
object BusRepository {
    val apiMobilibus: MobilibusApiService by lazy {
        val client = OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS).addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("User-Agent", "Dart/3.9 (dart:io)")
                .header("x-mob-app_name", "SOU").header("x-mob-project-id", "317").build())
        }.build()
        Retrofit.Builder().baseUrl("https://ss7u5urlxs.singularcdn.net.br/api/")
            .client(client).addConverterFactory(GsonConverterFactory.create()).build().create(MobilibusApiService::class.java)
    }
    private val details = mutableMapOf<Long, Pair<Instant, TripDetails>>()
    private val mutex = Mutex()
    suspend fun detalhes(tripId: Long): TripDetails = mutex.withLock {
        val now = Instant.now()
        details[tripId]?.takeIf { java.time.Duration.between(it.first, now).seconds < 3600 }?.second
            ?: apiMobilibus.buscarDetalhes(tripId=tripId.toString()).also { details[tripId] = now to it }
    }
    suspend fun tabela(linha: String) = requireNotNull(apiMobilibus.buscarTabelaHoraria(routeId=linha).timetable) { "Tabela indisponível" }
    suspend fun confirmarViagem(linha: String, sentido: Int, horario: BusSchedule): ViagemConfirmada {
        val hoje = LocalDate.now(RastreamentoCorreto.fuso)
        val trip = RastreamentoCorreto.encontrarViagem(tabela(linha), sentido, horario.horaSaida, hoje, horario.apiTripId)
        return ViagemConfirmada(trip.tripId, horario.horaSaida, detalhes(trip.tripId), hoje)
    }
    suspend fun buscarVeiculoCorreto(viagem: ViagemConfirmada, fixado: String?): BusVehicle? =
        RastreamentoCorreto.selecionarVeiculo(apiMobilibus.buscarVeiculoPorTripId(tripId=viagem.tripId.toString()), viagem.horario, fixado)
    fun buildGoogleMapsUrl(origLat: Double, origLng: Double, destLat: Double = origLat, destLng: Double = origLng) = "geo:0,0?q=$origLat,$origLng"
}
