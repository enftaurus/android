package software.royya.watch.features.ponddetails

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import software.royya.watch.R
import software.royya.watch.core.design.*
import software.royya.watch.domain.model.SensorReading

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PondDetailsScreen(
    viewModel: PondDetailsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.pond?.name ?: "Pond Details", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RoyyaBackground)
            )
        },
        containerColor = RoyyaBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Metrics Summary Grid (All parameters: DO, Temp, Turbidity, pH, TDS)
            ParametersOverview(uiState.readings)

            // Time Range Filter Selector (1h, 6h, 24h, 7d)
            TimeRangeFilter(
                selectedRange = uiState.selectedRange,
                onSelectRange = { viewModel.selectRange(it) }
            )

            // Dissolved Oxygen Trend Chart Card
            HistoryChartCard(
                title = "ఆక్సిజన్ ధోరణి (Dissolved Oxygen Trend)",
                subtitle = "సురక్షిత పరిమితి: 5.0 - 6.0 mg/L",
                points = uiState.historyPoints.map { it.dissolvedOxygen ?: 5.0f },
                lineColor = RoyyaPrimary,
                unit = stringResource(R.string.unit_mg_l)
            )

            // Temperature Trend Chart Card
            HistoryChartCard(
                title = "ఉష్ణోగ్రత ధోరణి (Temperature Trend)",
                subtitle = "సాధారణ పరిధి: 26 - 32°C",
                points = uiState.historyPoints.map { it.temperature ?: 28.0f },
                lineColor = Color(0xFFF59E0B),
                unit = stringResource(R.string.unit_celsius)
            )

            // Turbidity Sensor Voltage Trend Chart Card
            HistoryChartCard(
                title = "మడ్డితనం వోల్టేజ్ ధోరణి (Turbidity Voltage Trend)",
                subtitle = "సెన్సార్ వోల్టేజ్ (V) · > 3.5V హెచ్చరిక",
                points = uiState.historyPoints.map { it.turbidity ?: 0.0f },
                lineColor = Color(0xFF2563EB),
                unit = "V"
            )
        }
    }
}

@Composable
private fun ParametersOverview(readings: SensorReading?) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "సెన్సార్ పారామితులు (Sensor Parameters)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ParamMiniCard(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.metric_do),
                value = readings?.dissolvedOxygen?.let { "%.2f".format(it) } ?: "--",
                unit = "mg/L",
                color = RoyyaPrimary
            )
            ParamMiniCard(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.metric_temp),
                value = readings?.temperature?.let { "%.2f".format(it) } ?: "--",
                unit = "°C",
                color = Color(0xFFD97706)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ParamMiniCard(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.metric_turbidity),
                value = readings?.turbidity?.let { "%.2f".format(it) } ?: "--",
                unit = "V",
                color = Color(0xFF2563EB)
            )
            ParamMiniCard(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.metric_ph),
                value = readings?.ph?.let { "%.1f".format(it) } ?: "--",
                unit = "pH",
                color = Color(0xFF7C3AED)
            )
            ParamMiniCard(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.metric_tds),
                value = readings?.tds?.let { "%.0f".format(it) } ?: "--",
                unit = "ppm",
                color = Color(0xFF0D9488)
            )
        }
    }
}

@Composable
private fun ParamMiniCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    unit: String,
    color: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(RoyyaSurface)
            .border(1.dp, RoyyaBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = RoyyaTextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
                Spacer(modifier = Modifier.width(3.dp))
                Text(text = unit, style = MaterialTheme.typography.labelSmall, color = RoyyaTextSecondary)
            }
        }
    }
}

@Composable
private fun TimeRangeFilter(
    selectedRange: String,
    onSelectRange: (String) -> Unit
) {
    val ranges = listOf(
        "1h" to stringResource(R.string.range_1h),
        "6h" to stringResource(R.string.range_6h),
        "24h" to stringResource(R.string.range_24h),
        "7d" to stringResource(R.string.range_7d)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(RoyyaSurface)
            .border(1.dp, RoyyaBorder, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ranges.forEach { (key, label) ->
            val isSelected = selectedRange == key
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) RoyyaPrimary else Color.Transparent)
                    .clickable { onSelectRange(key) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) RoyyaSurface else RoyyaTextSecondary
                )
            }
        }
    }
}

@Composable
private fun HistoryChartCard(
    title: String,
    subtitle: String,
    points: List<Float>,
    lineColor: Color,
    unit: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(RoyyaSurface)
            .border(1.dp, RoyyaBorder, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = RoyyaTextSecondary)
            Spacer(modifier = Modifier.height(16.dp))

            if (points.isNotEmpty()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val min = points.minOrNull() ?: 0f
                    val max = points.maxOrNull() ?: 10f
                    val range = if (max == min) 1f else (max - min)

                    val path = Path()
                    points.forEachIndexed { i, pt ->
                        val x = (i.toFloat() / (points.size - 1).coerceAtLeast(1)) * width
                        val y = height - ((pt - min) / range) * (height * 0.8f) - (height * 0.1f)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }

                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw end dot
                    val lastX = width
                    val lastY = height - ((points.last() - min) / range) * (height * 0.8f) - (height * 0.1f)
                    drawCircle(
                        color = lineColor,
                        radius = 6.dp.toPx(),
                        center = Offset(lastX, lastY)
                    )
                }
            }
        }
    }
}
