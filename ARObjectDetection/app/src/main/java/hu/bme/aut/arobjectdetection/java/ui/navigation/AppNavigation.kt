package hu.bme.aut.arobjectdetection.java.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import hu.bme.aut.arobjectdetection.java.ml.AppRenderer
import hu.bme.aut.arobjectdetection.java.ml.ARCoreSessionLifecycleHelper
import hu.bme.aut.arobjectdetection.java.ml.MainViewModel
import hu.bme.aut.arobjectdetection.java.ui.screen.ARScreen
import hu.bme.aut.arobjectdetection.java.ui.screen.InfoScreen

@Composable
fun AppNavigation(
    renderer: AppRenderer,
    viewModel: MainViewModel,
    arCoreSessionHelper: ARCoreSessionLifecycleHelper,
    modifier: Modifier = Modifier
) {
    val backStack = remember { mutableStateListOf<Screen>(Screen.AR) }
    
    // Satisfy Navigation 3 / NavigationEvent requirements
    val dispatcherOwner = remember { 
        object : NavigationEventDispatcherOwner {
            override val navigationEventDispatcher = NavigationEventDispatcher()
        }
    }
    
    androidx.compose.runtime.CompositionLocalProvider(
        LocalNavigationEventDispatcherOwner provides dispatcherOwner
    ) {
        NavDisplay(
            modifier = modifier,
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                entry<Screen.AR> {
                    ARScreen(
                        renderer = renderer,
                        viewModel = viewModel,
                        arCoreSessionHelper = arCoreSessionHelper,
                        onInfoClick = { backStack.add(Screen.Info) }
                    )
                }

                entry<Screen.Info> {
                    InfoScreen(
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
            }
        )
    }
}
