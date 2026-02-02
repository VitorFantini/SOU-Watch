package com.example.primeiroapp2.presentation.data
import com.example.primeiroapp2.presentation.BusSchedule

// --- CLASSE DE DADOS ÚNICA ---
data class BusStop(
    val sequencia: Int,
    val nome: String,
    val lat: Double,
    val lng: Double
)


object BusDatabase {
    // =========================================================================
    // LINHA 01: PACAEMBU x UFSCAR
    // =========================================================================

    private val stopsLinha01_Ida = listOf(
        BusStop(1, "R. Irineu Rios 410 (Jd. Beatriz)", -22.037233, -47.904957),
        BusStop(2, "R. Hermínio Bernasconi 269", -22.037500, -47.902000),
        BusStop(3, "R. Hermínio Bernasconi 955", -22.036800, -47.900500),
        BusStop(4, "R. Maestro Adolpho R. Caputo 133", -22.036200, -47.899600),
        BusStop(5, "R. Maestro Adolpho R. Caputo 340", -22.036600, -47.897600),
        BusStop(6, "Rua Coronel Leopoldo Prado", -22.036173, -47.895802),
        BusStop(7, "Av. Theodureto De Camargo", -22.035349, -47.895271),
        BusStop(8, "Av. Sallum 1391", -22.032900, -47.896366),
        BusStop(9, "Av. Sallum 1231a", -22.031792, -47.897106),
        BusStop(10, "Av. Sallum 995", -22.029886, -47.898407),
        BusStop(11, "Av. Sallum 617", -22.027651, -47.899948),
        BusStop(12, "R. José Benetti 478", -22.025816, -47.900215),
        BusStop(13, "Av. Dr. Teixeira De Barros 245", -22.024807, -47.899254),
        BusStop(14, "R. Cândido Padim 71", -22.023352, -47.897968),
        BusStop(15, "Rua Bento Carlos 10", -22.022526, -47.895214),
        BusStop(16, "R. Santa Cruz 188", -22.023483, -47.892918),
        BusStop(17, "Av. São Carlos 1228 (Centro)", -22.022182, -47.890549),
        BusStop(18, "Av. São Carlos 1466", -22.020166, -47.890598),
        BusStop(19, "Av. São Carlos 1650", -22.018484, -47.890667),
        BusStop(20, "Av. São Carlos 1864", -22.016266, -47.890717),
        BusStop(21, "Av. São Carlos 2166", -22.013783, -47.890766),
        BusStop(22, "Av. São Carlos 2665", -22.010242, -47.890797),
        BusStop(23, "Av. São Carlos 2911", -22.007475, -47.890873),
        BusStop(24, "Rua São Joaquim 2420", -22.005075, -47.889225),
        BusStop(25, "Av. São Carlos 3200", -22.003172, -47.890934),
        BusStop(26, "Av. São Carlos 3606 (Vila Marina)", -22.000450, -47.891048),
        BusStop(27, "Av. Prof. Luís Augusto De Oliveira", -21.997807, -47.891083),
        BusStop(28, "Av. Salgado Filho", -21.996950, -47.889091),
        BusStop(29, "R. Santos Dumont 165", -21.995649, -47.886818),
        BusStop(30, "Av. do Bosque", -21.990183, -47.884434),
        BusStop(31, "Av. Biblioteca Comunitária (UFSCar)", -21.983515, -47.883297)
    )

    private val stopsLinha01_Volta_Normal = listOf(
        BusStop(1, "Av. Biblioteca Comunitária (Ponto Inicial)", -21.982553, -47.880603),
        BusStop(2, "Av. Biblioteca Comunitária", -21.983312, -47.882961),
        BusStop(3, "Rua Das Saíras", -21.990999, -47.883380),
        BusStop(4, "R. Prof. José Ferraz Camargo 454", -21.992580, -47.884548),
        BusStop(5, "R. Prof. José Ferraz Camargo 170", -21.995100, -47.887248),
        BusStop(6, "Avenida Salgado Filho", -21.996816, -47.889091),
        BusStop(7, "Av. Prof. Luís Augusto De Oliveira", -21.998155, -47.891357),
        BusStop(8, "Rua Bernardino Fernandes Nunes", -22.002353, -47.891170),
        BusStop(9, "Rua Dona Alexandrina (Centro)", -22.005094, -47.889648),
        BusStop(10, "Rua Dona Alexandrina (Santander)", -22.007963, -47.889961),
        BusStop(11, "Rua Dona Alexandrina 1550", -22.012014, -47.889865),
        BusStop(12, "Rua Dona Alexandrina 1366", -22.013885, -47.889781),
        BusStop(13, "Rua Dona Alexandrina 1063", -22.016649, -47.889717),
        BusStop(14, "Rua Dona Alexandrina 864", -22.018400, -47.889682),
        BusStop(15, "Rua Dona Alexandrina 672", -22.020017, -47.889617),
        BusStop(16, "R. Bento Carlos 467", -22.022483, -47.890983),
        BusStop(17, "R. Bento Carlos 197", -22.022516, -47.893318),
        BusStop(18, "Rua Visconde De Inhaúma 86", -22.022573, -47.895629),
        BusStop(19, "R. Gen. Osório 122", -22.021766, -47.897716),
        BusStop(20, "Av. José Pereira Lopes 112", -22.021850, -47.900451),
        BusStop(21, "Av. José Pereira Lopes 358", -22.022235, -47.903423),
        BusStop(22, "Av. Sallum 190", -22.023729, -47.902652),
        BusStop(23, "Av. Sallum 530", -22.026317, -47.900901),
        BusStop(24, "Av. Sallum 778", -22.028615, -47.899383),
        BusStop(25, "Avenida Sallum (Igreja)", -22.030422, -47.898124),
        BusStop(26, "Av. Sallum 1180", -22.031600, -47.897335),
        BusStop(27, "Av. Sallum 1376", -22.032850, -47.896465),
        BusStop(28, "Avenida Sallum 1587", -22.035095, -47.894958),
        BusStop(29, "R. Cel. Leopoldo Prado 780", -22.036127, -47.895988),
        BusStop(30, "R. Prof Helvidio Gouvêa 1", -22.036085, -47.896965),
        BusStop(31, "R. Prof Helvidio Gouvêa 2", -22.035442, -47.900459),
        BusStop(32, "R. Cap. Manoel Alves Carneiro", -22.035972, -47.902175),
        BusStop(33, "R. Irineu Rios 410 (Final)", -22.037233, -47.904956)
    )

    private val stopsLinha01_Volta_PE = listOf(
        BusStop(1, "Estrada Municipal Guilherme Scatena", -21.98800278, -47.87672424),
        BusStop(2, "Rua Dos Sabiás", -21.99066734, -47.88208008),
        BusStop(3, "R. Prof. José Ferraz Camargo 454", -21.99258041, -47.88454819),
        BusStop(4, "R. Prof. José Ferraz Camargo 170", -21.99510002, -47.88724899),
        BusStop(5, "Avenida Salgado Filho", -21.99681664, -47.88909149),
        BusStop(6, "Av. Prof. Luís Augusto De Oliveira", -21.99815559, -47.89135742),
        BusStop(7, "Rua Bernardino Fernandes Nunes", -22.00235367, -47.8911705),
        BusStop(8, "Rua Dona Alexandrina (Centro)", -22.00509453, -47.88964844),
        BusStop(9, "Rua Dona Alexandrina", -22.00796318, -47.88996124),
        BusStop(10, "Rua Dona Alexandrina 1550", -22.01201439, -47.88986588),
        BusStop(11, "Rua Dona Alexandrina 1366", -22.0138855, -47.88978195),
        BusStop(12, "Rua Dona Alexandrina 1063", -22.01664925, -47.8897171),
        BusStop(13, "Rua Dona Alexandrina 864", -22.01840019, -47.88968277),
        BusStop(14, "Rua Dona Alexandrina 672", -22.02001762, -47.88961792),
        BusStop(15, "R. Bento Carlos 467", -22.02248383, -47.89098358),
        BusStop(16, "R. Bento Carlos 197", -22.02251625, -47.89331818),
        BusStop(17, "Rua Visconde De Inhaúma 86", -22.02257347, -47.89562988),
        BusStop(18, "R. Gen. Osório 122", -22.02176666, -47.89771652),
        BusStop(19, "Av. José Pereira Lopes 112", -22.02185059, -47.90045166),
        BusStop(20, "Av. José Pereira Lopes 358", -22.02223587, -47.90342331),
        BusStop(21, "Av. Sallum 190", -22.02372932, -47.90265274),
        BusStop(22, "Av. Sallum 530", -22.0263176, -47.90090179),
        BusStop(23, "Av. Sallum 778", -22.02861595, -47.89938354),
        BusStop(24, "Avenida Sallum", -22.03042221, -47.89812469),
        BusStop(25, "Av. Sallum 1180", -22.03160095, -47.89733505),
        BusStop(26, "Av. Sallum 1376", -22.03285027, -47.8964653),
        BusStop(27, "Avenida Sallum 1587", -22.03509521, -47.8949585),
        BusStop(28, "R. Cel. Leopoldo Prado 780", -22.03612709, -47.89598846),
        BusStop(29, "R. Prof Helvidio Gouvêa 1", -22.03608513, -47.89696503),
        BusStop(30, "R. Prof Helvidio Gouvêa 2", -22.03544235, -47.90045929),
        BusStop(31, "R. Cap. Manoel Alves Carneiro", -22.0359726, -47.9021759),
        BusStop(32, "R. Irineu Rios 410 (Final)", -22.03723335, -47.90495682)
    )

    private val horariosLinha01_Ida = listOf(
        BusSchedule("t0600", "06:00 ( UFSCar Norte )"), BusSchedule("t0700", "07:00 ( UFSCar Norte )"),
        BusSchedule("t0735", "07:35 ( UFSCar Norte )"), BusSchedule("t0810", "08:10 ( UFSCar Norte )"),
        BusSchedule("t0830", "08:30 ( UFSCar Norte )"), BusSchedule("t0900", "09:00 ( UFSCar Norte )"),
        BusSchedule("t0930", "09:30 ( UFSCar Norte )"), BusSchedule("t1030", "10:30 ( UFSCar Norte )"),
        BusSchedule("t1100", "11:00 ( UFSCar Norte )"), BusSchedule("t1130", "11:30 ( UFSCar Norte )"),
        BusSchedule("t1200", "12:00 ( UFSCar Norte )"), BusSchedule("t1230", "12:30 ( UFSCar Norte )"),
        BusSchedule("t1300", "13:00 ( UFSCar Norte )"), BusSchedule("t1330", "13:30 ( UFSCar Norte )"),
        BusSchedule("t1400", "14:00 ( UFSCar Norte )"), BusSchedule("t1425", "14:25 ( UFSCar Norte )"),
        BusSchedule("t1430", "14:30 ( UFSCar Norte )"), BusSchedule("t1455", "14:55 ( UFSCar Norte )"),
        BusSchedule("t1500", "15:00 ( UFSCar Norte )"), BusSchedule("t1530", "15:30 ( UFSCar Norte )"),
        BusSchedule("t1600", "16:00 ( UFSCar Norte )"), BusSchedule("t1630", "16:30 ( UFSCar Norte )"),
        BusSchedule("t1700", "17:00 ( UFSCar Norte )"), BusSchedule("t1730", "17:30 ( UFSCar Norte )"),
        BusSchedule("t1800", "18:00 ( UFSCar Norte )"), BusSchedule("t1830", "18:30 ( UFSCar Norte )"),
        BusSchedule("t1900", "19:00 ( UFSCar Norte )"), BusSchedule("t1925", "19:25 ( UFSCar Norte )"),
        BusSchedule("t1950", "19:50 ( UFSCar Norte )"), BusSchedule("t2120", "21:20 ( UFSCar Norte )"),
        BusSchedule("t2200", "22:00 ( UFSCar Norte )"), BusSchedule("t2235", "22:35 ( UFSCar Norte )")
    )

    private val horariosLinha01_Volta = listOf(
        BusSchedule("v0600", "06:00 ( Pacaembu PE )"), BusSchedule("v0630", "06:30 ( Pacaembu )"),
        BusSchedule("v0700", "07:00 ( Pacaembu PE )"), BusSchedule("v0735", "07:35 ( Pacaembu )"),
        BusSchedule("v0810", "08:10 ( Pacaembu )"), BusSchedule("v0850", "08:50 ( Pacaembu )"),
        BusSchedule("v0915", "09:15 ( Pacaembu )"), BusSchedule("v0945", "09:45 ( Pacaembu )"),
        BusSchedule("v1015", "10:15 ( Pacaembu )"), BusSchedule("v1045", "10:45 ( Pacaembu )"),
        BusSchedule("v1115", "11:15 ( Pacaembu )"), BusSchedule("v1145", "11:45 ( Pacaembu )"),
        BusSchedule("v1215", "12:15 ( Pacaembu )"), BusSchedule("v1245", "12:45 ( Pacaembu )"),
        BusSchedule("v1315", "13:15 ( Pacaembu )"), BusSchedule("v1345", "13:45 ( Pacaembu )"),
        BusSchedule("v1415", "14:15 ( Pacaembu )"), BusSchedule("v1430", "14:30 ( Pacaembu )"),
        BusSchedule("v1445", "14:45 ( Pacaembu )"), BusSchedule("v1515", "15:15 ( Pacaembu )"),
        BusSchedule("v1545", "15:45 ( Pacaembu )"), BusSchedule("v1615", "16:15 ( Pacaembu )"),
        BusSchedule("v1645", "16:45 ( Pacaembu )"), BusSchedule("v1715", "17:15 ( Pacaembu )"),
        BusSchedule("v1745", "17:45 ( Pacaembu )"), BusSchedule("v1815", "18:15 ( Pacaembu )"),
        BusSchedule("v1845", "18:45 ( Pacaembu )"), BusSchedule("v1910", "19:10 ( Pacaembu )"),
        BusSchedule("v2005", "20:05 ( Pacaembu )"), BusSchedule("v2040", "20:40 ( Pacaembu )"),
        BusSchedule("v2130", "21:30 ( Pacaembu )"), BusSchedule("v2200", "22:00 ( Pacaembu )"),
        BusSchedule("v2235", "22:35 ( Pacaembu )"), BusSchedule("v2310", "23:10 ( Pacaembu )")
    )


    // =========================================================================
    // LINHA 06: CARDINALLI x VILA SÃO JOSÉ
    // =========================================================================

    private val stopsLinha06_Ida = listOf(
        BusStop(1, "R. Jesuíno de Arruda 3101", -22.01956367, -47.88046646),
        BusStop(2, "R. Maj. Manoel Antônio de Mattos 856", -22.02068329, -47.88079834),
        BusStop(3, "R. Maj. Manoel Antônio de Mattos 708", -22.02256584, -47.88076782),
        BusStop(4, "R. Maj. Manoel Antônio de Mattos 383", -22.02605057, -47.88069916),
        BusStop(5, "R. Maj. Manoel Antônio de Mattos 227", -22.02739906, -47.8806839),
        BusStop(6, "R. Maj. Manoel Antônio de Mattos 30", -22.02923393, -47.88061523),
        BusStop(7, "R. Raimundo Corrêa 873", -22.02866173, -47.88230515),
        BusStop(8, "R. Raimundo Corrêa 469", -22.02710152, -47.88596344),
        BusStop(9, "R. Rui Barbosa 276", -22.02441597, -47.88660049),
        BusStop(10, "Av. Comendador Alfredo Maffei", -22.02121925, -47.88331604),
        BusStop(11, "Av. São Carlos 1466", -22.0201664, -47.8905983),
        BusStop(12, "Av. São Carlos 1650", -22.01848412, -47.89066696),
        BusStop(13, "Av. São Carlos 1864", -22.01626587, -47.89071655),
        BusStop(14, "Av. São Carlos 2166", -22.0137825, -47.89076614),
        BusStop(15, "Av. São Carlos 2665", -22.01024246, -47.89079666),
        BusStop(16, "R. Dr. Orlando Damiano 2301", -22.00863266, -47.88816833),
        BusStop(17, "R. Dr. Orlando Damiano 2577", -22.0086174, -47.88529968),
        BusStop(18, "R. Dr. Orlando Damiano 907", -22.00858307, -47.88233185),
        BusStop(19, "R. Miguel Giometti 432", -22.00798416, -47.88106537),
        BusStop(20, "Av. Araraquara 222", -22.0038166, -47.88053513),
        BusStop(21, "Av. Araraquara 390", -22.00203323, -47.87988281),
        BusStop(22, "Av. Araraquara 422", -22.00045013, -47.87931824),
        BusStop(23, "Rua Panamá", -21.99663544, -47.88051987),
        BusStop(24, "Rua Honduras", -21.99803925, -47.87789917),
        BusStop(25, "Rua Honduras 2", -21.99891472, -47.87680435),
        BusStop(26, "Rua Paraguai (Final)", -22.00004768, -47.87388611)
    )

    private val stopsLinha06_Volta = listOf(
        BusStop(1, "Rua Paraguai (Início)", -22.00004768, -47.87388611),
        BusStop(2, "Rua Honduras", -21.99880219, -47.87682724),
        BusStop(3, "Rua Honduras 2", -21.99793625, -47.87789154),
        BusStop(4, "Rua Panamá", -21.99665642, -47.88064957),
        BusStop(5, "Rua Argentina", -21.99952698, -47.87979889),
        BusStop(6, "R. Miguel Giometti 1001", -22.00231934, -47.88122559),
        BusStop(7, "R. Tiradentes 1033", -22.00947189, -47.88220978),
        BusStop(8, "R. Tiradentes 835", -22.00950241, -47.88437271),
        BusStop(9, "R. Tiradentes 579", -22.00953102, -47.88628387),
        BusStop(10, "R. Tiradentes 269", -22.00958061, -47.88913727),
        BusStop(11, "Rua Dona Alexandrina 1550", -22.01201439, -47.88986588),
        BusStop(12, "Rua Dona Alexandrina 1366", -22.0138855, -47.88978195),
        BusStop(13, "Rua Dona Alexandrina 1063", -22.01664925, -47.8897171),
        BusStop(14, "Rua Dona Alexandrina 864", -22.01840019, -47.88968277),
        BusStop(15, "Rua Dona Alexandrina 672", -22.02001762, -47.88961792),
        BusStop(16, "Av. Comendador Alfredo Maffei", -22.02129936, -47.88523865),
        BusStop(17, "Av. Comendador Alfredo Maffei 2", -22.02121925, -47.88331604),
        BusStop(18, "R. São Paulo 371", -22.023983, -47.88565063),
        BusStop(19, "R. São Paulo 151", -22.02586746, -47.8856163),
        BusStop(20, "R. Raimundo Corrêa 872", -22.02864075, -47.88242722),
        BusStop(21, "Rua Totó Leite 31", -22.02938271, -47.87950134),
        BusStop(22, "Rua Totó Leite 281", -22.0276165, -47.87969971),
        BusStop(23, "Rua José Luiz Olaio", -22.02585793, -47.87850952),
        BusStop(24, "Rua Totó Leite 494", -22.02546692, -47.87971497),
        BusStop(25, "Rua Totó Leite 666", -22.02400017, -47.87978363),
        BusStop(26, "Rua Totó Leite 800", -22.02208328, -47.87979889),
        BusStop(27, "Av. São João Batista De La Salle", -22.02144432, -47.87282944),
        BusStop(28, "Rua Irmã Maria São Felix", -22.02017593, -47.87090683),
        BusStop(29, "Rua Irmã Maria São Felix 2", -22.02338409, -47.87123871),
        BusStop(30, "Av. José Ferro 143", -22.0237999, -47.87329865),
        BusStop(31, "R. Lucas Perrone 197", -22.02288246, -47.87680054),
        BusStop(32, "R. Eugênio Franco de Camargo 284", -22.02116585, -47.87791824),
        BusStop(33, "R. Eugênio F. Camargo 1335 (Final)", -22.01948357, -47.87805176)
    )

    private val horariosLinha06_Ida = listOf(
        BusSchedule("h0550", "05:50 ( Vila São José )"), BusSchedule("h0650", "06:50 ( Vila São José )"),
        BusSchedule("h0750", "07:50 ( Vila São José )"), BusSchedule("h0855", "08:55 ( Vila São José )"),
        BusSchedule("h1000", "10:00 ( Vila São José )"), BusSchedule("h1105", "11:05 ( Vila São José )"),
        BusSchedule("h1210", "12:10 ( Vila São José )"), BusSchedule("h1315", "13:15 ( Vila São José )"),
        BusSchedule("h1420", "14:20 ( Vila São José )"), BusSchedule("h1435", "14:35 ( Vila São José )"),
        BusSchedule("h1525", "15:25 ( Vila São José )"), BusSchedule("h1630", "16:30 ( Vila São José )"),
        BusSchedule("h1735", "17:35 ( Vila São José )"), BusSchedule("h1840", "18:40 ( Vila São José )")
    )

    private val horariosLinha06_Volta = listOf(
        BusSchedule("v0620", "06:20 ( Cardinalli )"), BusSchedule("v0720", "07:20 ( Cardinalli )"),
        BusSchedule("v0825", "08:25 ( Cardinalli )"), BusSchedule("v0930", "09:30 ( Cardinalli )"),
        BusSchedule("v1035", "10:35 ( Cardinalli )"), BusSchedule("v1140", "11:40 ( Cardinalli )"),
        BusSchedule("v1245", "12:45 ( Cardinalli )"), BusSchedule("v1350", "13:50 ( Cardinalli )"),
        BusSchedule("v1455", "14:55 ( Cardinalli )"), BusSchedule("v1600", "16:00 ( Cardinalli )"),
        BusSchedule("v1705", "17:05 ( Cardinalli )"), BusSchedule("v1810", "18:10 ( Cardinalli )"),
        BusSchedule("v1915", "19:15 ( Cardinalli )")
    )


    // =========================================================================
    // LINHA 32: MONTE CARLO x PAULISTANO
    // =========================================================================

    // ROTA IDA: Monte Carlo -> Paulistano
    private val stopsLinha32_Ida = listOf(
        BusStop(1, "Travessa Francisco Perrota", -22.04691696, -47.89626694),
        BusStop(2, "Rua Rudolfo Meise Geier 62", -22.04831696, -47.89668274),
        BusStop(3, "Rua Rudolf Meise Geier", -22.05006981, -47.8954277),
        BusStop(4, "Av. Santa Me. Cabrini 283", -22.04840279, -47.89365387),
        BusStop(5, "Avenida Santa Madre Cabrini", -22.04770279, -47.89265442),
        BusStop(6, "Av. Papa Paulo VI 635", -22.04591751, -47.89139938),
        BusStop(7, "R. Allan Kardec 1131", -22.04334068, -47.8880043),
        BusStop(8, "R. Allan Kardec 980", -22.04211617, -47.88871765),
        BusStop(9, "R. Allan Kardec 629", -22.04008293, -47.88981628),
        BusStop(10, "R. Allan Kardec 105", -22.0369339, -47.89158249),
        BusStop(11, "Avenida Dr. Padua Salles", -22.03555107, -47.89261246),
        BusStop(12, "Avenida São Carlos 381", -22.030756, -47.89030075),
        BusStop(13, "Av. São Carlos 660", -22.02738953, -47.89042664),
        BusStop(14, "Av. São Carlos 940", -22.0251503, -47.89049911),
        BusStop(15, "Av. São Carlos 1228", -22.02218246, -47.89054871),
        BusStop(16, "Av. São Carlos 1466", -22.0201664, -47.8905983),
        BusStop(17, "Av. São Carlos 1650", -22.01848412, -47.89066696),
        BusStop(18, "Av. São Carlos 1864", -22.01626587, -47.89071655),
        BusStop(19, "Av. São Carlos 2166", -22.0137825, -47.89076614),
        BusStop(20, "Avenida São Carlos 2665", -22.01024246, -47.89079666),
        BusStop(21, "Avenida São Carlos 2911", -22.0074749, -47.89087296),
        BusStop(22, "Rua São Joaquim 2420", -22.00507545, -47.88922501),
        BusStop(23, "R. Iwagiro Toyama 884", -22.003000, -47.890000),
        BusStop(24, "Rua Iwagiro Toyama 560", -22.001000, -47.891000),
        BusStop(25, "Rod. Eng. Thales de Lorena", -21.995000, -47.892000),
        BusStop(26, "Rod. Eng. Thales de Lorena (Final)", -21.990000, -47.893000)
    )

    // ROTA VOLTA: Paulistano -> Monte Carlo (DADOS COMPLETOS E CONECTADOS)
    private val stopsLinha32_Volta = listOf(
        BusStop(1, "Rua Luigi Mazziero (Início)", -21.9883728, -47.90135574),
        BusStop(2, "R. Ernestino Block 174", -21.98793411, -47.89955139),
        BusStop(3, "R. Iwagiro Toyama 883", -21.98991013, -47.89723587),
        BusStop(4, "R. Iwagiro Toyama 700", -21.9917717, -47.89717484),
        BusStop(5, "R. Iwagiro Toyama 508", -21.99358368, -47.89748383),
        BusStop(6, "R. Iwagiro Toyama 221", -21.99566078, -47.89745712),
        BusStop(7, "R. Iwagiro Toyama 30", -21.99749947, -47.89735031),
        BusStop(8, "Rua Das Camélias 135", -21.99869537, -47.89502716),
        BusStop(9, "Avenida Das Gardênias 302", -21.99938583, -47.89352417),
        BusStop(10, "Alameda Das Azaléias 236", -22.00191689, -47.89235687),
        BusStop(11, "Av. Trab. São-Carlense 451", -22.00436592, -47.89365005),
        BusStop(12, "Rua Dona Alexandrina (Centro)", -22.00509453, -47.88964844),
        BusStop(13, "R. Dona Alexandrina 1550", -22.012014, -47.889865),
        BusStop(14, "R. Bento Carlos 467", -22.022483, -47.890983),
        BusStop(15, "Av. São Carlos (Praça Itália)", -22.0215, -47.8950),
        BusStop(16, "R. Allan Kardec 105", -22.036933, -47.891582),
        BusStop(17, "R. Allan Kardec 629", -22.040082, -47.889816),
        BusStop(18, "Av. Papa Paulo VI 635", -22.045917, -47.891399),
        BusStop(19, "R. Rudolfo Meise Geier", -22.048316, -47.896682),
        BusStop(20, "Travessa Francisco Perrota (Final)", -22.046916, -47.896266)
    )

    private val horariosLinha32_Ida = listOf(
        BusSchedule("h0540", "05:40 ( Paulistano )"), BusSchedule("h0600", "06:00 ( Paulistano )"),
        BusSchedule("h0640", "06:40 ( Paulistano )"), BusSchedule("h0700", "07:00 ( Paulistano )"),
        BusSchedule("h0745", "07:45 ( Paulistano )"), BusSchedule("h0810", "08:10 ( Paulistano )"),
        BusSchedule("h0855", "08:55 ( Paulistano )"), BusSchedule("h0930", "09:30 ( Paulistano )"),
        BusSchedule("h1025", "10:25 ( Paulistano )"), BusSchedule("h1040", "10:40 ( Paulistano )"),
        BusSchedule("h1115", "11:15 ( Paulistano )"), BusSchedule("h1150", "11:50 ( Paulistano )"),
        BusSchedule("h1225", "12:25 ( Paulistano )"), BusSchedule("h1300", "13:00 ( Paulistano )"),
        BusSchedule("h1330", "13:30 ( Paulistano )"), BusSchedule("h1410", "14:10 ( Paulistano )"),
        BusSchedule("h1430", "14:30 ( Paulistano )"), BusSchedule("h1435", "14:35 ( Paulistano )"),
        BusSchedule("h1520", "15:20 ( Paulistano )"), BusSchedule("h1545", "15:45 ( Paulistano )"),
        BusSchedule("h1630", "16:30 ( Paulistano )"), BusSchedule("h1655", "16:55 ( Paulistano )"),
        BusSchedule("h1740", "17:40 ( Paulistano )"), BusSchedule("h1805", "18:05 ( Paulistano )"),
        BusSchedule("h1850", "18:50 ( Paulistano )"), BusSchedule("h1910", "19:10 ( Paulistano )"),
        BusSchedule("h2010", "20:10 ( Paulistano )"), BusSchedule("h2110", "21:10 ( Paulistano )"),
        BusSchedule("h2210", "22:10 ( Paulistano )"), BusSchedule("h2305", "23:05 ( Paulistano )")
    )

    private val horariosLinha32_Volta = listOf(
        BusSchedule("v0540", "05:40 ( Monte Carlo )"), BusSchedule("v0600", "06:00 ( Monte Carlo )"),
        BusSchedule("v0640", "06:40 ( Monte Carlo )"), BusSchedule("v0700", "07:00 ( Monte Carlo )"),
        BusSchedule("v0745", "07:45 ( Monte Carlo )"), BusSchedule("v0810", "08:10 ( Monte Carlo )"),
        BusSchedule("v0855", "08:55 ( Monte Carlo )"), BusSchedule("v0930", "09:30 ( Monte Carlo )"),
        BusSchedule("v1025", "10:25 ( Monte Carlo )"), BusSchedule("v1040", "10:40 ( Monte Carlo )"),
        BusSchedule("v1115", "11:15 ( Monte Carlo )"), BusSchedule("v1150", "11:50 ( Monte Carlo )"),
        BusSchedule("v1225", "12:25 ( Monte Carlo )"), BusSchedule("v1300", "13:00 ( Monte Carlo )"),
        BusSchedule("v1330", "13:30 ( Monte Carlo )"), BusSchedule("v1410", "14:10 ( Monte Carlo )"),
        BusSchedule("v1430", "14:30 ( Monte Carlo )"), BusSchedule("v1435", "14:35 ( Monte Carlo )"),
        BusSchedule("v1520", "15:20 ( Monte Carlo )"), BusSchedule("v1545", "15:45 ( Monte Carlo )"),
        BusSchedule("v1630", "16:30 ( Monte Carlo )"), BusSchedule("v1655", "16:55 ( Monte Carlo )"),
        BusSchedule("v1740", "17:40 ( Monte Carlo )"), BusSchedule("v1805", "18:05 ( Monte Carlo )"),
        BusSchedule("v1850", "18:50 ( Monte Carlo )"), BusSchedule("v1910", "19:10 ( Monte Carlo )"),
        BusSchedule("v2010", "20:10 ( Monte Carlo )"), BusSchedule("v2110", "21:10 ( Monte Carlo )"),
        BusSchedule("v2210", "22:10 ( Monte Carlo )"), BusSchedule("v2305", "23:05 ( Monte Carlo )")
    )


    // =========================================================================
    // MÉTODOS PÚBLICOS
    // =========================================================================

    /**
     * Retorna a lista de BusStops para uma linha e sentido específicos,
     * considerando a variação "PE" (Parque Ecológico) se aplicável.
     */
    fun getStopsForLine(lineId: String, sentidoId: Int, horario: String? = null): List<BusStop> {
        return when (lineId) {
            "942455" -> { // Linha 01
                if (sentidoId == 0) stopsLinha01_Ida
                else if (horario != null && horario.contains("PE")) stopsLinha01_Volta_PE
                else stopsLinha01_Volta_Normal
            }
            "942430" -> if (sentidoId == 0) stopsLinha06_Ida else stopsLinha06_Volta
            "942419" -> if (sentidoId == 0) stopsLinha32_Ida else stopsLinha32_Volta
            else -> emptyList()
        }
    }

    /**
     * Retorna a lista de BusSchedules (horários) para uma linha e sentido específicos.
     */
    fun getSchedulesForLine(lineId: String, sentidoId: Int): List<BusSchedule> {
        return when (lineId) {
            "942455" -> if (sentidoId == 0) horariosLinha01_Ida else horariosLinha01_Volta
            "942430" -> if (sentidoId == 0) horariosLinha06_Ida else horariosLinha06_Volta
            "942419" -> if (sentidoId == 0) horariosLinha32_Ida else horariosLinha32_Volta
            else -> emptyList()
        }
    }
}