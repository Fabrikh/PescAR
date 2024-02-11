package com.example.pescar

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.util.Base64
import android.util.Log
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseOutBounce
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pescar.ui.theme.RetroTestTheme
import com.google.android.filament.Engine
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.TrackingFailureReason
import dev.romainguy.kotlin.math.Float2
import io.github.sceneview.animation.Transition.animateRotation
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.isValid
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.math.Rotation
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
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.gson.JsonObject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlin.math.pow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.graphics.vector.ImageVector


private const val kModelFile = "models/lake_and_fish.glb"
private const val kModelFile_Rod = "models/bamboo_fishing_rod.glb"
private const val kMaxModelInstances = 1
private var lakeNode: ModelNode? = null
private var currentState = -1
private var onFocusShowcase = false
private var entered = false

/*
private val fishNames = listOf(
    "Arowana", "Betta", "Catfish", "Dolphin Fish", "Eel",
    "Flounder", "Guppy", "Haddock", "Ichthyosaur", "Jellyfish",
    "Koi", "Lionfish", "Mackerel", "Nemo", "Octopus", "Piranha",
    "Quillfish", "Rainbow Trout", "Salmon", "Tetra", "Uaru",
    "Viperfish", "Wrasse", "X-ray Tetra", "Yellowtail", "Zebra Danio", "Capybara"
)*/

class MainActivity : ComponentActivity() {

    lateinit var retroViewModel: RetroViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            retroViewModel = viewModel()

            val navController = rememberNavController()

            NavHost(navController, startDestination = "arbox") {

                composable("arbox") {ARBox(retroViewModel, navController)}
                composable("showcase") {ShowcaseBox(retroViewModel, navController)}
                composable("fishDetail/{fishId}", arguments = listOf(
                    navArgument("fishId") { type = NavType.IntType }
                )) { backStackEntry ->
                    FishDetailScreen(
                        fishId = backStackEntry.arguments?.getInt("fishId") ?: 0,
                        navController = navController,
                        retroViewModel = retroViewModel
                    )
                }
                composable("test"){TestBox(retroViewModel)}
            }


                //ARBox(retroViewModel)

                // A surface container using the 'background' color from the theme
                // Add a button to open the box when clicked

                //ShowcaseBox(retroViewModel)

        }
    }
}


@Composable
fun ARBox(retroViewModel: RetroViewModel, navController: NavController) {
    currentState = -1
    RetroTestTheme {
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

            var internalState by remember { mutableStateOf(-1) }

            LaunchedEffect(internalState) {

            }

            var sensorManager: SensorManager? = null
            var accelerometer: Sensor? = null
            var gyroscope: Sensor? = null
            val gyroscopeValues = FloatArray(3)
            var lastTimestamp: Long = 0
            var tiltDetected = false
            val context = LocalContext.current
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)



            val sensorEventListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
                        val currentTime = System.currentTimeMillis()
                        val timeDifference = currentTime - lastTimestamp
                        lastTimestamp = currentTime

                        if (timeDifference > 0) {
                            val rotationSpeed = event.values[0] // Adjust index based on the gyroscope data
                            val thresholdSpeed = 1.5f
                            if (rotationSpeed < -thresholdSpeed) {
                                Log.println(Log.INFO, "MyApp", "ENTRATO")
                                if (currentState == 0 && lakeNode != null && !entered) {
                                    entered = true
                                    lakeNode!!.stopAnimation(animationName = "NoHook")
                                    lakeNode!!.playAnimation(animationName = "HookIdle", loop = true)
                                    retroViewModel.getFishInfo(0)
                                    Log.println(Log.INFO,"Fish","done")

                                    var waitTheCatch = Random.nextInt(500, 3501)

                                    Handler().postDelayed({
                                        if(!onFocusShowcase)
                                            currentState = 1
                                        entered = false
                                    }, waitTheCatch.toLong())

                                }
                            }
                            if (rotationSpeed > thresholdSpeed) {
                                Log.println(Log.INFO, "MyApp", "ENTRATO")
                                // Rapid downward motion detected, trigger animation
                                if (currentState == 2 && !entered) {
                                    entered = true
                                    lakeNode!!.stopAnimation(animationName = "Catch")
                                    lakeNode!!.stopAnimation(animationName = "FishHooking")
                                    lakeNode!!.playAnimation(animationName = "NoHook", loop = true)
                                    lakeNode!!.playAnimation(animationName = "Idle", loop = true)
                                    currentState = 3
                                    entered = false
                                }
                            }
                        }
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    // Do nothing for now
                }
            }
            fun registerSensorListener() {
                sensorManager?.registerListener(sensorEventListener, gyroscope, SensorManager.SENSOR_DELAY_NORMAL)
            }

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
                onSessionCreated = { session ->/*
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

                    if (currentState == 1)
                        if (retroViewModel.retroUiState == RetroUiState.Error) {
                            lakeNode!!.stopAnimation(animationName = "HookIdle")
                            lakeNode!!.playAnimation(animationName = "NoHook", loop = true)
                            lakeNode!!.playAnimation(animationName = "Idle", loop = true)
                            Log.println(Log.ERROR, "MyApp", "RetroViewModel Error")
                            currentState = 0
                        } else if (retroViewModel.retroUiState != RetroUiState.Loading) {
                            lakeNode!!.stopAnimation(animationName = "HookIdle")
                            lakeNode!!.playAnimation(animationName = "FishHooking", loop = true)
                            lakeNode!!.playAnimation(animationName = "Catch", loop = true)
                            Log.println(Log.INFO, "MyApp", "RetroViewModel Success")
                            currentState = 2
                        }

                    if (internalState != currentState)
                        internalState = currentState


                    frame = updatedFrame
                },
                onGestureListener = rememberOnGestureListener(
                    onSingleTapConfirmed = { motionEvent, node ->

                        if (currentState == 3 || currentState == -1) {
                            currentState = 0
                            retroViewModel.retroUiState = RetroUiState.Loading
                        }


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
                                        anchor = anchor,
                                        retroViewModel = retroViewModel
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
                        //"Swing your phone forward to cast your fishing rod!"
                        stringResource(R.string.cast_fishing_rod)
                    }

                    1 -> {
                        //"Swing your phone forward to cast your fishing rod!"
                        stringResource(R.string.cast_fishing_rod)
                    }

                    2 -> {
                        //"Swing your phone backwards to catch the fish!"
                        stringResource(R.string.pull_fishing_rod)
                    }

                    else -> {
                        //var randomFish = fishNames[Random.nextInt(fishNames.size)]
                        //"Congratulations! You have caught a $randomFish"

                        when (retroViewModel.retroUiState) {
                            is RetroUiState.Success -> {

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable {
                                            if (currentState == 3 || currentState == -1) {
                                                currentState = 0
                                                retroViewModel.retroUiState = RetroUiState.Loading
                                            }
                                        }
                                ) {
                                    
                                    /*HomeScreen(
                                        uiState = retroViewModel.retroUiState,
                                        fishCount = retroViewModel.fishCount,
                                        false
                                    )*/
                                    
                                    TestBox(retroViewModel = retroViewModel)

                                }


                                ""
                            }

                            is RetroUiState.Error -> "Error on request"
                            is RetroUiState.Loading -> "Loading"
                        }
                    }
                }
            )

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Button(
                    onClick = {
                        currentState = -1
                        onFocusShowcase = true
                        navController.navigate("showcase")
                    },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(text = "ShowCase")
                }
            }


            // Register a lifecycle observer to start and stop sensor updates
            registerSensorListener()
        }



    }


}


fun createAnchorNode(
    engine: Engine,
    modelLoader: ModelLoader,
    materialLoader: MaterialLoader,
    modelInstances: MutableList<ModelInstance>,
    anchor: Anchor,
    retroViewModel: RetroViewModel
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
                if(currentState == 0 && !entered) {
                    entered = true
                    this.stopAnimation(animationName = "NoHook")
                    this.playAnimation(animationName = "HookIdle", loop = true)

                    //this.playAnimation(animationName = "HookIdle", loop = true)
                    retroViewModel.getFishInfo(0)
                    Log.println(Log.INFO,"Fish","done")

                    var waitTheCatch = Random.nextInt(500, 3501)
                    Handler().postDelayed({
                        if(!onFocusShowcase)
                            currentState = 1
                        entered = false
                    }, waitTheCatch.toLong())

                }
            }
            true
        }
        onLongPress = {e: MotionEvent ->
            if (currentState == 2 && !entered){
                entered = true
                this.stopAnimation(animationName = "Catch")
                this.stopAnimation(animationName = "FishHooking")
                this.playAnimation(animationName = "NoHook", loop = true)
                this.playAnimation(animationName = "Idle", loop = true)
                currentState = 3
                entered = false
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

fun decodeBase64ToImageBitmap(base64String: String): androidx.compose.ui.graphics.ImageBitmap? {
    return try {
        val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        bitmap.asImageBitmap()
    } catch (e: Exception) {
        Log.e("ImageDecoding", "Error decoding base64 image", e)
        null
    }
}

@Composable
fun FishGrid(fishPairs: List<Pair<String, String>>, navController: NavController) {
    LazyVerticalGrid(columns = GridCells.Fixed(3), // Set the number of columns here
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(fishPairs) { pair ->
            val fishId = pair.first
            val imageBitmap = decodeBase64ToImageBitmap(pair.second)
            imageBitmap?.let {
                Image(
                    bitmap = it,
                    contentDescription = "Fish ID: $fishId",
                    modifier = Modifier
                        .size(128.dp)
                        .clickable {
                            // Navigate to the fish detail screen with arguments
                            navController.navigate("fishDetail/$fishId")
                        }
                )
            }
        }
    }
}

fun convertJsonToPairs(jsonObject: JsonObject): List<Pair<String, String>> {
    return jsonObject.entrySet().map { entry ->
        entry.key to entry.value.asString
    }
}

@Composable
fun FishGridDisplay(viewModel: RetroViewModel, navController: NavController) {
    val state = viewModel.retroGridState
    when (state) {
        is RetroUiState.Loading -> {
            // Display a loading indicator
            Log.println(Log.INFO, "FishGrid", "Loading")
        }

        is RetroUiState.Success -> {
            val fishPairs = convertJsonToPairs(state.fishInfo)
            Log.println(Log.INFO, "FishGrid", fishPairs.toString())
            FishGrid(fishPairs, navController = navController)
        }

        is RetroUiState.Error -> {
            // Display an error message
            Log.println(Log.INFO, "FishGrid", "Error")
        }
    }
}

@Composable
fun ShowcaseBox(retroViewModel: RetroViewModel, navController: NavController){

    // Create a composable box with placeholder text

    RetroTestTheme {


        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fish Showcase",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f) // Takes up available space
                    )
                    Button(
                        onClick = {
                            onFocusShowcase = false
                            entered = false
                            navController.navigate("arbox")
                        },
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(text = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))


                FishGridDisplay(retroViewModel, navController)


                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FishDetailScreen(fishId: Int, navController: NavController, retroViewModel: RetroViewModel) {
    retroViewModel.getFishInfo(fishId)
    val state = retroViewModel.retroUiState
    var name = ""
    var image = ""
    var description = ""
    when (state) {
        is RetroUiState.Loading -> {
            // Display a loading indicator
            Log.println(Log.INFO, "FishDetail", "Loading")
        }

        is RetroUiState.Success -> {
            name = state.fishInfo.get("name").toString()
            name = name.substring(1, name.length - 1)
            image = state.fishInfo.get("image").toString()
            description = state.fishInfo.get("description").toString()
            description = description.substring(1, description.length - 1)
        }

        is RetroUiState.Error -> {
            // Display an error message
            Log.println(Log.INFO, "FishDetail", "Error")
        }
    }


    Column {
        TopAppBar(
            title = { Text(name) },
            navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            },

        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Fish ID: $fishId", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            val imageBitmap = decodeBase64ToImageBitmap(image)
            imageBitmap?.let {
                Image(
                    bitmap = it,
                    contentDescription = "Fish ID: $fishId",
                    modifier = Modifier
                        .size(128.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Detail: $description", fontSize = 20.sp)
        }
    }
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun TestBox(retroViewModel: RetroViewModel){

    val configuration = LocalConfiguration.current

    val screenHeight = configuration.screenHeightDp.dp

    //retroViewModel.getFishInfo(1)

    val scaleAnimation = remember { Animatable(0.5f) }
    val rotateYAnimation = remember { Animatable(180f) }
    val yAnimation = remember { Animatable(-1.5f) }

    when(retroViewModel.retroUiState){
        is RetroUiState.Success -> {
            LaunchedEffect("animationKey") {

                yAnimation.animateTo(0f, animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic))
                rotateYAnimation.animateTo(0f, animationSpec = tween(durationMillis = 1000 ,easing = CubicBezierEasing(0.0f, 0.42f, 1.0f, 0.58f)))
                scaleAnimation.animateTo(1.0f)

            }
        }

        else -> {

        }
    }



    var cardFace by remember {
        mutableStateOf(CardFace.Front)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scaleAnimation.value
                scaleY = scaleAnimation.value
                translationY = yAnimation.value * screenHeight.toPx()
            }



    ) {
        FlipCard(
            cardFace = cardFace,
            onClick = { cardFace = cardFace.next },
            modifier = Modifier
                .fillMaxWidth(.5f)
                .aspectRatio(1f),
            front = {
                HomeScreen(
                    uiState = retroViewModel.retroUiState,
                    fishCount = retroViewModel.fishCount,
                    backSide = false
                )
            },
            back = {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationY = 180f
                        },
                ) {
                    HomeScreen(
                        uiState = retroViewModel.retroUiState,
                        fishCount = retroViewModel.fishCount,
                        backSide = true
                    )
                }
            },
            rotation = rotateYAnimation
        )

    }








}

enum class CardFace(val angle: Float) {
    Front(0f) {
        override val next: CardFace
            get() = Back
    },
    Back(180f) {
        override val next: CardFace
            get() = Front
    };

    abstract val next: CardFace
}

@Composable
fun FlipCard(
    cardFace: CardFace,
    onClick: (CardFace) -> Unit,
    modifier: Modifier = Modifier,
    back: @Composable () -> Unit = {},
    front: @Composable () -> Unit = {},
    rotation: Animatable<Float,AnimationVector1D>
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationY = rotation.value
                scaleX = (rotation.value / 90 - 1).pow(2) / 2 + 0.5F
            }
    ) {
        if (rotation.value <= 90f) {
            front()
        } else {
            back()
        }
    }

}