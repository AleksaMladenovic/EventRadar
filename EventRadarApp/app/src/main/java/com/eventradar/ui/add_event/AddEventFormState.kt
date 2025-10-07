package com.eventradar.ui.add_event

import androidx.annotation.StringRes
import com.eventradar.data.model.EventCategory
import java.util.Date

data class AddEventFormState(
    // Vrednosti polja
    val name: String = "",
    val description: String = "",
    val category: EventCategory = EventCategory.OTHER,
    val eventDate: Date? = null,
    val eventTime: String = "", // npr. "19:30"
    val ageRestriction: Boolean = false, // Jednostavan switch
    val free: Boolean = true,
    val price: String = "",
    // URI slike koju je korisnik izabrao (za kasnije)
    // val eventImageUri: Uri? = null,

    // Statusi
    val isLoading: Boolean = false,

    // Polja za greške validacije (čuvamo ID-jeve string resursa)
    @StringRes val nameError: Int? = null,
    @StringRes val descriptionError: Int? = null,
    @StringRes val dateError: Int? = null,
    @StringRes val timeError: Int? = null,
    @StringRes val priceError: Int? = null
)