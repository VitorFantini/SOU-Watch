# 🚌 SOU Watch - Monitoramento de Ônibus (Wear OS)

> **App nativo para Wear OS capaz de rastrear ônibus em tempo real e calcular rotas sem depender do celular.**

![Platform](https://img.shields.io/badge/Platform-Wear_OS-blue?logo=android)
![Language](https://img.shields.io/badge/Language-Kotlin-purple?logo=kotlin)
![Architecture](https://img.shields.io/badge/Architecture-MVVM-green)

## 🎯 O Diferencial Técnico
Este projeto não utiliza APIs públicas documentadas. Todo o acesso aos dados foi construído através de **Engenharia Reversa** do aplicativo oficial de transporte.

* **🕵️ Engenharia Reversa & Segurança:** Mapeamento de endpoints privados via *Packet Sniffing* e injeção de certificados em ambiente controlado para a **quebra de SSL Pinning**.
* **🔄 Resiliência de Rede:** Algoritmo de "Caça em Cascata" que alterna entre dados da API, estimativas de tráfego do Google Maps e cálculos matemáticos locais para garantir que o usuário nunca fique sem resposta.
* **🎨 UI Nativa:** Interface desenvolvida 100% em **Jetpack Compose**, otimizada para legibilidade em telas circulares OLED.

## 📱 Funcionalidades
* 📍 **Rastreamento GPS Real:** Localização exata do veículo na linha.
* 🗺️ **Navegação Nativa:** Deep Linking direto para o Google Maps do relógio.
* 📂 **Modo Offline:** Tabelas de horários completas armazenadas localmente.

## 🛠️ Tech Stack
* **Linguagem:** Kotlin (Coroutines & Flow)
* **Interface:** Jetpack Compose for Wear OS
* **Arquitetura:** MVVM + Clean Architecture
* **Networking:** Retrofit (com Headers customizados para simulação de cliente oficial)
* **Maps:** Google Directions API & Intents

## 🚀 Como Rodar
1.  Clone o repositório.
2.  Abra no **Android Studio**.
3.  Crie um arquivo `local.properties` na raiz com sua chave:
    ```properties
    MAPS_API_KEY=Chave_Aqui
    ```
4.  Execute em um emulador ou dispositivo físico via ADB.

---
Desenvolvido por **[Vitor Fantini](https://github.com/VitorFantini)** 🧑🏻‍💻
