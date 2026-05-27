package com.example.ui

import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.util.Locale

// Structure representing milestones in the path
data class MilestoneNode(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val type: String, // "pitch" or "budget"
    val rewardCoins: Int,
    val isLocked: Boolean,
    val details: String
)

@Composable
fun DashboardScreen(
    streak: Int,
    coins: Int,
    xp: Int,
    onStartMilestone: (MilestoneNode) -> Unit,
    onNavigateToTab: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    var isTtsReady by remember { mutableStateOf(false) }

    // TTS engine initialization for Word of the Day "Practice" speech button
    DisposableEffect(Unit) {
        try {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    try {
                        val result = tts?.setLanguage(Locale.UK)
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            val result2 = tts?.setLanguage(Locale.US)
                            if (result2 == TextToSpeech.LANG_MISSING_DATA || result2 == TextToSpeech.LANG_NOT_SUPPORTED) {
                                tts?.setLanguage(Locale.getDefault())
                            }
                        }
                        isTtsReady = true
                    } catch (e: Exception) {
                        android.util.Log.e("DashboardScreen", "Error setting language on TTS", e)
                    }
                } else {
                    android.util.Log.e("DashboardScreen", "TTS initialization failed with status $status")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DashboardScreen", "Failed to initialize TextToSpeech", e)
        }
        onDispose {
            try {
                tts?.stop()
                tts?.shutdown()
            } catch (e: Exception) {
                android.util.Log.e("DashboardScreen", "Error disposing TTS", e)
            }
        }
    }

    fun speak(text: String) {
        // Notify user if volume might be muted
        try {
            val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
            if (audioManager != null) {
                val currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                if (currentVolume == 0) {
                    android.widget.Toast.makeText(context, "Note: Media volume is muted. Volume up to hear the pronunciation!", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DashboardScreen", "Failed to check media volume", e)
        }

        if (tts == null || !isTtsReady) {
            android.util.Log.w("DashboardScreen", "TTS is not ready yet.")
            android.widget.Toast.makeText(context, "Preparing voice engine, please try again...", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val speakResult = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            if (speakResult == TextToSpeech.ERROR) {
                android.util.Log.e("DashboardScreen", "TTS speak returned ERROR code")
                android.widget.Toast.makeText(context, "Voice playback failed on this device.", android.widget.Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("DashboardScreen", "Failed to speak text via TTS", e)
        }
    }

    val milestones = remember {
        listOf(
            MilestoneNode(
                id = "mamak_loan",
                title = "The Mamak Pitch",
                subtitle = "Pitch expansion to CIMB Bank",
                badge = "☕",
                type = "pitch",
                rewardCoins = 50,
                isLocked = false,
                details = "Pitch your Roti Canai stall expansion idea. Learn standard terms like 'interest rate' vs 'bunga', and avoid business slangs in formal requests."
            ),
            MilestoneNode(
                id = "pasar_malam",
                title = "Pasar Malam Budget",
                subtitle = "Interactive cost sandbox",
                badge = "🍿",
                type = "budget",
                rewardCoins = 60,
                isLocked = false,
                details = "Organize cost allocations in RM. Balance ingredients vs takeaway box prices. Ideal for basic retail business margin lessons."
            ),
            MilestoneNode(
                id = "corporate_bangsar",
                title = "Bangsar Interview",
                subtitle = "Salary & EPF Negotiation",
                badge = "💼",
                type = "pitch",
                rewardCoins = 80,
                isLocked = false,
                details = "Talk to a HR advisor. Negotiate a fresh graduate starting salary of RM 4,500. Learn 'basic salary', 'allowance' and 'equity'."
            ),
            MilestoneNode(
                id = "chinese_supplier",
                title = "Shopee Supplier",
                subtitle = "Negotiate Credit Terms",
                badge = "📦",
                type = "pitch",
                rewardCoins = 100,
                isLocked = false,
                details = "Negotiate Net-30 payment terms with an international electronics supplier inside the Shopee framework in English."
            )
        )
    }

    var selectedNode by remember { mutableStateOf<MilestoneNode?>(null) }
    var showBadgesDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BentoBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Section customized with specific branding
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Custom App Logo
                    coil.compose.AsyncImage(
                        model = "https://i.imgur.com/2TJihxS.png",
                        contentDescription = "i-FLEC App Logo",
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, BentoBorder, RoundedCornerShape(12.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Selamat Pagi 🌅",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoSlateMuted,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "i-FLEC Learn",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BentoSlateDark
                        )
                    }
                }

                // Profile Round Avatar/Favicon representing app brand
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White, CircleShape)
                        .border(2.dp, GoldenYellow, CircleShape)
                        .clip(CircleShape)
                        .clickable { showBadgesDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    coil.compose.AsyncImage(
                        model = "https://i.imgur.com/2TJihxS.png",
                        contentDescription = "Favicon",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp)
                            .clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
            }

            // Scrollable Bento Grid Layout
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // App Main Banner Header (Full Width Visual Display)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.5.dp, BentoBorder, RoundedCornerShape(24.dp)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        coil.compose.AsyncImage(
                            model = "https://i.imgur.com/DDXXGXD.png",
                            contentDescription = "Main App Banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                }

                // Bento Card 1: Progress Hero Card (Full Width) with Royal Purple to Magenta gradient and glowing depth
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(RoyalPurple, BrandMagenta)
                                )
                            ),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawBehind {
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.06f),
                                        radius = 85.dp.toPx(),
                                        center = androidx.compose.ui.geometry.Offset(this@drawBehind.size.width - 30.dp.toPx(), 45.dp.toPx())
                                    )
                                }
                                .padding(20.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Text(
                                            text = "Your Progress",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Level 4: Savvy Saver 💡",
                                            color = Color.White,
                                            fontSize = 26.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                    
                                    // A sleek floating badge representing piggy bank and coins
                                    Row(
                                        modifier = Modifier
                                            .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                                            .border(1.dp, GoldenYellow.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🐷", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("🪙", fontSize = 18.sp)
                                    }
                                }
                                Text(
                                    text = "English Proficiency: Intermediate",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    fontStyle = FontStyle.Italic,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Progress bar
                                    val percentage = 75
                                    LinearProgressIndicator(
                                        progress = { percentage / 100f },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = Color.White,
                                        trackColor = Color.White.copy(alpha = 0.3f),
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "$percentage%",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Quick Stat Chips inside Progress Bento
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                Color.White.copy(alpha = 0.15f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "🔥 $streak Days",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(
                                                Color.White.copy(alpha = 0.15f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "🪙 $coins coins",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(
                                                Color.White.copy(alpha = 0.15f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "⭐ $xp XP",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bento Card 2: AI Chat CTA (Full Width)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { onNavigateToTab("AI Coach") },
                        colors = CardDefaults.cardColors(containerColor = BentoEmerald),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("💬", fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Chat with Wira AI",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "Practice speaking about 'KWSP'",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Text(
                                text = "→",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Bento Row 3: Daily Vocab Card & Stage Highlight (2 Staggered Cards)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Word of the Day (Left Bento)
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(175.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, BentoBorder),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Word of the Day",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoIndigo,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "Dividend",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoSlateDark,
                                        fontStyle = FontStyle.Italic,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "MY: Dividen — Profit paid to shareholders.",
                                        fontSize = 10.sp,
                                        color = BentoSlateMuted,
                                        lineHeight = 13.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )

                                    Button(
                                        onClick = { speak("Dividend: profit paid to shareholders or company owners annually") },
                                        colors = ButtonDefaults.buttonColors(containerColor = BentoBackground),
                                        contentPadding = PaddingValues(0.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(30.dp)
                                    ) {
                                        Text(
                                            text = "Practice 🔊",
                                            color = BentoSlateDark,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Contextual Scenario Spotlight (Right Bento)
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(175.dp)
                                .clickable {
                                    val targetNode = milestones.firstOrNull { it.id == "pasar_malam" }
                                    if (targetNode != null) selectedNode = targetNode
                                },
                            colors = CardDefaults.cardColors(containerColor = BentoLightOrange),
                            border = BorderStroke(1.dp, GoldenYellow.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "SCENARIO",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoOrange,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "Pasar Malam Budgeting",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoSlateDark,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    Text(
                                        text = "Roleplay buying groceries in English while keeping RM50 budget.",
                                        fontSize = 10.sp,
                                        color = BentoSlateDark.copy(alpha = 0.7f),
                                        lineHeight = 13.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                // Social stack dots
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy((-6).dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(GoldenYellow.copy(alpha = 0.4f), CircleShape)
                                            .border(1.5.dp, Color.White, CircleShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(GoldenYellow.copy(alpha = 0.8f), CircleShape)
                                            .border(1.5.dp, Color.White, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "+24 others",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoOrange
                                    )
                                }
                            }
                        }
                    }
                }

                // Bento Row 4: Quick Action Grid (Stats & Badges Button)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clickable { onNavigateToTab("Analytics") },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, BentoBorder),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("📊", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Stats",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoSlateDark
                                    )
                                }
                            }
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clickable { showBadgesDialog = true },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, BentoBorder),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🏆", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Badges",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoSlateDark
                                    )
                                }
                            }
                        }
                    }
                }

                // Title Section for Comm-Path inside Bento Frame
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "YOUR RINGGIT COMM-PATH",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoSlateMuted,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }

                // Bento List of Milestones Node Content
                items(milestones) { node ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .border(
                                    BorderStroke(
                                        1.5.dp,
                                        if (node.isLocked) BentoBorder else BentoIndigo.copy(alpha = 0.3f)
                                    ),
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedNode = node },
                            colors = CardDefaults.cardColors(
                                containerColor = if (node.isLocked) Color(0xFFF1F5F9) else Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(
                                                if (node.isLocked) BentoSlateMuted.copy(alpha = 0.2f) else BentoLightIndigo,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(node.badge, fontSize = 24.sp)
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = node.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (node.isLocked) BentoSlateMuted else BentoSlateDark
                                        )
                                        Text(
                                            text = node.subtitle,
                                            fontSize = 12.sp,
                                            color = BentoSlateMuted
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("🟡", fontSize = 12.sp)
                                    Text(
                                        text = "+${node.rewardCoins}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = BentoSlateDark
                                    )
                                    Icon(
                                        imageVector = if (node.isLocked) Icons.Default.Lock else Icons.Default.PlayArrow,
                                        contentDescription = "Action Indicator",
                                        tint = if (node.isLocked) BentoSlateMuted else BentoIndigo,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .padding(start = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Milestone Bottom Sheet Drawer Dialog
        AnimatedVisibility(
            visible = selectedNode != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            val node = selectedNode
            if (node != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, BentoIndigo, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = node.title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoSlateDark
                            )

                            IconButton(onClick = { selectedNode = null }) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(BentoSlateMuted.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "×",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoSlateDark
                                    )
                                }
                            }
                        }

                        Text(
                            text = node.subtitle,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoIndigo,
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = node.details,
                            fontSize = 13.sp,
                            color = BentoSlateDark.copy(alpha = 0.8f),
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🟡", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+${node.rewardCoins} i-Coins",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoSlateDark
                                )
                            }

                            Button(
                                onClick = {
                                    onStartMilestone(node)
                                    selectedNode = null
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BentoIndigo)
                            ) {
                                Text(
                                    text = if (node.type == "pitch") "Pitch Now 🎤" else "Allocate costs 🍿",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Custom Bento Adaptive Badges Dialog / Overlay
        if (showBadgesDialog) {
            AlertDialog(
                onDismissRequest = { showBadgesDialog = false },
                confirmButton = {
                    TextButton(onClick = { showBadgesDialog = false }) {
                        Text("Close", color = BentoIndigo, fontWeight = FontWeight.Bold)
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏆 Your Ringgit Badges", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BentoSlateDark)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Complete real-world financial roleplays & budget simulations to unlock special badges!",
                            fontSize = 12.sp,
                            color = BentoSlateMuted,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        val badgeList = listOf(
                            Triple("☕ Mamak Pitch Champ", "Pitched expansion model to the bank.", coins >= 50),
                            Triple("🍿 Pasar Malam Budget Boss", "Completed local business retail budget.", coins >= 400),
                            Triple("💼 Salary Net Expert", "Negotiated gross-to-net allocations.", xp >= 110),
                            Triple("🎙️ Fluent Ringgit Communicator", "Maintained financial vocabulary streak.", streak >= 4)
                        )

                        badgeList.forEach { (title, desc, isEarned) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isEarned) BentoLightIndigo else BentoBackground,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isEarned) BentoIndigo.copy(alpha = 0.3f) else BentoBorder,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isEarned) "🌟" else "🔒",
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isEarned) BentoSlateDark else BentoSlateMuted
                                    )
                                    Text(
                                        text = desc,
                                        fontSize = 11.sp,
                                        color = BentoSlateMuted
                                    )
                                }
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = Color.White
            )
        }
    }
}
