package com.immersive.reader.ui.library

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.immersive.reader.R
import com.immersive.reader.core.model.Book
import com.immersive.reader.ui.LibraryViewModel
import com.immersive.reader.ui.components.BookCover
import com.immersive.reader.ui.components.ReadingProgress
import com.immersive.reader.ui.components.displayAuthor
import com.immersive.reader.ui.components.formatProgressPercent
import com.immersive.reader.ui.theme.ReaderColors

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onStartReading: (String) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val books by viewModel.books.collectAsStateWithLifecycle()
    val importing by viewModel.importing.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingDelete by remember { mutableStateOf<Book?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(importError) {
        importError?.let { message ->
            snackbarHostState.showSnackbar(message)
            importError = null
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        viewModel.importBook(context.contentResolver, uri) { result ->
            result.exceptionOrNull()?.let { error -> importError = error.message ?: "Unable to import this EPUB" }
        }
    }

    pendingDelete?.let { book ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove book?") },
            text = { Text("Remove ${book.title} and its local EPUB from your library?") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(book); pendingDelete = null }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }

    val continueBook = books.firstOrNull { it.lastOpenedAt != null }
    val gridBooks = if (continueBook == null) books else books.filter { it.id != continueBook.id }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                navigationIcon = {
                    Image(
                        painter = painterResource(R.drawable.ic_immersive_reader_mark),
                        contentDescription = stringResource(R.string.reader_mark_content_description),
                        modifier = Modifier.padding(start = 12.dp).size(34.dp),
                    )
                },
                title = {
                    Column {
                        Text(
                            "My library",
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Serif,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            "A quieter place to read",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, "Settings") }
                },
            )
        },
    ) { padding ->
        if (books.isEmpty()) {
            EmptyLibrary(
                modifier = Modifier.padding(padding),
                onImport = { importLauncher.launch(arrayOf("application/epub+zip")) },
                importing = importing,
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 148.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                continueBook?.let { book ->
                    item(
                        span = { GridItemSpan(maxLineSpan) },
                        key = "continue-${book.id}",
                    ) {
                        ContinueReadingCard(
                            book = book,
                            onClick = { onStartReading(book.id) },
                            onDelete = { pendingDelete = book },
                        )
                    }
                }
                items(gridBooks, key = Book::id) { book ->
                    BookCard(
                        book = book,
                        onClick = { onStartReading(book.id) },
                        onDelete = { pendingDelete = book },
                    )
                }
                item(key = "import") {
                    ImportCard(
                        onClick = { importLauncher.launch(arrayOf("application/epub+zip")) },
                        importing = importing,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyLibrary(modifier: Modifier, onImport: () -> Unit, importing: Boolean) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(listOf(ReaderColors.Sage, ReaderColors.Moss)),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_immersive_reader_mark),
                contentDescription = null,
                modifier = Modifier.size(54.dp),
            )
        }
        Spacer(Modifier.height(26.dp))
        Text(
            "Make room for a good book",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Import an EPUB and turn your phone into a calm, focused reading space.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))
        Button(onClick = onImport, enabled = !importing) {
            if (importing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Icon(Icons.Default.Add, null)
            }
            Spacer(Modifier.width(8.dp))
            Text(if (importing) "Importing…" else "Import EPUB")
        }
    }
}

@Composable
private fun ContinueReadingCard(book: Book, onClick: () -> Unit, onDelete: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(132.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BookCover(
                title = book.title,
                coverPath = book.coverPath,
                showFallbackTitle = false,
                modifier = Modifier.width(92.dp).fillMaxSize(),
            )
            Column(
                modifier = Modifier.weight(1f).padding(start = 16.dp, end = 4.dp, top = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    book.progression?.let { "Continue · ${formatProgressPercent(it)}" } ?: "Continue reading",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    book.displayAuthor(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.weight(1f))
                ReadingProgress(book.progression)
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, "Book actions")
                }
                BookActionsMenu(
                    expanded = menuExpanded,
                    onDismiss = { menuExpanded = false },
                    onRemove = { menuExpanded = false; onDelete() },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookCard(book: Book, onClick: () -> Unit, onDelete: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    Column {
        Box {
            BookCover(
                title = book.title,
                coverPath = book.coverPath,
                showFallbackTitle = book.coverPath == null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f)
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = { menuExpanded = true },
                    ),
            )
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, "Book actions", tint = androidx.compose.ui.graphics.Color.White)
                }
                BookActionsMenu(
                    expanded = menuExpanded,
                    onDismiss = { menuExpanded = false },
                    onRemove = { menuExpanded = false; onDelete() },
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            book.title,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Serif,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            book.displayAuthor(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        book.progression?.let {
            Spacer(Modifier.height(8.dp))
            ReadingProgress(it)
        }
    }
}

@Composable
private fun BookActionsMenu(expanded: Boolean, onDismiss: () -> Unit, onRemove: () -> Unit) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Remove") },
            onClick = onRemove,
        )
    }
}

@Composable
private fun ImportCard(onClick: () -> Unit, importing: Boolean) {
    Card(
        onClick = { if (!importing) onClick() },
        modifier = Modifier.fillMaxWidth().aspectRatio(0.68f),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = CardDefaults.outlinedCardBorder(),
        enabled = !importing,
    ) {
        Column(
            Modifier.fillMaxSize().padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (importing) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                Spacer(Modifier.height(12.dp))
                Text("Importing…", fontWeight = FontWeight.SemiBold)
            } else {
                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
                Spacer(Modifier.height(12.dp))
                Text("Import a book", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("EPUB", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
