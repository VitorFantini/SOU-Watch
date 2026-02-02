package com.example.primeiroapp2.presentation.data

data class BusLine(
    val id: String,
    val numero: String,
    val nome: String
)

// O Banco de Dados Estático: Lista fixa das linhas
object LineRepository {

    val supportedLines = listOf(
        BusLine(
            id = "942455",
            numero = "01",
            nome = "Pacaembu x UFSCar Norte"
        ),
        BusLine(
            id = "942430",
            numero = "06",
            nome = "Cardinalli x Vila São José"
        ),
        BusLine(
            id = "942419",
            numero = "32",
            nome = "Monte Carlo x Paulistano"
        )
    )

    fun getLineById(routeId: String): BusLine? {
        return supportedLines.find { it.id == routeId }
    }
}