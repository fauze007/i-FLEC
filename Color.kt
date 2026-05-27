package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Brand Core Colors as requested
val RoyalPurple = Color(0xFF5A2A82)       // Royal Purple (Ungu Diraja) - Banner background, core identity
val GoldenYellow = Color(0xFFFFC000)      // Golden Yellow (Kuning Keemasan) - Highlights, badges, star icons
val BrandMagenta = Color(0xFFC2185B)      // Magenta - Gradient pair with deep purple
val InstitutionalNavy = Color(0xFF1A237E) // Navy Blue (Biru Tentera Laut) - Institutional headers and principal texts
val EmeraldSuccess = Color(0xFF2E7D32)    // Emerald Green (Hijau Zamrud) - Success flags, positive indicators
val EmeraldLight = Color(0xFFE8F5E9)      // Soft Emerald Green background

// Bento Grid Design Theme Colors mapped to custom brand values
val BentoBackground = Color(0xFFF5F2F9)   // Sophisticated very soft purple-tinted slate background (avoiding plain gray)
val BentoIndigo = RoyalPurple             // Banners default to Royal Purple (#5A2A82)
val BentoLightIndigo = Color(0xFFF3E5F5)  // Royal Purple soft tinted card / highlights accent container background
val BentoSlateDark = InstitutionalNavy    // Institutional Text is Navy Blue (#1A237E)
val BentoSlateMuted = Color(0xFF5C6BC0)   // Approachable slate blue-muted tint for secondary texts
val BentoEmerald = EmeraldSuccess         // Success indicator is Emerald Green (#2E7D32)
val BentoLightEmerald = EmeraldLight      // Light green tint
val BentoOrange = GoldenYellow            // Star badges and highlights are Golden Yellow (#FFC000)
val BentoLightOrange = Color(0xFFFFFDE7)  // Soft yellow tint for highlights background
val BentoBorder = Color(0xFFE1BEE7)       // Soft purple border for high-cohesion cards

// Aliasing existing theme colors for seamless integration & backward safety
val PrimaryGold = GoldenYellow            // Primary Gold is Kuning Keemasan
val PrimaryDark = RoyalPurple             // Primary Dark is Ungu Diraja
val BackgroundCool = BentoBackground
val SurfaceWhite = Color(0xFFFFFFFF)
val TextNavy = InstitutionalNavy
val MutedGrey = BentoSlateMuted
val AccentTeal = EmeraldSuccess
val AccentCoral = BrandMagenta            // Magenta gradient/highlights pair
val LightGold = BentoLightIndigo
val LightTeal = EmeraldLight
val LightCoral = Color(0xFFFCE4EC)        // Light magenta pink
val CardBorder = BentoBorder

// Legacy compatibility values
val Purple80 = Color(0xFFD1C4E9)
val PurpleGrey80 = Color(0xFFB0BEC5)
val Pink80 = Color(0xFFF8BBD0)
val Purple40 = RoyalPurple
val PurpleGrey40 = InstitutionalNavy
val Pink40 = BrandMagenta


