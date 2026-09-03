package com.example.mondrain.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mondrain.data.ProjectEntity
import com.example.mondrain.dem.DemMode
import com.example.mondrain.ui.MonDrainViewModel
import com.example.mondrain.util.AppLanguage
import com.example.mondrain.util.MonStrings
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopEngineeringBar(
    viewModel: MonDrainViewModel,
    modifier: Modifier = Modifier
) {
    val language by viewModel.language.collectAsState()
    val allProjects by viewModel.allProjects.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()
    val demMode by viewModel.demMode.collectAsState()

    var projectDropdownExpanded by remember { mutableStateOf(false) }

    Surface(
        color = BackgroundLight,
        tonalElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = GeometricBorder.copy(alpha = 0.3f),
                shape = RoundedCornerShape(0.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Row 1: Logo & Title, Language Pill Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Title and Geometric Logo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrimaryNavy),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "M",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "MON-DRAIN",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp
                        )
                        Text(
                            text = "ENGINEER ИНЖЕНЕР",
                            color = PrimaryNavy,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }
                }

                // Geometric Language Switcher Pill (MN / EN)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = GeometricPill,
                    modifier = Modifier
                        .testTag("language_toggle_button")
                        .clickable { viewModel.toggleLanguage() }
                ) {
                    Row(
                        modifier = Modifier.padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // MN Option
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (language == AppLanguage.MONGOLIAN) Color.White else Color.Transparent)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "MN",
                                fontSize = 11.sp,
                                fontWeight = if (language == AppLanguage.MONGOLIAN) FontWeight.Bold else FontWeight.Medium,
                                color = if (language == AppLanguage.MONGOLIAN) TextPrimary else TextSecondary
                            )
                        }

                        // EN Option
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (language == AppLanguage.ENGLISH) Color.White else Color.Transparent)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "EN",
                                fontSize = 11.sp,
                                fontWeight = if (language == AppLanguage.ENGLISH) FontWeight.Bold else FontWeight.Medium,
                                color = if (language == AppLanguage.ENGLISH) TextPrimary else TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Active Project Selector Pill, CRS & DEM Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Project Selector Pill
                Box {
                    Surface(
                        color = PrimaryGeometricContainer,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .testTag("project_selector_button")
                            .clickable { projectDropdownExpanded = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = PrimaryNavy,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedProject?.projectName?.take(18) ?: if (language == AppLanguage.MONGOLIAN) "Төсөл сонгох" else "Select Project",
                                color = OnPrimaryGeometricContainer,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = OnPrimaryGeometricContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = projectDropdownExpanded,
                        onDismissRequest = { projectDropdownExpanded = false }
                    ) {
                        allProjects.forEach { project ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(project.projectName, fontWeight = FontWeight.SemiBold)
                                        Text(project.projectNumber, fontSize = 11.sp, color = TextSecondary)
                                    }
                                },
                                onClick = {
                                    viewModel.selectProject(project.id)
                                    projectDropdownExpanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (project.id == selectedProject?.id) Icons.Default.CheckCircle else Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = if (project.id == selectedProject?.id) PrimaryNavy else TextSecondary
                                    )
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // DEM MODE Selector Pill [ ONLINE ] / [ OFFLINE ]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(GeometricPill)
                        .padding(2.dp)
                ) {
                    Text(
                        text = "DEM:",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, end = 2.dp)
                    )

                    // ONLINE Option
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (demMode == DemMode.ONLINE) PrimaryNavy else Color.Transparent)
                            .clickable { viewModel.setDemMode(DemMode.ONLINE) }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "ONLINE",
                            color = if (demMode == DemMode.ONLINE) Color.White else TextSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // OFFLINE Option
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (demMode == DemMode.OFFLINE) PrimaryNavy else Color.Transparent)
                            .clickable { viewModel.setDemMode(DemMode.OFFLINE) }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "OFFLINE",
                            color = if (demMode == DemMode.OFFLINE) Color.White else TextSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
