package com.meridians.scribolt.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meridians.scribolt.viewmodel.NoteViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditNoteScreen(
    viewModel: NoteViewModel,
    noteId: Int?,
    onNavigateBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }
    var showSavedIndicator by remember { mutableStateOf(false) }
    var wordCount by remember { mutableIntStateOf(0) }
    var charCount by remember { mutableIntStateOf(0) }

    // Style options
    var selectedFontFamily by remember { mutableStateOf("default") }
    var selectedFontSize by remember { mutableIntStateOf(16) }
    var selectedTextColor by remember { mutableStateOf("#000000") }
    var selectedBackgroundColor by remember { mutableStateOf("#FFFFFF") }
    var showStylePanel by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Auto-save with debouncing
    LaunchedEffect(title, content, selectedFontFamily, selectedFontSize, selectedTextColor, selectedBackgroundColor) {
        if (title.isNotEmpty() || content.isNotEmpty()) {
            delay(1500)
            viewModel.saveNote(
                title = title,
                content = content,
                noteId = noteId,
                fontFamily = selectedFontFamily,
                fontSize = selectedFontSize,
                textColor = selectedTextColor,
                backgroundColor = selectedBackgroundColor
            )
            showSavedIndicator = true
            delay(2000)
            showSavedIndicator = false
        }

        wordCount = content.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
        charCount = content.length
    }

    LaunchedEffect(noteId) {
        if (noteId != null && noteId > 0) {
            viewModel.loadNote(noteId)
        } else {
            viewModel.clearCurrentNote()
        }
    }

    val currentNote by viewModel.currentNote.collectAsState()

    LaunchedEffect(currentNote) {
        currentNote?.let { note ->
            title = note.title
            content = note.content
            isFavorite = note.isFavorite
            selectedFontFamily = note.fontFamily
            selectedFontSize = note.fontSize
            selectedTextColor = note.textColor
            selectedBackgroundColor = note.backgroundColor
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete Note?") },
            text = { Text("This note will be permanently deleted. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentNote?.let { note ->
                            viewModel.deleteNote(note)
                        }
                        showDeleteDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Note deleted",
                                duration = SnackbarDuration.Short
                            )
                        }
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (noteId != null && noteId > 0) "Edit Note" else "New Note")
                        AnimatedVisibility(
                            visible = showSavedIndicator,
                            enter = fadeIn() + slideInVertically(),
                            exit = fadeOut() + slideOutVertically()
                        ) {
                            Text(
                                "Saved",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isFavorite = !isFavorite
                            viewModel.toggleFavorite(noteId, isFavorite)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = if (isFavorite) "Added to favorites" else "Removed from favorites",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    ) {
                        Icon(
                            if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Duplicate note") },
                                onClick = {
                                    showMenu = false
                                    currentNote?.let { note ->
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.duplicateNote(note)
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "Note duplicated",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Share") },
                                onClick = {
                                    showMenu = false
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Share functionality coming soon",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                }
                            )

                            if (noteId != null && noteId > 0) {
                                HorizontalDivider(
                                    Modifier,
                                    DividerDefaults.Thickness,
                                    DividerDefaults.color
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete note") },
                                    onClick = {
                                        showMenu = false
                                        showDeleteDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Column {
                // Style panel
                AnimatedVisibility(
                    visible = showStylePanel,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    StylePanel(
                        selectedFontFamily = selectedFontFamily,
                        onFontFamilyChange = { selectedFontFamily = it },
                        selectedFontSize = selectedFontSize,
                        onFontSizeChange = { selectedFontSize = it },
                        selectedTextColor = selectedTextColor,
                        onTextColorChange = { selectedTextColor = it },
                        selectedBackgroundColor = selectedBackgroundColor,
                        onBackgroundColorChange = { selectedBackgroundColor = it }
                    )
                }

                // Bottom toolbar - Always visible
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$wordCount words · $charCount characters",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Style button - Made more prominent
                            FilledTonalIconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showStylePanel = !showStylePanel
                                }
                            ) {
                                Icon(
                                    Icons.Default.Palette,
                                    contentDescription = "Style options",
                                    tint = if (showStylePanel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            // Floating Style Button - Alternative access point
            if (!showStylePanel) {
                FloatingActionButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showStylePanel = true
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = "Open style options"
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(parseColor(selectedBackgroundColor))
                .padding(16.dp)
        ) {
            val fontFamily = when (selectedFontFamily) {
                "serif" -> FontFamily.Serif
                "monospace" -> FontFamily.Monospace
                else -> FontFamily.Default
            }

            val textColor = parseColor(selectedTextColor)

            BasicTextField(
                value = title,
                onValueChange = { title = it },
                textStyle = TextStyle(
                    fontSize = (selectedFontSize + 8).sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily,
                    color = textColor
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box {
                        if (title.isEmpty()) {
                            Text(
                                "Title",
                                style = TextStyle(
                                    fontSize = (selectedFontSize + 8).sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = fontFamily,
                                    color = textColor.copy(alpha = 0.4f)
                                )
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider(
                Modifier,
                DividerDefaults.Thickness,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            BasicTextField(
                value = content,
                onValueChange = { content = it },
                textStyle = TextStyle(
                    fontSize = selectedFontSize.sp,
                    lineHeight = (selectedFontSize + 8).sp,
                    fontFamily = fontFamily,
                    color = textColor
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box {
                        if (content.isEmpty()) {
                            Text(
                                "Start writing...",
                                style = TextStyle(
                                    fontSize = selectedFontSize.sp,
                                    fontFamily = fontFamily,
                                    color = textColor.copy(alpha = 0.4f)
                                )
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

@Composable
fun StylePanel(
    selectedFontFamily: String,
    onFontFamilyChange: (String) -> Unit,
    selectedFontSize: Int,
    onFontSizeChange: (Int) -> Unit,
    selectedTextColor: String,
    onTextColorChange: (String) -> Unit,
    selectedBackgroundColor: String,
    onBackgroundColorChange: (String) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Font Family
            Text(
                "Font Style",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FontFamilyChip(
                    label = "Default",
                    fontFamily = FontFamily.Default,
                    isSelected = selectedFontFamily == "default",
                    onClick = { onFontFamilyChange("default") }
                )
                FontFamilyChip(
                    label = "Serif",
                    fontFamily = FontFamily.Serif,
                    isSelected = selectedFontFamily == "serif",
                    onClick = { onFontFamilyChange("serif") }
                )
                FontFamilyChip(
                    label = "Mono",
                    fontFamily = FontFamily.Monospace,
                    isSelected = selectedFontFamily == "monospace",
                    onClick = { onFontFamilyChange("monospace") }
                )
            }

            // Font Size
            Text(
                "Font Size",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { if (selectedFontSize > 12) onFontSizeChange(selectedFontSize - 2) }) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease size")
                }
                Text(
                    "${selectedFontSize}sp",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                IconButton(onClick = { if (selectedFontSize < 32) onFontSizeChange(selectedFontSize + 2) }) {
                    Icon(Icons.Default.Add, contentDescription = "Increase size")
                }
            }

            // Text Color
            Text(
                "Text Color",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            ColorPicker(
                selectedColor = selectedTextColor,
                onColorChange = onTextColorChange,
                colors = listOf(
                    "#000000", "#FFFFFF", "#FF0000", "#00FF00",
                    "#0000FF", "#FFFF00", "#FF00FF", "#00FFFF",
                    "#FFA500", "#800080", "#008080", "#808080"
                )
            )

            // Background Color
            Text(
                "Background Color",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            ColorPicker(
                selectedColor = selectedBackgroundColor,
                onColorChange = onBackgroundColorChange,
                colors = listOf(
                    "#FFFFFF", "#FFF9C4", "#FFECB3", "#FFE0B2",
                    "#FFCCBC", "#F8BBD0", "#E1BEE7", "#D1C4E9",
                    "#C5CAE9", "#BBDEFB", "#B2EBF2", "#B2DFDB"
                )
            )
        }
    }
}

@Composable
fun FontFamilyChip(
    label: String,
    fontFamily: FontFamily,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                label,
                fontFamily = fontFamily
            )
        }
    )
}

@Composable
fun ColorPicker(
    selectedColor: String,
    onColorChange: (String) -> Unit,
    colors: List<String>
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(colors) { colorHex ->
            ColorCircle(
                color = colorHex,
                isSelected = selectedColor == colorHex,
                onClick = { onColorChange(colorHex) }
            )
        }
    }
}

@Composable
fun ColorCircle(
    color: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(parseColor(color))
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = if (color == "#FFFFFF" || color == "#FFF9C4") Color.Black else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

fun parseColor(hexColor: String): Color {
    return try {
        Color(hexColor.toColorInt())
    } catch (_: Exception) {
        Color.Black
    }
}