package com.example.mondrain.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mondrain.dem.DemMode
import com.example.mondrain.ui.MonDrainViewModel
import com.example.mondrain.util.AppLanguage
import com.example.mondrain.util.MonStrings
import com.example.ui.theme.*

@Composable
fun RoadDemScreen(
    viewModel: MonDrainViewModel,
    modifier: Modifier = Modifier
) {
    val language by viewModel.language.collectAsState()
    val isMn = language == AppLanguage.MONGOLIAN
    val context = LocalContext.current

    val alignment by viewModel.roadAlignment.collectAsState()
    val demMode by viewModel.demMode.collectAsState()
    val demMetadata by viewModel.demMetadata.collectAsState()
    val elevationGrid by viewModel.elevationGrid.collectAsState()
    val isDemLoading by viewModel.isDemLoading.collectAsState()
    val demStatusMessage by viewModel.demStatusMessage.collectAsState()

    // File Pickers
    val kmlPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val fileName = uri.lastPathSegment ?: "alignment.kml"
                viewModel.loadRoadAlignmentFromStream(stream, fileName)
            }
        }
    }

    val hgtPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val fileName = uri.lastPathSegment ?: "N48E106.hgt"
                viewModel.importOfflineHgt(stream, fileName)
            }
        }
    }

    val tiffPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                viewModel.importOfflineGeoTiff(stream)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Road Alignment Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SurfaceCard,
            border = BorderStroke(1.dp, GeometricBorder),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AltRoute, contentDescription = null, tint = PrimaryNavy)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isMn) "Авто замын трасс ба пикетчлэл" else "Road Alignment & Stationing",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Button(
                        onClick = { kmlPickerLauncher.launch("*/*") },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy, contentColor = Color.White),
                        modifier = Modifier.testTag("import_kml_button")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isMn) "KML / KMZ Оруулах" else "Import KML/KMZ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (alignment != null) {
                    val al = alignment!!
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatBox(label = if (isMn) "Нийт урт" else "Total Length", value = "${String.format("%.2f", al.totalLengthMeters / 1000.0)} км", modifier = Modifier.weight(1f))
                        StatBox(label = if (isMn) "Пикет тоо" else "Stations", value = "${al.stations.size}", modifier = Modifier.weight(1f))
                        StatBox(label = if (isMn) "Цэгүүд" else "Vertices", value = "${al.points.size}", modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Эхлэх пикет: ${al.stations.firstOrNull()?.stationLabel ?: "ПК 0+00"} | Төгсөх пикет: ${al.stations.lastOrNull()?.stationLabel ?: "ПК 0+00"}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                } else {
                    Text(
                        text = if (isMn) "Трасс сонгогдоогүй байна. KML эсвэл KMZ файл оруулна уу." else "No road alignment loaded. Please import KML or KMZ.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Section 2: DEM & Terrain System Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SurfaceCard,
            border = BorderStroke(1.dp, GeometricBorder),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Terrain, contentDescription = null, tint = PrimaryNavy)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isMn) "Дижитал өндрийн загвар (DEM)" else "Digital Elevation Model (DEM)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    // Prominent DEM Mode Switcher
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(GeometricPill)
                            .padding(2.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (demMode == DemMode.ONLINE) PrimaryNavy else Color.Transparent,
                            modifier = Modifier
                                .testTag("dem_mode_online_button")
                                .clickable { viewModel.setDemMode(DemMode.ONLINE) }
                        ) {
                            Text(
                                text = "ONLINE",
                                color = if (demMode == DemMode.ONLINE) Color.White else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (demMode == DemMode.OFFLINE) PrimaryNavy else Color.Transparent,
                            modifier = Modifier
                                .testTag("dem_mode_offline_button")
                                .clickable { viewModel.setDemMode(DemMode.OFFLINE) }
                        ) {
                            Text(
                                text = "OFFLINE",
                                color = if (demMode == DemMode.OFFLINE) Color.White else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mode-specific Controls
                if (demMode == DemMode.ONLINE) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE0F2FE).copy(alpha = 0.4f))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = if (isMn) "Олон улсын нээлттэй DEM эх сурвалжууд:" else "Public High-Resolution Elevation Sources:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryNavy
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = { viewModel.fetchOnlineDem("NASADEM") },
                                modifier = Modifier.weight(1f).testTag("fetch_nasadem_button")
                            ) {
                                Text("NASADEM 30m", fontSize = 11.sp)
                            }
                            FilledTonalButton(
                                onClick = { viewModel.fetchOnlineDem("SRTM") },
                                modifier = Modifier.weight(1f).testTag("fetch_srtm_button")
                            ) {
                                Text("SRTM Global 30m", fontSize = 11.sp)
                            }
                        }
                    }
                } else {
                    // OFFLINE Mode
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFEF3C7).copy(alpha = 0.4f))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = if (isMn) "Оффлайн гадаргын өгөгдөл (Интернет шаардахгүй):" else "Offline Terrain Files (Zero Internet Required):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF92400E)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { hgtPickerLauncher.launch("*/*") },
                                modifier = Modifier.weight(1f).testTag("import_hgt_button")
                            ) {
                                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SRTM (.hgt)", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { tiffPickerLauncher.launch("*/*") },
                                modifier = Modifier.weight(1f).testTag("import_geotiff_button")
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("GeoTIFF DEM", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Loading or Status message
                if (isDemLoading) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(demStatusMessage ?: "Уншиж байна...", fontSize = 12.sp, color = BrightCyan)
                    }
                } else if (demStatusMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = demStatusMessage!!,
                        fontSize = 11.sp,
                        color = if (demStatusMessage!!.contains("алдаа") || demStatusMessage!!.contains("боломжгүй")) DangerRed else SuccessGreen
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // DEM Metadata Inspection Grid
                if (demMetadata != null) {
                    val meta = demMetadata!!
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MetaRow("Эх сурвалж (Source):", meta.sourceName)
                        MetaRow("Нарийвчлал (Resolution):", meta.resolution)
                        MetaRow("Координатын систем (CRS):", meta.crs)
                        MetaRow("Өндрийн эхлэл (Vertical Datum):", meta.verticalDatum)
                        MetaRow("Өндрийн хэлбэлзэл (Z range):", "${String.format("%.1f", meta.minElevation)} м - ${String.format("%.1f", meta.maxElevation)} м")
                        MetaRow("Кэшийн төлөв (Cache status):", meta.cacheStatus)
                    }
                }
            }
        }

        // Section 3: Longitudinal Elevation Profile Chart along Road
        if (alignment != null && elevationGrid != null) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isMn) "Трассын дагуух өндрийн урт хуваарилалт (Longitudinal Profile)" else "Longitudinal Elevation Profile",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val stations = alignment!!.stations
                    val dem = elevationGrid!!

                    // Draw Profile Canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F172A))
                            .padding(8.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val minZ = dem.minElevation.toFloat()
                        val maxZ = (dem.maxElevation.toFloat() + 10f).coerceAtLeast(minZ + 20f)

                        val profilePath = Path()
                        val totalDist = alignment!!.totalLengthMeters.toFloat().coerceAtLeast(1f)

                        stations.forEachIndexed { index, st ->
                            val z = dem.getElevation(st.point.lat, st.point.lon)?.toFloat() ?: minZ
                            val x = (st.stationMeters.toFloat() / totalDist) * w
                            val y = h - ((z - minZ) / (maxZ - minZ)) * h

                            if (index == 0) profilePath.moveTo(x, y) else profilePath.lineTo(x, y)
                        }

                        // Draw profile stroke
                        drawPath(
                            path = profilePath,
                            color = Color(0xFF00E5FF),
                            style = Stroke(width = 2.5f)
                        )

                        // Baseline grid line
                        drawLine(
                            color = Color(0xFF334155),
                            start = Offset(0f, h - 1f),
                            end = Offset(w, h - 1f),
                            strokeWidth = 1f
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stations.firstOrNull()?.stationLabel ?: "ПК 0+00", fontSize = 10.sp, color = SlateGray)
                        Text("Z_min: ${String.format("%.0f", dem.minElevation)} м", fontSize = 10.sp, color = SlateGray)
                        Text("Z_max: ${String.format("%.0f", dem.maxElevation)} м", fontSize = 10.sp, color = SlateGray)
                        Text(stations.lastOrNull()?.stationLabel ?: "ПК 0+00", fontSize = 10.sp, color = SlateGray)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, GeometricBorder),
        shadowElevation = 1.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryNavy
            )
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, color = SlateGray)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
