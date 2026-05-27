package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class BudgetBucket(
    val name: String,
    val description: String,
    val targetAmount: Int,
    var allocatedAmount: Int,
    val icon: String,
    val color: Color
)

@Composable
fun BudgetSimulatorScreen(
    onRewardEarned: (coinsEarned: Int) -> Unit
) {
    var totalRM = 3000
    var rentAmount by remember { mutableStateOf(500) }
    var foodAmount by remember { mutableStateOf(400) }
    var ptptnAmount by remember { mutableStateOf(200) }
    var asbAmount by remember { mutableStateOf(300) }
    var mamakAmount by remember { mutableStateOf(200) }
    var bufferAmount by remember { mutableStateOf(400) }

    val allocatedTotal = rentAmount + foodAmount + ptptnAmount + asbAmount + mamakAmount + bufferAmount
    val overBudget = allocatedTotal > totalRM
    val exactlySpent = allocatedTotal == totalRM

    val budgetStatusColor by animateColorAsState(
        targetValue = when {
            overBudget -> AccentCoral
            exactlySpent -> AccentTeal
            else -> PrimaryGold
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCool)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // Upper Title Text
        Text(
            text = "MALAYSIAN RM BUDGETER",
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = MutedGrey,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // Master Budget Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(2.5.dp, budgetStatusColor, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Salary RM $totalRM / month",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MutedGrey
                )
                
                Spacer(modifier = Modifier.height(4.dp))

                // Big allocated text
                Text(
                    text = "RM $allocatedTotal",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = budgetStatusColor
                )

                Text(
                    text = when {
                        overBudget -> "⚠️ Terlebih budget! Reduce allocations!"
                        exactlySpent -> "⭐ Perfect! Balanced budget! Claim rewards!"
                        else -> "Allocate RM ${totalRM - allocatedTotal} remaining to claim i-Coins!"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (overBudget) AccentCoral else TextNavy,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Horizontal fluid stack chart
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(CardBorder)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        val rentWeight = rentAmount.toFloat() / totalRM
                        val foodWeight = foodAmount.toFloat() / totalRM
                        val ptptnWeight = ptptnAmount.toFloat() / totalRM
                        val asbWeight = asbAmount.toFloat() / totalRM
                        val mamakWeight = mamakAmount.toFloat() / totalRM
                        val bufferWeight = bufferAmount.toFloat() / totalRM

                        if (rentWeight > 0f) Box(modifier = Modifier.fillMaxHeight().weight(rentWeight).background(AccentTeal))
                        if (foodWeight > 0f) Box(modifier = Modifier.fillMaxHeight().weight(foodWeight).background(PrimaryGold))
                        if (ptptnWeight > 0f) Box(modifier = Modifier.fillMaxHeight().weight(ptptnWeight).background(AccentCoral))
                        if (asbWeight > 0f) Box(modifier = Modifier.fillMaxHeight().weight(asbWeight).background(Color(0xFF8E44AD)))
                        if (mamakWeight > 0f) Box(modifier = Modifier.fillMaxHeight().weight(mamakWeight).background(Color(0xFFE67E22)))
                        if (bufferWeight > 0f) Box(modifier = Modifier.fillMaxHeight().weight(bufferWeight).background(Color(0xFF34495E)))
                    }
                }
            }
        }

        // Draggable-simulated tactile bucket grid
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BudgetRowItem(
                title = "Rent in KL (Cheras Flat)",
                desc = "Essential shelter. Average RM 800.",
                amount = rentAmount,
                color = AccentTeal,
                icon = "🏠",
                onAmountChanged = { rentAmount = (rentAmount + it).coerceAtLeast(0) }
            )

            BudgetRowItem(
                title = "Nasi Lemak & Cafe Dining",
                desc = "RM 10 standard / RM 15 cafe run.",
                amount = foodAmount,
                color = PrimaryGold,
                icon = "🍛",
                onAmountChanged = { foodAmount = (foodAmount + it).coerceAtLeast(0) }
            )

            BudgetRowItem(
                title = "PTPTN Student Loan",
                desc = "Maintains good credit score (CCRIS).",
                amount = ptptnAmount,
                color = AccentCoral,
                icon = "🎓",
                onAmountChanged = { ptptnAmount = (ptptnAmount + it).coerceAtLeast(0) }
            )

            BudgetRowItem(
                title = "ASB Savings (Amanah Saham)",
                desc = "Compound interest wealth builder.",
                amount = asbAmount,
                color = Color(0xFF8E44AD),
                icon = "📈",
                onAmountChanged = { asbAmount = (asbAmount + it).coerceAtLeast(0) }
            )

            BudgetRowItem(
                title = "Mamak Hanging Out",
                desc = "Teh Tarik & Roti Canai with buddies.",
                amount = mamakAmount,
                color = Color(0xFFE67E22),
                icon = "🥤",
                onAmountChanged = { mamakAmount = (mamakAmount + it).coerceAtLeast(0) }
            )

            BudgetRowItem(
                title = "Emergency Cushion",
                desc = "Keep inside liquid Touch & Go.",
                amount = bufferAmount,
                color = Color(0xFF34495E),
                icon = "🛡️",
                onAmountChanged = { bufferAmount = (bufferAmount + it).coerceAtLeast(0) }
            )
        }

        // Sticky button to redeem coins when balanced
        Button(
            onClick = {
                if (exactlySpent) {
                    onRewardEarned(60) // Claim 60 coins
                }
            },
            enabled = exactlySpent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 80.dp)
                .height(56.dp)
                .clip(RoundedCornerShape(24.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentTeal,
                disabledContainerColor = MutedGrey.copy(alpha = 0.5f)
            )
        ) {
            Text(
                text = if (exactlySpent) "Claim Balanced Budget Reward! 🟡" else "Please Balance Budget Room to Claim",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun BudgetRowItem(
    title: String,
    desc: String,
    amount: Int,
    color: Color,
    icon: String,
    onAmountChanged: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon space
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text space
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextNavy
                )
                Text(
                    text = desc,
                    fontSize = 12.sp,
                    color = MutedGrey
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Allocated: RM $amount",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            // Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Minus
                Button(
                    onClick = { onAmountChanged(-50) },
                    modifier = Modifier.size(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LightGold),
                    contentPadding = PaddingValues(0.dp),
                    shape = CircleShape
                ) {
                    Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextNavy)
                }

                // Plus
                Button(
                    onClick = { onAmountChanged(50) },
                    modifier = Modifier.size(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                    contentPadding = PaddingValues(0.dp),
                    shape = CircleShape
                ) {
                    Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextNavy)
                }
            }
        }
    }
}
