package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.SleekBottomNavigationBar
import com.example.ui.screens.*
import com.example.ui.theme.GscanBackgroundLight
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DocumentViewModel
import com.example.ui.viewmodel.GstInvoiceViewModel
import com.example.ui.viewmodel.PdfEditorViewModel
import com.example.ui.viewmodel.ScannerViewModel

sealed class ScreenRoute {
    object Home : ScreenRoute()
    data class Documents(val category: String? = null) : ScreenRoute()
    object Tools : ScreenRoute()
    object Settings : ScreenRoute()
    object Scanner : ScreenRoute()
    object PageEditor : ScreenRoute()
    object HandwritingToWord : ScreenRoute()
    object ProductCounter : ScreenRoute()
    object DwgViewer : ScreenRoute()
    data class GstInvoiceMaker(val initialTemplate: com.example.data.model.GstInvoiceTemplate? = null) : ScreenRoute()
    data class PdfEditor(val docId: Long) : ScreenRoute()
    data class OcrResult(val documentTitle: String, val text: String, val docId: Long) : ScreenRoute()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val documentViewModel: DocumentViewModel = viewModel()
                val scannerViewModel: ScannerViewModel = viewModel()
                val pdfEditorViewModel: PdfEditorViewModel = viewModel()
                val gstInvoiceViewModel: GstInvoiceViewModel = viewModel()

                var currentRoute by remember { mutableStateOf<ScreenRoute>(ScreenRoute.Home) }
                val backStack = remember { mutableStateListOf<ScreenRoute>() }

                fun navigateTo(route: ScreenRoute, clearBackStack: Boolean = false) {
                    if (clearBackStack) {
                        backStack.clear()
                    } else {
                        backStack.add(currentRoute)
                    }
                    currentRoute = route
                }

                fun navigateBack() {
                    if (backStack.isNotEmpty()) {
                        currentRoute = backStack.removeLast()
                    } else if (currentRoute !is ScreenRoute.Home) {
                        currentRoute = ScreenRoute.Home
                    }
                }

                BackHandler(enabled = currentRoute !is ScreenRoute.Home || backStack.isNotEmpty()) {
                    navigateBack()
                }

                val showBottomBar = currentRoute is ScreenRoute.Home ||
                        currentRoute is ScreenRoute.Documents ||
                        currentRoute is ScreenRoute.Tools ||
                        currentRoute is ScreenRoute.Settings

                val activeBottomNavKey = when (currentRoute) {
                    is ScreenRoute.Home -> "home"
                    is ScreenRoute.Documents -> "documents"
                    is ScreenRoute.Tools -> "tools"
                    is ScreenRoute.Settings -> "settings"
                    else -> "home"
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            SleekBottomNavigationBar(
                                currentRoute = activeBottomNavKey,
                                onNavigate = { tab ->
                                    when (tab) {
                                        "home" -> navigateTo(ScreenRoute.Home, clearBackStack = true)
                                        "documents" -> navigateTo(ScreenRoute.Documents(null))
                                        "tools" -> navigateTo(ScreenRoute.Tools)
                                        "settings" -> navigateTo(ScreenRoute.Settings)
                                    }
                                },
                                onScanClick = {
                                    scannerViewModel.resetScanner()
                                    navigateTo(ScreenRoute.Scanner)
                                }
                            )
                        }
                    },
                    containerColor = GscanBackgroundLight
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(GscanBackgroundLight)
                    ) {
                        when (val route = currentRoute) {
                            is ScreenRoute.Home -> {
                                HomeScreen(
                                    viewModel = documentViewModel,
                                    modifier = Modifier.padding(innerPadding),
                                    onNavigateToScanner = {
                                        scannerViewModel.resetScanner()
                                        navigateTo(ScreenRoute.Scanner)
                                    },
                                    onNavigateToPdfEditor = { docId ->
                                        pdfEditorViewModel.loadDocument(docId)
                                        navigateTo(ScreenRoute.PdfEditor(docId))
                                    },
                                    onNavigateToDocuments = { cat ->
                                        navigateTo(ScreenRoute.Documents(cat))
                                    },
                                    onNavigateToTools = {
                                        navigateTo(ScreenRoute.Tools)
                                    },
                                    onNavigateToSettings = {
                                        navigateTo(ScreenRoute.Settings)
                                    },
                                    onNavigateToHandwritingToWord = {
                                        navigateTo(ScreenRoute.HandwritingToWord)
                                    },
                                    onNavigateToProductCounter = {
                                        navigateTo(ScreenRoute.ProductCounter)
                                    },
                                    onNavigateToDwgViewer = {
                                        navigateTo(ScreenRoute.DwgViewer)
                                    },
                                    onNavigateToGstInvoiceMaker = {
                                        navigateTo(ScreenRoute.GstInvoiceMaker())
                                    }
                                )
                            }

                            is ScreenRoute.Documents -> {
                                DocumentsScreen(
                                    viewModel = documentViewModel,
                                    modifier = Modifier.padding(innerPadding),
                                    initialCategory = route.category,
                                    onNavigateToPdfEditor = { docId ->
                                        pdfEditorViewModel.loadDocument(docId)
                                        navigateTo(ScreenRoute.PdfEditor(docId))
                                    },
                                    onNavigateToScanner = {
                                        scannerViewModel.resetScanner()
                                        navigateTo(ScreenRoute.Scanner)
                                    }
                                )
                            }

                            is ScreenRoute.Tools -> {
                                ToolsScreen(
                                    viewModel = documentViewModel,
                                    modifier = Modifier.padding(innerPadding),
                                    onNavigateToScanner = {
                                        scannerViewModel.resetScanner()
                                        navigateTo(ScreenRoute.Scanner)
                                    },
                                    onNavigateToDocuments = {
                                        navigateTo(ScreenRoute.Documents(null))
                                    },
                                    onNavigateToPdfEditor = { docId ->
                                        pdfEditorViewModel.loadDocument(docId)
                                        navigateTo(ScreenRoute.PdfEditor(docId))
                                    },
                                    onNavigateToHandwritingToWord = {
                                        navigateTo(ScreenRoute.HandwritingToWord)
                                    },
                                    onNavigateToProductCounter = {
                                        navigateTo(ScreenRoute.ProductCounter)
                                    },
                                    onNavigateToDwgViewer = {
                                        navigateTo(ScreenRoute.DwgViewer)
                                    },
                                    onNavigateToGstInvoiceMaker = { template ->
                                        if (template != null) {
                                            gstInvoiceViewModel.setTemplate(template)
                                        }
                                        navigateTo(ScreenRoute.GstInvoiceMaker(template))
                                    }
                                )
                            }

                            is ScreenRoute.HandwritingToWord -> {
                                HandwritingToWordScreen(
                                    onNavigateBack = {
                                        navigateBack()
                                    }
                                )
                            }

                            is ScreenRoute.ProductCounter -> {
                                ProductCounterScreen(
                                    onNavigateBack = {
                                        navigateBack()
                                    }
                                )
                            }

                            is ScreenRoute.DwgViewer -> {
                                DwgViewerScreen(
                                    onNavigateBack = {
                                        navigateBack()
                                    }
                                )
                            }

                            is ScreenRoute.GstInvoiceMaker -> {
                                val selectedTemplate = route.initialTemplate
                                LaunchedEffect(selectedTemplate) {
                                    if (selectedTemplate != null) {
                                        gstInvoiceViewModel.setTemplate(selectedTemplate)
                                    }
                                }
                                GstInvoiceMakerScreen(
                                    viewModel = gstInvoiceViewModel,
                                    onNavigateBack = {
                                        navigateBack()
                                    }
                                )
                            }

                            is ScreenRoute.Settings -> {
                                SettingsScreen(
                                    viewModel = documentViewModel
                                )
                            }

                            is ScreenRoute.Scanner -> {
                                ScannerScreen(
                                    viewModel = scannerViewModel,
                                    onNavigateToPageEditor = {
                                        navigateTo(ScreenRoute.PageEditor)
                                    },
                                    onCloseScanner = {
                                        navigateBack()
                                    }
                                )
                            }

                            is ScreenRoute.PageEditor -> {
                                PageEditorScreen(
                                    viewModel = scannerViewModel,
                                    onNavigateBackToScanner = {
                                        navigateBack()
                                    },
                                    onDocumentSaved = { newDocId ->
                                        documentViewModel.refresh()
                                        pdfEditorViewModel.loadDocument(newDocId)
                                        navigateTo(ScreenRoute.PdfEditor(newDocId), clearBackStack = true)
                                    }
                                )
                            }

                            is ScreenRoute.PdfEditor -> {
                                PdfEditorScreen(
                                    documentId = route.docId,
                                    viewModel = pdfEditorViewModel,
                                    onNavigateBack = {
                                        documentViewModel.refresh()
                                        navigateBack()
                                    },
                                    onNavigateToOcrResult = { ocrText ->
                                        val doc = pdfEditorViewModel.uiState.value.document
                                        navigateTo(
                                            ScreenRoute.OcrResult(
                                                documentTitle = doc?.title ?: "Document",
                                                text = ocrText,
                                                docId = route.docId
                                            )
                                        )
                                    }
                                )
                            }

                            is ScreenRoute.OcrResult -> {
                                OcrResultScreen(
                                    documentTitle = route.documentTitle,
                                    ocrText = route.text,
                                    onSaveEditedText = { editedText ->
                                        val doc = pdfEditorViewModel.uiState.value.document
                                        if (doc != null) {
                                            pdfEditorViewModel.updateDocument(doc.copy(ocrText = editedText))
                                        }
                                    },
                                    onNavigateBack = {
                                        navigateBack()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
