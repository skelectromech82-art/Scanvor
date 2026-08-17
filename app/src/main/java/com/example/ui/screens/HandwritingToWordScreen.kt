package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ui.theme.*
import com.example.utils.DocxGenerator
import com.example.utils.HandwritingEngine
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandwritingToWordScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var documentTitle by remember { mutableStateOf("Handwritten_Notes") }
    var transcribedText by remember {
        mutableStateOf(
            """# Meeting Minutes & Action Items
Date: ${java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())}

## Discussion Summary
- Reviewed quarterly project deliverables and timelines.
- Finalized architectural specifications for the client deployment.
- Assigned core testing workflows to QA leads.

## Action Items
1. Prepare preliminary engineering sign-off by Friday.
2. Coordinate with procurement for equipment delivery.
3. Schedule follow-up sync for next Monday at 10:00 AM."""
        )
    }

    var isProcessing by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) } // 0: Editor, 1: Original Image, 2: Word Preview

    // Gallery Picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bmp = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bmp != null) {
                    selectedBitmap = bmp
                    isProcessing = true
                    coroutineScope.launch {
                        try {
                            val text = HandwritingEngine.convertHandwritingToText(bmp, context)
                            transcribedText = text
                        } catch (e: Exception) {
                            Toast.makeText(context, "Recognition error: ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isProcessing = false
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Handwriting to Word",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Convert handwritten notes into editable .docx files",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0F172A)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { galleryLauncher.launch("image/*") }
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Pick Image",
                            tint = GscanPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GscanBackgroundLight
                )
            )
        },
        containerColor = GscanBackgroundLight
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Document Title Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = GscanPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    OutlinedTextField(
                        value = documentTitle,
                        onValueChange = { documentTitle = it },
                        label = { Text("Word Document Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GscanPrimary,
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation Tabs (Editor | Original Image | Preview)
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color(0xFFF1F5F9),
                contentColor = GscanPrimary,
                indicator = {},
                divider = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .padding(3.dp)
            ) {
                listOf("Text Editor", "Original Note", "Word Preview").forEachIndexed { index, title ->
                    val isSelected = activeTab == index
                    Tab(
                        selected = isSelected,
                        onClick = { activeTab = index },
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color.White else Color.Transparent)
                            .padding(vertical = 10.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) GscanPrimary else Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Content Area based on Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isProcessing) {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = GscanPrimary, strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Transcribing Handwriting...",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Isolating cursive text, letters, and structure",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                } else {
                    when (activeTab) {
                        0 -> {
                            // Text Editor Card with formatting toolbar
                            Card(
                                modifier = Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Formatting Quick Bar
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFF8FAFC))
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = { transcribedText = "# Heading\n$transcribedText" },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("H1", fontWeight = FontWeight.Bold, color = GscanPrimary)
                                        }
                                        TextButton(
                                            onClick = { transcribedText = "## Subheading\n$transcribedText" },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("H2", fontWeight = FontWeight.Bold, color = GscanPrimary)
                                        }
                                        TextButton(
                                            onClick = { transcribedText += "\n- Bullet item" },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("• List", fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                        }
                                        TextButton(
                                            onClick = { transcribedText += "\n1. Step item" },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("1. Num", fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = "${transcribedText.split(Regex("\\s+")).filter { it.isNotBlank() }.size} words",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }

                                    HorizontalDivider(color = Color(0xFFF1F5F9))

                                    OutlinedTextField(
                                        value = transcribedText,
                                        onValueChange = { transcribedText = it },
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent
                                        ),
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 15.sp,
                                            lineHeight = 22.sp,
                                            color = Color(0xFF1E293B)
                                        )
                                    )
                                }
                            }
                        }

                        1 -> {
                            // Original Image view
                            Card(
                                modifier = Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                if (selectedBitmap != null) {
                                    Image(
                                        bitmap = selectedBitmap!!.asImageBitmap(),
                                        contentDescription = "Original Handwritten Note",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(8.dp)
                                    )
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EditNote,
                                            contentDescription = null,
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "No image selected yet",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF475569)
                                        )
                                        Text(
                                            text = "Pick a photo of handwritten notes or blackboard",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = { galleryLauncher.launch("image/*") },
                                            colors = ButtonDefaults.buttonColors(containerColor = GscanPrimary),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Choose Handwriting Photo")
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            // Formatted Document Preview
                            Card(
                                modifier = Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(20.dp)
                                ) {
                                    Text(
                                        text = documentTitle,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = GscanPrimary
                                    )
                                    Text(
                                        text = "Microsoft Word Document Format (.docx)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF64748B)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(color = Color(0xFFE2E8F0))
                                    Spacer(modifier = Modifier.height(16.dp))

                                    transcribedText.split("\n").forEach { line ->
                                        val trimmed = line.trim()
                                        if (trimmed.startsWith("# ")) {
                                            Text(
                                                text = trimmed.removePrefix("# "),
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A),
                                                modifier = Modifier.padding(vertical = 6.dp)
                                            )
                                        } else if (trimmed.startsWith("## ")) {
                                            Text(
                                                text = trimmed.removePrefix("## "),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF1E293B),
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        } else if (trimmed.startsWith("- ") || trimmed.startsWith("• ")) {
                                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                                Text("• ", color = GscanPrimary, fontWeight = FontWeight.Bold)
                                                Text(trimmed.substring(2), style = MaterialTheme.typography.bodyMedium, color = Color(0xFF334155))
                                            }
                                        } else if (trimmed.isNotBlank()) {
                                            Text(
                                                text = line,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFF334155),
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons (Export as Word .docx | Copy | Share)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Handwritten Notes", transcribedText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied text to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF334155)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Text")
                }

                Button(
                    onClick = {
                        try {
                            val outputDir = File(context.filesDir, "documents").apply { mkdirs() }
                            val safeName = documentTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                            val docxFile = File(outputDir, "${safeName}.docx")
                            DocxGenerator.createDocxFile(docxFile, documentTitle, transcribedText)

                            // Share DOCX
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", docxFile)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Open / Share Word (.docx)"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.weight(1.4f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GscanPrimary)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export Word (.docx)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
