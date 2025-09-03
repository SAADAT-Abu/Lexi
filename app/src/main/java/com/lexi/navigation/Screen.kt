package com.lexi.navigation

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object UserSetup : Screen("user_setup")
    object ApiKey : Screen("api_key")
    object ModelSelection : Screen("model_selection")
    object Chat : Screen("chat")
    object Settings : Screen("settings")
}