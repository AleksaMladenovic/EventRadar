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
    const val ADD_EVENT_SCREEN = "add_event/{lat}/{lng}"
    const val EVENT_DETAILS_SCREEN = "event_details/{eventId}"
    const val PUBLIC_PROFILE_SCREEN = "public_profile/{userId}"
    const val USER_EVENTS_SCREEN = "user_events/{userId}"
}
