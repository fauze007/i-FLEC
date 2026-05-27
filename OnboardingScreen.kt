package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun OnboardingScreen(
    onComplete: (userGoal: String, userRole: String) -> Unit
) {
    var step by remember { mutableStateOf(1) }
    var selectedGoal by remember { mutableStateOf("Master Pitching & Get Funding") }
    var selectedRole by remember { mutableStateOf("Gen-Z Fresh Graduate / Startup Founder") }
    var selectedAccent by remember { mutableStateOf("Professional English + Malaysian Translation Help") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(LightGold, BackgroundCool)
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "i-FLEC",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextNavy
                )
                
                Text(
                    text = "Step $step/3",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MutedGrey,
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Animated / Bouncing Logo Banner
            if (step == 1) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentHeight()
                        .padding(vertical = 8.dp)
                ) {
                    // Modern layered illustration using deep purple to magenta gradient and Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(RoyalPurple, BrandMagenta)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Decorative ambient circles for depth
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.08f),
                                radius = 90.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(30.dp.toPx(), 40.dp.toPx())
                            )
                            drawCircle(
                                color = GoldenYellow.copy(alpha = 0.12f),
                                radius = 70.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset((size.width - 40.dp.toPx()), (size.height - 30.dp.toPx()))
                            )
                        }

                        // Layered character and iconography layout
                        Row(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Student mascot representation inside a glowing border displaying the actual app logo
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                                    .border(2.dp, GoldenYellow, RoundedCornerShape(24.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                coil.compose.AsyncImage(
                                    model = "https://i.imgur.com/2TJihxS.png",
                                    contentDescription = "i-FLEC App Logo",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            // Educational and Financial Icons Stack
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                CardBadge(icon = "💡", text = "English Pitch")
                                CardBadge(icon = "🐷", text = "Piggy Bank")
                                CardBadge(icon = "🪙", text = "RM Wealth")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "i-FLEC",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = InstitutionalNavy,
                        textAlign = TextAlign.Center,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Fluid Learning, Financial Empowerment",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandMagenta,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "The AI-driven speech coach tailored for Malaysian youth. Transition smoothly from everyday English to corporate, fundable business proposals in RM.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = InstitutionalNavy.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            // Step Contents
            when (step) {
                1 -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "What is your primary learning goal?",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextNavy,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        val goals = listOf(
                            "Master Pitching & Get Funding",
                            "Ace Interviews & Salary Negotiation",
                            "Daily Budgeting & Ringgit Management",
                            "Shopee/Lazada Supplier Credit Terms"
                        )

                        goals.forEach { goal ->
                            GoalSelectionCard(
                                title = goal,
                                isSelected = selectedGoal == goal,
                                onClick = { selectedGoal = goal }
                            )
                        }
                    }
                }
                2 -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Select your target profile",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextNavy
                        )

                        val roles = listOf(
                            "Gen-Z Fresh Graduate / Startup Founder",
                            "University Student / Pocket Money Saver",
                            "Online Shop Owner / Marketplace Seller",
                            "Corporate Executive trying to boost RM salary"
                        )

                        roles.forEach { role ->
                            GoalSelectionCard(
                                title = role,
                                isSelected = selectedRole == role,
                                onClick = { selectedRole = role }
                            )
                        }
                    }
                }
                3 -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Choose Coach Tuning",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextNavy
                        )

                        val accents = listOf(
                            "Professional English + Malaysian Translation Help",
                            "Strict Standard English (No Manglish)",
                            "Bilingual (English & Bahasa Malaysia)",
                            "High-Fluency Business English Only"
                        )

                        accents.forEach { accent ->
                            GoalSelectionCard(
                                title = accent,
                                isSelected = selectedAccent == accent,
                                onClick = { selectedAccent = accent }
                            )
                        }
                    }
                }
            }

            // Navigation CTA
            Button(
                onClick = {
                    if (step < 3) {
                        step += 1
                    } else {
                        onComplete(selectedGoal, selectedRole)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(24.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Text(
                    text = if (step < 3) "Continue" else "Launch My AI Coach 🚀",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextNavy
                )
            }
        }
    }
}

@Composable
fun GoalSelectionCard(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) LightGold else Color.White)
            .border(
                width = if (isSelected) 3.dp else 1.5.dp,
                color = if (isSelected) PrimaryDark else CardBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextNavy,
                modifier = Modifier.weight(1f)
            )
            
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = PrimaryDark)
            )
        }
    }
}

@Composable
fun CardBadge(icon: String, text: String) {
    Row(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

