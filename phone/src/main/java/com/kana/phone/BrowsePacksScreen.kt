package com.kana.phone

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    var filterQuestionLang by remember { mutableStateOf("") }
    var filterAnswerLang by remember { mutableStateOf("") }
    var localTokens by remember { mutableStateOf(WordStorage.loadAllPacks(context).map { it.token }.toSet()) }
    var downloadingTokens by remember { mutableStateOf(setOf<String>()) }

    fun loadPacks() {
        loading = true
        ApiClient.fetchPublicPacks(search = searchQuery, tag = selectedTag, questionLang = filterQuestionLang, answerLang = filterAnswerLang) { _, result ->
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

            // Language filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val languages = listOf("", "Japanese", "English", "Spanish", "French", "German", "Korean", "Chinese", "Russian", "Portuguese", "Italian", "Arabic", "Hindi", "Turkish", "Vietnamese", "Thai", "Indonesian", "Dutch", "Polish", "Swedish", "Other")

                var qExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = qExpanded,
                    onExpandedChange = { qExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = langWithFlag(filterQuestionLang).ifBlank { "Any" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Questions", fontSize = 12.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = qExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = qExpanded,
                        onDismissRequest = { qExpanded = false }
                    ) {
                        languages.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(langWithFlag(lang).ifBlank { "Any" }) },
                                onClick = {
                                    filterQuestionLang = lang
                                    qExpanded = false
                                    loadPacks()
                                }
                            )
                        }
                    }
                }

                var aExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = aExpanded,
                    onExpandedChange = { aExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = langWithFlag(filterAnswerLang).ifBlank { "Any" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Answers", fontSize = 12.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = aExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = aExpanded,
                        onDismissRequest = { aExpanded = false }
                    ) {
                        languages.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(langWithFlag(lang).ifBlank { "Any" }) },
                                onClick = {
                                    filterAnswerLang = lang
                                    aExpanded = false
                                    loadPacks()
                                }
                            )
                        }
                    }
                }
            }

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
                        val isDownloaded = pack.token in localTokens
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
                                        verticalAlignment = Alignment.Top
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
                                            if (pack.questionLang.isNotBlank() || pack.answerLang.isNotBlank()) {
                                                Text(
                                                    "${langWithFlag(pack.questionLang).ifBlank { "?" }} \u2192 ${langWithFlag(pack.answerLang).ifBlank { "?" }}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                                )
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            if (pack.downloadCount > 0) {
                                                Text(
                                                    "${pack.downloadCount} downloads",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                                )
                                            }
                                            val ago = timeAgo(pack.updatedAt)
                                            if (ago.isNotBlank()) {
                                                Text(
                                                    ago,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
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

                                    val isLoggedIn = AppSettings.isLoggedIn(context)
                                    var packAvg by remember { mutableStateOf(pack.avgRating) }
                                    var packRatingCount by remember { mutableStateOf(pack.ratingCount) }
                                    var userRating by remember { mutableStateOf(0) }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Download button
                                        if (isDownloaded) {
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

                                        // Stars
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(1.dp)
                                        ) {
                                            if (packRatingCount > 0) {
                                                Text(
                                                    "%.1f".format(packAvg),
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                    modifier = Modifier.padding(end = 4.dp)
                                                )
                                            }
                                            (1..5).forEach { star ->
                                                val filled = if (userRating > 0) star <= userRating else star <= packAvg + 0.5f
                                                Text(
                                                    if (filled) "\u2605" else "\u2606",
                                                    fontSize = 18.sp,
                                                    color = if (filled)
                                                        MaterialTheme.colorScheme.primary
                                                    else
                                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                                    modifier = Modifier.clickable {
                                                        if (!isLoggedIn) {
                                                            Toast.makeText(context, "Login to rate packs", Toast.LENGTH_SHORT).show()
                                                            return@clickable
                                                        }
                                                        ApiClient.ratePack(pack.token, star, context) { success, newAvg, newCount ->
                                                            if (success) {
                                                                userRating = star
                                                                packAvg = newAvg
                                                                packRatingCount = newCount
                                                            } else {
                                                                Toast.makeText(context, "Rating failed", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
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
        }
    }
}
