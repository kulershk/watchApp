package com.kana.phone

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackEditorScreen(
    editId: String?,
    onBack: () -> Unit,
    context: android.content.Context
) {
    var packName by remember { mutableStateOf("") }
    var words by remember { mutableStateOf(mutableListOf(EditWord())) }
    var isPublic by remember { mutableStateOf(false) }
    var tags by remember { mutableStateOf("") }
    var questionLang by remember { mutableStateOf("") }
    var answerLang by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(editId != null) }
    var saving by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var showImport by remember { mutableStateOf(false) }
    var savedId by remember { mutableStateOf(editId) }
    var collaborators by remember { mutableStateOf<List<Collaborator>>(emptyList()) }
    var friendCodeInput by remember { mutableStateOf("") }
    var addingFriend by remember { mutableStateOf(false) }

    LaunchedEffect(editId) {
        if (editId != null) {
            ApiClient.fetchPackForEdit(editId, context) { success, pack ->
                if (success && pack != null) {
                    packName = pack.name
                    isPublic = pack.isPublic
                    tags = pack.tags
                    questionLang = pack.questionLang
                    answerLang = pack.answerLang
                    words = pack.words.toMutableList().ifEmpty { mutableListOf(EditWord()) }
                } else {
                    Toast.makeText(context, "Failed to load pack", Toast.LENGTH_SHORT).show()
                }
                loading = false
            }
            ApiClient.fetchCollaborators(editId, context) { _, result ->
                collaborators = result
            }
        }
    }

    var showLeaveConfirm by remember { mutableStateOf(false) }

    BackHandler { showLeaveConfirm = true }

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text("Leave without saving?") },
            text = { Text("Any unsaved changes will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    showLeaveConfirm = false
                    onBack()
                }) { Text("Leave") }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) { Text("Stay") }
            }
        )
    }

    Scaffold(
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

            // Language selectors
            item {
                val languages = languageList
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Languages", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "What languages are used in this pack",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Question language
                            var qExpanded by remember { mutableStateOf(false) }
                            var qSearch by remember { mutableStateOf("") }
                            val qFiltered = if (qSearch.isBlank()) languages else languages.filter {
                                langWithFlag(it).contains(qSearch, ignoreCase = true) || it.contains(qSearch, ignoreCase = true)
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = langWithFlag(questionLang).ifBlank { "Not set" },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Cards") },
                                    trailingIcon = {
                                        Icon(if (qExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown, contentDescription = null)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
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
                                            text = { Text(langWithFlag(lang).ifBlank { "Not set" }) },
                                            onClick = {
                                                questionLang = lang
                                                qExpanded = false
                                                qSearch = ""
                                            }
                                        )
                                    }
                                }
                            }

                            // Answer language
                            var aExpanded by remember { mutableStateOf(false) }
                            var aSearch by remember { mutableStateOf("") }
                            val aFiltered = if (aSearch.isBlank()) languages else languages.filter {
                                langWithFlag(it).contains(aSearch, ignoreCase = true) || it.contains(aSearch, ignoreCase = true)
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = langWithFlag(answerLang).ifBlank { "Not set" },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Explanations") },
                                    trailingIcon = {
                                        Icon(if (aExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown, contentDescription = null)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
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
                                            text = { Text(langWithFlag(lang).ifBlank { "Not set" }) },
                                            onClick = {
                                                answerLang = lang
                                                aExpanded = false
                                                aSearch = ""
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
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

            // Collaborators (only for saved packs)
            val collabId = savedId
            if (collabId != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Collaborators",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (collaborators.isEmpty()) {
                                Text(
                                    "No collaborators yet",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            } else {
                                collaborators.forEach { collab ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(collab.displayName, fontSize = 14.sp)
                                            Text(
                                                collab.friendCode,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                ApiClient.removeCollaborator(collabId, collab.id, context) { success ->
                                                    if (success) {
                                                        collaborators = collaborators.filter { it.id != collab.id }
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) { Text("Remove", fontSize = 12.sp) }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = friendCodeInput,
                                    onValueChange = { friendCodeInput = it.uppercase().take(6) },
                                    label = { Text("Friend code") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        cursorColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Button(
                                    onClick = {
                                        if (friendCodeInput.length == 6) {
                                            addingFriend = true
                                            ApiClient.addCollaborator(collabId, friendCodeInput, context) { success, message ->
                                                addingFriend = false
                                                if (success) {
                                                    friendCodeInput = ""
                                                    ApiClient.fetchCollaborators(collabId, context) { _, result ->
                                                        collaborators = result
                                                    }
                                                }
                                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    enabled = friendCodeInput.length == 6 && !addingFriend,
                                    shape = RoundedCornerShape(8.dp)
                                ) { Text(if (addingFriend) "..." else "Add") }
                            }
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
                                "One word per line: card|explanation|hint",
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
                var showDeleteConfirm by remember { mutableStateOf(false) }
                val dismissState = rememberDismissState(
                    confirmValueChange = { dismissValue ->
                        if (dismissValue == DismissValue.DismissedToStart) {
                            showDeleteConfirm = true
                            false // Don't actually dismiss, wait for confirmation
                        } else false
                    }
                )

                if (showDeleteConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirm = false },
                        title = { Text("Delete card?") },
                        text = { Text("Are you sure you want to delete card #${index + 1}?") },
                        confirmButton = {
                            TextButton(onClick = {
                                showDeleteConfirm = false
                                val updated = words.toMutableList()
                                updated.removeAt(index)
                                if (updated.isEmpty()) updated.add(EditWord())
                                words = updated
                            }) { Text("Delete") }
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
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (word.enabled)
                            MaterialTheme.colorScheme.surface
                        else
                            MaterialTheme.colorScheme.surfaceVariant
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
                            label = { Text("Card") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                cursorColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Image upload for question
                        WordImageControls(
                            image = word.image,
                            index = index,
                            context = context,
                            onImageChanged = { newImage ->
                                val updated = words.toMutableList()
                                updated[index] = updated[index].copy(image = newImage)
                                words = updated
                            }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedTextField(
                            value = word.answer,
                            onValueChange = {
                                val updated = words.toMutableList()
                                updated[index] = updated[index].copy(answer = it)
                                words = updated
                            },
                            label = { Text("Explanation") },
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
                            label = { Text("Hint") },
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

                    }
                }
                    }
                )
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
                            val validWords = words.filter { (it.question.isNotBlank() || it.image.isNotBlank()) && it.answer.isNotBlank() }
                            if (validWords.isEmpty()) {
                                Toast.makeText(context, "Add at least one word", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            saving = true
                            if (savedId != null) {
                                ApiClient.savePack(savedId!!, packName, validWords, context, isPublic, tags, questionLang, answerLang) { success, msg ->
                                    if (success) {
                                        PackUpdater.updatePack(context, savedId!!) { _, _ -> }
                                        onBack()
                                    }
                                    saving = false
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                ApiClient.createPack(packName, validWords, context, isPublic, tags, questionLang, answerLang) { success, result ->
                                    saving = false
                                    if (success) {
                                        savedId = result
                                        PackUpdater.downloadPack(context, result) { _, _ -> }
                                        Toast.makeText(context, "Created!", Toast.LENGTH_SHORT).show()
                                        onBack()
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
                                savedId != null -> "Save"
                                else -> "Create"
                            },
                            fontSize = 16.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    if (savedId != null) {
                        Button(
                            onClick = {
                                ApiClient.deletePack(savedId!!, context) { success, msg ->
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

@Composable
fun WordImageControls(
    image: String,
    index: Int,
    context: android.content.Context,
    onImageChanged: (String) -> Unit
) {
    var uploading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        uploading = true
        try {
            // Decode and resize if needed (max 800px)
            val inputStream = context.contentResolver.openInputStream(uri)
            val decodeOpts = android.graphics.BitmapFactory.Options().apply {
                inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
            }
            val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream, null, decodeOpts)
            inputStream?.close()
            if (originalBitmap == null) {
                uploading = false
                Toast.makeText(context, "Failed to read image", Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }
            val maxDim = 800
            val bmp = if (originalBitmap.width > maxDim || originalBitmap.height > maxDim) {
                val scale = minOf(maxDim.toFloat() / originalBitmap.width, maxDim.toFloat() / originalBitmap.height)
                val w = (originalBitmap.width * scale).toInt()
                val h = (originalBitmap.height * scale).toInt()
                android.graphics.Bitmap.createScaledBitmap(originalBitmap, w, h, true)
            } else originalBitmap
            val hasAlpha = bmp.hasAlpha()
            val ext = if (hasAlpha) "png" else "jpg"
            val tempFile = File(context.cacheDir, "upload_img_$index.$ext")
            tempFile.outputStream().use { out ->
                if (hasAlpha) {
                    bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                } else {
                    bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
                }
            }
            if (bmp !== originalBitmap) bmp.recycle()
            originalBitmap.recycle()
            ApiClient.uploadImage(tempFile, context) { success, result ->
                uploading = false
                if (success) {
                    onImageChanged(result)
                } else {
                    Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
                }
                tempFile.delete()
            }
        } catch (e: Exception) {
            uploading = false
            Toast.makeText(context, "Failed to read image", Toast.LENGTH_SHORT).show()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (image.isNotBlank()) {
            // Show thumbnail preview
            val cachedFile = ImageCache.getCachedFile(context, image)
            if (cachedFile != null) {
                val bitmap = remember(image) {
                    android.graphics.BitmapFactory.decodeFile(cachedFile.absolutePath)
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Question image",
                        modifier = Modifier.size(48.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                Text(
                    "img: ${image.take(12)}...",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            TextButton(
                onClick = {
                    ApiClient.deleteImage(image, context)
                    onImageChanged("")
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) { Text("Remove", fontSize = 12.sp) }
        }

        OutlinedButton(
            onClick = { launcher.launch("image/*") },
            enabled = !uploading,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                when {
                    uploading -> "Uploading..."
                    image.isNotBlank() -> "Change Image"
                    else -> "Add Image"
                },
                fontSize = 12.sp
            )
        }
    }
}
