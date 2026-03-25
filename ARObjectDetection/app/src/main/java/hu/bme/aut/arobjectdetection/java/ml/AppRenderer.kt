/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package hu.bme.aut.arobjectdetection.java.ml

import android.opengl.Matrix
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.Anchor
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import com.google.ar.core.TrackingState
import hu.bme.aut.arobjectdetection.java.common.helpers.DisplayRotationHelper
import hu.bme.aut.arobjectdetection.java.common.samplerender.SampleRender
import hu.bme.aut.arobjectdetection.java.common.samplerender.arcore.BackgroundRenderer
import hu.bme.aut.arobjectdetection.java.ml.classification.DetectedObjectResult
import hu.bme.aut.arobjectdetection.java.ml.classification.MLKitObjectDetector
import hu.bme.aut.arobjectdetection.java.ml.classification.ObjectDetector
import hu.bme.aut.arobjectdetection.java.ml.render.LabelRender
import hu.bme.aut.arobjectdetection.java.ml.render.PointCloudRender
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.core.exceptions.SessionPausedException
import java.util.Collections
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * Renders the HelloAR application into using our example Renderer.
 */
class AppRenderer(val activity: MainActivity) : DefaultLifecycleObserver, SampleRender.Renderer, CoroutineScope by MainScope() {
  companion object {
    val TAG = "HelloArRenderer"
  }

  val displayRotationHelper = DisplayRotationHelper(activity)
  lateinit var backgroundRenderer: BackgroundRenderer
  val pointCloudRender = PointCloudRender()
  val labelRenderer = LabelRender()

  val viewMatrix = FloatArray(16)
  val projectionMatrix = FloatArray(16)
  val viewProjectionMatrix = FloatArray(16)

  val arLabeledAnchors = Collections.synchronizedList(mutableListOf<ARLabeledAnchor>())
  var scanButtonWasPressed = false

  val mlKitAnalyzer = MLKitObjectDetector(activity)
  var currentAnalyzer: ObjectDetector = mlKitAnalyzer

  private var onScanningStateChanged: ((Boolean) -> Unit)? = null
  private var onResetEnabledChanged: ((Boolean) -> Unit)? = null
  private var onSnackbarRequested: ((String) -> Unit)? = null

  fun setCallbacks(
    onScanningStateChanged: (Boolean) -> Unit,
    onResetEnabledChanged: (Boolean) -> Unit,
    onSnackbarRequested: (String) -> Unit
  ) {
    this.onScanningStateChanged = onScanningStateChanged
    this.onResetEnabledChanged = onResetEnabledChanged
    this.onSnackbarRequested = onSnackbarRequested
  }

  fun onScanTriggered() {
    scanButtonWasPressed = true
    onScanningStateChanged?.invoke(true)
  }

  fun onResetTriggered() {
    arLabeledAnchors.clear()
    onResetEnabledChanged?.invoke(false)
  }

  override fun onResume(owner: LifecycleOwner) {
    displayRotationHelper.onResume()
  }

  override fun onPause(owner: LifecycleOwner) {
    displayRotationHelper.onPause()
  }

  override fun onSurfaceCreated(render: SampleRender) {
    backgroundRenderer = BackgroundRenderer(render).apply {
      setUseDepthVisualization(render, false)
    }
    pointCloudRender.onSurfaceCreated(render)
    labelRenderer.onSurfaceCreated(render)
  }

  override fun onSurfaceChanged(render: SampleRender?, width: Int, height: Int) {
    displayRotationHelper.onSurfaceChanged(width, height)
  }

  var objectResults: List<DetectedObjectResult>? = null

  override fun onDrawFrame(render: SampleRender) {
    val session = activity.arCoreSessionHelper.sessionCache ?: return
    session.setCameraTextureNames(intArrayOf(backgroundRenderer.cameraColorTexture.textureId))

    displayRotationHelper.updateSessionIfNeeded(session)

    val frame = try {
      session.update()
    } catch (e: SessionPausedException) {
      // The session is pausing, ignore this frame.
      return
    } catch (e: CameraNotAvailableException) {
      Log.e(TAG, "Camera not available during onDrawFrame", e)
      onSnackbarRequested?.invoke("Camera not available. Try restarting the app.")
      return
    }

    backgroundRenderer.updateDisplayGeometry(frame)
    backgroundRenderer.drawBackground(render)

    val camera = frame.camera
    camera.getViewMatrix(viewMatrix, 0)
    camera.getProjectionMatrix(projectionMatrix, 0, 0.01f, 100.0f)
    Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

    if (camera.trackingState != TrackingState.TRACKING) {
      return
    }

    frame.acquirePointCloud().use { pointCloud ->
      pointCloudRender.drawPointCloud(render, pointCloud, viewProjectionMatrix)
    }

    if (scanButtonWasPressed) {
      scanButtonWasPressed = false
      val cameraImage = frame.tryAcquireCameraImage()
      if (cameraImage != null) {
        launch(Dispatchers.IO) {
          val cameraId = session.cameraConfig.cameraId
          val imageRotation = displayRotationHelper.getCameraSensorToDisplayRotation(cameraId)
          objectResults = currentAnalyzer.analyze(cameraImage, imageRotation)
          cameraImage.close()
        }
      }
    }

    val objects = objectResults
    if (objects != null) {
      objectResults = null
      Log.i(TAG, "$currentAnalyzer got objects: $objects")
      val anchors = objects.mapNotNull { obj ->
        val (atX, atY) = obj.centerCoordinate
        val anchor = createAnchor(atX.toFloat(), atY.toFloat(), frame) ?: return@mapNotNull null
        ARLabeledAnchor(anchor, obj.label)
      }
      arLabeledAnchors.addAll(anchors)
      
      launch(Dispatchers.Main) {
        onResetEnabledChanged?.invoke(arLabeledAnchors.isNotEmpty())
        onScanningStateChanged?.invoke(false)
        when {
          objects.isEmpty() && currentAnalyzer == mlKitAnalyzer && !mlKitAnalyzer.hasCustomModel() ->
            onSnackbarRequested?.invoke("Default ML Kit model returned no results.")
          objects.isEmpty() ->
            onSnackbarRequested?.invoke("Classification model returned no results.")
          anchors.size != objects.size ->
            onSnackbarRequested?.invoke("Objects classified, but could not be attached to an anchor.")
        }
      }
    }

    for (arDetectedObject in arLabeledAnchors) {
      val anchor = arDetectedObject.anchor
      if (anchor.trackingState != TrackingState.TRACKING) continue
      labelRenderer.draw(
        render,
        viewProjectionMatrix,
        anchor.pose,
        camera.pose,
        arDetectedObject.label
      )
    }
  }

  fun Frame.tryAcquireCameraImage() = try {
    acquireCameraImage()
  } catch (e: NotYetAvailableException) {
    null
  }

  private val convertFloats = FloatArray(4)
  private val convertFloatsOut = FloatArray(4)

  fun createAnchor(xImage: Float, yImage: Float, frame: Frame): Anchor? {
    convertFloats[0] = xImage
    convertFloats[1] = yImage
    frame.transformCoordinates2d(
      Coordinates2d.IMAGE_PIXELS,
      convertFloats,
      Coordinates2d.VIEW,
      convertFloatsOut
    )

    val hits = frame.hitTest(convertFloatsOut[0], convertFloatsOut[1])
    val result = hits.getOrNull(0) ?: return null
    return result.trackable.createAnchor(result.hitPose)
  }
}

data class ARLabeledAnchor(val anchor: Anchor, val label: String)