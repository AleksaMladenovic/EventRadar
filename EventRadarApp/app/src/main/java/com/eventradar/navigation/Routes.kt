package com.eventradar.navigation

object Routes {
    const val ROOT_GRAPH = "root_graph"
    const val AUTH_GRAPH = "auth_graph"
    const val MAIN_GRAPH = "main_graph"

    const val WELCOME_SCREEN = "welcome"
    const val LOGIN_SCREEN = "login"
    const val REGISTER_SCREEN = "register"
    const val MAP_SCREEN = "map?lat={lat}&lng={lng}"
    const val PROFILE_SCREEN = "profile"
    const val EVENTS_LIST_SCREEN = "events_list"
    const val RANKING_SCREEN = "ranking"
    const val ADD_EVENT_SCREEN = "add_event?eventId={eventId}&lat={lat}&lng={lng}"
    const val EVENT_DETAILS_SCREEN = "event_details/{eventId}"
    const val PUBLIC_PROFILE_SCREEN = "public_profile/{userId}"
    const val USER_EVENTS_SCREEN = "user_events/{createdByUserId}"
    const val LOCATION_PICKER_SCREEN = "location_picker/{lat}/{lng}"
    const val ATTENDING_EVENTS_SCREEN = "attending_events/{attendingUserId}"
    const val EDIT_PROFILE_SCREEN = "edit_profile"
    const val CHANGE_PASSWORD_SCREEN = "change_password"
}
