package com.example.snowflakebg

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.snowflakebg.ui.theme.SnowFlakeBgTheme
import kotlinx.coroutines.delay
import kotlin.random.Random

// Data classes
data class Snowflake(
    var x: Float,
    var y: Float,
    val size: Float,
    val speed: Float
)

class TiltData {
    var tiltX by mutableFloatStateOf(0f)
    var tiltY by mutableFloatStateOf(0f)
}

// Composable to remember and calculate tilt data
@Composable
fun rememberTiltData(): TiltData {
    val context = LocalContext.current
    val tiltData = remember { TiltData() }
    val sensorManager =
        remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }

    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.values?.let { values ->
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, values)

                    val orientationAngles = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)

                    tiltData.tiltX = Math.toDegrees(orientationAngles[2].toDouble()).toFloat() / 90f
                    tiltData.tiltY = Math.toDegrees(orientationAngles[1].toDouble()).toFloat() / 90f
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    return tiltData
}

// Snowfall animation
@Composable
fun SnowfallBackground(
    modifier: Modifier = Modifier,
    count: Int = 10,
    tiltData: TiltData,
    containerColor: Color,
    snowflakeColor: Color
) {
    var snowflakes by remember { mutableStateOf<List<Snowflake>>(emptyList()) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    LaunchedEffect(Unit) {
        while (true) {
            snowflakes = snowflakes.map { snowflake ->
                val newX = snowflake.x + snowflake.speed * tiltData.tiltX
                val newY = snowflake.y + snowflake.speed

                Snowflake(
                    x = if (newX > canvasSize.width || newX < 0) Random.nextFloat() * canvasSize.width else newX,
                    y = if (newY > canvasSize.height) 0f else newY,
                    size = snowflake.size,
                    speed = snowflake.speed
                )
            }
            delay(30L)
        }
    }

    Canvas(
        modifier = modifier
    ) {
        if (canvasSize != size) {
            canvasSize = Size(size.width, size.height)
            snowflakes = List(count) {
                Snowflake(
                    x = Random.nextFloat() * canvasSize.width,
                    y = 0f,
                    size = Random.nextFloat() * 10 + 5,
                    speed = Random.nextFloat() + 1f
                )
            }
        }

        snowflakes.forEach { snowflake ->
            translate(snowflake.x, snowflake.y) {
                drawPath(
                    path = Path().apply {
                        moveTo(0f, -10f)
                        lineTo(0f, 10f)
                        moveTo(-10f, 0f)
                        lineTo(10f, 0f)
                        moveTo(-7f, -7f)
                        lineTo(7f, 7f)
                        moveTo(7f, -7f)
                        lineTo(-7f, 7f)
                    },
                    color = snowflakeColor,
                    style = Stroke(width = 2f)
                )
            }
        }

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    containerColor.copy(alpha = 0.7f),
                    Color.Transparent,
                    Color.Transparent,
                    containerColor.copy(alpha = 0.7f)
                ),
                startY = 0f,
                endY = size.height
            ),
            size = size
        )
    }
}

// Main Activity
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SnowFlakeBgTheme {
                val tiltData = rememberTiltData()
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "@",
                                    style = MaterialTheme.typography.displaySmall,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        SnowfallBackground(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            tiltData = tiltData,
                            containerColor = MaterialTheme.colorScheme.surface,
                            snowflakeColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
