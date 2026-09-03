package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mondrain.ui.MonDrainViewModel
import com.example.mondrain.ui.components.TopEngineeringBar
import com.example.mondrain.ui.screens.*
import com.example.mondrain.util.AppLanguage
import com.example.ui.theme.*

enum class MainDestination(val labelMn: String, val labelEn: String, val icon: ImageVector) {
    PROJECTS("Төсөл", "Projects", Icons.Default.Folder),
    ROAD_DEM("Трасс/DEM", "Road/DEM", Icons.Default.Terrain),
    MAP("Зураг", "GIS Map", Icons.Default.Map),
    HYDROLOGY("Гидрологи", "Hydro", Icons.Default.WaterDrop),
    CULVERTS("Хоолой", "Culverts", Icons.Default.Engineering),
    REPORT("Тайлан", "Report", Icons.Default.Description)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MonDrainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val language by viewModel.language.collectAsState()
                val isMn = language == AppLanguage.MONGOLIAN

                var currentDestination by remember { mutableStateOf(MainDestination.MAP) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopEngineeringBar(viewModel = viewModel)
                    },
                    bottomBar = {
                        Surface(
                            color = CanvasNeutral,
                            tonalElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = GeometricBorder,
                                    shape = RoundedCornerShape(0.dp)
                                )
                        ) {
                            NavigationBar(
                                containerColor = CanvasNeutral,
                                contentColor = TextPrimary,
                                tonalElevation = 0.dp,
                                modifier = Modifier.testTag("main_bottom_navigation")
                            ) {
                                MainDestination.values().forEach { dest ->
                                    val isSelected = currentDestination == dest
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = { currentDestination = dest },
                                        icon = {
                                            Icon(
                                                imageVector = dest.icon,
                                                contentDescription = dest.labelEn,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = if (isMn) dest.labelMn else dest.labelEn,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = PrimaryNavy,
                                            selectedTextColor = PrimaryNavy,
                                            indicatorColor = PrimaryGeometricContainer,
                                            unselectedIconColor = TextSecondary.copy(alpha = 0.6f),
                                            unselectedTextColor = TextSecondary.copy(alpha = 0.6f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentDestination) {
                            MainDestination.PROJECTS -> ProjectsScreen(
                                viewModel = viewModel,
                                onNavigateToMap = { currentDestination = MainDestination.MAP }
                            )
                            MainDestination.ROAD_DEM -> RoadDemScreen(viewModel = viewModel)
                            MainDestination.MAP -> EngineeringMapScreen(viewModel = viewModel)
                            MainDestination.HYDROLOGY -> HydrologyScreen(
                                viewModel = viewModel,
                                onNavigateToMap = { currentDestination = MainDestination.MAP }
                            )
                            MainDestination.CULVERTS -> CulvertScreen(viewModel = viewModel)
                            MainDestination.REPORT -> ReportScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
