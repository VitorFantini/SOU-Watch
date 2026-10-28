package com.example.primeiroapp2.presentation.data
fun mensagemConsulta(e: Exception): String {
    android.util.Log.w("SouWatch", "Falha na consulta", e)
    return when(e) {
        is java.net.UnknownHostException, is java.net.ConnectException -> "Sem conexão. Verifique a internet e tente novamente."
        is java.net.SocketTimeoutException -> "A consulta demorou. Tente novamente."
        is IllegalArgumentException -> "Dados indisponíveis para esta viagem. Selecione o horário novamente."
        else -> "Não foi possível atualizar. Tente novamente."
    }
}
