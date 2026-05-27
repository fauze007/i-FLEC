package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun AchievementsView(
    badgeName: String,
    rewardAmount: Int,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(4.dp, PrimaryGold, RoundedCornerShape(28.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier.padding(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated rotating trophy/badge representation
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(LightGold, CircleShape)
                    .border(3.dp, PrimaryDark, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🏆",
                    fontSize = 58.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "MILESTONE UNLOCKED! 🌟",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = AccentTeal,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = badgeName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextNavy,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Splendid job! You successfully balanced the local business scenario, expanded your professional vocabulary, and successfully claimed Ringgit coins!",
                fontSize = 14.sp,
                color = TextNavy.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Coins reward card
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(LightTeal, RoundedCornerShape(16.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text("🟡", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$rewardAmount i-Coins Secured!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextNavy
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(
                    text = "Awesome! Continue Quest 🔥",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextNavy
                )
            }
        }
    }
}
