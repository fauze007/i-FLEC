package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.data.LearningRepository
import com.example.data.FirebaseManager
import com.example.data.UserProgressEntity
import com.example.ui.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Initialize standard/manual programmatic Firebase
    FirebaseManager.initialize(applicationContext)

    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        MainAppContent()
      }
    }
  }
}

@Composable
fun MainAppContent() {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  
  // Initialize Database and Repository
  val repository = remember {
    val db = AppDatabase.getDatabase(context)
    LearningRepository(db)
  }

  // Reactive updates on user progress parameters
  val progress by repository.progressFlow.collectAsState(initial = null)
  
  // Initialize progress in DB if needed
  LaunchedEffect(Unit) {
    repository.initializeProgressIfNeeded()
  }

  val localCoins = progress?.coins ?: 450
  val localStreak = progress?.streak ?: 4
  val localXp = progress?.xp ?: 120

  // Active view states
  var onboarded by remember { mutableStateOf(false) }
  var activeTab by remember { mutableStateOf("Dashboard") }
  
  // Milestone navigation connector
  var directMilestoneLaunch by remember { mutableStateOf<MilestoneNode?>(null) }
  
  // Achievement/Badge overlay trigger states
  var achievementOverlayBadge by remember { mutableStateOf<String?>(null) }
  var achievementOverlayCoins by remember { mutableStateOf(0) }

  // Award handling helper
  fun awardCoins(amount: Int, badgeToUnlock: String? = null) {
    coroutineScope.launch {
      repository.incrementCoins(amount)
      repository.updateStreak()
      if (badgeToUnlock != null) {
        achievementOverlayBadge = badgeToUnlock
        achievementOverlayCoins = amount
      }
    }
  }

  if (!onboarded) {
    OnboardingScreen(
      onComplete = { goal, role ->
        onboarded = true
        // Increment initial launch streak helper
        coroutineScope.launch {
          repository.updateStreak()
        }
      }
    )
  } else {
    Box(modifier = Modifier.fillMaxSize().background(BackgroundCool)) {
      Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
          NavigationBar(
            modifier = Modifier
              .navigationBarsPadding()
              .padding(horizontal = 14.dp, vertical = 6.dp)
              .clip(RoundedCornerShape(24.dp))
              .border(1.5.dp, CardBorder, RoundedCornerShape(24.dp)),
            containerColor = Color.White,
            tonalElevation = 6.dp
          ) {
            val tabs = listOf(
              Triple("Dashboard", Icons.Default.Home, "Path"),
              Triple("Budget Simulator", Icons.Default.ShoppingCart, "Costs"),
              Triple("AI Coach", Icons.Default.Person, "Tutor"),
              Triple("Analytics", Icons.Default.Star, "Stats")
            )

            tabs.forEach { (tabName, icon, label) ->
              val isSelected = activeTab == tabName
              NavigationBarItem(
                selected = isSelected,
                onClick = { 
                  activeTab = tabName 
                  directMilestoneLaunch = null
                },
                icon = {
                  Icon(
                    imageVector = icon,
                    contentDescription = tabName,
                    tint = if (isSelected) TextNavy else MutedGrey
                  )
                },
                label = {
                  Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) TextNavy else MutedGrey
                  )
                },
                colors = NavigationBarItemDefaults.colors(
                  indicatorColor = PrimaryGold
                )
              )
            }
          }
        }
      ) { innerPadding ->
        Box(
          modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
        ) {
          // Dynamic view switching based on active tab
          when (activeTab) {
            "Dashboard" -> {
              DashboardScreen(
                streak = localStreak,
                coins = localCoins,
                xp = localXp,
                onStartMilestone = { milestone ->
                  if (milestone.type == "pitch") {
                    directMilestoneLaunch = milestone
                    activeTab = "AI Coach"
                  } else {
                    activeTab = "Budget Simulator"
                  }
                },
                onNavigateToTab = { tabName ->
                  activeTab = tabName
                }
              )
            }
            "Budget Simulator" -> {
              BudgetSimulatorScreen(
                onRewardEarned = { amount ->
                  awardCoins(amount, "💰 Pasar Malam Budget Boss")
                  activeTab = "Dashboard"
                }
              )
            }
            "AI Coach" -> {
              AiCoachScreen(
                onProgressUpdate = { amount ->
                  // Give consistent rewards for chat prompts and triggers
                  awardCoins(amount, "🎙️ Fluent Ringgit Communicator")
                }
              )
            }
            "Analytics" -> {
              PerformanceStatsScreen(
                currentCoins = localCoins,
                currentStreak = localStreak,
                currentXp = localXp,
                onUpdateLocalProgress = { coins, streak, xp ->
                  coroutineScope.launch {
                    val db = AppDatabase.getDatabase(context)
                    val current = db.userProgressDao().getProgressDirect() ?: UserProgressEntity()
                    db.userProgressDao().updateProgress(
                      current.copy(
                        coins = coins,
                        streak = streak,
                        xp = xp,
                        lastActiveTime = System.currentTimeMillis()
                      )
                    )
                  }
                }
              )
            }
          }
        }
      }

      // Large Victory Modal Overlay for claimed Achievements
      AnimatedVisibility(
        visible = achievementOverlayBadge != null,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = Modifier.align(Alignment.Center)
      ) {
        val badge = achievementOverlayBadge
        if (badge != null) {
          Box(
              modifier = Modifier
                  .fillMaxSize()
                  .background(Color.Black.copy(alpha = 0.6f)),
              contentAlignment = Alignment.Center
          ) {
            AchievementsView(
              badgeName = badge,
              rewardAmount = achievementOverlayCoins,
              onDismiss = {
                achievementOverlayBadge = null
              }
            )
          }
        }
      }
    }
  }
}

