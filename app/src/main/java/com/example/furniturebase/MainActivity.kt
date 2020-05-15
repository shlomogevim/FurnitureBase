package com.example.furniturebase

import android.graphics.Color
import android.media.CamcorderProfile
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.Toast
import com.google.ar.core.Anchor
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.collision.Box
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.CompletableFuture

class MainActivity : AppCompatActivity() {

    lateinit var arFragment: ArFragment
    val viewNodes = mutableListOf<Node>()
    private lateinit var photoSaver: PhotoSaver
    private lateinit var videoRecorder: VideoRecorder
    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        arFragment = fragment as ArFragment
        setupDubleTapLister()
        getCurrentScene().addOnUpdateListener {
            rotateViewNodsTotheUser()
        }
        setupFab()
    }

    private fun setupDubleTapLister() {
        arFragment.setOnTapArPlaneListener { hitResult, _, _ ->
            loadModel { modelRenderable, viewRenderable ->     // simple, just display the model in the hit(anchoreNode)
                addNodeToScence(hitResult.createAnchor(), modelRenderable, viewRenderable)
            }
        }
    }

    private fun createDeleteButton(): Button {  //  you can creat any view even entire layout
        return Button(this).apply {
            text = "  עולם קטן מיועד למחשבות קטנות  "
            setBackgroundColor(Color.RED)
            setTextColor(Color.WHITE)
        }
    }

    private fun getCurrentScene() = arFragment.arSceneView.scene

    private fun rotateViewNodsTotheUser() {
        for (node in viewNodes) {
            node.renderable?.let {
                val camPos = getCurrentScene().camera.worldPosition
                val viewNodePos = node.worldPosition
                val dir = Vector3.subtract(camPos, viewNodePos)
                node.worldRotation = Quaternion.lookRotation(dir, Vector3.up())
            }
        }
    }

    private fun addNodeToScence(
        anchor: Anchor,
        modelRenderable: ModelRenderable,
        viewRenderable: ViewRenderable
    ) {
        val anchorNode = AnchorNode(anchor)
        val modelNode = TransformableNode(arFragment.transformationSystem).apply {
            renderable = modelRenderable
            setParent(anchorNode)
            getCurrentScene().addChild(anchorNode)
            select()
        }
        val viewNode = Node().apply {
            renderable = null // not seen at first
            setParent(modelNode)
            val box = modelNode.renderable?.collisionShape as Box
            localPosition = Vector3(0f, box.size.y, 0f)  //x,y,z
            (viewRenderable.view as Button).setOnClickListener {   // fine definition to buttom id
                getCurrentScene().removeChild(anchorNode)           // remove the model amd the view
                viewNodes.remove(this)
            }
        }
        viewNodes.add(viewNode)
        modelNode.setOnTapListener { _, _ ->
            if (!modelNode.isTransforming) {  // if the model is not movving
                if (viewNode.renderable == null) {
                    viewNode.renderable = viewRenderable
                } else {
                    viewNode.renderable = null
                }

            }

        }

    }

    private fun loadModel(callback: (ModelRenderable, ViewRenderable) -> Unit) {

        val modelRenderable = ModelRenderable.builder()
            .setSource(this, R.raw.earth_ball)
            .build()
        val viewRenderable = ViewRenderable.builder()
            .setView(this, createDeleteButton())
            .build()
        CompletableFuture.allOf(modelRenderable, viewRenderable)
            .thenAccept {
                callback(modelRenderable.get(), viewRenderable.get())
            }
            .exceptionally {
                Toast.makeText(this, "Error", Toast.LENGTH_LONG).show()
                null
            }
    }
    private fun setupFab() {
        photoSaver = PhotoSaver(this)
        videoRecorder = VideoRecorder(this).apply {
            sceneView = arFragment.arSceneView
            setVideoQuality(CamcorderProfile.QUALITY_1080P, resources.configuration.orientation)
        }
        fab.setOnClickListener {
            if (!isRecording) {
                eliminateDot()
                photoSaver.takePhoto(arFragment.arSceneView)
            }
        }

        fab.setOnLongClickListener {
            eliminateDot()
            isRecording = videoRecorder.toggleRecordingState()
            true
        }

        fab.setOnTouchListener { view, motionEvent ->


            if (motionEvent.action == MotionEvent.ACTION_UP && isRecording) {

                isRecording = videoRecorder.toggleRecordingState()

                Toast.makeText(this, "Saved video to gallery!", Toast.LENGTH_LONG).show()

                true

            } else false

        }

    }

    private fun eliminateDot() {
        arFragment.arSceneView.planeRenderer.isVisible = false
        arFragment.planeDiscoveryController.hide()
        arFragment.planeDiscoveryController.setInstructionView(null)
    }
}
