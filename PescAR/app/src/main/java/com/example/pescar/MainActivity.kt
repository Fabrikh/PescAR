package com.example.pescar

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Handler
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.gson.JsonObject
import kotlin.math.pow
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import coil.size.Size
import com.google.android.filament.MaterialInstance
import com.google.ar.sceneform.rendering.Material
import io.github.sceneview.material.setEmissiveStrength
import io.github.sceneview.node.RenderableNode


private const val kModelFile = "models/lake_and_fish.glb"
private const val kModelFile_Rod = "models/bamboo_fishing_rod.glb"
private const val kMaxModelInstances = 1
private var lakeNode: ModelNode? = null
private var currentState = -1
private  var tutorialState = true
private var onFocusShowcase = false

private var entered = false

private var DIFFICULTY = 1 // 0: Easy, 1: Medium, 2: Hard


object FishPreferences {
    private const val PREF_NAME = "FishPrefs"
    private const val FISH_CAUGHT_KEY = "FishCaught"
    private const val LURE_KEY = "Lure"

    fun saveFishCaught(context: Context, fishId: Int) {
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val existingFish = getCaughtFish(context).toMutableSet()
        existingFish.add(fishId.toString()) // Keep as string since SharedPreferences stores Set<String>
        sharedPrefs.edit().putStringSet(FISH_CAUGHT_KEY, existingFish).apply()
    }

    fun getCaughtFish(context: Context): Set<String> { // Change return type to Set<String>
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getStringSet(FISH_CAUGHT_KEY, emptySet()) ?: emptySet()
    }

    fun saveLure(context: Context, lureId: Int){
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().putInt(LURE_KEY, lureId).apply()
    }

    fun getLure(context: Context): Int {
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getInt(LURE_KEY, 1)
    }
}

class MainActivity : ComponentActivity() {
    lateinit var retroViewModel: RetroViewModel
    lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        mediaPlayer = MediaPlayer.create(this, R.raw.soundtrack)
        mediaPlayer?.isLooping = true // Riproduzione in loop
        mediaPlayer?.start()

        setContent {

            retroViewModel = viewModel()

            val navController = rememberNavController()
            val buttonMediaPlayer = MediaPlayer.create(LocalContext.current, R.raw.buttoncut)



            NavHost(navController, startDestination = "menu") {

                composable("arbox/{tutorialmode}", arguments = listOf(
                    navArgument("tutorialmode") { type = NavType.BoolType }
                )) { backStackEntry ->
                    ARBox(
                        tutorialmode = backStackEntry.arguments?.getBoolean("tutorialmode") ?: false,
                        retroViewModel = retroViewModel,
                        navController = navController,
                        buttonMediaPlayer = buttonMediaPlayer
                    )
                }
                composable("showcase") {ShowcaseBox(retroViewModel, navController, buttonMediaPlayer)}

                composable("menu") { MenuScreen(retroViewModel, navController, buttonMediaPlayer) }

                composable("fishDetail/{fishId}", arguments = listOf(
                    navArgument("fishId") { type = NavType.IntType }
                )) { backStackEntry ->
                    FishDetailScreen(
                        fishId = backStackEntry.arguments?.getInt("fishId") ?: 0,
                        navController = navController,
                        retroViewModel = retroViewModel
                    )
                }
                //composable("test"){TestBox(retroViewModel)}
            }


        }
    }
    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause() // Mette in pausa la riproduzione quando l'app è in pausa
    }

    override fun onResume() {
        super.onResume()
        mediaPlayer?.start() // Riprende la riproduzione quando l'app riprende
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release() // Rilascia le risorse quando l'app viene distrutta
    }
}


@Composable
fun GifImage(
    modifier: Modifier = Modifier,
    imagename: Any
) {
    val context = LocalContext.current
    val imageLoader = ImageLoader.Builder(context)
        .components {
            if (SDK_INT >= 28) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .build()
    Image(
        painter = rememberAsyncImagePainter(
            ImageRequest.Builder(context).data(data = imagename).apply(block = {
                size(Size.ORIGINAL)
            }).build(), imageLoader = imageLoader
        ),
        contentDescription = null,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun TutorialPopup(content: (@Composable () -> Unit)?, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = {
            if (content != null) {
                content()
            } else {
                Text("No content available.")
            }
        },
        confirmButton = {
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Blue,
                    contentColor = Color.White
                )
            ) {
                Text("Got it!")
            }
        }
    )
}

@Composable
fun ARBox(tutorialmode: Boolean, retroViewModel: RetroViewModel, navController: NavController, buttonMediaPlayer: MediaPlayer) {

    val mContext = LocalContext.current
    val castMediaPlayer = MediaPlayer.create(mContext, R.raw.cast)
    val reelinMediaPlayer = MediaPlayer.create(mContext, R.raw.reelin)
    currentState = -1
    tutorialState = true

    Log.println(Log.INFO,"SAVEDLURE", FishPreferences.getLure(mContext).toString())

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
            var tutorialState by remember { mutableStateOf(true) }
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
                                    castMediaPlayer.start()
                                    retroViewModel.getFishInfo(-1, DIFFICULTY)
                                    Log.println(Log.INFO,"Fish","done")

                                    var waitTheCatch = Random.nextInt(500, 3501)

                                    Handler().postDelayed({
                                        if(!onFocusShowcase){
                                            currentState = 1
                                            tutorialState = true
                                        }

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
                                    reelinMediaPlayer.start()
                                    currentState = 3
                                    tutorialState = true
                                    entered = false

                                    val state = retroViewModel.retroUiState
                                    when (state) {
                                        is RetroUiState.Loading -> {

                                        }

                                        is RetroUiState.Success -> {
                                            Log.println(Log.INFO, "Fish", "CAUGHT")
                                            FishPreferences.saveFishCaught(context, state.fishInfo.get("id").asInt)
                                        }

                                        is RetroUiState.Error -> {

                                        }
                                    }
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

            fun triggerVibration() {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?

                // Check if the device has a vibrator
                if (vibrator != null && vibrator.hasVibrator()) {
                    // Vibrate for 500 milliseconds
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val vibrationEffect = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
                        vibrator.vibrate(vibrationEffect)
                    } else {
                        // For older devices without vibration effect support
                        vibrator.vibrate(500)
                    }
                }
            }

            val rainbowColors = listOf(
                Color.Red,
                Color.Yellow,
                Color.Green,
                Color.Blue,
                Color.Cyan,
                Color.Magenta,
            )


            fun changeLureColor(lureId: Int){

                if (currentState == -1) return

                val BAITMATERIALS = arrayOf(
                    materialLoader.createColorInstance(
                        color = Color(0.773f, 0.773f, 0.773f, 1.0f)
                    ),
                    materialLoader.createColorInstance(
                        color = rainbowColors.random(),
                    ),
                    materialLoader.createColorInstance(
                        color = Color(0.396f, 0.945f, 0.161f, 1.0f),
                        reflectance = 2f,
                        roughness = 2f
                    ),
                    materialLoader.createColorInstance(
                        color = Color(0.941f, 0.792f, 0.031f, 1.0f),
                        metallic = 2f,
                        reflectance = 1f
                    )
                )

                (lakeNode?.nodes?.get("Hook") as RenderableNode).materialInstance = BAITMATERIALS[lureId-1] as MaterialInstance
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
                onSessionCreated = { session ->
                },

                onSessionUpdated = { session, updatedFrame ->

                    if (currentState == 1)
                        if (retroViewModel.retroUiState == RetroUiState.Error) {
                            lakeNode!!.stopAnimation(animationName = "HookIdle")
                            lakeNode!!.playAnimation(animationName = "NoHook", loop = true)
                            lakeNode!!.playAnimation(animationName = "Idle", loop = true)
                            Log.println(Log.ERROR, "MyApp", "RetroViewModel Error")
                            currentState = 0
                            tutorialState = true
                        } else if (retroViewModel.retroUiState != RetroUiState.Loading) {
                            lakeNode!!.stopAnimation(animationName = "HookIdle")
                            lakeNode!!.playAnimation(animationName = "FishHooking", loop = true)
                            lakeNode!!.playAnimation(animationName = "Catch", loop = true)
                            Log.println(Log.INFO, "MyApp", "RetroViewModel Success")
                            currentState = 2
                            tutorialState = true
                            triggerVibration()
                        }

                    if (internalState != currentState)
                        internalState = currentState


                    frame = updatedFrame
                },
                onGestureListener = rememberOnGestureListener(
                    onSingleTapConfirmed = { motionEvent, node ->

                        if (currentState == 3 || currentState == -1) {
                            currentState = 0
                            tutorialState = true
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
                                        retroViewModel = retroViewModel,
                                        castMediaPlayer,
                                        reelinMediaPlayer,
                                        context = context,
                                        changeLureColor = ::changeLureColor
                                    )

                                }
                        }

                    })


            )

            Image(

                painter = painterResource(id = R.drawable.rod2),
                contentDescription = "Fishing Rod",
                modifier = Modifier
                    .fillMaxHeight()
                    .graphicsLayer {
                        rotationZ = 110f
                        scaleX = 2.2f
                        scaleY = -2.0f
                    }
                    .offset(x = (-40).dp, y = 90.dp)
            )
            /*
            val text = when (currentState) {
                -1 -> if (tutorialmode){
                    if(childNodes.isEmpty()){
                        stringResource(R.string.point_your_phone_down)
                    } else {
                        stringResource(R.string.place_grid)
                    }
                } else ""
                0 -> if (tutorialmode) stringResource(R.string.cast_fishing_rod) else ""
                1 -> if (tutorialmode) stringResource(R.string.cast_fishing_rod) else ""
                2 -> if (tutorialmode) stringResource(R.string.pull_fishing_rod) else ""
                else -> {
                    when (val state = retroViewModel.retroUiState) {
                        is RetroUiState.Success -> {
                            val fishId = state.fishInfo.get("id").asInt

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable {
                                        if (currentState == 3 || currentState == -1) {
                                            currentState = 0
                                            retroViewModel.retroUiState = RetroUiState.Loading
                                        }
                                    }
                            ) {
                                if (tutorialmode) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .background(
                                                Color.Black.copy(alpha = 0.7f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(8.dp)
                                            .padding(top = 16.dp, start = 32.dp, end = 32.dp)
                                    ) {
                                        Text(
                                            textAlign = TextAlign.Center,
                                            fontSize = 28.sp,
                                            color = Color.White,
                                            text = stringResource(R.string.catch_screen) // Replace with your tutorial text
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .height(460.dp)
                                        .align(Alignment.Center)
                                        .clickable { navController.navigate("fishDetail/$fishId") }
                                ) {
                                    TestBox(retroViewModel = retroViewModel)
                                }
                            }

                            ""
                        }
                        is RetroUiState.Error -> "Error on request"
                        is RetroUiState.Loading -> "Loading"
                    }
                }
            }

            Box(
                modifier = Modifier
                    .systemBarsPadding()
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp, start = 32.dp, end = 32.dp)
            ) {
                if (text.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .background(
                                Color.Black.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                            .align(Alignment.Center)
                    ) {
                        Text(
                            textAlign = TextAlign.Center,
                            fontSize = 28.sp,
                            color = Color.White,
                            text = text
                        )
                    }
                }
            }

            */

            var currentPopup by remember { mutableStateOf(1) }
            if (tutorialmode && tutorialState) {
                when(currentState) {
                    -1 -> when (currentPopup) {
                        1 -> {
                            TutorialPopup(
                                content = {
                                          Column {
                                              Text(stringResource(R.string.point_your_phone_down))
                                          }
                                          },
                                onClose = { currentPopup = 2 }
                            )
                        }

                        2 -> {
                            TutorialPopup(
                                content = {
                                    Column {
                                        Text(stringResource(R.string.my_collection))
                                    }
                                },
                                onClose = { currentPopup = 3 }
                            )
                        }

                        3 -> {
                            TutorialPopup(
                                content = {
                                    Column {
                                        Text(stringResource(R.string.lures))
                                    }
                                },
                                onClose = {
                                    currentPopup = 1
                                    tutorialState = false
                                }
                            )
                        }
                    }
                    0 -> {
                        TutorialPopup(
                            content = {
                                Column {
                                    Text(stringResource(R.string.cast_fishing_rod))
                                    /*
                                    Image(
                                        painter = painterResource(id = R.drawable.cast_draw), // Replace with your image resource
                                        contentDescription = "Cast Tutorial",
                                        modifier = Modifier.fillMaxWidth(),
                                        contentScale = ContentScale.Crop
                                    )
                                    */
                                    val imageLoader = ImageLoader.Builder(context)
                                        .components {
                                            if (SDK_INT >= 28) {
                                                add(ImageDecoderDecoder.Factory())
                                            } else {
                                                add(GifDecoder.Factory())
                                            }
                                        }
                                        .build()
                                    GifImage(imagename = R.drawable.resource_throw)
                                }
                            },
                            onClose = {
                                tutorialState = false
                            }
                        )
                    }
                    1 -> {
                        null
                    }
                    2 -> {
                        TutorialPopup(
                            content = {
                                Column {
                                    Text(stringResource(R.string.pull_fishing_rod))
                                    val imageLoader = ImageLoader.Builder(context)
                                        .components {
                                            if (SDK_INT >= 28) {
                                                add(ImageDecoderDecoder.Factory())
                                            } else {
                                                add(GifDecoder.Factory())
                                            }
                                        }
                                        .build()
                                    GifImage(imagename = R.drawable.resource_catch)
                                }
                            },
                            onClose = {
                                tutorialState = false
                            }
                        )
                    }
                    3 -> {
                        TutorialPopup(
                            content = {
                                Column {
                                    Text(stringResource(R.string.catch_screen))
                                }
                            },
                            onClose = {
                                tutorialState = false
                            }
                        )
                    }
                    else -> {null}
                }

            } else {
                val text = if(currentState > 2){
                    when (val state = retroViewModel.retroUiState) {
                        is RetroUiState.Success -> {
                            val fishId = state.fishInfo.get("id").asInt

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable {
                                        if (currentState == 3 || currentState == -1) {
                                            currentState = 0
                                            tutorialState = true
                                            retroViewModel.retroUiState = RetroUiState.Loading
                                        }
                                    }
                            ) {

                                Box(
                                    modifier = Modifier
                                        .height(460.dp)
                                        .align(Alignment.Center)
                                        .clickable { navController.navigate("fishDetail/$fishId") }
                                ) {
                                    TestBox(retroViewModel = retroViewModel)
                                }
                            }

                            ""
                        }
                        is RetroUiState.Error -> "Error on request"
                        is RetroUiState.Loading -> "Loading"
                    }
                } else {
                    ""
                }

                Box(
                    modifier = Modifier
                        .systemBarsPadding()
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp, start = 32.dp, end = 32.dp)
                ) {
                    if (text.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Color.Black.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp)
                                .align(Alignment.Center)
                        ) {
                            Text(
                                textAlign = TextAlign.Center,
                                fontSize = 28.sp,
                                color = Color.White,
                                text = text
                            )
                        }
                    }
                }
            }


            var onFocusBaits by remember { mutableStateOf(false) }

            fun closeBaitGrid(){
                onFocusBaits = !onFocusBaits
            }





            if(onFocusBaits){
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        //.then(if (!onFocusBaits) Modifier.alpha(0f) else Modifier.alpha(1f))
                        .clickable { onFocusBaits = false }

                    ,
                    contentAlignment = Alignment.Center,
                ) {
                    BaitGrid(onFocusBaits, context, { closeBaitGrid() }, ::changeLureColor, buttonMediaPlayer)
                }
            }



            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomStart,
            ) {
                ElevatedButton(onClick = {
                    currentState = -1
                    tutorialState = true
                    onFocusShowcase = true
                    buttonMediaPlayer.start()

                    navController.navigate("showcase")
                },
                    modifier = Modifier.width(180.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(31, 111, 139, 255),
                        contentColor = Color.White),
                    border = BorderStroke(2.dp,Color(22, 89, 112, 120))
                ) {
                    Text("MY COLLECTION",fontWeight = FontWeight.Bold)
                }
            }

            class QuarterCircleShape : Shape {
                override fun createOutline(
                    size: androidx.compose.ui.geometry.Size,
                    layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                    density: Density
                ): Outline {
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, size.height)
                        arcTo(
                            rect = Rect(0f, 0f, size.width * 2, size.height * 2),
                            startAngleDegrees = 180f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false
                        )
                        lineTo(size.width, size.height)

                        close()
                    }
                    return Outline.Generic(path)
                }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomEnd,
            ) {
                ElevatedButton(onClick = {
                    onFocusBaits = !onFocusBaits
                    buttonMediaPlayer.start()
                },
                    modifier = Modifier.width(130.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(31, 111, 139, 255),
                        contentColor = Color.White),
                    border = BorderStroke(2.dp,Color(22, 89, 112, 120)),
                    shape = QuarterCircleShape()
                ) {
                    Column(
                        modifier = Modifier,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(
                                id = mContext.resources.getIdentifier(
                                    "lure" +
                                            FishPreferences.getLure(mContext).toString(),
                                    "drawable",
                                    mContext.packageName
                                )
                            ),
                            contentDescription = null,
                            modifier = Modifier
                                .aspectRatio(1f), // Mantieni l'aspect ratio dell'immagine
                            contentScale = ContentScale.Fit

                        )
                        Text("LURES", fontWeight = FontWeight.Bold)
                    }
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
    retroViewModel: RetroViewModel,
    castMediaPlayer: MediaPlayer,
    reelinMediaPlayer: MediaPlayer,
    context: Context,
    changeLureColor: (lureId: Int) -> Unit
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

        /*
        this.modelInstance.materialInstances[0] = materialLoader.createColorInstance(
            color = Color(0.0f, 0.0f, 1.0f, 1.0f)
        )*/


        Log.println(Log.INFO,"Model", this.nodes.toString())
        Log.println(Log.INFO,"Model", this.renderableNodes.toString())
        /*
        val test = this.nodes["Hook"].apply {
            setMaterialInstance( materialInstance = materialLoader.createColorInstance(
                color = Color(0.0f, 0.102f, 1.0f, 1.0f)
            ))
        }*/

        onFling = {e1: MotionEvent?, e2: MotionEvent, v: Float2 ->

            if(e1 != null && e2.y < e1.y) {
                if(currentState == 0 && !entered) {
                    entered = true
                    this.stopAnimation(animationName = "NoHook")

                    this.playAnimation(animationName = "HookIdle", loop = true)
                    castMediaPlayer.start()

                    //this.playAnimation(animationName = "HookIdle", loop = true)
                    retroViewModel.getFishInfo(-1, DIFFICULTY)
                    Log.println(Log.INFO,"Fish","done")

                    var waitTheCatch = Random.nextInt(500, 3501)
                    Handler().postDelayed({
                        if(!onFocusShowcase){
                            currentState = 1
                            tutorialState = true
                        }

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
                reelinMediaPlayer.start()
                currentState = 3
                tutorialState = true
                entered = false
                val state = retroViewModel.retroUiState
                when (state) {
                    is RetroUiState.Loading -> {

                    }

                    is RetroUiState.Success -> {
                        Log.println(Log.INFO, "Fish", "CAUGHT")
                        FishPreferences.saveFishCaught(context, state.fishInfo.get("id").asInt)
                    }

                    is RetroUiState.Error -> {

                    }
                }
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
    changeLureColor(FishPreferences.getLure(context))
    currentState = 0
    tutorialState = true
    return anchorNode
}

fun Modifier.grayScale(): Modifier {
    val saturationMatrix = ColorMatrix().apply { setToSaturation(0f) }
    val saturationFilter = ColorFilter.colorMatrix(saturationMatrix)
    val paint = Paint().apply { colorFilter = saturationFilter }

    return drawWithCache {
        val canvasBounds = Rect(Offset.Zero, size)
        onDrawWithContent {
            drawIntoCanvas {
                it.saveLayer(canvasBounds, paint)
                drawContent()
                it.restore()
            }
        }
    }
}

@Composable
fun FishGrid(fishPairs: List<Pair<Int, String>>, navController: NavController, buttonMediaPlayer: MediaPlayer) {
    val caughtFishIds = FishPreferences.getCaughtFish(LocalContext.current)

    LazyVerticalGrid(columns = GridCells.Fixed(3), // Set the number of columns here
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        items(fishPairs) { pair ->
            val fishId = pair.first
            val isCaught = caughtFishIds.contains(fishId.toString())
            val imageBitmap = DecodedImage(pair.second)
            // Apply different modifiers based on whether the fish is caught or not
            val modifier = if (isCaught) {
                // If caught, make the image clickable with no filter applied
                Modifier
                    .size(128.dp)
                    .clickable {
                        buttonMediaPlayer.start()
                        navController.navigate("fishDetail/$fishId")
                    }
            } else {
                // If not caught, apply a grayscale filter and do not make it clickable
                Modifier
                    .size(128.dp)
                    .grayScale()
            }
            imageBitmap?.let {
                Image(
                    bitmap = it,
                    contentDescription = "Fish ID: $fishId",
                    modifier = modifier
                )
            }
        }
    }
}

fun convertJsonToPairs(jsonObject: JsonObject): List<Pair<Int, String>> {
    return jsonObject.entrySet().map { entry ->
        entry.key.toInt() to entry.value.asString
    }.sortedBy { it.first }
}

@Composable
fun FishGridDisplay(viewModel: RetroViewModel, navController: NavController, buttonMediaPlayer: MediaPlayer) {
    val state = viewModel.retroGridState
    when (state) {
        is RetroUiState.Loading -> {
            // Display a loading indicator
            Log.println(Log.INFO, "FishGrid", "Loading")
        }

        is RetroUiState.Success -> {
            val fishPairs = convertJsonToPairs(state.fishInfo)
            Log.println(Log.INFO, "FishGrid", fishPairs.toString())
            FishGrid(fishPairs, navController = navController, buttonMediaPlayer = buttonMediaPlayer)
        }

        is RetroUiState.Error -> {
            // Display an error message
            Log.println(Log.INFO, "FishGrid", "Error")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowcaseBox(retroViewModel: RetroViewModel, navController: NavController, buttonMediaPlayer: MediaPlayer){

    RetroTestTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background layer
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.4f),
                                Color.White.copy(alpha = 0.6f)
                            )
                        )
                    )
                    .paint(
                        painter = painterResource(id = R.drawable.showcaseback),
                        contentScale = ContentScale.FillHeight,
                        alpha = 0.3f
                    )
            )

            // Overlay layer with visual noise or texture (simulated here with a gradient)
            // In a real app, you might overlay a PNG asset with a noise texture or similar
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    )
                    .paint(
                        painter = painterResource(id = R.drawable.underwater1),
                        contentScale = ContentScale.FillHeight,
                        alpha = 0.3f
                    )

            )

            // Your content here
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CenterAlignedTopAppBar(
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xAB000000)),
                        title = {
                            Text(
                                text = "Fish Collection",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                onFocusShowcase = false
                                entered = false
                                navController.popBackStack()
                            }
                            ) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        }
                    )

                    FishGridDisplay(retroViewModel, navController, buttonMediaPlayer)

                }
            }
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FishDetailScreen(fishId: Int, navController: NavController, retroViewModel: RetroViewModel) {

    LaunchedEffect(fishId){
        Log.println(Log.INFO, "FishDetail", "Enter")
        retroViewModel.retroUiState = RetroUiState.Loading
        retroViewModel.getFishInfo(fishId, DIFFICULTY)
    }
    DisposableEffect(fishId){
        onDispose {
            Log.println(Log.INFO, "FishDetail", "Exit")
            retroViewModel.retroUiState = RetroUiState.Loading
        }
    }

    RetroTestTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background layer
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.4f),
                                Color.White.copy(alpha = 0.6f)
                            )
                        )
                    )
                    .paint(
                        painter = painterResource(id = R.drawable.showcaseback),
                        contentScale = ContentScale.FillHeight,
                        alpha = 0.3f
                    )
            )

            // Overlay layer with visual noise or texture (simulated here with a gradient)
            // In a real app, you might overlay a PNG asset with a noise texture or similar
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    )
                    .paint(
                        painter = painterResource(id = R.drawable.underwater1),
                        contentScale = ContentScale.FillHeight,
                        alpha = 0.3f
                    )

            )

            // Your content here
            Column {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xAB000000)),
                    title = { Text("") },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                )
            }

            val state = retroViewModel.retroUiState
            when (state) {
                is RetroUiState.Loading -> {
                    // Display a loading indicator
                    Log.println(Log.INFO, "FishDetail", "Loading")
                }

                is RetroUiState.Success -> {
                    HomeScreen(
                        uiState = retroViewModel.retroUiState,
                        fishCount = retroViewModel.fishCount,
                        false
                    )
                }

                is RetroUiState.Error -> {
                    // Display an error message
                    Log.println(Log.INFO, "FishDetail", "Error")
                }
            }
        }

    }
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun TestBox(retroViewModel: RetroViewModel){

    val configuration = LocalConfiguration.current

    val screenHeight = configuration.screenHeightDp.dp

    val scaleAnimation = remember { Animatable(0.5f) }
    val rotateYAnimation = remember { Animatable(180f) }
    val yAnimation = remember { Animatable(-1.5f) }

    val cardMediaPlayer = MediaPlayer.create(LocalContext.current, R.raw.card)

    when(retroViewModel.retroUiState){
        is RetroUiState.Success -> {
            LaunchedEffect("animationKey") {


                yAnimation.animateTo(0f, animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic))
                rotateYAnimation.animateTo(0f, animationSpec = tween(durationMillis = 1000 ,easing = CubicBezierEasing(0.0f, 0.42f, 1.0f, 0.58f)))
                scaleAnimation.animateTo(1.0f)
                cardMediaPlayer.start()
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

@Composable
fun MenuScreen(retroViewModel: RetroViewModel, navController: NavController, buttonMediaPlayer: MediaPlayer){


    Box(modifier = Modifier
        .background(Color(45, 67, 208))
        .paint(
            painter = painterResource(id = R.drawable.menuback),
            contentScale = ContentScale.FillWidth
        )
        .fillMaxSize(),
        contentAlignment = Alignment.Center){

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Image(painter = painterResource(id = R.drawable.logocard2), contentDescription = null,
                modifier = Modifier.scale(2f))

            Spacer(modifier = Modifier.height(200.dp))

            ElevatedButton(onClick = {
                var tutorialmode = false
                navController.navigate("arbox/$tutorialmode")
                buttonMediaPlayer.start()
                                     },
                modifier = Modifier.width(180.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(31, 111, 139, 255)),
                border = BorderStroke(2.dp,Color(22, 89, 112, 120))
            ) {
                Text("START",fontWeight = FontWeight.Bold)
            }
            ElevatedButton(onClick = {
                var tutorialmode = true
                navController.navigate("arbox/$tutorialmode")
                buttonMediaPlayer.start()
            },
                modifier = Modifier.width(180.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(31, 111, 139, 255)),
                border = BorderStroke(2.dp,Color(22, 89, 112, 120))
            ) {
                Text("TUTORIAL", fontWeight = FontWeight.Bold)
            }
            ElevatedButton(onClick = {
                navController.navigate("showcase")
                buttonMediaPlayer.start()
                                     },
                modifier = Modifier.width(180.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(31, 111, 139, 255)),
                border = BorderStroke(2.dp,Color(22, 89, 112, 120))
            ) {
                Text("MY COLLECTION",fontWeight = FontWeight.Bold)
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun PreviewTest() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .size(
                LocalConfiguration.current.screenWidthDp.dp * 0.8f,
                LocalConfiguration.current.screenHeightDp.dp * 0.5f
            ),
    ) {
        CenterAlignedTopAppBar(
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color(0xE2383838)
            ),
            title = {
                Text(
                    text = "Lures",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        )
        Test()
    }
}
@Composable
fun Test(){


        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .background(
                    Color.Gray
                )
                .padding(10.dp)

                //.border(1.dp, color = Color(96, 119, 128, 255))
                .fillMaxSize()
        ) {

            Column (
                horizontalAlignment = Alignment.CenterHorizontally,
            ){

                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    val maxHeight = constraints.maxHeight
                    val maxWidth = constraints.maxWidth

                    Image(
                        painter = painterResource(
                            id = LocalContext.current.resources.getIdentifier(
                                "lure1",
                                "drawable",
                                LocalContext.current.packageName
                            )
                        ),
                        contentDescription = null,
                        modifier = Modifier
                            .size(
                                minOf(maxWidth, maxHeight).dp * 0.5f
                            )
                            .aspectRatio(1f), // Mantieni l'aspect ratio dell'immagine
                        contentScale = ContentScale.Fit
                    )
                }

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally){
                    Text("Nome esca", fontWeight = FontWeight.Bold, fontSize = TextUnit(
                        20F,
                        TextUnitType.Sp
                    ))
                    Text("Questa è una descrizione di lunghezza contenuta",
                        textAlign = TextAlign.Center
                        )
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {

                    ElevatedButton(
                        onClick = {

                        },
                        modifier = Modifier.width(100.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(
                                119,
                                18,
                                14,
                                255
                            ),
                            contentColor = Color.White
                        ),
                        border = BorderStroke(2.dp, Color(82, 44, 52, 255))
                    ) {
                        Text("BACK", fontWeight = FontWeight.Bold)
                    }

                    ElevatedButton(
                        onClick = {

                        },
                        modifier = Modifier.width(100.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(
                                26,
                                214,
                                4,
                                255
                            ),
                            contentColor = Color.White
                        ),
                        border = BorderStroke(2.dp, Color(34, 100, 9, 255))
                    ) {
                        Text("EQUIP", fontWeight = FontWeight.Bold)
                    }
                }

            }
        }



            }



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaitGrid(focus: Boolean, context: Context, closeBaitGrid: () -> Unit, changeLureColor: (lureId: Int) -> Unit, buttonMediaPlayer: MediaPlayer){

    val baits = (1..4).toList()
    var selectedBait = remember { mutableStateOf(1) }
    var gridState = remember { mutableStateOf(0) }

    val lureDescriptions = listOf(
        AnnotatedString(
            "A common lure used for a common fish",
            spanStyles = listOf(
                AnnotatedString.Range(SpanStyle(color= colorResource(R.color.common)), 25, 31)
            )
        ),
        AnnotatedString(
            "Increase the chance to get a rare fish",
            spanStyles = listOf(
                AnnotatedString.Range(SpanStyle(color= colorResource(R.color.rare)), 29, 33)
            )
        ),
        AnnotatedString(
            "Increase the chance to get an abyssal type fish",
            spanStyles = listOf(
                AnnotatedString.Range(SpanStyle(color=colorResource(R.color.abyssal)), 30, 37)
            )
        ),
        AnnotatedString(
            "Increase the chance to get a legendary fish",
            spanStyles = listOf(
                AnnotatedString.Range(SpanStyle(color=colorResource(R.color.legendary)), 29, 38)
            )
        )

    )


    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .size(LocalConfiguration.current.screenWidthDp.dp * 0.8f,
                LocalConfiguration.current.screenHeightDp.dp * 0.5f)
        ,
    ) {
        CenterAlignedTopAppBar(
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color(0xE2383838)
            ),
            title = {
                Text(
                    text = "Lures",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        )

        Box(
            modifier = Modifier
                .background(
                    Color(180, 180, 180, 255)
                )
                .padding(10.dp)

                //.border(1.dp, color = Color(96, 119, 128, 255))
                .fillMaxSize()
                ,
            contentAlignment = Alignment.Center
        ) {

            when(gridState.value){
                0 -> {

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .padding(0.dp)
                            .fillMaxSize()
                    ) {
                        items(baits) { bait ->
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .background(
                                        Color(68, 66, 66, 255)
                                    )
                                    .clickable {
                                        selectedBait.value = bait
                                        //FishPreferences.saveLure(context, selectedBait)
                                        buttonMediaPlayer.start()
                                        gridState.value = 1
                                        //closeBaitGrid()
                                    }
                                    .border(1.dp, color = Color(56, 53, 53, 255))
                                    .padding(8.dp)
                                //.fillMaxSize()
                            ) {

                                Image(
                                    painter = painterResource(id = LocalContext.current.resources.getIdentifier(
                                        "lure$bait","drawable", LocalContext.current.packageName)),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .aspectRatio(1f), // Mantieni l'aspect ratio dell'immagine
                                    contentScale = ContentScale.Fit

                                )
                            }


                        }
                    }
                }
                1 -> {

                        Column (
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ){

                            BoxWithConstraints(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                val maxHeight = constraints.maxHeight
                                val maxWidth = constraints.maxWidth

                                Image(
                                    painter = painterResource(
                                        id = LocalContext.current.resources.getIdentifier(
                                            "lure" + selectedBait.value.toString(),
                                            "drawable",
                                            LocalContext.current.packageName)
                                        ),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(
                                            minOf(maxWidth, maxHeight).dp * 0.5f
                                        )
                                        .aspectRatio(1f), // Mantieni l'aspect ratio dell'immagine
                                    contentScale = ContentScale.Fit
                                )
                            }

                            Column(modifier = Modifier
                                .weight(2f)
                                .background(Color(61, 61, 61, 255)),
                                horizontalAlignment = Alignment.CenterHorizontally){
                                Text(
                                    text = stringResource(
                                        id = LocalContext.current.resources.getIdentifier(
                                            "lure" + selectedBait.value.toString()+"_name",
                                            "string",
                                            LocalContext.current.packageName)),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = TextUnit(30F, TextUnitType.Sp),
                                    modifier = Modifier.padding(5.dp),
                                    color = Color.White
                                )
                                Text(text = lureDescriptions[selectedBait.value.toInt()-1],
                                    textAlign = TextAlign.Center,
                                    fontSize = TextUnit(20F, TextUnitType.Sp),
                                    modifier = Modifier.padding(horizontal = 5.dp),
                                    color = Color.White
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {

                                ElevatedButton(
                                    onClick = {
                                        gridState.value = 0
                                        buttonMediaPlayer.start()
                                    },
                                    modifier = Modifier.width(100.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(196, 27, 21, 255),
                                        contentColor = Color.White
                                    ),
                                    border = BorderStroke(2.dp, Color(82, 44, 52, 255))
                                ) {
                                    Text("BACK", fontWeight = FontWeight.Bold)
                                }

                                ElevatedButton(
                                    onClick = {
                                        //selectedBait = bait
                                        FishPreferences.saveLure(context, selectedBait.value)
                                        gridState.value = 0
                                        buttonMediaPlayer.start()
                                        closeBaitGrid()
                                        changeLureColor(selectedBait.value.toInt())

                                    },
                                    modifier = Modifier.width(100.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(
                                            26,
                                            214,
                                            4,
                                            255
                                        ),
                                        contentColor = Color.White
                                    ),
                                    border = BorderStroke(2.dp, Color(34, 100, 9, 255))
                                ) {
                                    Text("EQUIP", fontWeight = FontWeight.Bold)
                                }
                            }

                        }
                }
            }

        }
    }

    //Image(painter = painterResource(id = R.drawable.test), contentDescription = null,)
}