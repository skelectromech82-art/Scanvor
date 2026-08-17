package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DocumentCategory
import com.example.ui.components.CategoryChips
import com.example.ui.components.DocumentCard
import com.example.ui.components.EmptyState
import com.example.ui.theme.*
import com.example.ui.viewmodel.DocumentViewModel
import com.example.ui.viewmodel.SortOrder
import com.example.utils.PdfEngine
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    viewModel: DocumentViewModel,
    initialCategory: String? = null,
    onNavigateToPdfEditor: (Long) -> Unit,
    onNavigateToScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showSortMenu by remember { mutableStateOf(false) }
    var showMergeDialog by remember { mutableStateOf(false) }
    var mergeTitle by remember { mutableStateOf("Merged_Document") }

    LaunchedEffect(initialCategory) {
        if (initialCategory != null) {
            val cat = try {
                DocumentCategory.values().firstOrNull { it.displayName.equals(initialCategory, ignoreCase = true) || it.name.equals(initialCategory, ignoreCase = true) }
            } catch (e: Exception) { null }
            if (cat != null) {
                viewModel.setCategory(cat)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (uiState.isMultiSelectMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${uiState.selectedDocIds.size} Selected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                        if (uiState.selectedDocIds.size >= 2) {
                            IconButton(onClick = { showMergeDialog = true }) {
                                Icon(Icons.Default.MergeType, contentDescription = "Merge PDFs")
                            }
                        }
                        IconButton(onClick = { viewModel.deleteSelectedToTrash() }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = "My Documents",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Default.Sort, contentDescription = "Sort Options")
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                Text(
                                    text = "Sort By",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                                SortOrder.values().forEach { order ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(order.displayName)
                                                if (uiState.sortOrder == order) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        },
                                        onClick = {
                                            viewModel.setSortOrder(order)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Input
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search by filename, OCR text, tags...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Category Chips
            CategoryChips(
                categories = DocumentCategory.values().toList(),
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = { viewModel.setCategory(it) },
                modifier = Modifier.padding(vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Document List
            if (uiState.documents.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.FolderOpen,
                    title = if (uiState.searchQuery.isNotBlank()) "No matching documents" else "Folder is empty",
                    description = if (uiState.searchQuery.isNotBlank()) "Try changing your search keywords" else "Scan or import files to populate this folder.",
                    actionText = if (uiState.searchQuery.isBlank()) "Scan Document" else null,
                    onActionClick = onNavigateToScanner,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.documents, key = { it.id }) { doc ->
                        val isSelected = uiState.selectedDocIds.contains(doc.id)
                        DocumentCard(
                            document = doc,
                            isSelected = isSelected,
                            isSelectionMode = uiState.isMultiSelectMode,
                            onClick = {
                                if (uiState.isMultiSelectMode) {
                                    viewModel.toggleDocumentSelection(doc.id)
                                } else {
                                    onNavigateToPdfEditor(doc.id)
                                }
                            },
                            onLongClick = {
                                viewModel.toggleDocumentSelection(doc.id)
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(doc.id, !doc.isFavorite) },
                            onShare = {
                                val file = File(doc.pdfFilePath)
                                if (file.exists()) PdfEngine.sharePdf(context, file, doc.title)
                            },
                            onDelete = { viewModel.moveToTrash(doc.id) }
                        )
                    }
                }
            }
        }
    }

    // Merge Confirmation Dialog
    if (showMergeDialog) {
        AlertDialog(
            onDismissRequest = { showMergeDialog = false },
            title = { Text("Merge ${uiState.selectedDocIds.size} Documents", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Combine all selected documents into a single new multi-page PDF.")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = mergeTitle,
                        onValueChange = { mergeTitle = it },
                        label = { Text("New Merged PDF Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMergeDialog = false
                        viewModel.mergeSelectedDocuments(mergeTitle) { newDocId ->
                            onNavigateToPdfEditor(newDocId)
                        }
                    }
                ) {
                    Text("Merge PDFs")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMergeDialog = false }) { Text("Cancel") }
            }
        )
    }
}
