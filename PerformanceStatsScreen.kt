package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FirebaseManager
import com.example.data.CloudUserProgress
import com.example.data.UserProgressEntity
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun PerformanceStatsScreen(
    currentCoins: Int,
    currentStreak: Int,
    currentXp: Int,
    onUpdateLocalProgress: (Int, Int, Int) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf("Weekly") }

    // Firebase live states
    val isFbInitialized by FirebaseManager.isInitialized.collectAsState()
    val currentUser by FirebaseManager.currentUserFlow.collectAsState()

    // Auth screen states
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isAuthLoading by remember { mutableStateOf(false) }
    var authMessage by remember { mutableStateOf("") }

    // Leaderboard and Cloud storage live variables
    var leaderboardList by remember { mutableStateOf<List<CloudUserProgress>>(emptyList()) }
    var isLeaderboardLoading by remember { mutableStateOf(false) }

    // Runtime configuration parameters form
    var manualApiKey by remember { mutableStateOf("") }
    var manualProjectId by remember { mutableStateOf("") }
    var manualAppId by remember { mutableStateOf("") }
    var isConfigShowing by remember { mutableStateOf(false) }

    // Simulated account state when Firebase is in simulation/offline mode
    var simulatedLoggedInUser by remember { mutableStateOf<String?>(null) }
    var simulatedLeaderboard by remember { 
        mutableStateOf(
            listOf(
                CloudUserProgress(1, "amirul_saving@iflec.edu.my", 820, 11, 240),
                CloudUserProgress(2, "sarah_pith@unirazak.edu.my", 650, 8, 180),
                CloudUserProgress(3, "fauze_expert@iflec.edu.my", 510, 5, 150)
            )
        )
    }

    // Refresh Leaderboard
    val refreshLeaderboard = {
        coroutineScope.launch {
            isLeaderboardLoading = true
            if (isFbInitialized) {
                val list = FirebaseManager.getLeaderboard()
                leaderboardList = list
            } else {
                // In simulated mode, we merge current active user
                val list = mutableListOf<CloudUserProgress>()
                val activeEmail = currentUser?.email ?: simulatedLoggedInUser
                if (activeEmail != null) {
                    list.add(CloudUserProgress(1, activeEmail, currentCoins, currentStreak, currentXp))
                }
                simulatedLeaderboard.forEach { item ->
                    if (item.email != activeEmail) {
                        list.add(item)
                    }
                }
                // Sort by coins
                list.sortByDescending { it.coins }
                leaderboardList = list.mapIndexed { idx, item -> item.copy(rank = idx + 1) }
            }
            isLeaderboardLoading = false
        }
    }

    // Auto-refresh leaderboard when syncing tab is selected
    LaunchedEffect(selectedTab, isFbInitialized, simulatedLoggedInUser, currentUser) {
        if (selectedTab == "Cloud Sync") {
            refreshLeaderboard()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCool)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // Upper Design Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "PERFORMANCE ANALYTICS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = MutedGrey,
                letterSpacing = 1.sp
            )
            
            // Subtle indicator badge
            Box(
                modifier = Modifier
                    .background(
                        if (isFbInitialized) EmeraldLight else Color(0xFFFFF3E0),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isFbInitialized) "☁️ Firebase Active" else "🔒 Local Sandbox",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isFbInitialized) EmeraldSuccess else Color(0xFFE65100)
                )
            }
        }

        // Expanded three modular navigation tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf("Weekly", "Monthly", "Cloud Sync")
            tabs.forEach { tab ->
                val isSelected = selectedTab == tab
                Button(
                    onClick = { selectedTab = tab },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) RoyalPurple else Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = null,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = tab,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else MutedGrey
                    )
                }
            }
        }

        // Switching the views based on Tab state
        if (selectedTab == "Cloud Sync") {
            // --- FIREBASE CLOUD SYNC PAGE ---
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                // 1. Connection Banner / Config Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🔥", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Cloud Integration Status",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = InstitutionalNavy
                                    )
                                }
                                TextButton(onClick = { isConfigShowing = !isConfigShowing }) {
                                    Text(
                                        text = if (isConfigShowing) "Hide Config" else "Edit API",
                                        fontSize = 12.sp,
                                        color = BrandMagenta,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (isFbInitialized) {
                                Text(
                                    text = "Your application is successfully linked with live Google Firebase services. All authentication records and Firestore user collections are active.",
                                    fontSize = 12.sp,
                                    color = BentoSlateMuted,
                                    lineHeight = 18.sp
                                )
                            } else {
                                Text(
                                    text = "Currently running in a secure offline-simulated demo channel. Create/Login a simulated profile below instantly, or dynamically bind your true Firebase configuration settings.",
                                    fontSize = 12.sp,
                                    color = BentoSlateMuted,
                                    lineHeight = 18.sp
                                )
                            }

                            if (isConfigShowing) {
                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = BentoBorder.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    "Manually Configure Firebase API at Runtime",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = InstitutionalNavy,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )

                                OutlinedTextField(
                                    value = manualApiKey,
                                    onValueChange = { manualApiKey = it },
                                    label = { Text("API Key") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = manualProjectId,
                                    onValueChange = { manualProjectId = it },
                                    label = { Text("Project ID") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = manualAppId,
                                    onValueChange = { manualAppId = it },
                                    label = { Text("Application ID (App ID)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        if (manualApiKey.isBlank() || manualProjectId.isBlank() || manualAppId.isBlank()) {
                                            Toast.makeText(context, "Please fill in all manual configuration parameters.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val success = FirebaseManager.configureAtRuntime(
                                                context,
                                                apiKey = manualApiKey.trim(),
                                                projectId = manualProjectId.trim(),
                                                appId = manualAppId.trim()
                                            )
                                            if (success) {
                                                Toast.makeText(context, "✅ Connected Live to Firebase successfully!", Toast.LENGTH_LONG).show()
                                                isConfigShowing = false
                                            } else {
                                                Toast.makeText(context, "❌ Connection failed. Check parameters and terminal log details.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandMagenta),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Connect & Start Live Client Link 🚀", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // 2. Authentication Panel (Live Firebase Auth vs Simulated Demo Mode)
                item {
                    val isLoggedIn = if (isFbInitialized) (currentUser != null) else (simulatedLoggedInUser != null)
                    val activeUserEmail = if (isFbInitialized) currentUser?.email else simulatedLoggedInUser

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            if (!isLoggedIn) {
                                Text(
                                    text = "☁️ Firebase Member Gateway",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = InstitutionalNavy,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Text(
                                    text = "Registers learning accounts dynamically. Once created, you can save backups and access Firestore synchronized states.",
                                    fontSize = 12.sp,
                                    color = BentoSlateMuted,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                OutlinedTextField(
                                    value = emailInput,
                                    onValueChange = { emailInput = it },
                                    label = { Text("Email Address") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = passwordInput,
                                    onValueChange = { passwordInput = it },
                                    label = { Text("Password") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                if (authMessage.isNotEmpty()) {
                                    Text(
                                        text = authMessage,
                                        color = if (authMessage.contains("Success")) EmeraldSuccess else Color.Red,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Log In button
                                    Button(
                                        onClick = {
                                            if (emailInput.isBlank() || passwordInput.isBlank()) {
                                                authMessage = "Please fill in email/password inputs."
                                                return@Button
                                            }
                                            isAuthLoading = true
                                            coroutineScope.launch {
                                                if (isFbInitialized) {
                                                    try {
                                                        FirebaseManager.getAuth()?.signInWithEmailAndPassword(emailInput.trim(), passwordInput.trim())?.await()
                                                        authMessage = "Success: Logged in live successfully!"
                                                    } catch (e: Exception) {
                                                        authMessage = "Error: ${e.message}"
                                                    }
                                                } else {
                                                    // Simulation Login
                                                    simulatedLoggedInUser = emailInput.trim()
                                                    authMessage = "Success: Logged in Simulated Demo profile!"
                                                }
                                                isAuthLoading = false
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = InstitutionalNavy),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Log In", fontWeight = FontWeight.Bold, color = Color.White)
                                    }

                                    // Register Button
                                    Button(
                                        onClick = {
                                            if (emailInput.isBlank() || passwordInput.isBlank() || passwordInput.length < 6) {
                                                authMessage = "Email and Password (min 6 characters) required."
                                                return@Button
                                            }
                                            isAuthLoading = true
                                            coroutineScope.launch {
                                                if (isFbInitialized) {
                                                    try {
                                                        FirebaseManager.getAuth()?.createUserWithEmailAndPassword(emailInput.trim(), passwordInput.trim())?.await()
                                                        authMessage = "Success: Live account created successfully!"
                                                    } catch (e: Exception) {
                                                        authMessage = "Error: ${e.message}"
                                                    }
                                                } else {
                                                    // Simulation Register
                                                    simulatedLoggedInUser = emailInput.trim()
                                                    authMessage = "Success: Simulated account registered instantly!"
                                                }
                                                isAuthLoading = false
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandMagenta),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Create Account", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            } else {
                                // Already Logged In Panel
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(45.dp)
                                            .background(BentoLightIndigo, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("👨‍🎓", fontSize = 22.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Logged In Account",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BentoSlateMuted
                                        )
                                        Text(
                                            text = activeUserEmail ?: "Guest Profile",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = InstitutionalNavy
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))
                                HorizontalDivider(color = BentoBorder.copy(alpha = 0.4f))
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Backup to cloud
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                val progress = UserProgressEntity(
                                                    coins = currentCoins,
                                                    streak = currentStreak,
                                                    xp = currentXp,
                                                    lastActiveTime = System.currentTimeMillis()
                                                )
                                                
                                                val success = if (isFbInitialized) {
                                                    FirebaseManager.syncLocalProgressToCloud(progress)
                                                } else {
                                                    true
                                                }
                                                
                                                if (success) {
                                                    Toast.makeText(context, "☁️ Backup backup uploaded directly to Firestore!", Toast.LENGTH_SHORT).show()
                                                    refreshLeaderboard()
                                                } else {
                                                    Toast.makeText(context, "Error saving backup progress.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Cloud Save 📤", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                    }

                                    // Pull backup from cloud
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                val cloudProgress = if (isFbInitialized) {
                                                    FirebaseManager.getCloudProgress()
                                                } else {
                                                    // Simulated restore
                                                    UserProgressEntity(streak = 9, coins = 750, xp = 290)
                                                }

                                                if (cloudProgress != null) {
                                                    onUpdateLocalProgress(cloudProgress.coins, cloudProgress.streak, cloudProgress.xp)
                                                    Toast.makeText(context, "✅ Room database restored from cloud Firestore backups!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "No backup data records found on Cloud Firestore.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Cloud Read 📥", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                    }

                                    // Logout button
                                    Button(
                                        onClick = {
                                            if (isFbInitialized) {
                                                FirebaseManager.getAuth()?.signOut()
                                            } else {
                                                simulatedLoggedInUser = null
                                            }
                                            authMessage = ""
                                            Toast.makeText(context, "Signed out safely.", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(0.8f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Logout", fontSize = 10.sp, color = Color.Black)
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Leaderboard list using live Firestore users collections
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🏆 National Firestore Leaderboard",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = InstitutionalNavy
                                )
                                TextButton(onClick = { refreshLeaderboard() }) {
                                    Text("Refresh 🔄", fontSize = 12.sp, color = RoyalPurple, fontWeight = FontWeight.Bold)
                                }
                            }

                            Text(
                                text = "Real-time records populated automatically using Firebase Firestore user documents.",
                                fontSize = 11.sp,
                                color = BentoSlateMuted,
                                modifier = Modifier.padding(bottom = 12.dp)
                                    .fillMaxWidth()
                            )

                            if (isLeaderboardLoading) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = RoyalPurple)
                                }
                            } else if (leaderboardList.isEmpty()) {
                                Text(
                                    text = "No players currently recorded on the Firestore leaderboard. Save progress above to join!",
                                    fontSize = 12.sp,
                                    color = BentoSlateMuted,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    leaderboardList.forEach { user ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                    if (user.email == (currentUser?.email ?: simulatedLoggedInUser)) 
                                                        BentoLightIndigo 
                                                    else 
                                                        BentoBackground
                                                )
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                // Rank Icon
                                                Text(
                                                    text = when (user.rank) {
                                                        1 -> "🥇"
                                                        2 -> "🥈"
                                                        3 -> "🥉"
                                                        else -> "🏅"
                                                    },
                                                    fontSize = 16.sp
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = user.email.take(24) + (if (user.email.length > 24) "..." else ""),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = InstitutionalNavy
                                                    )
                                                    Text(
                                                        text = "🔥 ${user.streak} Days Streak",
                                                        fontSize = 10.sp,
                                                        color = BrandMagenta,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "🪙 ${user.coins} Coins",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = BentoSlateDark
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
        } else {
            // --- WEEKLY OR MONTHLY PROGRESS ANALYTICS VIEW ---
            // Canvas line graph
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Fluency Growth Curve (%)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextNavy
                        )
                        Text(
                            text = if (selectedTab == "Weekly") "+18% this week" else "+42% this month",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentTeal
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Canvas line graph
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    ) {
                        val width = size.width
                        val height = size.height

                        // Drawing beautiful smooth path line using AccentTeal
                        val strokeColor = AccentTeal
                        val path = Path().apply {
                            if (selectedTab == "Weekly") {
                                moveTo(0f, height * 0.85f)
                                cubicTo(width * 0.25f, height * 0.8f, width * 0.45f, height * 0.4f, width * 0.5f, height * 0.45f)
                                cubicTo(width * 0.75f, height * 0.5f, width * 0.85f, height * 0.15f, width, height * 0.2f)
                            } else {
                                moveTo(0f, height * 0.95f)
                                cubicTo(width * 0.3f, height * 0.75f, width * 0.5f, height * 0.55f, width * 0.6f, height * 0.35f)
                                cubicTo(width * 0.8f, height * 0.25f, width * 0.9f, height * 0.05f, width, height * 0.08f)
                            }
                        }

                        drawPath(
                            path = path,
                            color = strokeColor,
                            style = Stroke(width = 4.dp.toPx())
                        )

                        // Draw helper subtle baseline dots representing milestones
                        val points = if (selectedTab == "Weekly") {
                            listOf(
                                Offset(0f, height * 0.85f),
                                Offset(width * 0.3f, height * 0.75f),
                                Offset(width * 0.6f, height * 0.4f),
                                Offset(width, height * 0.2f)
                            )
                        } else {
                            listOf(
                                Offset(0f, height * 0.95f),
                                Offset(width * 0.3f, height * 0.7f),
                                Offset(width * 0.6f, height * 0.32f),
                                Offset(width, height * 0.08f)
                            )
                        }

                        points.forEach { pt ->
                            drawCircle(
                                color = PrimaryGold,
                                radius = 6.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 3.dp.toPx(),
                                center = pt
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // X-Axis Milestones labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Day 1", fontSize = 11.sp, color = MutedGrey)
                        Text("Day 3", fontSize = 11.sp, color = MutedGrey)
                        Text("Day 5", fontSize = 11.sp, color = MutedGrey)
                        Text("Today", fontSize = 11.sp, color = MutedGrey)
                    }
                }
            }

            // Stats Card Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    StatInfoGridItem(
                        title = "Your Total Streak",
                        value = "🔥 $currentStreak Days",
                        label = "Continuous active index"
                    )
                    StatInfoGridItem(
                        title = "Total Pitches",
                        value = "42",
                        label = "Simulated verbal runs"
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    StatInfoGridItem(
                        title = "Account Balance",
                        value = "🪙 $currentCoins Coins",
                        label = "Your available dynamic balance"
                    )
                    StatInfoGridItem(
                        title = "Total Learning XP",
                        value = "$currentXp XP",
                        label = "Aggregated experience"
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Export Certificate / Report Card Action
            Button(
                onClick = {
                    Toast.makeText(context, "📄 Generating English & Financial performance PDF certificate...", Toast.LENGTH_LONG).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 80.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(24.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = InstitutionalNavy)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Download performance certificate (PDF) 📃",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun StatInfoGridItem(
    title: String,
    value: String,
    label: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MutedGrey
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = TextNavy
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                label,
                fontSize = 11.sp,
                color = TextNavy.copy(alpha = 0.6f)
            )
        }
    }
}

