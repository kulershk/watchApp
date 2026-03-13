package com.kana.phone

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
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

enum class Screen { LOGIN, REGISTER, QUIZ, SETTINGS, PACK_LIST, PACK_EDITOR, BROWSE }
enum class Tab { QUIZ, PACKS, BROWSE, SETTINGS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContent(context: android.content.Context, initialQuiz: QuizItem?) {
    val startScreen = when {
        !AppSettings.isLoggedIn(context) -> Screen.LOGIN
        initialQuiz != null -> Screen.QUIZ
        else -> Screen.QUIZ
    }
    var currentScreen by remember { mutableStateOf(startScreen) }
    var selectedTab by remember { mutableStateOf(Tab.QUIZ) }
    var quizItem by remember { mutableStateOf(initialQuiz ?: getRandomQuizItem(context)) }
    var editId by remember { mutableStateOf<String?>(null) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var updateAvailable by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var lastVersionCheck by remember { mutableStateOf(0L) }

    fun checkVersionUpdate() {
        val now = System.currentTimeMillis()
        if (now - lastVersionCheck < 5 * 60 * 1000) return // 5 min cooldown
        lastVersionCheck = now
        val currentVersion = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (_: Exception) { null }
        if (currentVersion != null) {
            ApiClient.checkVersion(context) { latestPhone, _ ->
                if (latestPhone != null && latestPhone != currentVersion) {
                    updateAvailable = latestPhone
                } else {
                    updateAvailable = null
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        PackUpdater.checkForUpdates(context) { syncedNames ->
            syncMessage = "Synced: ${syncedNames.joinToString(", ")}"
        }
        if (AppSettings.isLoggedIn(context)) {
            ApiClient.pushWatchSyncPacks(context, AppSettings.getEnabledPacks(context))
        }
        checkVersionUpdate()
    }

    LaunchedEffect(selectedTab) {
        checkVersionUpdate()
        if (selectedTab == Tab.QUIZ && quizItem == null) {
            quizItem = getRandomQuizItem(context)
        }
    }

    LaunchedEffect(syncMessage) {
        if (syncMessage != null) {
            snackbarHostState.showSnackbar(syncMessage!!)
            syncMessage = null
        }
    }

    // Full-screen flows without bottom nav
    when (currentScreen) {
        Screen.LOGIN -> {
            BackHandler { (context as? ComponentActivity)?.moveTaskToBack(true) }
            LoginScreen(
                onLoggedIn = { currentScreen = Screen.QUIZ },
                onGoToRegister = { currentScreen = Screen.REGISTER }
            )
            return
        }
        Screen.REGISTER -> {
            BackHandler { currentScreen = Screen.LOGIN }
            RegisterScreen(
                onRegistered = { currentScreen = Screen.QUIZ },
                onGoToLogin = { currentScreen = Screen.LOGIN }
            )
            return
        }
        Screen.PACK_EDITOR -> {
            BackHandler { currentScreen = Screen.PACK_LIST; selectedTab = Tab.PACKS }
            PackEditorScreen(
                editId = editId,
                onBack = { currentScreen = Screen.PACK_LIST; selectedTab = Tab.PACKS },
                context = context
            )
            return
        }
        else -> {}
    }

    // Main app with bottom navigation
    BackHandler { (context as? ComponentActivity)?.moveTaskToBack(true) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            val navBarColor = MaterialTheme.colorScheme.surface
            val navColors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                indicatorColor = navBarColor
            )
            NavigationBar(
                containerColor = navBarColor,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == Tab.QUIZ,
                    onClick = {
                        selectedTab = Tab.QUIZ
                        currentScreen = Screen.QUIZ
                        if (quizItem == null) quizItem = getRandomQuizItem(context)
                    },
                    icon = { Icon(Icons.Filled.PlayArrow, contentDescription = "Quiz") },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = selectedTab == Tab.PACKS,
                    onClick = { selectedTab = Tab.PACKS; currentScreen = Screen.PACK_LIST },
                    icon = { Icon(Icons.Filled.FolderOpen, contentDescription = "My Packs") },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = selectedTab == Tab.BROWSE,
                    onClick = { selectedTab = Tab.BROWSE; currentScreen = Screen.BROWSE },
                    icon = { Icon(Icons.Filled.Explore, contentDescription = "Browse") },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = selectedTab == Tab.SETTINGS,
                    onClick = { selectedTab = Tab.SETTINGS; currentScreen = Screen.SETTINGS },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                    colors = navColors
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (updateAvailable != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
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
                                "Version $updateAvailable",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        Button(
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
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Update", fontSize = 13.sp)
                        }
                    }
                }
            }
            Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                Tab.QUIZ -> {
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
                            onBack = { (context as? ComponentActivity)?.moveTaskToBack(true) }
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No packs enabled", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { selectedTab = Tab.PACKS; currentScreen = Screen.PACK_LIST }) {
                                Text("Go to My Packs")
                            }
                        }
                    }
                }
                Tab.PACKS -> PackListScreen(
                    onBack = { (context as? ComponentActivity)?.moveTaskToBack(true) },
                    onCreatePack = {
                        editId = null
                        currentScreen = Screen.PACK_EDITOR
                    },
                    onEditPack = { id ->
                        editId = id
                        currentScreen = Screen.PACK_EDITOR
                    },
                    context = context
                )
                Tab.BROWSE -> BrowsePacksScreen(
                    onBack = { (context as? ComponentActivity)?.moveTaskToBack(true) },
                    context = context
                )
                Tab.SETTINGS -> SettingsScreen(
                    onBack = { (context as? ComponentActivity)?.moveTaskToBack(true) },
                    onLogout = {
                        AppSettings.logout(context)
                        currentScreen = Screen.LOGIN
                    },
                    context = context
                )
            }
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
                    val opts = android.graphics.BitmapFactory.Options().apply {
                        inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                    }
                    android.graphics.BitmapFactory.decodeFile(imageFile.absolutePath, opts)
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
                    modifier = Modifier.fillMaxWidth().then(
                        if (hasAudio) Modifier
                            .border(2.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(12.dp))
                            .clickable { AudioCache.play(context, audioUrl) }
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                        else Modifier
                    )
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
                    IconButton(
                        onClick = { revealed = true },
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            Icons.Filled.MenuBook,
                            contentDescription = "Show explanation",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
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
                    Icon(Icons.Filled.ArrowForward, contentDescription = "Next")
                }
            }
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
    var isActive by remember { mutableStateOf(AppSettings.isNotificationsActive(context)) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Reminders
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Reminders",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        item {
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
                        if (isActive) "Every ${presets[selectedIndex]} min" else "Tap to enable",
                        fontSize = 12.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
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
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                if (BuildConfig.DEBUG) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "DEV MODE — ${BuildConfig.API_BASE}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

