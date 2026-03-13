package com.kana.phone

import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.*

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
    var localIds by remember { mutableStateOf(WordStorage.loadAllPacks(context).map { it.id }.toSet()) }
    var downloadingIds by remember { mutableStateOf(setOf<String>()) }
    var previewPack by remember { mutableStateOf<RemotePack?>(null) }
    var previewWords by remember { mutableStateOf<List<Word>>(emptyList()) }
    var previewLoading by remember { mutableStateOf(false) }
    var verifiedOnly by remember { mutableStateOf(AppSettings.getBrowseVerifiedOnly(context)) }
    var showUnverifiedWarning by remember { mutableStateOf(false) }

    fun loadPacks() {
        loading = true
        ApiClient.fetchPublicPacks(search = searchQuery, tag = selectedTag, questionLang = filterQuestionLang, answerLang = filterAnswerLang, verifiedOnly = verifiedOnly) { _, result ->
            packs = result
            loading = false
        }
    }

    LaunchedEffect(Unit) { loadPacks() }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
            // Share code dialog
            var showShareCodeDialog by remember { mutableStateOf(false) }
            var shareCodeInput by remember { mutableStateOf("") }
            var redeemingCode by remember { mutableStateOf(false) }

            if (showShareCodeDialog) {
                AlertDialog(
                    onDismissRequest = { if (!redeemingCode) { showShareCodeDialog = false; shareCodeInput = "" } },
                    title = { Text("Enter Share Code") },
                    text = {
                        OutlinedTextField(
                            value = shareCodeInput,
                            onValueChange = { shareCodeInput = it.take(8) },
                            placeholder = { Text("8-character code") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (shareCodeInput.length == 8) {
                                    redeemingCode = true
                                    PackUpdater.redeemShareCode(context, shareCodeInput) { success, message ->
                                        redeemingCode = false
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                        if (success) {
                                            showShareCodeDialog = false
                                            shareCodeInput = ""
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Code must be 8 characters", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = shareCodeInput.length == 8 && !redeemingCode
                        ) { Text(if (redeemingCode) "Downloading..." else "Redeem") }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showShareCodeDialog = false; shareCodeInput = "" },
                            enabled = !redeemingCode
                        ) { Text("Cancel") }
                    }
                )
            }

            // Search bar + code button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search packs...") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                loadPacks()
                            }) {
                                Text("✕", fontSize = 18.sp)
                            }
                        }
                        IconButton(onClick = { loadPacks() }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            OutlinedIconButton(
                onClick = { showShareCodeDialog = true },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Filled.Key, contentDescription = "Enter share code")
            }
            }

            // Language filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val languages = languageList

                var qExpanded by remember { mutableStateOf(false) }
                var qSearch by remember { mutableStateOf("") }
                val qFiltered = if (qSearch.isBlank()) languages else languages.filter {
                    langWithFlag(it).contains(qSearch, ignoreCase = true) || it.contains(qSearch, ignoreCase = true)
                }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = langWithFlag(filterQuestionLang).ifBlank { "Any" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cards", fontSize = 12.sp) },
                        trailingIcon = {
                            Icon(if (qExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { qExpanded = !qExpanded; qSearch = "" })
                    DropdownMenu(
                        expanded = qExpanded,
                        onDismissRequest = { qExpanded = false },
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        OutlinedTextField(
                            value = qSearch,
                            onValueChange = { qSearch = it },
                            placeholder = { Text("Search...", fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        qFiltered.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(langWithFlag(lang).ifBlank { "Any" }) },
                                onClick = {
                                    filterQuestionLang = lang
                                    qExpanded = false
                                    qSearch = ""
                                    loadPacks()
                                }
                            )
                        }
                    }
                }

                var aExpanded by remember { mutableStateOf(false) }
                var aSearch by remember { mutableStateOf("") }
                val aFiltered = if (aSearch.isBlank()) languages else languages.filter {
                    langWithFlag(it).contains(aSearch, ignoreCase = true) || it.contains(aSearch, ignoreCase = true)
                }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = langWithFlag(filterAnswerLang).ifBlank { "Any" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Explanations", fontSize = 12.sp) },
                        trailingIcon = {
                            Icon(if (aExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { aExpanded = !aExpanded; aSearch = "" })
                    DropdownMenu(
                        expanded = aExpanded,
                        onDismissRequest = { aExpanded = false },
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        OutlinedTextField(
                            value = aSearch,
                            onValueChange = { aSearch = it },
                            placeholder = { Text("Search...", fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        aFiltered.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(langWithFlag(lang).ifBlank { "Any" }) },
                                onClick = {
                                    filterAnswerLang = lang
                                    aExpanded = false
                                    aSearch = ""
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

            // Verified only toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show verified only", fontSize = 14.sp)
                Switch(
                    checked = verifiedOnly,
                    onCheckedChange = { checked ->
                        if (!checked) {
                            showUnverifiedWarning = true
                        } else {
                            verifiedOnly = true
                            AppSettings.setBrowseVerifiedOnly(context, true)
                            loadPacks()
                        }
                    }
                )
            }

            // Unverified warning dialog
            if (showUnverifiedWarning) {
                AlertDialog(
                    onDismissRequest = { showUnverifiedWarning = false },
                    title = { Text("Show unverified packs?") },
                    text = { Text("Unverified packs may contain inappropriate content. Use at your own risk.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showUnverifiedWarning = false
                            verifiedOnly = false
                            AppSettings.setBrowseVerifiedOnly(context, false)
                            loadPacks()
                        }) { Text("Show All") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showUnverifiedWarning = false }) { Text("Cancel") }
                    }
                )
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
                    items(packs, key = { it.id }) { pack ->
                        val isDownloaded = pack.id in localIds
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                previewPack = pack
                                previewLoading = true
                                previewWords = emptyList()
                                ApiClient.fetchPackPreview(pack.id) { success, words ->
                                    previewLoading = false
                                    if (success) previewWords = words
                                }
                            },
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
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    pack.name.ifBlank { "Unnamed" },
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                VerificationBadge(pack.verificationStatus)
                                            }
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
                                                        }
                                                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary
                                                ),
                                                shape = RoundedCornerShape(8.dp)
                                            ) { Icon(Icons.Filled.Download, contentDescription = "Download") }
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
                                                        ApiClient.ratePack(pack.id, star, context) { success, newAvg, newCount ->
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

            // Preview dialog
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

@Composable
fun VerificationBadge(status: String) {
    when (status) {
        "accepted" -> Icon(
            Icons.Filled.Verified,
            contentDescription = "Verified",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        "pending" -> Icon(
            Icons.Filled.Schedule,
            contentDescription = "Pending verification",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
        "neutral" -> Icon(
            Icons.Filled.Help,
            contentDescription = "Unverified",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp)
        )
    }
}
