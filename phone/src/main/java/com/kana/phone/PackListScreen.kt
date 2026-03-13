package com.kana.phone

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PackListScreen(
    onBack: () -> Unit,
    onCreatePack: () -> Unit,
    onEditPack: (String) -> Unit,
    context: android.content.Context
) {
    var packs by remember { mutableStateOf<List<RemotePack>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var localPacks by remember { mutableStateOf(WordStorage.loadAllPacks(context)) }
    var localIds by remember { mutableStateOf(localPacks.map { it.id }.toSet()) }
    var downloadingIds by remember { mutableStateOf(setOf<String>()) }

    var syncingIds by remember { mutableStateOf(setOf<String>()) }
    var enabledPacks by remember { mutableStateOf(AppSettings.getEnabledPacks(context)) }
    var showEditWarningForPack by remember { mutableStateOf<RemotePack?>(null) }
    var previewPack by remember { mutableStateOf<RemotePack?>(null) }
    var previewWords by remember { mutableStateOf<List<Word>>(emptyList()) }
    var previewLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Auto-sync all local packs (including downloaded from others)
        PackUpdater.checkForUpdates(context) { _ ->
            localPacks = WordStorage.loadAllPacks(context)
            localIds = localPacks.map { it.id }.toSet()
        }

        ApiClient.fetchPackList(context) { success, result ->
            packs = result
            loading = false

            // Auto-sync owned/collaborated packs that are out of date
            if (success) {
                for (remotePack in result) {
                    val localPack = localPacks.find { it.id == remotePack.id }
                    if (localPack != null && localPack.updated != remotePack.updatedAt) {
                        syncingIds = syncingIds + remotePack.id
                        PackUpdater.updatePack(context, remotePack.id) { _, _ ->
                            syncingIds = syncingIds - remotePack.id
                            localPacks = WordStorage.loadAllPacks(context)
                            localIds = localPacks.map { it.id }.toSet()
                        }
                    }
                }
            }
        }
    }

    // Local-only packs (downloaded from others, not in user's remote packs)
    val remoteIds = packs.map { it.id }.toSet()
    val downloadedOnly = localPacks.filter { it.id !in remoteIds }

    Box(modifier = Modifier.fillMaxSize()) {
        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (packs.isEmpty() && downloadedOnly.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
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
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(packs, key = { it.id }) { pack ->
                    val isLocal = pack.id in localIds
                    val isSyncing = pack.id in syncingIds

                    Card(
                        onClick = {
                            previewPack = pack
                            previewLoading = true
                            previewWords = emptyList()
                            ApiClient.fetchPackPreview(pack.id) { success, words ->
                                previewLoading = false
                                if (success) previewWords = words
                            }
                        },
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            pack.name.ifBlank { "Unnamed" },
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (pack.isPublic) {
                                            VerificationBadge(pack.verificationStatus)
                                        }
                                    }
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
                                    if (isLocal) {
                                        Switch(
                                            checked = pack.id in enabledPacks,
                                            onCheckedChange = { checked ->
                                                val current = enabledPacks.toMutableSet()
                                                if (checked) current.add(pack.id) else current.remove(pack.id)
                                                AppSettings.setEnabledPacks(context, current)
                                                enabledPacks = current
                                            }
                                        )
                                    }
                                    if (!pack.isOwner) {
                                        Text(
                                            "Shared",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                            }

                            if (isSyncing) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "Syncing...",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else if (pack.updatedAt.isNotBlank()) {
                                val dateStr = pack.updatedAt.take(10)
                                Text(
                                    "Updated: $dateStr",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
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
                                            onClick = {},
                                            label = { Text(tag, fontSize = 12.sp) }
                                        )
                                    }
                                }
                            }

                            // Rating
                            if (pack.isPublic && pack.ratingCount > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    Text(
                                        "%.1f".format(pack.avgRating),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                    (1..5).forEach { star ->
                                        val filled = star <= pack.avgRating + 0.5f
                                        Text(
                                            if (filled) "\u2605" else "\u2606",
                                            fontSize = 16.sp,
                                            color = if (filled)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        )
                                    }
                                    Text(
                                        "(${pack.ratingCount})",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledTonalButton(
                                        onClick = {
                                            if (isLocal) {
                                                if (pack.isPublic && pack.verificationStatus in listOf("accepted", "neutral")) {
                                                    showEditWarningForPack = pack
                                                } else {
                                                    onEditPack(pack.id)
                                                }
                                            } else {
                                                Toast.makeText(context, "Download the pack first to edit", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        enabled = isLocal,
                                        shape = RoundedCornerShape(8.dp)
                                    ) { Text("Edit") }

                                    var sharing by remember { mutableStateOf(false) }
                                    IconButton(
                                        onClick = {
                                            sharing = true
                                            ApiClient.generateShareCode(pack.id, context) { success, code ->
                                                sharing = false
                                                if (success) {
                                                    val packName = pack.name.ifBlank { "Unnamed" }
                                                    val shareText = "Check out my word pack \"$packName\" on Language Learning!\nDownload pack with this code: $code"
                                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "text/plain"
                                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                                    }
                                                    context.startActivity(Intent.createChooser(intent, "Share pack"))
                                                } else {
                                                    Toast.makeText(context, code, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        enabled = !sharing
                                    ) {
                                        Icon(Icons.Filled.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                if (pack.id in localIds) {
                                    OutlinedButton(
                                        onClick = {
                                            WordStorage.deletePack(context, pack.id)
                                            localPacks = WordStorage.loadAllPacks(context)
                                            localIds = localPacks.map { it.id }.toSet()
                                            Toast.makeText(context, "Uninstalled from phone", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Uninstall")
                                    }
                                } else if (pack.id in downloadingIds) {
                                    OutlinedButton(onClick = {}, enabled = false, shape = RoundedCornerShape(8.dp)) {
                                        Text("Downloading...")
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            downloadingIds = downloadingIds + pack.id
                                            PackUpdater.downloadPack(context, pack.id) { success, message ->
                                                downloadingIds = downloadingIds - pack.id
                                                if (success) {
                                                    localIds = localIds + pack.id
                                                    enabledPacks = AppSettings.getEnabledPacks(context)
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

                    items(downloadedOnly, key = { "local_${it.id}" }) { pack ->
                        var showDeleteConfirm by remember { mutableStateOf(false) }
                        val dismissState = rememberDismissState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == DismissValue.DismissedToStart) {
                                    showDeleteConfirm = true
                                    false
                                } else false
                            }
                        )

                        if (showDeleteConfirm) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirm = false },
                                title = { Text("Remove pack?") },
                                text = { Text("Remove \"${pack.name.ifBlank { "Unnamed" }}\" from your device?") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        showDeleteConfirm = false
                                        WordStorage.deletePack(context, pack.id)
                                        localPacks = WordStorage.loadAllPacks(context)
                                        localIds = localPacks.map { it.id }.toSet()
                                    }) { Text("Remove") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
                                }
                            )
                        }

                        SwipeToDismiss(
                            state = dismissState,
                            directions = setOf(DismissDirection.EndToStart),
                            background = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.onError
                                    )
                                }
                            },
                            dismissContent = {
                        Card(
                            onClick = {
                                previewPack = RemotePack(
                                    id = pack.id,
                                    name = pack.name,
                                    wordCount = pack.words.size,
                                    updatedAt = pack.updated,
                                    questionLang = pack.questionLang,
                                    answerLang = pack.answerLang
                                )
                                previewLoading = false
                                previewWords = pack.words
                            },
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
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                pack.name.ifBlank { "Unnamed" },
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (pack.verificationStatus != "none") {
                                                VerificationBadge(pack.verificationStatus)
                                            }
                                        }
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
                                    }
                                    Switch(
                                        checked = pack.id in enabledPacks,
                                        onCheckedChange = { checked ->
                                            val current = enabledPacks.toMutableSet()
                                            if (checked) current.add(pack.id) else current.remove(pack.id)
                                            AppSettings.setEnabledPacks(context, current)
                                            enabledPacks = current
                                        }
                                    )
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
                                                onClick = {},
                                                label = { Text(tag, fontSize = 12.sp) }
                                            )
                                        }
                                    }
                                }

                                // Rating
                                if (pack.ratingCount > 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                                    ) {
                                        Text(
                                            "%.1f".format(pack.avgRating),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                        (1..5).forEach { star ->
                                            val filled = star <= pack.avgRating + 0.5f
                                            Text(
                                                if (filled) "\u2605" else "\u2606",
                                                fontSize = 16.sp,
                                                color = if (filled)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                            )
                                        }
                                        Text(
                                            "(${pack.ratingCount})",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                            }
                        )
                    }
                }
            }
        }

        // Edit warning dialog for verified/neutral public packs
        if (showEditWarningForPack != null) {
            val pack = showEditWarningForPack!!
            AlertDialog(
                onDismissRequest = { showEditWarningForPack = null },
                title = { Text("Edit published pack?") },
                text = { Text("Editing a published pack will reset its verification status to pending. Continue?") },
                confirmButton = {
                    TextButton(onClick = {
                        showEditWarningForPack = null
                        onEditPack(pack.id)
                    }) { Text("Edit") }
                },
                dismissButton = {
                    TextButton(onClick = { showEditWarningForPack = null }) { Text("Cancel") }
                }
            )
        }

        // FAB
        FloatingActionButton(
            onClick = onCreatePack,
            containerColor = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("+", fontSize = 24.sp, color = MaterialTheme.colorScheme.onPrimary)
        }

        if (previewPack != null) {
            PackPreviewDialog(
                pack = previewPack!!,
                words = previewWords,
                loading = previewLoading,
                context = context,
                onDismiss = { previewPack = null }
            )
        }
    }
}
