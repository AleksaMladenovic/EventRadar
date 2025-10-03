package com.eventradar.ui.map

enum class LocationPermissionState {
    LOADING,    // Stanje dok proveravamo dozvole
    GRANTED,    // Stanje kada je dozvola data
    DENIED      // Stanje kada je dozvola odbijena
}
