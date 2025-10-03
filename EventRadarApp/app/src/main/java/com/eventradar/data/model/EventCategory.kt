package com.eventradar.data.model

import androidx.annotation.StringRes
import com.eventradar.R
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.BitmapDescriptorFactory

// Pomoćna funkcija ostaje ovde, ali je sada 'private' jer je detalj implementacije
private fun hueFromColor(color: Color): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (color.red * 255).toInt(),
        (color.green * 255).toInt(),
        (color.blue * 255).toInt(),
        hsv
    )
    return hsv[0]
}

enum class EventCategory(
    @StringRes val displayNameResId: Int,
    val color: Color,
    val markerHue: Float
) {
    MUSIC(R.string.category_music, Color.Blue, hueFromColor(Color.Blue)),
    SPORT(R.string.category_sport, Color.Green, hueFromColor(Color.Green)),
    CULTURE(R.string.category_culture, Color.Magenta, hueFromColor(Color.Magenta)),
    FOOD(R.string.category_food, Color.Red, hueFromColor(Color.Red)),
    EDUCATION(R.string.category_education, Color.Cyan, hueFromColor(Color.Cyan)),

    // Specijalni slučaj za OTHER
    OTHER(R.string.category_other, Color.Gray, BitmapDescriptorFactory.HUE_AZURE);

    companion object {
        fun fromString(name: String?): EventCategory {
            return values().find { it.name == name } ?: OTHER
        }
    }
}

