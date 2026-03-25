package hu.bme.aut.arobjectdetection.java.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Screen : NavKey {
    @Serializable
    data object AR : Screen
    
    @Serializable
    data object Info : Screen
}
