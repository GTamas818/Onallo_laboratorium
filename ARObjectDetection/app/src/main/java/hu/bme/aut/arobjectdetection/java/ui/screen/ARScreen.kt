package hu.bme.aut.arobjectdetection.java.ui.screen

import android.opengl.GLSurfaceView
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import hu.bme.aut.arobjectdetection.java.common.samplerender.SampleRender
import hu.bme.aut.arobjectdetection.java.ml.AppRenderer
import hu.bme.aut.arobjectdetection.java.ml.ARCoreSessionLifecycleHelper
import hu.bme.aut.arobjectdetection.java.ml.MainViewModel
import hu.bme.aut.arobjectdetection.ml.R

@Composable
fun ARScreen(
    renderer: AppRenderer,
    viewModel: MainViewModel,
    arCoreSessionHelper: ARCoreSessionLifecycleHelper,
    onInfoClick: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var surfaceView by remember { mutableStateOf<GLSurfaceView?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        ARView(
            renderer = renderer,
            onSurfaceViewCreated = { surfaceView = it }
        )

        LifecycleManager(
            lifecycleOwner = lifecycleOwner,
            arCoreSessionHelper = arCoreSessionHelper,
            renderer = renderer,
            surfaceView = surfaceView
        )

        InfoButton(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp),
            onClick = onInfoClick
        )

        BottomControls(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            isScanning = viewModel.isScanning,
            canReset = viewModel.canReset,
            onScanClick = { viewModel.triggerScan() },
            onResetClick = { viewModel.triggerReset() }
        )

        MessageSnackbar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp),
            message = viewModel.snackbarMessage,
            onDismiss = { viewModel.clearSnackbar() }
        )
    }
}

@Composable
fun ARView(
    renderer: AppRenderer,
    onSurfaceViewCreated: (GLSurfaceView) -> Unit
) {
    AndroidView(
        factory = { ctx ->
            GLSurfaceView(ctx).apply {
                SampleRender(this, renderer, ctx.assets)
                onSurfaceViewCreated(this)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun LifecycleManager(
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    arCoreSessionHelper: ARCoreSessionLifecycleHelper,
    renderer: AppRenderer,
    surfaceView: GLSurfaceView?
) {
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { owner, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    arCoreSessionHelper.onResume(owner)
                    renderer.onResume(owner)
                    surfaceView?.onResume()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    surfaceView?.onPause()
                    renderer.onPause(owner)
                    arCoreSessionHelper.onPause(owner)
                }
                Lifecycle.Event.ON_DESTROY -> {
                    renderer.onDestroy(owner)
                    arCoreSessionHelper.onDestroy(owner)
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@Composable
fun InfoButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        color = Color.Black.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Default.Info,
                contentDescription = "Information",
                tint = Color.White
            )
        }
    }
}

@Composable
fun BottomControls(
    modifier: Modifier = Modifier,
    isScanning: Boolean,
    canReset: Boolean,
    onScanClick: () -> Unit,
    onResetClick: () -> Unit
) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = canReset,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            ActionButton(
                onClick = onResetClick,
                icon = Icons.Default.Clear,
                text = stringResource(R.string.clear),
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        }

        ScanButton(
            isScanning = isScanning,
            onClick = onScanClick
        )
    }
}

@Composable
fun ActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.height(56.dp),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 24.dp),
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = containerColor)
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(text = text)
    }
}

@Composable
fun ScanButton(
    isScanning: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isScanning,
        modifier = Modifier
            .height(56.dp)
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        AnimatedContent(
            targetState = isScanning,
            transitionSpec = {
                (fadeIn(animationSpec = tween(150, delayMillis = 75)) + scaleIn(initialScale = 0.8f)) togetherWith
                (fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.8f))
            },
            label = "ScanButtonAnimation"
        ) { scanning ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (scanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = LocalContentColor.current,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(text = stringResource(R.string.scan_busy))
                } else {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(text = stringResource(R.string.scan_available))
                }
            }
        }
    }
}

@Composable
fun MessageSnackbar(
    modifier: Modifier = Modifier,
    message: String?,
    onDismiss: () -> Unit
) {
    message?.let {
        Snackbar(
            modifier = modifier.padding(horizontal = 16.dp),
            action = {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
            }
        ) {
            Text(text = it)
        }
    }
}
