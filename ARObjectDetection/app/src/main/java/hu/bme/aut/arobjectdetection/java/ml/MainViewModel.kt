package hu.bme.aut.arobjectdetection.java.ml

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    var isScanning by mutableStateOf(false)
        private set

    var canReset by mutableStateOf(false)
        private set

    var snackbarMessage by mutableStateOf<String?>(null)
        private set

    private var onScanTriggered: (() -> Unit)? = null
    private var onResetTriggered: (() -> Unit)? = null

    fun setOnScanTriggered(action: () -> Unit) {
        onScanTriggered = action
    }

    fun setOnResetTriggered(action: () -> Unit) {
        onResetTriggered = action
    }

    fun triggerScan() {
        if (!isScanning) {
            onScanTriggered?.invoke()
        }
    }

    fun triggerReset() {
        onResetTriggered?.invoke()
    }

    fun setScanningActive(active: Boolean) {
        isScanning = active
    }

    fun setResetEnabled(enabled: Boolean) {
        canReset = enabled
    }

    fun showSnackbar(message: String) {
        snackbarMessage = message
    }

    fun clearSnackbar() {
        snackbarMessage = null
    }
}
