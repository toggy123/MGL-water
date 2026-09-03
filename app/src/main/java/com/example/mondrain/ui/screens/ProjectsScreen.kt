package com.example.mondrain.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mondrain.data.ProjectEntity
import com.example.mondrain.ui.MonDrainViewModel
import com.example.mondrain.util.AppLanguage
import com.example.mondrain.util.MonStrings
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ProjectsScreen(
    viewModel: MonDrainViewModel,
    onNavigateToMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val language by viewModel.language.collectAsState()
    val isMn = language == AppLanguage.MONGOLIAN

    val allProjects by viewModel.allProjects.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()

    var showNewProjectDialog by remember { mutableStateOf(false) }
    var projectToRename by remember { mutableStateOf<ProjectEntity?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportedJsonText by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header with Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isMn) "Төслийн сан" else "Project Repository",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (isMn) "Нийт ${allProjects.size} төсөл бүртгэгдсэн" else "${allProjects.size} projects registered",
                    fontSize = 12.sp,
                    color = SlateGray
                )
            }

            Row {
                OutlinedButton(
                    onClick = { showImportDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GeometricBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    modifier = Modifier.testTag("import_project_button")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isMn) "Импорт" else "Import", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { showNewProjectDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy, contentColor = Color.White),
                    modifier = Modifier.testTag("new_project_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isMn) "Шинэ төсөл" else "New Project", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Projects List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(allProjects, key = { it.id }) { project ->
                val isSelected = project.id == selectedProject?.id
                ProjectCard(
                    project = project,
                    isSelected = isSelected,
                    isMn = isMn,
                    onSelect = {
                        viewModel.selectProject(project.id)
                        onNavigateToMap()
                    },
                    onDuplicate = { viewModel.duplicateProject(project.id) },
                    onRename = { projectToRename = project },
                    onDelete = { viewModel.deleteProject(project.id) },
                    onExport = {
                        coroutineScope.launch {
                            exportedJsonText = viewModel.exportProjectJson()
                            showExportDialog = true
                        }
                    }
                )
            }
        }
    }

    // New Project Dialog
    if (showNewProjectDialog) {
        NewProjectDialog(
            isMn = isMn,
            onDismiss = { showNewProjectDialog = false },
            onCreate = { name, num, client, designer, loc, prov, dist, road, sec ->
                viewModel.createProject(name, num, client, designer, loc, prov, dist, road, sec)
                showNewProjectDialog = false
            }
        )
    }

    // Rename Dialog
    if (projectToRename != null) {
        var newName by remember { mutableStateOf(projectToRename!!.projectName) }
        AlertDialog(
            onDismissRequest = { projectToRename = null },
            title = { Text(if (isMn) "Төслийн нэр солих" else "Rename Project") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(if (isMn) "Төслийн нэр" else "Project Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("rename_project_input")
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.renameProject(projectToRename!!.id, newName)
                    projectToRename = null
                }) {
                    Text(if (isMn) "Хадгалах" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToRename = null }) {
                    Text(if (isMn) "Цуцлах" else "Cancel")
                }
            }
        )
    }

    // Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(if (isMn) "Төсөл Экспорт (JSON)" else "Export Project (JSON)") },
            text = {
                Column {
                    Text(
                        if (isMn) "Төслийн бүх тооцоо, трасс ба хоолойн өгөгдлийг JSON хэлбэрээр экспортлов."
                        else "Project data and culvert calculations exported as JSON.",
                        fontSize = 12.sp,
                        color = SlateGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exportedJsonText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    clipboardManager.setText(AnnotatedString(exportedJsonText))
                    Toast.makeText(context, if (isMn) "JSON хуулагдлаа!" else "Copied JSON to clipboard!", Toast.LENGTH_SHORT).show()
                    showExportDialog = false
                }) {
                    Text(if (isMn) "Хуулах" else "Copy")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(if (isMn) "Хаах" else "Close")
                }
            }
        )
    }

    // Import Dialog
    if (showImportDialog) {
        var importText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(if (isMn) "Төсөл Импортлох (JSON)" else "Import Project (JSON)") },
            text = {
                Column {
                    Text(
                        if (isMn) "Өмнө экспортлосон төслийн JSON текстийг энд буулгана уу:"
                        else "Paste exported project JSON text below:",
                        fontSize = 12.sp,
                        color = SlateGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        placeholder = { Text("{\"version\": \"1.0\", ...}") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (importText.isNotBlank()) {
                        coroutineScope.launch {
                            try {
                                viewModel.importProjectJson(importText)
                                Toast.makeText(context, if (isMn) "Төсөл амжилттай импортлогдлоо!" else "Project imported successfully!", Toast.LENGTH_SHORT).show()
                                showImportDialog = false
                            } catch (e: Exception) {
                                Toast.makeText(context, "Алдаа: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }) {
                    Text(if (isMn) "Импортлох" else "Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text(if (isMn) "Цуцлах" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProjectCard(
    project: ProjectEntity,
    isSelected: Boolean,
    isMn: Boolean,
    onSelect: () -> Unit,
    onDuplicate: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit
) {
    Surface(
        shape = if (isSelected) RoundedCornerShape(24.dp) else RoundedCornerShape(20.dp),
        color = if (isSelected) PrimaryGeometricContainer else SurfaceCard,
        shadowElevation = if (isSelected) 2.dp else 1.dp,
        border = if (isSelected) null else BorderStroke(1.dp, GeometricBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("project_card_${project.id}")
            .clickable { onSelect() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                if (isSelected) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = PrimaryNavy,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = if (isMn) "ИДЭВХТЭЙ" else "ACTIVE",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Text(
                    text = "№ ${project.projectNumber}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) OnPrimaryGeometricContainer else TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = project.projectName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) OnPrimaryGeometricContainer else TextPrimary
            )

            Text(
                text = "${String.format("%.1f", project.roadLengthMeters / 1000.0)} км  •  ${project.dateCreated}",
                fontSize = 12.sp,
                color = if (isSelected) OnPrimaryGeometricContainer.copy(alpha = 0.8f) else TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata Chips Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetaChip(
                    icon = Icons.Default.Place,
                    text = "${project.province}, ${project.district}",
                    isSelected = isSelected
                )
                MetaChip(
                    icon = Icons.Default.AltRoute,
                    text = "${String.format("%.1f", project.roadLengthMeters / 1000.0)} км",
                    isSelected = isSelected
                )
                MetaChip(
                    icon = Icons.Default.Person,
                    text = project.designer,
                    isSelected = isSelected
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDuplicate, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Duplicate",
                        tint = if (isSelected) OnPrimaryGeometricContainer else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onRename, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Rename",
                        tint = if (isSelected) OnPrimaryGeometricContainer else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onExport, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Export",
                        tint = PrimaryNavy,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = DangerRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = onSelect,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryNavy,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(if (isMn) "Нээх" else "Open", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MetaChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    isSelected: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color.White.copy(alpha = 0.65f) else CanvasNeutral)
            .border(
                width = 1.dp,
                color = if (isSelected) Color.Transparent else GeometricBorder.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = if (isSelected) OnPrimaryGeometricContainer else TextSecondary
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) OnPrimaryGeometricContainer else TextSecondary
        )
    }
}

@Composable
private fun NewProjectDialog(
    isMn: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("PRJ-${System.currentTimeMillis() % 10000}") }
    var client by remember { mutableStateOf("Зам, тээврийн хөгжлийн яам") }
    var designer by remember { mutableStateOf("Авто замын инженер") }
    var province by remember { mutableStateOf("Төв аймаг") }
    var district by remember { mutableStateOf("Борнуур сум") }
    var roadName by remember { mutableStateOf("Улсын чанартай авто зам") }
    var roadSection by remember { mutableStateOf("ПК 0+00 - ПК 15+00") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isMn) "Шинэ төсөл үүсгэх" else "Create New Project", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (isMn) "Төслийн нэр" else "Project Name") },
                    modifier = Modifier.fillMaxWidth().testTag("new_project_name_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = number,
                        onValueChange = { number = it },
                        label = { Text(if (isMn) "Төслийн №" else "Number") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = designer,
                        onValueChange = { designer = it },
                        label = { Text(if (isMn) "Инженер" else "Designer") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = province,
                        onValueChange = { province = it },
                        label = { Text(if (isMn) "Аймаг" else "Province") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = district,
                        onValueChange = { district = it },
                        label = { Text(if (isMn) "Сум" else "District") },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = roadName,
                    onValueChange = { roadName = it },
                    label = { Text(if (isMn) "Замын нэр" else "Road Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = roadSection,
                    onValueChange = { roadSection = it },
                    label = { Text(if (isMn) "Замын хэсэг" else "Road Section") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, number, client, designer, "Монгол улс", province, district, roadName, roadSection) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_project_button")
            ) {
                Text(if (isMn) "Үүсгэх" else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isMn) "Цуцлах" else "Cancel")
            }
        }
    )
}
