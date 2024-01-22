package io.github.sceneview.sample.armodelviewer.compose

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.Matrix.multiplyMV
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.filament.Engine
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.TrackingFailureReason
import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.abs
import io.github.sceneview.animation.Transition.animateRotation
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.isValid
import io.github.sceneview.ar.getDescription
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.collision.Matrix
import io.github.sceneview.collision.Matrix.multiply
import io.github.sceneview.collision.Quaternion
import io.github.sceneview.collision.Vector3
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.toNewQuaternion
import io.github.sceneview.math.toOldQuaternion
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNode
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberView
import io.github.sceneview.sample.SceneviewTheme
import java.util.Collections.rotate
import kotlin.math.abs
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

private const val kModelFile = "models/lake_and_fish.glb"
private const val kModelFile_Rod = "models/bamboo_fishing_rod.glb"
private const val kMaxModelInstances = 1
private var lakeNode: ModelNode? = null
private var currentState = -1
private val fishNames = listOf(
    "Arowana", "Betta", "Catfish", "Dolphin Fish", "Eel",
    "Flounder", "Guppy", "Haddock", "Ichthyosaur", "Jellyfish",
    "Koi", "Lionfish", "Mackerel", "Nemo", "Octopus", "Piranha",
    "Quillfish", "Rainbow Trout", "Salmon", "Tetra", "Uaru",
    "Viperfish", "Wrasse", "X-ray Tetra", "Yellowtail", "Zebra Danio", "Capybara"
)

class MainActivity : ComponentActivity() {

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SceneviewTheme {
                // A surface container using the 'background' color from the theme
                Box(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // The destroy calls are automatically made when their disposable effect leaves
                    // the composition or its key changes.
                    val engine = rememberEngine()
                    val modelLoader = rememberModelLoader(engine)
                    val materialLoader = rememberMaterialLoader(engine)
                    val cameraNode = rememberARCameraNode(engine)
                    val childNodes = rememberNodes()
                    val view = rememberView(engine)
                    val collisionSystem = rememberCollisionSystem(view)
                    var planeRenderer by remember { mutableStateOf(true) }

                    val modelInstances = remember { mutableListOf<ModelInstance>() }
                    var modelInstancesRod = remember { mutableListOf<ModelInstance>() }
                    var trackingFailureReason by remember {
                        mutableStateOf<TrackingFailureReason?>(null)
                    }
                    val cameraNode2 = rememberARCameraNode(engine)
                    val centerNode = rememberNode(engine)
                        .addChildNode(cameraNode2)
                    val cameraTransition = rememberInfiniteTransition(label = "CameraTransition")
                    val cameraRotation by cameraTransition.animateRotation(
                        initialValue = Rotation(y = 0.0f),
                        targetValue = Rotation(y = 360.0f),
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 7.seconds.toInt(DurationUnit.MILLISECONDS))
                        )
                    )
                    var frame by remember { mutableStateOf<Frame?>(null) }
                    ARScene(
                        modifier = Modifier.fillMaxSize(),
                        childNodes = childNodes,
                        engine = engine,
                        view = view,
                        modelLoader = modelLoader,
                        collisionSystem = collisionSystem,
                        sessionConfiguration = { session, config ->
                            config.depthMode =
                                when (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                                    true -> Config.DepthMode.AUTOMATIC
                                    else -> Config.DepthMode.DISABLED
                                }
                            config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                            config.lightEstimationMode =
                                Config.LightEstimationMode.ENVIRONMENTAL_HDR
                        },
                        cameraNode = cameraNode,
                        planeRenderer = planeRenderer,
                        onTrackingFailureChanged = {
                            trackingFailureReason = it
                        },
                        onSessionCreated = {session ->/*
                            /*val anchorPose = Pose.makeTranslation(0.07f,0.0f,-0.1f) // Adjust the translation as needed
                            val alt_session = session
                            try {
                                val anchor = alt_session.createAnchor(anchorPose)
                                // ... rest of your code
                            } catch (e: Exception) {
                                Log.e("MyApp", "Error creating anchor: ${e.message}")
                            }
                            val anchor = session.createAnchor(cameraNode.pose)*/
                            rodNode = ModelNode(
                                modelInstance = modelInstancesRod.apply {
                                    if (isEmpty()) {
                                        this += modelLoader.createInstancedModel(kModelFile_Rod, kMaxModelInstances)
                                    }
                                }.removeLast(),
                                // Scale to fit in a 0.5 meters cube
                                scaleToUnits = 1.0f

                            )
                            //val anchornode = AnchorNode(engine = engine, anchor = anchor)
                            //anchornode.addChildNode(rodNode)
                            childNodes += rodNode*/

                        },

                        onSessionUpdated = { session, updatedFrame ->
                            /*val offset: Position = Position(0.0f, 0.0f, -1.0f)

                            // Apply the offset to the camera's forward direction
                            val offsetMatrix = Matrix().apply {
                                setTranslation(Vector3(offset.x, offset.y, offset.z))
                            }
                            val cameraPosition = cameraNode.position
                            val cameraRotation = cameraNode.rotation
                            val cameraQuaternion = cameraNode.quaternion


                            val cameraMatrix = Matrix().apply {
                                makeTrs(Vector3(cameraPosition.x, cameraPosition.y, cameraPosition.z), cameraQuaternion.toOldQuaternion(), Vector3(1.0f, 1.0f, 1.0f))
                            }

                            // Multiply the cameraMatrix by the offsetMatrix
                            val combinedMatrix = Matrix().apply {
                                multiply(cameraMatrix, offsetMatrix, this)
                            }

                            // Extract the final position, quaternion, and scale from the combined matrix
                            val finalPosition = Vector3().apply {
                                combinedMatrix.decomposeTranslation(this)
                            }

                            val finalQuaternion = Quaternion().apply {
                                combinedMatrix.extractQuaternion(this)
                            }

                            val finalScale = Vector3().apply {
                                combinedMatrix.decomposeScale(this)
                            }

                            // Set the position, quaternion, and scale to the rodNode
                            rodNode.transform(
                                position = Position(finalPosition.x, finalPosition.y, finalPosition.z),
                                quaternion = finalQuaternion.toNewQuaternion(),
                                scale = Position(finalScale.x, finalScale.y, finalScale.z)
                            )

                            Log.println(Log.INFO, "MyApp", rodNode.position.toString())*/
                            frame = updatedFrame
                        },
                        onGestureListener = rememberOnGestureListener(
                            onSingleTapConfirmed = { motionEvent, node ->
                                if(currentState == 2)
                                    currentState = 0
                                if (node == null && childNodes.isEmpty()) {
                                    val hitResults =
                                        frame?.hitTest(motionEvent.x, motionEvent.y)
                                    hitResults?.firstOrNull {
                                        it.isValid(
                                            depthPoint = false,
                                            point = false
                                        )
                                    }?.createAnchorOrNull()
                                        ?.let { anchor ->
                                            planeRenderer = false
                                            childNodes += createAnchorNode(
                                                engine = engine,
                                                modelLoader = modelLoader,
                                                materialLoader = materialLoader,
                                                modelInstances = modelInstances,
                                                anchor = anchor
                                            )

                                        }
                                }

                            })


                    )
                    Image(

                        painter = painterResource(id = R.drawable.rod),
                        contentDescription = "Fishing Rod",
                        modifier = Modifier
                            .fillMaxHeight()
                            .graphicsLayer {
                                rotationZ = -75f
                                scaleX = 3f
                                scaleY = 3f
                            }
                            .offset(x = 40.dp, y = 15.dp)
                    )
                    Text(
                        modifier = Modifier
                            .systemBarsPadding()
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp, start = 32.dp, end = 32.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 28.sp,
                        color = Color.White,
                        text = when (currentState) {
                            -1 -> {
                                stringResource(R.string.point_your_phone_down)
                            }
                            0 -> {
                                "Swing your phone forward to cast your fishing rod!"
                            }
                            1 -> {
                                "Swing your phone backwards to catch the fish!"
                            }
                            else -> {
                                var randomFish = fishNames[Random.nextInt(fishNames.size)]
                                "Congratulations! You have caught a $randomFish"
                            }
                        }
                    )

                }

                sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
                accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

                // Register a lifecycle observer to start and stop sensor updates
                registerSensorListener()
            }
        }
    }

    private fun registerSensorListener() {
        sensorManager?.registerListener(sensorEventListener, gyroscope, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private var gyroscope: Sensor? = null
    private val gyroscopeValues = FloatArray(3)
    private var lastTimestamp: Long = 0
    private var tiltDetected = false

    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
                val currentTime = System.currentTimeMillis()
                val timeDifference = currentTime - lastTimestamp
                lastTimestamp = currentTime

                if (timeDifference > 0) {
                    val rotationSpeed = event.values[0] // Adjust index based on the gyroscope data
                    val thresholdSpeed = 1.5f // Adjust as needed
                    Log.println(Log.INFO, "MyApp", rotationSpeed.toString())
                    // Check for negative rotation speed to detect downward tilt
                    if (rotationSpeed < -thresholdSpeed) {
                        Log.println(Log.INFO, "MyApp", "ENTRATO")
                        // Rapid downward motion detected, trigger animation
                        tiltDetected = true
                        if (currentState == 0 && lakeNode != null && tiltDetected) {
                            lakeNode!!.stopAnimation(animationName = "NoHook")
                            lakeNode!!.playAnimation(animationName = "HookIdle", loop = true)

                            var waitTheCatch = Random.nextInt(500, 3501)
                            Handler().postDelayed({
                                lakeNode!!.stopAnimation(animationName = "HookIdle")
                                lakeNode!!.playAnimation(animationName = "FishHooking", loop = true)
                                lakeNode!!.playAnimation(animationName = "Catch", loop = true)

                                currentState = 1
                            }, waitTheCatch.toLong())

                            tiltDetected = false
                        }
                    }
                    if (rotationSpeed > thresholdSpeed) {
                        Log.println(Log.INFO, "MyApp", "ENTRATO")
                        // Rapid downward motion detected, trigger animation
                        tiltDetected = true
                        if (currentState == 1 && tiltDetected) {
                            lakeNode!!.stopAnimation(animationName = "Catch")
                            lakeNode!!.stopAnimation(animationName = "FishHooking")
                            lakeNode!!.playAnimation(animationName = "NoHook", loop = true)
                            lakeNode!!.playAnimation(animationName = "Idle", loop = true)
                            currentState = 2
                            tiltDetected = false
                        }
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Do nothing for now
        }
    }
    fun createAnchorNode(
        engine: Engine,
        modelLoader: ModelLoader,
        materialLoader: MaterialLoader,
        modelInstances: MutableList<ModelInstance>,
        anchor: Anchor,
    ): AnchorNode {

        val anchorNode = AnchorNode(engine = engine, anchor = anchor)
        val modelNode = ModelNode(
            modelInstance = modelInstances.apply {
                if (isEmpty()) {
                    this += modelLoader.createInstancedModel(kModelFile, kMaxModelInstances)
                }
            }.removeLast(),
            // Scale to fit in a 0.5 meters cube
            scaleToUnits = 1.0f
        ).apply {
            // Model Node needs to be editable for independent rotation from the anchor rotation
            isEditable = false
            isPositionEditable = false
            isRotationEditable = false
            onFling = {e1: MotionEvent?, e2: MotionEvent, v: Float2 ->

                if(e1 != null && e2.y < e1.y) {
                    if(currentState == 0) {

                        this.stopAnimation(animationName = "NoHook")
                        this.playAnimation(animationName = "HookIdle", loop = true)

                        //this.playAnimation(animationName = "HookIdle", loop = true)

                        var waitTheCatch = Random.nextInt(500, 3501)
                        Handler().postDelayed({
                            this.stopAnimation(animationName = "HookIdle")
                            this.playAnimation(animationName = "FishHooking", loop = true)
                            this.playAnimation(animationName = "Catch", loop = true)

                            currentState = 1
                        }, waitTheCatch.toLong())

                    }
                }
                true
            }
            onLongPress = {e: MotionEvent ->
                if (currentState == 1){
                    this.stopAnimation(animationName = "Catch")
                    this.stopAnimation(animationName = "FishHooking")
                    this.playAnimation(animationName = "NoHook", loop = true)
                    this.playAnimation(animationName = "Idle", loop = true)
                    currentState = 2
                }
                true
            }
        }
        val boundingBoxNode = CubeNode(
            engine,
            size = modelNode.extents,
            center = modelNode.center,
            materialInstance = materialLoader.createColorInstance(Color.White.copy(alpha = 0.5f))
        ).apply {
            isVisible = false
        }
        modelNode.stopAnimation(animationName = "Catch")
        modelNode.stopAnimation(animationName = "HookIdle")
        modelNode.playAnimation(animationName = "Idle", loop = true)
        modelNode.playAnimation(animationName = "NoHook", loop = true)

        modelNode.addChildNode(boundingBoxNode)
        anchorNode.addChildNode(modelNode)

        listOf(modelNode, anchorNode).forEach {
            it.onEditingChanged = { editingTransforms ->
                boundingBoxNode.isVisible = editingTransforms.isNotEmpty()
            }
        }
        anchorNode.apply {
            isEditable = false
            isPositionEditable = false
            isRotationEditable = false
            isScaleEditable = false
        }

        lakeNode = modelNode
        currentState = 0
        return anchorNode
    }


}
