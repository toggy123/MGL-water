package com.example.mondrain.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mondrain.data.DrainageCrossingEntity
import com.example.mondrain.ui.MonDrainViewModel
import com.example.mondrain.util.AppLanguage
import com.example.ui.theme.*

@Composable
fun HydrologyScreen(
    viewModel: MonDrainViewModel,
    onNavigateToMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val language by viewModel.language.collectAsState()
    val isMn = language == AppLanguage.MONGOLIAN

    val crossings by viewModel.crossings.collectAsState()
    val catchments by viewModel.catchments.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()

    var selectedReturnPeriod by remember { mutableStateOf(2.0) }
    var selectedCrossingForEdit by remember { mutableStateOf<DrainageCrossingEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Top Banner & Recalculate Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isMn) "Гидрологийн тооцоо" else "Hydrological Analysis",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (isMn) "БНбД 2.01.14-83 & Рационал арга (D8 Flow Routing)" else "BNbD 2.01.14-83 & Rational Method (D8)",
                    fontSize = 12.sp,
                    color = SlateGray
                )
            }

            Button(
                onClick = { viewModel.runDelineationAndHydrology() },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy, contentColor = Color.White),
                modifier = Modifier.testTag("recalculate_hydrology_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isMn) "Дахин тооцоолох" else "Recalculate", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Return Period & Norm Selection Bar
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SurfaceCard,
            border = BorderStroke(1.dp, GeometricBorder),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = if (isMn) "Тооцоот үерийн хангагдлын магадлал (Return Period P%):" else "Design Flood Frequency (P%):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        1.0 to "P = 1% (100-жил)",
                        2.0 to "P = 2% (50-жил)",
                        3.0 to "P = 3% (33-жил)",
                        5.0 to "P = 5% (20-жил)"
                    ).forEach { (p, label) ->
                        val isSelected = selectedReturnPeriod == p
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedReturnPeriod = p
                                viewModel.runDelineationAndHydrology()
                            },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (isMn) "Авто зам огтлох ус хурах талбайнууд (${crossings.size})" else "Catchment Basins & Road Crossings (${crossings.size})",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Catchment Basins List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(crossings, key = { it.id }) { crossing ->
                CatchmentCard(
                    crossing = crossing,
                    isMn = isMn,
                    onEdit = { selectedCrossingForEdit = crossing },
                    onViewOnMap = {
                        viewModel.selectCrossing(crossing)
                        onNavigateToMap()
                    }
                )
            }
        }
    }

    // Edit Parameters Dialog
    if (selectedCrossingForEdit != null) {
        val c = selectedCrossingForEdit!!
        var runoffC by remember { mutableStateOf(c.runoffCoeff.toString()) }
        var areaKm2 by remember { mutableStateOf(c.catchmentAreaKm2.toString()) }

        AlertDialog(
            onDismissRequest = { selectedCrossingForEdit = null },
            title = { Text("${c.stationLabel} - ${if (isMn) "Гидрологи өөрчлөх" else "Edit Hydrology"}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = areaKm2,
                        onValueChange = { areaKm2 = it },
                        label = { Text(if (isMn) "Талбай F (км²)" else "Catchment Area (km²)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = runoffC,
                        onValueChange = { runoffC = it },
                        label = { Text(if (isMn) "Урсацын коэффициент C" else "Runoff Coeff C (0.2-0.8)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val a = areaKm2.toDoubleOrNull() ?: c.catchmentAreaKm2
                    val rc = runoffC.toDoubleOrNull() ?: c.runoffCoeff
                    val newQ = 0.278 * rc * 45.0 * a
                    viewModel.updateCrossing(
                        c.copy(
                            catchmentAreaKm2 = a,
                            runoffCoeff = rc,
                            designDischargeM3s = newQ
                        )
                    )
                    selectedCrossingForEdit = null
                }) {
                    Text(if (isMn) "Хадгалах" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedCrossingForEdit = null }) {
                    Text(if (isMn) "Цуцлах" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun CatchmentCard(
    crossing: DrainageCrossingEntity,
    isMn: Boolean,
    onEdit: () -> Unit,
    onViewOnMap: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, GeometricBorder),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
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
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(PrimaryNavy)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = crossing.stationLabel,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryGeometricContainer
                ) {
                    Text(
                        text = "Q_p = ${String.format("%.2f", crossing.designDischargeM3s)} м³/с",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnPrimaryGeometricContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Hydro Metrics Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricColumn(if (isMn) "Талбай F" else "Area F", "${String.format("%.2f", crossing.catchmentAreaKm2)} км²", modifier = Modifier.weight(1f))
                MetricColumn(if (isMn) "Урт L" else "Length L", "${String.format("%.2f", crossing.streamLengthKm)} км", modifier = Modifier.weight(1f))
                MetricColumn(if (isMn) "Налуу J" else "Slope J", "${String.format("%.1f", crossing.slopePercent)}%", modifier = Modifier.weight(1f))
                MetricColumn(if (isMn) "Коэфф. C" else "Runoff C", "${String.format("%.2f", crossing.runoffCoeff)}", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextSecondary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isMn) "Үзүүлэлт засах" else "Edit Params", fontSize = 11.sp, color = TextSecondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onViewOnMap,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isMn) "Газрын зураг" else "Map", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CanvasNeutral)
            .border(1.dp, GeometricBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Text(text = label.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
    }
}
