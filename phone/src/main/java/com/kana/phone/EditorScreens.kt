package com.kana.phone

import android.media.MediaPlayer
import android.widget.Toast
import kotlinx.coroutines.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackListScreen(
    onBack: () -> Unit,
    onCreatePack: () -> Unit,
    onEditPack: (String) -> Unit,
    onDownloadPack: (String) -> Unit,
    context: android.content.Context
) {
    var packs by remember { mutableStateOf<List<RemotePack>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var localTokens by remember { mutableStateOf(WordStorage.loadAllPacks(context).map { it.token }.toSet()) }
    var downloadingTokens by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(Unit) {
        ApiClient.fetchPackList(context) { success, result ->
            packs = result
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Word Packs") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("\u2190 Back", color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreatePack,
                containerColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("+", fontSize = 24.sp, color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (packs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No packs yet", fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onCreatePack, shape = RoundedCornerShape(8.dp)) {
                        Text("Create First Pack")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(packs, key = { it.token }) { pack ->
                    val isLocal = pack.token in localTokens

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        pack.name.ifBlank { "Unnamed" },
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${pack.wordCount} words",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                if (pack.isPublic) {
                                    Text(
                                        "Public",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            if (pack.updatedAt.isNotBlank()) {
                                val dateStr = pack.updatedAt.take(10)
                                Text(
                                    "Updated: $dateStr",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(
                                    onClick = { onEditPack(pack.token) },
                                    shape = RoundedCornerShape(8.dp)
                                ) { Text("Edit") }

                                if (pack.token in localTokens) {
                                    OutlinedButton(onClick = {}, enabled = false, shape = RoundedCornerShape(8.dp)) {
                                        Text("Downloaded")
                                    }
                                } else if (pack.token in downloadingTokens) {
                                    OutlinedButton(onClick = {}, enabled = false, shape = RoundedCornerShape(8.dp)) {
                                        Text("Downloading...")
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            downloadingTokens = downloadingTokens + pack.token
                                            PackUpdater.downloadPack(context, pack.token) { success, message ->
                                                downloadingTokens = downloadingTokens - pack.token
                                                if (success) {
                                                    localTokens = localTokens + pack.token
                                                }
                                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) { Text("Download") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackEditorScreen(
    editToken: String?,
    onBack: () -> Unit,
    context: android.content.Context
) {
    var packName by remember { mutableStateOf("") }
    var words by remember { mutableStateOf(mutableListOf(EditWord())) }
    var isPublic by remember { mutableStateOf(false) }
    var tags by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(editToken != null) }
    var saving by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var showImport by remember { mutableStateOf(false) }
    var savedToken by remember { mutableStateOf(editToken) }

    LaunchedEffect(editToken) {
        if (editToken != null) {
            ApiClient.fetchPackForEdit(editToken, context) { success, pack ->
                if (success && pack != null) {
                    packName = pack.name
                    isPublic = pack.isPublic
                    tags = pack.tags
                    words = pack.words.toMutableList().ifEmpty { mutableListOf(EditWord()) }
                } else {
                    Toast.makeText(context, "Failed to load pack", Toast.LENGTH_SHORT).show()
                }
                loading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editToken != null) "Edit Pack" else "Create Pack") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("\u2190 Back", color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Pack name
            item {
                OutlinedTextField(
                    value = packName,
                    onValueChange = { packName = it },
                    label = { Text("Pack Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            // Public toggle & tags
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Public", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "Others can browse and download",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Switch(
                                checked = isPublic,
                                onCheckedChange = { isPublic = it }
                            )
                        }

                        if (isPublic) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = tags,
                                onValueChange = { tags = it },
                                label = { Text("Tags (comma separated)") },
                                placeholder = { Text("japanese, beginner, food") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    cursorColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }

            // Bulk import
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Words (${words.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { showImport = !showImport }, shape = RoundedCornerShape(8.dp)) {
                            Text(if (showImport) "Hide Import" else "Import")
                        }
                    }
                }
            }

            if (showImport) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "One word per line: question|answer|reading",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = importText,
                                onValueChange = { importText = it },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                                placeholder = { Text("cat|neko|ねこ") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    cursorColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                shape = RoundedCornerShape(8.dp),
                                onClick = {
                                    val newWords = importText.lines()
                                        .map { it.trim() }
                                        .filter { it.isNotBlank() }
                                        .map { line ->
                                            val parts = line.split("|")
                                            EditWord(
                                                question = parts.getOrElse(0) { "" },
                                                answer = parts.getOrElse(1) { "" },
                                                reading = parts.getOrElse(2) { "" }
                                            )
                                        }
                                    if (newWords.isNotEmpty()) {
                                        val current = words.toMutableList()
                                        // Remove empty placeholder words
                                        current.removeAll { it.question.isBlank() && it.answer.isBlank() }
                                        current.addAll(newWords)
                                        words = current
                                        importText = ""
                                        showImport = false
                                    }
                                }
                            ) { Text("Add Words") }
                        }
                    }
                }
            }

            // Word cards
            itemsIndexed(words.toList()) { index, word ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (word.enabled)
                            MaterialTheme.colorScheme.surface
                        else
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "#${index + 1}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (word.enabled) "On" else "Off",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Switch(
                                    checked = word.enabled,
                                    onCheckedChange = {
                                        val updated = words.toMutableList()
                                        updated[index] = updated[index].copy(enabled = it)
                                        words = updated
                                    },
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedTextField(
                            value = word.question,
                            onValueChange = {
                                val updated = words.toMutableList()
                                updated[index] = updated[index].copy(question = it)
                                words = updated
                            },
                            label = { Text("Question") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                cursorColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedTextField(
                            value = word.answer,
                            onValueChange = {
                                val updated = words.toMutableList()
                                updated[index] = updated[index].copy(answer = it)
                                words = updated
                            },
                            label = { Text("Answer") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                cursorColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedTextField(
                            value = word.reading,
                            onValueChange = {
                                val updated = words.toMutableList()
                                updated[index] = updated[index].copy(reading = it)
                                words = updated
                            },
                            label = { Text("Reading (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                cursorColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        WordAudioControls(
                            audio = word.audio,
                            index = index,
                            context = context,
                            onAudioChanged = { newAudio ->
                                val updated = words.toMutableList()
                                updated[index] = updated[index].copy(audio = newAudio)
                                words = updated
                            }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        TextButton(
                            onClick = {
                                val updated = words.toMutableList()
                                updated.removeAt(index)
                                if (updated.isEmpty()) updated.add(EditWord())
                                words = updated
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Remove")
                        }
                    }
                }
            }

            // Add word button
            item {
                OutlinedButton(
                    onClick = {
                        val updated = words.toMutableList()
                        updated.add(EditWord())
                        words = updated
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+ Add Word")
                }
            }

            // Save / Delete buttons
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (packName.isBlank()) {
                                Toast.makeText(context, "Pack name required", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val validWords = words.filter { it.question.isNotBlank() && it.answer.isNotBlank() }
                            if (validWords.isEmpty()) {
                                Toast.makeText(context, "Add at least one word", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            saving = true
                            if (savedToken != null) {
                                ApiClient.savePack(savedToken!!, packName, validWords, context, isPublic, tags) { success, msg ->
                                    if (success) {
                                        // Re-download pack locally so quiz uses updated audio
                                        PackUpdater.updatePack(context, savedToken!!) { _, _ -> }
                                    }
                                    saving = false
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                ApiClient.createPack(packName, validWords, context, isPublic, tags) { success, result ->
                                    saving = false
                                    if (success) {
                                        savedToken = result
                                        // Download pack locally so quiz has the audio
                                        PackUpdater.downloadPack(context, result) { _, _ -> }
                                        Toast.makeText(context, "Created! Token: $result", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        enabled = !saving,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            when {
                                saving -> "Saving..."
                                savedToken != null -> "Save"
                                else -> "Create"
                            },
                            fontSize = 16.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    if (savedToken != null) {
                        Button(
                            onClick = {
                                ApiClient.deletePack(savedToken!!, context) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    if (success) onBack()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Delete", fontSize = 16.sp, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BrowsePacksScreen(
    onBack: () -> Unit,
    context: android.content.Context
) {
    var packs by remember { mutableStateOf<List<RemotePack>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf("") }
    var localTokens by remember { mutableStateOf(WordStorage.loadAllPacks(context).map { it.token }.toSet()) }
    var downloadingTokens by remember { mutableStateOf(setOf<String>()) }

    fun loadPacks() {
        loading = true
        ApiClient.fetchPublicPacks(search = searchQuery, tag = selectedTag) { _, result ->
            packs = result
            loading = false
        }
    }

    LaunchedEffect(Unit) { loadPacks() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Browse Packs") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("\u2190 Back", color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search packs...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                trailingIcon = {
                    FilledTonalButton(
                        onClick = { loadPacks() },
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("Search") }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            // Tag filter chips
            if (selectedTag.isNotBlank()) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tag: $selectedTag", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = {
                        selectedTag = ""
                        loadPacks()
                    }) {
                        Text("Clear", fontSize = 12.sp)
                    }
                }
            }

            if (loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (packs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No public packs found", fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(packs, key = { it.token }) { pack ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            pack.name.ifBlank { "Unnamed" },
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "${pack.wordCount} words",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                        if (pack.author.isNotBlank()) {
                                            Text(
                                                "by ${pack.author}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }

                                // Tags
                                if (pack.tags.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        pack.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { tag ->
                                            SuggestionChip(
                                                onClick = {
                                                    selectedTag = tag
                                                    loadPacks()
                                                },
                                                label = { Text(tag, fontSize = 12.sp) }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (pack.token in localTokens) {
                                    OutlinedButton(onClick = {}, enabled = false, shape = RoundedCornerShape(8.dp)) {
                                        Text("Downloaded")
                                    }
                                } else if (pack.token in downloadingTokens) {
                                    OutlinedButton(onClick = {}, enabled = false, shape = RoundedCornerShape(8.dp)) {
                                        Text("Downloading...")
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            downloadingTokens = downloadingTokens + pack.token
                                            PackUpdater.downloadPack(context, pack.token) { success, message ->
                                                downloadingTokens = downloadingTokens - pack.token
                                                if (success) {
                                                    localTokens = localTokens + pack.token
                                                }
                                                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) { Text("Download") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WordAudioControls(
    audio: String,
    index: Int,
    context: android.content.Context,
    onAudioChanged: (String) -> Unit
) {
    var recording by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    val recorder = remember { AudioRecorderHelper(context) }
    var hasPermission by remember { mutableStateOf(recorder.hasPermission()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            if (recorder.startRecording()) {
                recording = true
            } else {
                Toast.makeText(context, "Failed to start recording", Toast.LENGTH_SHORT).show()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { recorder.cleanup() }
    }

    if (audio.isNotBlank()) {
        // Has audio - show play & delete
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                shape = RoundedCornerShape(8.dp),
                onClick = {
                    if (playing) return@FilledTonalButton
                    playing = true
                    // Download first then play locally
                    CoroutineScope(Dispatchers.IO).launch {
                        AudioCache.download(context, audio)
                        withContext(Dispatchers.Main) {
                            val cached = AudioCache.getCachedFile(context, audio)
                            if (cached != null) {
                                try {
                                    val player = MediaPlayer()
                                    player.setDataSource(cached.absolutePath)
                                    player.setOnCompletionListener {
                                        it.release()
                                        playing = false
                                    }
                                    player.setOnErrorListener { mp, _, _ ->
                                        mp.release()
                                        playing = false
                                        true
                                    }
                                    player.prepare()
                                    player.start()
                                } catch (_: Exception) {
                                    playing = false
                                    Toast.makeText(context, "Playback failed", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                playing = false
                                Toast.makeText(context, "Failed to download audio", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                enabled = !playing
            ) {
                Text(if (playing) "Playing..." else "\u25b6 Play")
            }

            TextButton(
                onClick = {
                    ApiClient.deleteAudio(audio, context)
                    onAudioChanged("")
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete Audio")
            }
        }
    } else {
        // No audio - show record button
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (recording) {
                Button(
                    shape = RoundedCornerShape(8.dp),
                    onClick = {
                        val file = recorder.stopRecording()
                        recording = false
                        if (file != null && file.exists()) {
                            uploading = true
                            ApiClient.uploadAudio(file, context) { success, result ->
                                uploading = false
                                if (success) {
                                    onAudioChanged(result)
                                } else {
                                    Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
                                }
                                file.delete()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("\u25a0 Stop")
                }
                Text(
                    "Recording...",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (uploading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
                Text(
                    "Uploading...",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 8.dp)
                )
            } else {
                OutlinedButton(
                    onClick = {
                        if (hasPermission) {
                            if (recorder.startRecording()) {
                                recording = true
                            } else {
                                Toast.makeText(context, "Failed to start recording", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("\u25cf Record Audio")
                }
            }
        }
    }
}
