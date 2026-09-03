package com.example.mondrain.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mondrain.data.DrainageCrossingEntity
import com.example.mondrain.dem.ElevationGrid
import com.example.mondrain.gis.GeoBounds
import com.example.mondrain.gis.GeoPoint
import com.example.mondrain.gis.GisEngine
import com.example.mondrain.gis.ParsedAlignment
import com.example.mondrain.hydrology.CatchmentBoundary
import com.example.mondrain.hydrology.FlowPath
import com.example.mondrain.ui.MonDrainViewModel
import com.example.mondrain.util.AppLanguage
import com.example.mondrain.util.MonStrings
import com.example.ui.theme.*
import kotlin.math.*

@Composable
fun EngineeringMapScreen(
    viewModel: MonDrainViewModel,
    modifier: Modifier = Modifier
) {
    val language by viewModel.language.collectAsState()
    val isMn = language == AppLanguage.MONGOLIAN

    val selectedProject by viewModel.selectedProject.collectAsState()
    val alignment by viewModel.roadAlignment.collectAsState()
    val demGrid by viewModel.elevationGrid.collectAsState()
    val catchments by viewModel.catchments.collectAsState()
    val flowPaths by viewModel.flowPaths.collectAsState()
    val crossings by viewModel.crossings.collectAsState()
    val selectedCrossing by viewModel.selectedCrossing.collectAsState()
    val cursorCoord by viewModel.mapCursorCoords.collectAsState()

    val showRoad by viewModel.showRoad.collectAsState()
    val showDem by viewModel.showDem.collectAsState()
    val showContours by viewModel.showContours.collectAsState()
    val showCatchments by viewModel.showCatchments.collectAsState()
    val showFlowPaths by viewModel.showFlowPaths.collectAsState()
    val showCrossings by viewModel.showCrossings.collectAsState()

    var showLayersDialog by remember { mutableStateOf(false) }

    // Map Transform State
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val projectBounds = alignment?.bounds ?: demGrid?.bounds ?: GeoBounds(48.2, 48.35, 106.1, 106.2)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasNeutral)
    ) {

        // Canvas Map
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("engineering_gis_canvas")
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(0.5f, 15.0f)
                        panOffset += pan
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        // Calculate geographic coordinates from tap screen offset
                        // and check if a culvert was tapped
                    }
                }
        ) {
            val canvasW = size.width
            val canvasH = size.height

            // Draw subtle geometric dot grid background
            val gridSpacing = 24.dp.toPx()
            var gx = 0f
            while (gx < canvasW) {
                var gy = 0f
                while (gy < canvasH) {
                    drawCircle(
                        color = GeometricBorder.copy(alpha = 0.35f),
                        radius = 1.2f,
                        center = Offset(gx, gy)
                    )
                    gy += gridSpacing
                }
                gx += gridSpacing
            }

            val baseMargin = 40f
            val drawW = canvasW - 2 * baseMargin
            val drawH = canvasH - 2 * baseMargin

            fun geoToCanvas(lat: Double, lon: Double): Offset {
                val relX = ((lon - projectBounds.minLon) / projectBounds.lonSpan).toFloat()
                val relY = ((projectBounds.maxLat - lat) / projectBounds.latSpan).toFloat()

                val screenX = baseMargin + relX * drawW
                val screenY = baseMargin + relY * drawH

                val centerX = canvasW / 2f
                val centerY = canvasH / 2f

                val zoomedX = centerX + (screenX - centerX) * zoomScale + panOffset.x
                val zoomedY = centerY + (screenY - centerY) * zoomScale + panOffset.y

                return Offset(zoomedX, zoomedY)
            }

            // 1. Draw DEM Hillshade & Elevation Tint Raster
            if (showDem && demGrid != null) {
                drawDemElevationLayer(demGrid!!, ::geoToCanvas, zoomScale)
            }

            // 2. Draw Contour lines
            if (showContours && demGrid != null) {
                drawContourLines(demGrid!!, ::geoToCanvas)
            }

            // 3. Draw Catchment Boundaries
            if (showCatchments) {
                drawCatchmentPolygons(catchments, ::geoToCanvas)
            }

            // 4. Draw Drainage Flow Paths (Streams)
            if (showFlowPaths) {
                drawStreamFlowPaths(flowPaths, ::geoToCanvas)
            }

            // 5. Draw Road Alignment & Station Ticks
            if (showRoad && alignment != null) {
                drawRoadAlignment(alignment!!, ::geoToCanvas, zoomScale)
            }

            // 6. Draw Culvert Crossings with Adequacy Status
            if (showCrossings) {
                drawCulvertMarkers(crossings, selectedCrossing, ::geoToCanvas)
            }
        }

        // Top-Left Floating Info Chips (DEM Source, CRS)
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.95f),
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, GeometricBorder)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(
                        text = if (isMn) "DEM ЭХ СУРВАЛЖ" else "DEM SOURCE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = if (demGrid != null) "NASADEM (30m)" else "DEM Empty",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNavy
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.95f),
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, GeometricBorder)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(
                        text = "CRS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = selectedProject?.coordinateSystem ?: "UTM Zone 48N",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }

        // Top-Right Floating Controls (Circular buttons matching Geometric Balance)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 3.dp,
                border = BorderStroke(1.dp, GeometricBorder),
                modifier = Modifier
                    .size(40.dp)
                    .clickable { showLayersDialog = true }
                    .testTag("gis_layers_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Layers,
                        contentDescription = "Layers",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 3.dp,
                border = BorderStroke(1.dp, GeometricBorder),
                modifier = Modifier
                    .size(40.dp)
                    .clickable {
                        zoomScale = 1.0f
                        panOffset = Offset.Zero
                    }
                    .testTag("gis_reset_zoom_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = "Center",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // North Arrow Indicator
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 3.dp,
                border = BorderStroke(1.dp, GeometricBorder),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "North Arrow",
                            tint = DangerRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "N",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // Bottom-Right Coordinate Chip (Monospace pill)
        val activeLat = cursorCoord?.lat ?: projectBounds.centerLat
        val activeLon = cursorCoord?.lon ?: projectBounds.centerLon
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = 0.95f),
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, GeometricBorder),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(14.dp)
        ) {
            Text(
                text = "${String.format("%.3f", activeLat)}° N, ${String.format("%.3f", activeLon)}° E",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // Bottom-Left Overlay: Catchment Legend & Metric Scale Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Catchment Legend Pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.95f),
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, GeometricBorder)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .border(width = 2.dp, color = PrimaryNavy, shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isMn) "Ус хурах талбай (Polygon)" else "Catchment Area (Polygon)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }
            }

            // Metric Scale Bar
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White.copy(alpha = 0.95f),
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, GeometricBorder)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(4.dp)
                            .background(PrimaryNavy)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${String.format("%.1f", (1.0 / zoomScale))} км",
                        color = TextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Selected Crossing Bottom Card in Geometric Balance Style
        if (selectedCrossing != null) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, GeometricBorder),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(if (selectedCrossing!!.isAdequate) SuccessGreen else DangerRed)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${selectedCrossing!!.stationLabel} - ${selectedCrossing!!.culvertType}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        IconButton(onClick = { viewModel.selectCrossing(null) }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Grid of 2 Metric Cards matching HTML
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, GeometricBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (isMn) "ЗАРЦУУЛАЛТ (Q ТОЙЦ)" else "DISCHARGE (Q DESIGN)",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${String.format("%.2f", selectedCrossing!!.designDischargeM3s)} m³/s",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryNavy
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, GeometricBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (isMn) "ТАЛБАЙ (AREA)" else "BASIN AREA",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${String.format("%.2f", selectedCrossing!!.catchmentAreaKm2)} km²",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryNavy
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (selectedCrossing!!.isAdequate)
                            (if (isMn) "Гидравлик шаардлага хангасан (HW/D ≤ 1.2)" else "Hydraulic criteria satisfied (HW/D ≤ 1.2)")
                        else
                            (if (isMn) "Нэвтрүүлэх чадвар хүрэлцэхгүй! Хоолойг томруулах шаардлагатай." else "Capacity exceeded! Resize culvert."),
                        color = if (selectedCrossing!!.isAdequate) SuccessGreen else DangerRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    // Layer Visibility Dialog
    if (showLayersDialog) {
        AlertDialog(
            onDismissRequest = { showLayersDialog = false },
            title = { Text(if (isMn) "Давхаргуудын тохиргоо" else "Map Layer Toggles", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    LayerToggleRow(if (isMn) "Авто замын трасс (Road alignment)" else "Road Alignment", viewModel.showRoad)
                    LayerToggleRow(if (isMn) "Өндрийн загвар DEM (Terrain)" else "DEM Elevation", viewModel.showDem)
                    LayerToggleRow(if (isMn) "Хэвтээ шугамууд (Contours)" else "Contour Lines", viewModel.showContours)
                    LayerToggleRow(if (isMn) "Ус хурах талбай (Catchments)" else "Catchment Basins", viewModel.showCatchments)
                    LayerToggleRow(if (isMn) "Урсацын суваг (Streams)" else "Flow Paths", viewModel.showFlowPaths)
                    LayerToggleRow(if (isMn) "Ус зайлуулах хоолойнууд (Culverts)" else "Culvert Crossings", viewModel.showCrossings)
                    LayerToggleRow(if (isMn) "GPS байршил" else "GPS Location", viewModel.showGps)
                }
            },
            confirmButton = {
                TextButton(onClick = { showLayersDialog = false }) {
                    Text(if (isMn) "Хаах" else "Close")
                }
            }
        )
    }
}

@Composable
private fun LayerToggleRow(label: String, flow: kotlinx.coroutines.flow.MutableStateFlow<Boolean>) {
    val state by flow.collectAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp)
        Switch(
            checked = state,
            onCheckedChange = { flow.value = it }
        )
    }
}

private fun DrawScope.drawDemElevationLayer(
    dem: ElevationGrid,
    geoToCanvas: (Double, Double) -> Offset,
    zoomScale: Float
) {
    val rStep = max(1, dem.rows / 35)
    val cStep = max(1, dem.cols / 35)

    for (r in 0 until dem.rows - rStep step rStep) {
        val latTop = dem.bounds.maxLat - r * (dem.bounds.latSpan / (dem.rows - 1))
        val latBot = dem.bounds.maxLat - (r + rStep) * (dem.bounds.latSpan / (dem.rows - 1))

        for (c in 0 until dem.cols - cStep step cStep) {
            val lonLeft = dem.bounds.minLon + c * (dem.bounds.lonSpan / (dem.cols - 1))
            val lonRight = dem.bounds.minLon + (c + cStep) * (dem.bounds.lonSpan / (dem.cols - 1))

            val elev = dem.data[r][c]
            val norm = if (dem.maxElevation > dem.minElevation) {
                ((elev - dem.minElevation) / (dem.maxElevation - dem.minElevation)).toFloat().coerceIn(0f, 1f)
            } else 0.5f

            // Topo color ramp: Deep valley (dark slate) -> Steppe green -> Ochre -> Mountain ridge (warm sand)
            val cellColor = when {
                norm < 0.25f -> Color(0xFF1E293B)
                norm < 0.50f -> Color(0xFF2E4057)
                norm < 0.75f -> Color(0xFF5C6B73)
                else -> Color(0xFF9DB4C0)
            }.copy(alpha = 0.55f)

            val pTopLeft = geoToCanvas(latTop, lonLeft)
            val pBotRight = geoToCanvas(latBot, lonRight)

            val rectW = abs(pBotRight.x - pTopLeft.x) + 1f
            val rectH = abs(pBotRight.y - pTopLeft.y) + 1f

            drawRect(
                color = cellColor,
                topLeft = Offset(min(pTopLeft.x, pBotRight.x), min(pTopLeft.y, pBotRight.y)),
                size = Size(rectW, rectH)
            )
        }
    }
}

private fun DrawScope.drawContourLines(
    dem: ElevationGrid,
    geoToCanvas: (Double, Double) -> Offset
) {
    // Subtle isohypses contour lines
    val steps = 6
    val minZ = dem.minElevation
    val maxZ = dem.maxElevation
    val dZ = (maxZ - minZ) / (steps + 1)

    for (k in 1..steps) {
        val targetZ = minZ + k * dZ
        // Draw approximate contour arcs
        val contourPath = Path()
        var started = false

        for (c in 0 until dem.cols step 3) {
            val lon = dem.bounds.minLon + c * (dem.bounds.lonSpan / (dem.cols - 1))
            for (r in 0 until dem.rows - 1 step 2) {
                val z1 = dem.data[r][c]
                val z2 = dem.data[r + 1][c]
                if ((z1 <= targetZ && z2 >= targetZ) || (z1 >= targetZ && z2 <= targetZ)) {
                    val lat = dem.bounds.maxLat - (r + 0.5) * (dem.bounds.latSpan / (dem.rows - 1))
                    val pt = geoToCanvas(lat, lon)
                    if (!started) {
                        contourPath.moveTo(pt.x, pt.y)
                        started = true
                    } else {
                        contourPath.lineTo(pt.x, pt.y)
                    }
                    break
                }
            }
        }
        drawPath(
            path = contourPath,
            color = Color(0xFF4A5568).copy(alpha = 0.4f),
            style = Stroke(width = 1.0f)
        )
    }
}

private fun DrawScope.drawCatchmentPolygons(
    catchments: List<CatchmentBoundary>,
    geoToCanvas: (Double, Double) -> Offset
) {
    val colors = listOf(
        Color(0x3300ADB5),
        Color(0x33FFB703),
        Color(0x3306D6A0),
        Color(0x33118AB2),
        Color(0x339D4EDD)
    )
    val borderColors = listOf(
        Color(0xAA00ADB5),
        Color(0xAAFFB703),
        Color(0xAA06D6A0),
        Color(0xAA118AB2),
        Color(0xAA9D4EDD)
    )

    catchments.forEachIndexed { index, cat ->
        if (cat.polygonPoints.size >= 3) {
            val path = Path()
            val first = geoToCanvas(cat.polygonPoints[0].lat, cat.polygonPoints[0].lon)
            path.moveTo(first.x, first.y)

            for (i in 1 until cat.polygonPoints.size) {
                val pt = geoToCanvas(cat.polygonPoints[i].lat, cat.polygonPoints[i].lon)
                path.lineTo(pt.x, pt.y)
            }
            path.close()

            val fillColor = colors[index % colors.size]
            val borderColor = borderColors[index % borderColors.size]

            drawPath(path = path, color = fillColor)
            drawPath(path = path, color = borderColor, style = Stroke(width = 1.5f))
        }
    }
}

private fun DrawScope.drawStreamFlowPaths(
    flowPaths: List<FlowPath>,
    geoToCanvas: (Double, Double) -> Offset
) {
    flowPaths.forEach { fp ->
        if (fp.points.size >= 2) {
            val path = Path()
            val first = geoToCanvas(fp.points[0].lat, fp.points[0].lon)
            path.moveTo(first.x, first.y)

            for (i in 1 until fp.points.size) {
                val pt = geoToCanvas(fp.points[i].lat, fp.points[i].lon)
                path.lineTo(pt.x, pt.y)
            }
            drawPath(
                path = path,
                color = Color(0xFF00E5FF).copy(alpha = 0.75f),
                style = Stroke(width = 2.0f, cap = StrokeCap.Round)
            )
        }
    }
}

private fun DrawScope.drawRoadAlignment(
    alignment: ParsedAlignment,
    geoToCanvas: (Double, Double) -> Offset,
    zoomScale: Float
) {
    if (alignment.points.size < 2) return

    val roadPath = Path()
    val first = geoToCanvas(alignment.points[0].lat, alignment.points[0].lon)
    roadPath.moveTo(first.x, first.y)

    for (i in 1 until alignment.points.size) {
        val pt = geoToCanvas(alignment.points[i].lat, alignment.points[i].lon)
        roadPath.lineTo(pt.x, pt.y)
    }

    // Road Casing (Dark border)
    drawPath(
        path = roadPath,
        color = Color(0xFF0B192C),
        style = Stroke(width = 7.0f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Road Surface (High-contrast gold line)
    drawPath(
        path = roadPath,
        color = Color(0xFFFFC107),
        style = Stroke(width = 4.0f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Station Tick marks every 1 km
    alignment.stations.forEach { st ->
        if (st.stationMeters % 1000.0 < 50.0 || st.stationMeters == 0.0) {
            val pt = geoToCanvas(st.point.lat, st.point.lon)
            drawCircle(
                color = Color.White,
                radius = 3.5f,
                center = pt
            )
        }
    }
}

private fun DrawScope.drawCulvertMarkers(
    crossings: List<DrainageCrossingEntity>,
    selected: DrainageCrossingEntity?,
    geoToCanvas: (Double, Double) -> Offset
) {
    crossings.forEach { c ->
        val pt = geoToCanvas(c.latitude, c.longitude)
        val isSelected = selected?.id == c.id

        val markerColor = when {
            !c.isAdequate -> Color(0xFFEF476F) // Inadequate (Red)
            c.scourProtectionRequired -> Color(0xFFFFB703) // Scour apron needed (Gold)
            else -> Color(0xFF06D6A0) // Adequate (Green)
        }

        // Outer Glow / Ring
        drawCircle(
            color = if (isSelected) Color.White else markerColor.copy(alpha = 0.35f),
            radius = if (isSelected) 14f else 9f,
            center = pt
        )

        // Solid Culvert Node
        drawCircle(
            color = markerColor,
            radius = if (isSelected) 8f else 6f,
            center = pt
        )

        // Center dot
        drawCircle(
            color = Color.Black,
            radius = 2.5f,
            center = pt
        )
    }
}
