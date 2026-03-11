package com.kana.phone

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    var questionLang by remember { mutableStateOf("") }
    var answerLang by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(editToken != null) }
    var saving by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var showImport by remember { mutableStateOf(false) }
    var savedToken by remember { mutableStateOf(editToken) }
    var collaborators by remember { mutableStateOf<List<Collaborator>>(emptyList()) }
    var friendCodeInput by remember { mutableStateOf("") }
    var addingFriend by remember { mutableStateOf(false) }

    LaunchedEffect(editToken) {
        if (editToken != null) {
            ApiClient.fetchPackForEdit(editToken, context) { success, pack ->
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
            ApiClient.fetchCollaborators(editToken, context) { _, result ->
                collaborators = result
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

            // Language selectors
            item {
                val languages = listOf("", "Japanese", "English", "Spanish", "French", "German", "Korean", "Chinese", "Russian", "Portuguese", "Italian", "Arabic", "Hindi", "Turkish", "Vietnamese", "Thai", "Indonesian", "Dutch", "Polish", "Swedish", "Other")
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
                            ExposedDropdownMenuBox(
                                expanded = qExpanded,
                                onExpandedChange = { qExpanded = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = questionLang.ifBlank { "Not set" },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Questions") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = qExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    singleLine = true,
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
                                            text = { Text(lang.ifBlank { "Not set" }) },
                                            onClick = {
                                                questionLang = lang
                                                qExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Answer language
                            var aExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = aExpanded,
                                onExpandedChange = { aExpanded = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = answerLang.ifBlank { "Not set" },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Answers") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = aExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    singleLine = true,
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
                                            text = { Text(lang.ifBlank { "Not set" }) },
                                            onClick = {
                                                answerLang = lang
                                                aExpanded = false
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
            val collabToken = savedToken
            if (collabToken != null) {
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
                                                ApiClient.removeCollaborator(collabToken, collab.id, context) { success ->
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
                                            ApiClient.addCollaborator(collabToken, friendCodeInput, context) { success, message ->
                                                addingFriend = false
                                                if (success) {
                                                    friendCodeInput = ""
                                                    ApiClient.fetchCollaborators(collabToken, context) { _, result ->
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
                                ApiClient.savePack(savedToken!!, packName, validWords, context, isPublic, tags, questionLang, answerLang) { success, msg ->
                                    if (success) {
                                        // Re-download pack locally so quiz uses updated audio
                                        PackUpdater.updatePack(context, savedToken!!) { _, _ -> }
                                    }
                                    saving = false
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                ApiClient.createPack(packName, validWords, context, isPublic, tags, questionLang, answerLang) { success, result ->
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
