package com.example.mondrain.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mondrain.report.ReportEngine
import com.example.mondrain.ui.MonDrainViewModel
import com.example.mondrain.util.AppLanguage
import com.example.ui.theme.*

@Composable
fun ReportScreen(
    viewModel: MonDrainViewModel,
    modifier: Modifier = Modifier
) {
    val language by viewModel.language.collectAsState()
    val isMn = language == AppLanguage.MONGOLIAN

    val selectedProject by viewModel.selectedProject.collectAsState()
    val crossings by viewModel.crossings.collectAsState()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var reportFormat by remember { mutableIntStateOf(0) } // 0: Text, 1: HTML

    val textReport = remember(selectedProject, crossings, language) {
        if (selectedProject != null) {
            ReportEngine.generateTextReport(selectedProject!!, crossings, language)
        } else ""
    }

    val htmlReport = remember(selectedProject, crossings, language) {
        if (selectedProject != null) {
            ReportEngine.generateHtmlReport(selectedProject!!, crossings, language)
        } else ""
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header & Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isMn) "Инженерийн тайлан" else "Engineering Report",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (isMn) "БНбД 2.01.14-83 & Авто замын норм" else "BNbD 2.01.14-83 & Road Standard Compliance",
                    fontSize = 12.sp,
                    color = SlateGray
                )
            }

            Row {
                OutlinedButton(
                    onClick = {
                        val contentToCopy = if (reportFormat == 0) textReport else htmlReport
                        clipboardManager.setText(AnnotatedString(contentToCopy))
                        Toast.makeText(context, if (isMn) "Тайлан хуулагдлаа!" else "Report copied!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GeometricBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    modifier = Modifier.testTag("copy_report_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isMn) "Хуулах" else "Copy", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, if (reportFormat == 0) textReport else htmlReport)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, if (isMn) "Тайлан илгээх" else "Share Report")
                        context.startActivity(shareIntent)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy, contentColor = Color.White),
                    modifier = Modifier.testTag("share_report_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isMn) "Илгээх" else "Share", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Format Switcher Tab (Geometric Segmented Pill)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(GeometricPill)
                .padding(4.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (reportFormat == 0) PrimaryNavy else Color.Transparent,
                modifier = Modifier
                    .weight(1f)
                    .clickable { reportFormat = 0 }
            ) {
                Text(
                    text = "Текст тайлан / Text Report",
                    color = if (reportFormat == 0) Color.White else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (reportFormat == 1) PrimaryNavy else Color.Transparent,
                modifier = Modifier
                    .weight(1f)
                    .clickable { reportFormat = 1 }
            ) {
                Text(
                    text = "HTML / Хэвлэх эх бэлтгэл",
                    color = if (reportFormat == 1) Color.White else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Report Viewer Canvas / Container
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SurfaceCard,
            border = BorderStroke(1.dp, GeometricBorder),
            shadowElevation = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            SelectionContainer {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = if (reportFormat == 0) textReport else htmlReport,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
