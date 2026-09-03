package com.example.mondrain.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mondrain.data.DrainageCrossingEntity
import com.example.mondrain.hydraulic.CulvertEngine
import com.example.mondrain.hydraulic.CulvertHydraulicAnalysis
import com.example.mondrain.hydraulic.CulvertType
import com.example.mondrain.ui.MonDrainViewModel
import com.example.mondrain.util.AppLanguage
import com.example.mondrain.util.MonStrings
import com.example.ui.theme.*
import kotlin.math.*

@Composable
fun CulvertScreen(
    viewModel: MonDrainViewModel,
    modifier: Modifier = Modifier
) {
    val language by viewModel.language.collectAsState()
    val isMn = language == AppLanguage.MONGOLIAN

    val crossings by viewModel.crossings.collectAsState()
    val calcDischarge by viewModel.calcDischarge.collectAsState()
    val calcSlope by viewModel.calcSlope.collectAsState()
    val calcType by viewModel.calcType.collectAsState()
    val calcSpan by viewModel.calcSpan.collectAsState()
    val calcHeight by viewModel.calcHeight.collectAsState()
    val calcBarrels by viewModel.calcBarrels.collectAsState()
    val calcAnalysis by viewModel.calcAnalysis.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Schedule, 1: Sandbox Sizer

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Tab Selector Row (Geometric Segmented Pill)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(GeometricPill)
                .padding(4.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (activeTab == 0) PrimaryNavy else Color.Transparent,
                modifier = Modifier
                    .weight(1f)
                    .clickable { activeTab = 0 }
            ) {
                Text(
                    text = if (isMn) "Төслийн хоолойн хуваарь" else "Culvert Schedule",
                    color = if (activeTab == 0) Color.White else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (activeTab == 1) PrimaryNavy else Color.Transparent,
                modifier = Modifier
                    .weight(1f)
                    .clickable { activeTab = 1 }
            ) {
                Text(
                    text = if (isMn) "Гидравлик тооцооны загвар" else "Hydraulic Sizer Sandbox",
                    color = if (activeTab == 1) Color.White else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (activeTab == 0) {
            // Culvert Schedule Table
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isMn) "Нийт ${crossings.size} ус зайлуулах хоолой" else "${crossings.size} Culvert Structures",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Button(
                    onClick = { viewModel.autoSizeAllCulverts() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy, contentColor = Color.White),
                    modifier = Modifier.testTag("auto_size_all_button")
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isMn) "Автоматаар оновчлох" else "Auto-Size All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(crossings, key = { it.id }) { crossing ->
                    CulvertScheduleCard(crossing = crossing, isMn = isMn)
                }
            }
        } else {
            // Interactive Hydraulic Sizer Sandbox with Live Cross-Section Canvas
            CulvertSandbox(
                analysis = calcAnalysis,
                q = calcDischarge,
                slope = calcSlope,
                type = calcType,
                span = calcSpan,
                height = calcHeight,
                barrels = calcBarrels,
                isMn = isMn,
                onUpdate = { nQ, nS, nType, nSpan, nH, nB ->
                    viewModel.updateSandboxParams(nQ, nS, nType, nSpan, nH, nB)
                }
            )
        }
    }
}

@Composable
private fun CulvertScheduleCard(
    crossing: DrainageCrossingEntity,
    isMn: Boolean
) {
    val isAdequate = crossing.isAdequate
    val statusColor = if (isAdequate) SuccessGreen else DangerRed

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, GeometricBorder),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().testTag("culvert_item_${crossing.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = crossing.stationLabel,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (crossing.culvertType.contains("BOX")) "Тэгш өнцөгт" else "Дугуй",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isAdequate) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ) {
                    Text(
                        text = if (isAdequate) (if (isMn) "ХАНГАСАН" else "PASS") else (if (isMn) "ХҮРЭЛЦЭХГҮЙ" else "FAIL"),
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Specs Row
            val typeStr = if (crossing.culvertType.contains("BOX")) {
                "${crossing.barrels} × (${crossing.culvertSpanOrDiameterM} × ${crossing.culvertHeightM} м)"
            } else {
                "${crossing.barrels} × Ø${crossing.culvertSpanOrDiameterM} м"
            }

            Text(
                text = "Байгууламж: $typeStr",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryNavy
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Hydraulic Parameters Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HydroStatBox("Q тооц", "${String.format("%.2f", crossing.designDischargeM3s)} м³/с", modifier = Modifier.weight(1f))
                HydroStatBox("Q чадал", "${String.format("%.2f", crossing.capacityDischargeM3s)} м³/с", modifier = Modifier.weight(1f))
                HydroStatBox("HW / H", "${String.format("%.2f", crossing.headwaterRatio)}", modifier = Modifier.weight(1f))
                HydroStatBox("Хурд V", "${String.format("%.2f", crossing.flowVelocityMs)} м/с", modifier = Modifier.weight(1f))
            }

            if (crossing.scourProtectionRequired) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFFBEB))
                        .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isMn) "Угаагдлаас хамгаалах чулуун бэхэлгээ (Rip-rap apron) хийх" else "Stone rip-rap scour protection required",
                        fontSize = 11.sp,
                        color = Color(0xFF92400E),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun HydroStatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CanvasNeutral)
            .border(1.dp, GeometricBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Text(text = label.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
    }
}

@Composable
private fun CulvertSandbox(
    analysis: CulvertHydraulicAnalysis?,
    q: Double,
    slope: Double,
    type: CulvertType,
    span: Double,
    height: Double,
    barrels: Int,
    isMn: Boolean,
    onUpdate: (Double, Double, CulvertType, Double, Double, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Live Culvert Cross-Section Diagram
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = if (isMn) "Хөндлөн огтлолын диаграмм (Culvert Cross-Section & HW)" else "Culvert Cross-Section & Headwater",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .padding(8.dp)
                ) {
                    val w = size.width
                    val h = size.height

                    // 1. Road embankment slope line
                    val roadY = 25f
                    drawLine(color = Color(0xFFFFB703), start = Offset(w * 0.25f, roadY), end = Offset(w * 0.75f, roadY), strokeWidth = 5f)
                    drawLine(color = Color(0xFF475569), start = Offset(0f, h - 20f), end = Offset(w * 0.25f, roadY), strokeWidth = 3f)
                    drawLine(color = Color(0xFF475569), start = Offset(w * 0.75f, roadY), end = Offset(w, h - 20f), strokeWidth = 3f)

                    // 2. Culvert Barrels
                    val culvertBottomY = h - 30f
                    val barrelHeightPx = (h * 0.5f).coerceIn(40f, 75f)
                    val barrelWidthPx = (barrelHeightPx * (span / max(0.5, height))).toFloat().coerceIn(35f, 90f)

                    val totalWidth = barrels * barrelWidthPx + (barrels - 1) * 12f
                    val startX = (w - totalWidth) / 2f

                    val hwRatio = analysis?.headwaterRatio ?: 0.8
                    val waterY = (culvertBottomY - barrelHeightPx * min(1.3, hwRatio)).toFloat()

                    // Draw each barrel
                    for (b in 0 until barrels) {
                        val bx = startX + b * (barrelWidthPx + 12f)
                        val culvertTopY = culvertBottomY - barrelHeightPx

                        if (type == CulvertType.PIPE) {
                            // Circular Culvert
                            val radius = barrelWidthPx / 2f
                            val center = Offset(bx + radius, culvertBottomY - radius)

                            // Water fill inside pipe
                            drawCircle(color = Color(0xFF0284C7).copy(alpha = 0.45f), radius = radius, center = center)
                            // Concrete Pipe Wall
                            drawCircle(color = Color(0xFF94A3B8), radius = radius, center = center, style = Stroke(width = 6f))
                        } else {
                            // Box Culvert
                            val boxRect = Size(barrelWidthPx, barrelHeightPx)
                            // Water fill
                            drawRect(color = Color(0xFF0284C7).copy(alpha = 0.45f), topLeft = Offset(bx, culvertTopY), size = boxRect)
                            // Concrete Box Wall
                            drawRect(color = Color(0xFF94A3B8), topLeft = Offset(bx, culvertTopY), size = boxRect, style = Stroke(width = 6f))
                        }
                    }

                    // 3. Water surface line at Headwater (HW)
                    drawLine(
                        color = Color(0xFF00E5FF),
                        start = Offset(startX - 25f, waterY),
                        end = Offset(startX + totalWidth + 25f, waterY),
                        strokeWidth = 3f
                    )
                }

                if (analysis != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("HW = ${String.format("%.2f", analysis.headwaterM)} м (HW/D: ${String.format("%.2f", analysis.headwaterRatio)})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrightCyan)
                        Text(analysis.flowControl, fontSize = 11.sp, color = SlateGray)
                        Text(
                            text = if (analysis.isAdequate) (if (isMn) "ХАНГАСАН" else "ADEQUATE") else (if (isMn) "ХҮРЭЛЦЭХГҮЙ" else "INADEQUATE"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (analysis.isAdequate) SuccessGreen else DangerRed
                        )
                    }
                }
            }
        }

        // Sandbox Parameter Adjustments
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (isMn) "Хэмжээ ба гидравлик параметрүүд:" else "Culvert Sizing Parameters:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                // Culvert Type Switcher
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(CulvertType.PIPE to "Дугуй (Pipe)", CulvertType.BOX to "Тэгш өнцөгт (Box)").forEach { (t, label) ->
                        FilterChip(
                            selected = type == t,
                            onClick = { onUpdate(q, slope, t, if (t == CulvertType.PIPE) 1.25 else 2.0, if (t == CulvertType.PIPE) 1.25 else 2.0, barrels) },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Barrels (1, 2, 3)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isMn) "Нүхний тоо:" else "Barrels:", fontSize = 12.sp, modifier = Modifier.width(90.dp))
                    listOf(1 to "Дан (1)", 2 to "Хос (2)", 3 to "Гурвалсан (3)").forEach { (b, bLabel) ->
                        FilterChip(
                            selected = barrels == b,
                            onClick = { onUpdate(q, slope, type, span, height, b) },
                            label = { Text(bLabel, fontSize = 11.sp) },
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }

                // Diameter / Span selector
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (type == CulvertType.PIPE) "Голч Ø (м):" else "Өргөн B (м):", fontSize = 12.sp, modifier = Modifier.width(90.dp))
                    val sizes = if (type == CulvertType.PIPE) listOf(0.75, 1.0, 1.25, 1.5, 2.0) else listOf(1.5, 2.0, 2.5, 3.0, 4.0)
                    sizes.forEach { s ->
                        FilterChip(
                            selected = span == s,
                            onClick = { onUpdate(q, slope, type, s, if (type == CulvertType.PIPE) s else height, barrels) },
                            label = { Text("${s}м", fontSize = 11.sp) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }

                // Discharge Q slider
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (isMn) "Тооцоот урсац Q_p:" else "Design Discharge Q_p:", fontSize = 12.sp)
                        Text("${String.format("%.1f", q)} м³/с", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrightCyan)
                    }
                    Slider(
                        value = q.toFloat(),
                        onValueChange = { onUpdate(it.toDouble(), slope, type, span, height, barrels) },
                        valueRange = 0.5f..20.0f
                    )
                }

                // Slope S slider
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (isMn) "Хоолойн налуу i:" else "Culvert Slope S:", fontSize = 12.sp)
                        Text("${String.format("%.1f", slope)} %", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = slope.toFloat(),
                        onValueChange = { onUpdate(q, it.toDouble(), type, span, height, barrels) },
                        valueRange = 0.5f..5.0f
                    )
                }
            }
        }
    }
}
