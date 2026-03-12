package com.kana.phone

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.kana.phone.theme.KanaPhoneTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* continue regardless */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val initialQuiz = if (intent.getStringExtra("nav_route") == "quiz") {
            QuizItem(
                character = intent.getStringExtra("character") ?: "",
                romaji = intent.getStringExtra("romaji") ?: "",
                type = intent.getStringExtra("type") ?: "",
                reading = intent.getStringExtra("reading") ?: "",
                audioUrl = intent.getStringExtra("audio_url") ?: "",
                imageUrl = intent.getStringExtra("image_url") ?: ""
            )
        } else null

        setContent {
            KanaPhoneTheme {
                AppContent(this, initialQuiz)
            }
        }
    }
}

data class QuizItem(
    val character: String,
    val romaji: String,
    val type: String,
    val reading: String,
    val audioUrl: String,
    val imageUrl: String = ""
)

fun getRandomQuizItem(context: android.content.Context): QuizItem? {
    val words = WordStorage.getEnabledWords(context)
    if (words.isEmpty()) return null
    val item = words.random()
    return QuizItem(item.question, item.answer, "WORD", item.reading, item.audioUrl, item.imageUrl)
}

enum class Screen { LOGIN, REGISTER, HOME, QUIZ, SETTINGS, PACK_LIST, PACK_EDITOR, BROWSE }

@Composable
fun AppContent(context: android.content.Context, initialQuiz: QuizItem?) {
    val startScreen = when {
        !AppSettings.isLoggedIn(context) -> Screen.LOGIN
        initialQuiz != null -> Screen.QUIZ
        else -> Screen.HOME
    }
    var currentScreen by remember { mutableStateOf(startScreen) }
    var quizItem by remember { mutableStateOf(initialQuiz) }
    var editToken by remember { mutableStateOf<String?>(null) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var updateAvailable by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        PackUpdater.checkForUpdates(context) { syncedNames ->
            syncMessage = "Synced: ${syncedNames.joinToString(", ")}"
        }
        // Push enabled packs to server for watch sync
        if (AppSettings.isLoggedIn(context)) {
            ApiClient.pushWatchSyncPacks(context, AppSettings.getEnabledPacks(context))
        }
        // Check for app updates
        val currentVersion = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (_: Exception) { null }
        if (currentVersion != null) {
            ApiClient.checkVersion(context) { latestPhone, _ ->
                if (latestPhone != null && latestPhone != currentVersion) {
                    updateAvailable = latestPhone
                }
            }
        }
    }

    when (currentScreen) {
        Screen.LOGIN -> LoginScreen(
            onLoggedIn = { currentScreen = Screen.HOME },
            onGoToRegister = { currentScreen = Screen.REGISTER }
        )
        Screen.REGISTER -> RegisterScreen(
            onRegistered = { currentScreen = Screen.HOME },
            onGoToLogin = { currentScreen = Screen.LOGIN }
        )
        Screen.HOME -> HomeScreen(
            context = context,
            syncMessage = syncMessage,
            onSyncMessageDismissed = { syncMessage = null },
            updateAvailable = updateAvailable,
            onStartQuiz = {
                val item = getRandomQuizItem(context)
                if (item != null) {
                    quizItem = item
                    currentScreen = Screen.QUIZ
                } else {
                    Toast.makeText(context, "Nothing enabled! Check Settings.", Toast.LENGTH_SHORT).show()
                }
            },
            onPacks = { currentScreen = Screen.PACK_LIST },
            onBrowse = { currentScreen = Screen.BROWSE },
            onSettings = { currentScreen = Screen.SETTINGS }
        )
        Screen.QUIZ -> {
            val item = quizItem
            if (item != null) {
                QuizScreen(
                    character = item.character,
                    romaji = item.romaji,
                    type = item.type,
                    reading = item.reading,
                    audioUrl = item.audioUrl,
                    imageUrl = item.imageUrl,
                    onNext = {
                        val next = getRandomQuizItem(context)
                        if (next != null) {
                            quizItem = next
                        }
                    },
                    onBack = { currentScreen = Screen.HOME }
                )
            }
        }
        Screen.SETTINGS -> SettingsScreen(
            onBack = { currentScreen = Screen.HOME },
            onLogout = {
                AppSettings.logout(context)
                currentScreen = Screen.LOGIN
            },
            context = context
        )
        Screen.PACK_LIST -> PackListScreen(
            onBack = { currentScreen = Screen.HOME },
            onCreatePack = {
                editToken = null
                currentScreen = Screen.PACK_EDITOR
            },
            onEditPack = { token ->
                editToken = token
                currentScreen = Screen.PACK_EDITOR
            },
            onDownloadPack = { token ->
                PackUpdater.downloadPack(context, token) { _, message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            },
            context = context
        )
        Screen.BROWSE -> BrowsePacksScreen(
            onBack = { currentScreen = Screen.HOME },
            context = context
        )
        Screen.PACK_EDITOR -> PackEditorScreen(
            editToken = editToken,
            onBack = { currentScreen = Screen.PACK_LIST },
            context = context
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    context: android.content.Context,
    syncMessage: String? = null,
    onSyncMessageDismissed: () -> Unit = {},
    updateAvailable: String? = null,
    onStartQuiz: () -> Unit,
    onPacks: () -> Unit,
    onBrowse: () -> Unit,
    onSettings: () -> Unit
) {
    var isActive by remember { mutableStateOf(AppSettings.isNotificationsActive(context)) }
    val intervalMinutes = remember { AppSettings.getIntervalMinutes(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(syncMessage) {
        if (syncMessage != null) {
            snackbarHostState.showSnackbar(syncMessage)
            onSyncMessageDismissed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Language Learning", fontWeight = FontWeight.Bold)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (updateAvailable != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Update available",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Version $updateAvailable is available",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        FilledTonalButton(
                            onClick = {
                                try {
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse("market://details?id=${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Update", fontSize = 13.sp)
                        }
                    }
                }
            }
            Button(
                onClick = onStartQuiz,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Start Quiz", fontSize = 18.sp, modifier = Modifier.padding(8.dp))
            }

            Button(
                onClick = onPacks,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("My Packs", fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text("Create & edit", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }

            Button(
                onClick = onBrowse,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Browse Packs", fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text("Public packs from others", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }

            Button(
                onClick = {
                    isActive = !isActive
                    if (isActive) {
                        NotificationScheduler.schedule(context, AppSettings.getIntervalMinutes(context))
                    } else {
                        NotificationScheduler.cancel(context)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActive)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (isActive) "Stop Reminders" else "Start Reminders",
                        fontSize = 16.sp, modifier = Modifier.padding(4.dp)
                    )
                    Text(
                        if (isActive) "Every $intervalMinutes min" else "Tap to enable",
                        fontSize = 12.sp
                    )
                }
            }

            OutlinedButton(
                onClick = onSettings,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Settings", fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun QuizScreen(
    character: String,
    romaji: String,
    type: String,
    reading: String,
    audioUrl: String,
    imageUrl: String = "",
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val hasAudio = audioUrl.isNotBlank() && AudioCache.getCachedFile(context, audioUrl) != null
    val imageFile = if (imageUrl.isNotBlank()) ImageCache.getCachedFile(context, imageUrl) else null
    var revealed by remember(character) { mutableStateOf(false) }
    var hintShown by remember(character) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (imageFile != null) {
                val bitmap = remember(imageUrl) {
                    android.graphics.BitmapFactory.decodeFile(imageFile.absolutePath)
                }
                if (bitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Question image",
                        modifier = Modifier
                            .sizeIn(maxWidth = 250.dp, maxHeight = 250.dp)
                            .then(
                                if (hasAudio) Modifier
                                    .border(2.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(12.dp))
                                    .clickable { AudioCache.play(context, audioUrl) }
                                    .padding(4.dp)
                                else Modifier
                            ),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                }
                if (character.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = character,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = character,
                    fontSize = if (character.length > 4) 48.sp else 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = if (hasAudio) Modifier
                        .border(2.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(12.dp))
                        .clickable { AudioCache.play(context, audioUrl) }
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                    else Modifier
                )
            }

            if (reading.isNotBlank() && (hintShown || revealed)) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = reading,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!revealed) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (reading.isNotBlank() && !hintShown) {
                        OutlinedButton(onClick = { hintShown = true }, shape = RoundedCornerShape(8.dp)) {
                            Text("Hint")
                        }
                    }
                    Button(
                        onClick = { revealed = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Show Answer")
                    }
                }
            } else {
                Text(
                    text = romaji,
                    fontSize = if (romaji.length > 8) 32.sp else 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onNext,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Next")
                }
            }
        }

        TextButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Text("\u2190 Back", color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onLogout: () -> Unit, context: android.content.Context) {
    val presets = listOf(5, 10, 15, 20, 30, 45, 60, 90, 120)
    var selectedIndex by remember {
        mutableStateOf(presets.indexOf(AppSettings.getIntervalMinutes(context)).coerceAtLeast(0))
    }
    var packs by remember { mutableStateOf(WordStorage.loadAllPacks(context)) }
    var enabledPacks by remember { mutableStateOf(AppSettings.getEnabledPacks(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Notification Interval",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilledTonalButton(
                        shape = RoundedCornerShape(8.dp),
                        onClick = {
                            if (selectedIndex > 0) {
                                selectedIndex--
                                AppSettings.setIntervalMinutes(context, presets[selectedIndex])
                                if (AppSettings.isNotificationsActive(context)) {
                                    NotificationScheduler.schedule(context, presets[selectedIndex])
                                }
                            }
                        }
                    ) { Text("\u25c0", fontSize = 18.sp) }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Text(
                            "${presets[selectedIndex]}",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "minutes",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    FilledTonalButton(
                        shape = RoundedCornerShape(8.dp),
                        onClick = {
                            if (selectedIndex < presets.size - 1) {
                                selectedIndex++
                                AppSettings.setIntervalMinutes(context, presets[selectedIndex])
                                if (AppSettings.isNotificationsActive(context)) {
                                    NotificationScheduler.schedule(context, presets[selectedIndex])
                                }
                            }
                        }
                    ) { Text("\u25b6", fontSize = 18.sp) }
                }
            }

            if (packs.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Word Packs",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                items(packs, key = { it.token }) { pack ->
                    val isEnabled = pack.token in enabledPacks

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pack.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${pack.words.size} words \u2022 ${pack.updated}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                Switch(
                                    checked = isEnabled,
                                    onCheckedChange = { checked ->
                                        val current = enabledPacks.toMutableSet()
                                        if (checked) current.add(pack.token) else current.remove(pack.token)
                                        AppSettings.setEnabledPacks(context, current)
                                        enabledPacks = current
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(
                                    shape = RoundedCornerShape(8.dp),
                                    onClick = {
                                        PackUpdater.updatePack(context, pack.token) { _, message ->
                                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                            packs = WordStorage.loadAllPacks(context)
                                        }
                                    }
                                ) { Text("Update") }

                                Button(
                                    onClick = {
                                        WordStorage.deletePack(context, pack.token)

                                        packs = WordStorage.loadAllPacks(context)
                                        enabledPacks = AppSettings.getEnabledPacks(context)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) { Text("Delete") }
                            }
                        }
                    }
                }
            }

            // Watch Pairing
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Watch Sync",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            item {
                var pairCode by remember { mutableStateOf<String?>(null) }
                var generating by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (pairCode != null) {
                            Text(
                                "Enter this code on your watch",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                pairCode!!,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 8.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Expires in 5 minutes",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(onClick = { pairCode = null }, shape = RoundedCornerShape(8.dp)) {
                                Text("Done")
                            }
                        } else {
                            Text(
                                "Pair your watch to sync packs automatically",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    generating = true
                                    ApiClient.requestPairCode(context) { success, code ->
                                        generating = false
                                        if (success) {
                                            pairCode = code
                                        } else {
                                            Toast.makeText(context, "Failed: $code", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                enabled = !generating,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(if (generating) "Generating..." else "Generate Pair Code")
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                val userEmail = AppSettings.getUserEmail(context) ?: ""
                val friendCode = AppSettings.getFriendCode(context) ?: ""
                if (userEmail.isNotBlank()) {
                    Text(
                        "Signed in as $userEmail",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    if (friendCode.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Friend Code:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            Text(
                                friendCode,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 2.sp,
                                modifier = Modifier.clickable {
                                    clipboardManager.setPrimaryClip(android.content.ClipData.newPlainText("Friend Code", friendCode))
                                    Toast.makeText(context, "Friend code copied!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        Text(
                            "Share this code so friends can collaborate on your packs",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Logout")
                }
            }

            item {
                val versionName = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                } catch (_: Exception) { "?" }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Version $versionName",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
