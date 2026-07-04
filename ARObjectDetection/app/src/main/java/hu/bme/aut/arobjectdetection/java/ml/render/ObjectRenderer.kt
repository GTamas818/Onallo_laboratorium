package hu.bme.aut.arobjectdetection.java.ml.render

import android.opengl.Matrix
import com.google.ar.core.Pose
import hu.bme.aut.arobjectdetection.java.common.samplerender.Mesh
import hu.bme.aut.arobjectdetection.java.common.samplerender.SampleRender
import hu.bme.aut.arobjectdetection.java.common.samplerender.Shader
import hu.bme.aut.arobjectdetection.java.common.samplerender.Texture
import java.io.IOException

/**
 * Renders a 3D object from an OBJ file.
 */
class ObjectRenderer {
  private lateinit var mesh: Mesh
  private lateinit var shader: Shader
  private lateinit var texture: Texture

  // Temporary matrices for calculations to avoid allocations during rendering.
  private val modelMatrix = FloatArray(16)
  private val modelViewMatrix = FloatArray(16)
  private val modelViewProjectionMatrix = FloatArray(16)

  /**
   * Initializes the renderer with the given model and texture assets.
   */
  fun onSurfaceCreated(render: SampleRender, modelPath: String, texturePath: String) {
    try {
      mesh = Mesh.createFromAsset(render, modelPath)
      texture = Texture.createFromAsset(
        render,
        texturePath,
        Texture.WrapMode.CLAMP_TO_EDGE,
        Texture.ColorFormat.SRGB
      )

      shader = Shader.createFromAssets(render, "shaders/object.vert", "shaders/object.frag", null)
        .setTexture("u_Texture", texture)
    } catch (e: IOException) {
      throw RuntimeException("Failed to read asset file", e)
    }
  }

  /**
   * Draws the object at the specified [pose] with the given [scaleFactor].
   * [angleX], [angleY], [angleZ] are rotation angles in degrees.
   */
  fun draw(
    render: SampleRender,
    viewMatrix: FloatArray,
    projectionMatrix: FloatArray,
    pose: Pose,
    scaleFactor: Float,
    angleX: Float = 0f,
    angleY: Float = 0f,
    angleZ: Float = 0f
  ) {
    pose.toMatrix(modelMatrix, 0)

    // Apply rotations (Pitch, Yaw, Roll)
    Matrix.rotateM(modelMatrix, 0, angleY, 0f, 1f, 0f) // Yaw
    Matrix.rotateM(modelMatrix, 0, angleX, 1f, 0f, 0f) // Pitch
    Matrix.rotateM(modelMatrix, 0, angleZ, 0f, 0f, 1f) // Roll

    Matrix.scaleM(modelMatrix, 0, scaleFactor, scaleFactor, scaleFactor)

    Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
    Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)

    shader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
    render.draw(mesh, shader)
  }
}
