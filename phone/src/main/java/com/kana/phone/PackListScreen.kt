package com.kana.phone

import android.widget.Toast
import androidx.compose.foundation.layout.*
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
    var localPacks by remember { mutableStateOf(WordStorage.loadAllPacks(context)) }
    var localTokens by remember { mutableStateOf(localPacks.map { it.token }.toSet()) }
    var downloadingTokens by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(Unit) {
        ApiClient.fetchPackList(context) { success, result ->
            packs = result
            loading = false
        }
    }

    // Local-only packs (downloaded from others, not in user's remote packs)
    val remoteTokens = packs.map { it.token }.toSet()
    val downloadedOnly = localPacks.filter { it.token !in remoteTokens }

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
        } else if (packs.isEmpty() && downloadedOnly.isEmpty()) {
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
                                        "${pack.wordCount} words" + if (pack.downloadCount > 0) " \u2022 ${pack.downloadCount} downloads" else "",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    if (pack.questionLang.isNotBlank() || pack.answerLang.isNotBlank()) {
                                        Text(
                                            "${langWithFlag(pack.questionLang).ifBlank { "?" }} \u2192 ${langWithFlag(pack.answerLang).ifBlank { "?" }}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    if (!pack.isOwner) {
                                        Text(
                                            "Shared",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.tertiary
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

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        if (isLocal) {
                                            onEditPack(pack.token)
                                        } else {
                                            Toast.makeText(context, "Download the pack first to edit", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    enabled = isLocal,
                                    shape = RoundedCornerShape(8.dp)
                                ) { Text("Edit") }

                                if (pack.token in localTokens) {
                                    OutlinedButton(
                                        onClick = {
                                            WordStorage.deletePack(context, pack.token)
                                            localPacks = WordStorage.loadAllPacks(context)
                                            localTokens = localPacks.map { it.token }.toSet()
                                            Toast.makeText(context, "Uninstalled from phone", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Uninstall")
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

                // Downloaded packs (from others)
                if (downloadedOnly.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Downloaded",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    items(downloadedOnly, key = { "local_${it.token}" }) { pack ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    pack.name.ifBlank { "Unnamed" },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${pack.words.size} words" + if (pack.downloadCount > 0) " \u2022 ${pack.downloadCount} downloads" else "",
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

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        WordStorage.deletePack(context, pack.token)
                                        localPacks = WordStorage.loadAllPacks(context)
                                        localTokens = localPacks.map { it.token }.toSet()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) { Text("Remove from phone") }
                            }
                        }
                    }
                }
            }
        }
    }
}
