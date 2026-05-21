package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioSynth
import com.example.game.*
import com.example.music.*
import com.example.ui.theme.*
import kotlin.math.abs

class MainActivity : ComponentActivity() {

    // Central campaign state database helper
    private val campaignState = GameCampaign()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Main reactive entry trigger
                var uiTick by remember { mutableStateOf(0) }
                campaignState.onStateChanged = {
                    uiTick++
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GameMainScreen(campaignState = campaignState, uiTick = uiTick)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameMainScreen(campaignState: GameCampaign, uiTick: Int) {
    // Keep track of ticks inside composable
    val customTick = uiTick

    // Navigation Tab indicators: 0=Map/Quests, 1=Practice/Improvisation, 2=Shop, 3=Stats/HQ
    var selectedTab by remember { mutableStateOf(0) }
    
    // Active encounter state (null if not in active combat quiz, otherwise index of active question 0..4)
    var activeEncounterWorldId by remember { mutableStateOf<Int?>(null) }
    var activeQuestionIndex by remember { mutableStateOf(0) }
    val activeQuestions = remember(activeEncounterWorldId) {
        activeEncounterWorldId?.let { QuestionGenerator.generateQuestionsForWorld(it) } ?: emptyList()
    }
    var selectedEncounterChoice by remember { mutableStateOf<Int?>(null) }
    var showEncounterExplanation by remember { mutableStateOf(false) }
    var encounterFinishedResultState by remember { mutableStateOf<EncounterFinished?>(null) } // null or perfect/success
    var isBossFight by remember { mutableStateOf(false) }

    // Backup state if defeated
    val stats = campaignState.stats
    val masteries = campaignState.masteries

    Scaffold(
        bottomBar = {
            if (activeEncounterWorldId == null) {
                NavigationBar(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                    containerColor = RpgSurfaceLight,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Map, contentDescription = "Mapa") },
                        label = { Text("Mundos", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = RpgGold,
                            selectedTextColor = RpgGold,
                            unselectedIconColor = Color.LightGray,
                            unselectedTextColor = Color.LightGray,
                            indicatorColor = RpgDarkBg
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.MusicNote, contentDescription = "Soprar") },
                        label = { Text("Improviso", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = RpgGold,
                            selectedTextColor = RpgGold,
                            unselectedIconColor = Color.LightGray,
                            unselectedTextColor = Color.LightGray,
                            indicatorColor = RpgDarkBg
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Taverna") },
                        label = { Text("Taverna", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = RpgGold,
                            selectedTextColor = RpgGold,
                            unselectedIconColor = Color.LightGray,
                            unselectedTextColor = Color.LightGray,
                            indicatorColor = RpgDarkBg
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.Person, contentDescription = "QG") },
                        label = { Text("Personagem", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = RpgGold,
                            selectedTextColor = RpgGold,
                            unselectedIconColor = Color.LightGray,
                            unselectedTextColor = Color.LightGray,
                            indicatorColor = RpgDarkBg
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(RpgDarkBg, Color(0xFF15151D))
                    )
                )
                .padding(innerPadding)
        ) {
            // HUD COMPONENT (Always visible on top of game map to prevent disconnect)
            GameTopHUD(campaignState = campaignState)

            Box(modifier = Modifier.weight(1f)) {
                if (activeEncounterWorldId != null) {
                    // Quiz Encounter Combat screen overlay
                    EncounterCombatScreen(
                        worldId = activeEncounterWorldId!!,
                        questions = activeQuestions,
                        activeIndex = activeQuestionIndex,
                        selectedChoice = selectedEncounterChoice,
                        showExplanation = showEncounterExplanation,
                        finishedState = encounterFinishedResultState,
                        isBoss = isBossFight,
                        campaignState = campaignState,
                        onChoiceSelected = { selectedEncounterChoice = it },
                        onSendAnswer = {
                            val question = activeQuestions[activeQuestionIndex]
                            val isCorrect = selectedEncounterChoice == question.correctAnswerIndex
                            
                            showEncounterExplanation = true
                            if (isCorrect) {
                                AudioSynth.playSFX(AudioSynth.SFXType.HIT_SUCCESS)
                                // Add check for spaced repetition deck success
                                campaignState.registerNoteSuccess(question.id)
                            } else {
                                AudioSynth.playSFX(AudioSynth.SFXType.HIT_FAIL)
                                campaignState.registerNoteError(question.id, if (question.isPistonQuestion) "PISTON" else "CHROMATIC")
                                campaignState.failChallenge(
                                    category = GameWorlds.WORLDS.find { it.id == activeEncounterWorldId }?.skillCategory ?: SkillCategory.NATURAL_NOTES,
                                    customHpPenalty = if (isBossFight) 30 else 18
                                )
                            }
                        },
                        onNextOrFinish = {
                            if (activeQuestionIndex + 1 < activeQuestions.size && campaignState.stats.hp > 0) {
                                // proceed next question
                                activeQuestionIndex++
                                selectedEncounterChoice = null
                                showEncounterExplanation = false
                            } else {
                                // finished the list or Dead
                                if (campaignState.stats.hp <= 0) {
                                    encounterFinishedResultState = EncounterFinished.FAILED_DEAD
                                } else {
                                    // Complete Campaign
                                    val worldObj = GameWorlds.WORLDS.find { it.id == activeEncounterWorldId!! }!!
                                    campaignState.completeChallenge(
                                        category = worldObj.skillCategory,
                                        wasPerfect = true,
                                        speedBonus = true
                                    )
                                    AudioSynth.playSFX(AudioSynth.SFXType.LEVEL_UP)
                                    encounterFinishedResultState = EncounterFinished.SUCCESS_COMPLETED
                                }
                            }
                        },
                        onForceExit = {
                            activeEncounterWorldId = null
                            activeQuestionIndex = 0
                            selectedEncounterChoice = null
                            showEncounterExplanation = false
                            encounterFinishedResultState = null
                        }
                    )
                } else if (stats.hp <= 0) {
                    // Full Defeated State Screen with restoration helpers
                    CharacterDefeatedScreen(campaignState = campaignState)
                } else {
                    // Main layout screen navigation switcher
                    when (selectedTab) {
                        0 -> WorldMapScreen(
                            campaignState = campaignState,
                            onStartLevel = { worldId, boss ->
                                activeEncounterWorldId = worldId
                                isBossFight = boss
                                activeQuestionIndex = 0
                                selectedEncounterChoice = null
                                showEncounterExplanation = false
                                encounterFinishedResultState = null
                            }
                        )
                        1 -> InteractivePracticeScreen(campaignState = campaignState)
                        2 -> TavernShopScreen(campaignState = campaignState)
                        3 -> CharacterHQScreen(campaignState = campaignState)
                    }
                }
            }
        }
    }
}

enum class EncounterFinished {
    SUCCESS_COMPLETED,
    FAILED_DEAD
}

// --- SUB-SCREEN 1: TOP PROFILE STATS BAR (HUD) ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameTopHUD(campaignState: GameCampaign) {
    val stats = campaignState.stats

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .shadow(6.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = RpgSurfaceLight),
        border = BorderStroke(1.dp, RpgCardBorder)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar status block
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(RpgGold, RpgBronze)
                                )
                            )
                            .border(1.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = "Avatar",
                            tint = RpgDarkBg,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        val title = when {
                            stats.level >= 10 -> "Arcanista do Metal"
                            stats.level >= 7 -> "Soprador de Aço"
                            stats.level >= 4 -> "Cavaleiro do Bocal"
                            else -> "Recruta do Trompete"
                        }
                        Text(
                            text = title,
                            color = RpgGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Nível ${stats.level}",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Trumpet Type selection badge (Live Key conversion)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(0.4f))
                        .clickable {
                            val nextType = if (stats.trumpetType == TrumpetType.BB_TRUMPET) {
                                TrumpetType.C_TRUMPET
                            } else {
                                TrumpetType.BB_TRUMPET
                            }
                            campaignState.setTrumpetKey(nextType)
                            AudioSynth.playSFX(AudioSynth.SFXType.ITEM_BUY)
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Leaderboard,
                        contentDescription = "Chave",
                        tint = RpgGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (stats.trumpetType == TrumpetType.BB_TRUMPET) "Si♭ (Bb)" else "Dó (C)",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Balance Coins indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Ouro",
                        tint = RpgGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${stats.gold} GP",
                        color = RpgGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.testTag("player_gold")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Bars Grid for fast visual check
            Row(modifier = Modifier.fillMaxWidth()) {
                // HP Bar
                Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "VIDA (HP)",
                            color = RpgRubyHp,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "${stats.hp}/${stats.maxHp}",
                            color = RpgRubyHp,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    LinearProgressIndicator(
                        progress = { stats.getHpProgress() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = RpgRubyHp,
                        trackColor = Color.Black.copy(0.4f)
                    )
                }

                // XP Bar
                Column(modifier = Modifier.weight(1f).padding(start = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "DOMÍNIO (XP)",
                            color = RpgCyanXp,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "${stats.xp}/${stats.maxXp}",
                            color = RpgCyanXp,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    LinearProgressIndicator(
                        progress = { stats.getXpProgress() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = RpgCyanXp,
                        trackColor = Color.Black.copy(0.4f)
                    )
                }
            }
        }
    }
}

// --- SUB-SCREEN 2: WORLD CAMPAIGN MAP (SCREEN 0) ---

@Composable
fun WorldMapScreen(
    campaignState: GameCampaign,
    onStartLevel: (worldId: Int, isBoss: Boolean) -> Unit
) {
    var expandedWorldId by remember { mutableStateOf<Int?>(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        Text(
            text = "🌎 MAPA DE AVENTURA MUSICAL",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        GameWorlds.WORLDS.forEach { world ->
            val userMastery = campaignState.masteries[world.skillCategory.id] ?: 0
            
            // Check world lock constraints! Each world checks preceding world mastery or base
            var isLocked = false
            if (world.id > 1) {
                val previousWorldCategory = GameWorlds.WORLDS[world.id - 2].skillCategory
                val previousMastery = campaignState.masteries[previousWorldCategory.id] ?: 0
                if (previousMastery < 35) {
                    isLocked = true
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable {
                        if (!isLocked) {
                            expandedWorldId = if (expandedWorldId == world.id) null else world.id
                        }
                    }
                    .shadow(4.dp, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = if (isLocked) Color(0xFF1E1E24).copy(0.6f) else RpgSurfaceLight
                ),
                border = BorderStroke(
                    1.dp,
                    if (expandedWorldId == world.id) RpgGold else RpgCardBorder.copy(0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isLocked) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Bloqueado",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = world.name,
                                    color = if (isLocked) Color.Gray else RpgGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = "Dificuldade: ${world.difficultyClass}",
                                color = if (world.difficultyClass == "Fácil") Color(0xFF81C784) else RpgBronze,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Circular Mastery Ring Progress (Custom Drawing for high game appeal!)
                        Box(
                            modifier = Modifier.size(45.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    color = Color.Black.copy(0.3f),
                                    style = Stroke(width = 4.dp.toPx())
                                )
                                drawArc(
                                    color = if (isLocked) Color.Gray else RpgForestGreen,
                                    startAngle = -90f,
                                    sweepAngle = (userMastery / 100f) * 360f,
                                    useCenter = false,
                                    style = Stroke(width = 4.dp.toPx())
                                )
                            }
                            Text(
                                text = "$userMastery%",
                                color = if (isLocked) Color.Gray else Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Expanded Level steps & Boss fight option
                    AnimatedVisibility(
                        visible = expandedWorldId == world.id && !isLocked,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Divider(color = RpgCardBorder)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = world.description,
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.SansSerif,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )

                            // Linear sequence indicator showing learning curve
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(0.3f))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Habilidade",
                                        tint = RpgCyanXp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = world.skillCategory.displayName,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "Requisito: ${world.requiredMastery}% Domínio de Base",
                                            color = Color.Gray,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                // Start Normal Training Quest Quiz button
                                Button(
                                    onClick = { onStartLevel(world.id, false) },
                                    colors = ButtonDefaults.buttonColors(containerColor = RpgForestGreen),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("start_quest_${world.id}")
                                ) {
                                    Text(
                                        "TREINO",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Boss card block
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1E1E)),
                                border = BorderStroke(1.dp, RpgRubyHp.copy(0.6f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = "Boss",
                                                tint = RpgRubyHp,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "CHEFE: ${world.bossName}",
                                                color = RpgRubyHp,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Text(
                                            text = world.bossDescription,
                                            color = Color.LightGray,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.SansSerif
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Start Boss battle
                                    Button(
                                        onClick = { onStartLevel(world.id, true) },
                                        colors = ButtonDefaults.buttonColors(containerColor = RpgRubyHp),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("start_boss_${world.id}")
                                    ) {
                                        Text(
                                            "X-CHEFÃO",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Locked state note info below items
                    if (isLocked) {
                        val previousWorldIndex = world.id - 2
                        val previousWorldName = GameWorlds.WORLDS[previousWorldIndex].name
                        Text(
                            text = "Bloqueado! Necessita de 35% de Domínio no '${previousWorldName}' para acessar.",
                            color = Color.LightGray.copy(0.7f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}


// --- SUB-SCREEN 3: ACTIVE QUIZ ENCOUNTER ROOM ---

@Composable
fun EncounterCombatScreen(
    worldId: Int,
    questions: List<GameQuestion>,
    activeIndex: Int,
    selectedChoice: Int?,
    showExplanation: Boolean,
    finishedState: EncounterFinished?,
    isBoss: Boolean,
    campaignState: GameCampaign,
    onChoiceSelected: (Int) -> Unit,
    onSendAnswer: () -> Unit,
    onNextOrFinish: () -> Unit,
    onForceExit: () -> Unit
) {
    if (finishedState != null) {
        // ENCOUNTER RESULTS DISPLAY PARCHMENT
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = if (finishedState == EncounterFinished.SUCCESS_COMPLETED) Color(0xFF142410) else Color(0xFF2C1010)
                ),
                border = BorderStroke(2.dp, if (finishedState == EncounterFinished.SUCCESS_COMPLETED) RpgForestGreen else RpgRubyHp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (finishedState == EncounterFinished.SUCCESS_COMPLETED) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = "Fim",
                        tint = if (finishedState == EncounterFinished.SUCCESS_COMPLETED) RpgGold else RpgRubyHp,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (finishedState == EncounterFinished.SUCCESS_COMPLETED) "VITÓRIA GLORIOSA!" else "BOCAL ENTUPIDO (DERROTA)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val detail = if (finishedState == EncounterFinished.SUCCESS_COMPLETED) {
                        "Você superou os desafios teóricos e mecânicos do templo! Suas habilidades musicais aumentaram e moedas de ouro foram adicionadas ao seu bolso."
                    } else {
                        "Seu bocal cansou e os pistões travaram! Você perdeu a batalha de domínio. Use o ouro na Taverna para comprar Óleo de Pistão ou uma Poção de Cura para restaurar sua embocadura!"
                    }

                    Text(
                        text = detail,
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.SansSerif,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { onForceExit() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (finishedState == EncounterFinished.SUCCESS_COMPLETED) RpgForestGreen else RpgRubyHp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "RETORNAR AO MAPA",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
        return
    }

    if (questions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nenhum exercício encontrado para este templo.", color = Color.White)
        }
        return
    }

    val currentQuestion = questions[activeIndex]
    val currentWorldZone = remember(worldId) { GameWorlds.WORLDS.find { it.id == worldId } }

    // First Turn: Teach Scope / Show Connection Overviews before starting the challenge
    var introConfirmed by remember(worldId, activeIndex) { mutableStateOf(false) }

    // Interative physical Valves and Piston indicators
    val playerValves = remember { mutableStateListOf(false, false, false) } // Piston 1, 2, 3

    // Get correct parameters dynamically
    val targetPistons = remember(currentQuestion) {
        if (currentQuestion.pistonMappingRequired.isNotEmpty()) {
            currentQuestion.pistonMappingRequired
        } else {
            val note = TrumpetMusic.NOTES.find { kotlin.math.abs(it.freq - currentQuestion.soundFreqToPlay) < 1.1f }
            note?.pistons ?: "0"
        }
    }

    val targetNoteName = remember(currentQuestion) {
        val note = TrumpetMusic.NOTES.find { kotlin.math.abs(it.freq - currentQuestion.soundFreqToPlay) < 1.1f }
        note?.representationPt ?: "Nota Alvo"
    }

    // Interactive Recovery Training Mode State
    var isRecoveryOpen by remember { mutableStateOf(false) }
    var recoveryShakeTrigger by remember { mutableStateOf(false) }

    // Reset valves and states on question shift
    LaunchedEffect(currentQuestion) {
        playerValves[0] = false
        playerValves[1] = false
        playerValves[2] = false
        isRecoveryOpen = false
    }

    if (currentWorldZone != null && !introConfirmed) {
        // WORLD INTRODUCTION STORY / PORTRAIT
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                colors = CardDefaults.cardColors(containerColor = RpgSurfaceLight),
                border = BorderStroke(2.dp, RpgGold.copy(0.7f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentWorldZone.name.uppercase(),
                        color = RpgGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "🏛️ PORTAL DO TEMPLO HARMONICO",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Divider(color = RpgCardBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Animated portrait of the boss
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.radialGradient(listOf(RpgRubyHp.copy(0.3f), Color.Transparent)))
                            .border(1.5.dp, RpgGold, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBoss) Icons.Default.Warning else Icons.Default.Casino,
                            contentDescription = "Representação do Desafio",
                            tint = if (isBoss) RpgRubyHp else RpgGold,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isBoss) "⚔️ DUELO INTEGRADO CONTRA CHEFE" else "🎯 MISSÃO DE DESAFIO",
                        color = if (isBoss) RpgRubyHp else RpgCyanXp,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (isBoss) currentWorldZone.bossName else "O Guardião Acadêmico",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    // ESCADA DE APRENDIZADO DETAILED DISCLOSURE
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(0.4f)),
                        border = BorderStroke(1.dp, RpgCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "ESPAÇO METODOLÓGICO:",
                                color = RpgGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Text(
                                text = "📚 Conteúdo Musical:",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                            Text(
                                text = currentWorldZone.description,
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.SansSerif,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // How it connects
                            Text(
                                text = "🔗 Integração de Transição:",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                            val conexaoDesc = when (worldId) {
                                1 -> "Conecta o puro fluxo do sopro e embocadura com o equilíbrio melódico básico de oitavas cromáticas do Mundo 2."
                                2 -> "Aproveita o sequenciamento natural do Mundo 1 e os expande com as enarmonias que prepararão os dedilhados rápidos do Mundo 3."
                                3 -> "Fixa a agilidade mecânica do Mundo 2 para estruturar as formas de intervalos em blocos que criam as escalas no Mundo 4."
                                4 -> "Usa os caminhos harmônicos livres construídos no Mundo 3 para injetar novas notas expressivas (como a Blue Note) no Mundo 5."
                                5 -> "Traz a tensão do Blues explorada no Mundo 4 para viabilizar solos fluidos autorais e maduros na Improvisação Guiada do Mundo 6."
                                else -> "Sintetiza todas as teorias e dedilhados passados para liberar a expressão máxima de performance na Base Prática."
                            }
                            Text(
                                text = conexaoDesc,
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.SansSerif,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Boss integration test logic explanation
                            Text(
                                text = "👾 O Teste Integrado do Chefe:",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                            Text(
                                text = currentWorldZone.bossDescription,
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = { introConfirmed = true },
                        colors = ButtonDefaults.buttonColors(containerColor = RpgGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ATACAR TEMPLO DESAFIO 🌬️",
                            color = RpgDarkBg,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        // Challenge Header HUD
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isBoss) Icons.Default.Warning else Icons.Default.Casino,
                    contentDescription = "Ícone de Duelo",
                    tint = if (isBoss) RpgRubyHp else RpgGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isBoss) "⚔️ DUELO: ${currentWorldZone?.bossName}" else "🎯 MISSÃO DE DEDILHADO",
                    color = if (isBoss) RpgRubyHp else RpgGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = "${activeIndex + 1}/${questions.size} Fases",
                color = Color.LightGray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // COMBAT ARENA GRAPHIC PANEL (Boss HP and Player HP display)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(0.4f))
                .border(1.dp, RpgCardBorder, RoundedCornerShape(12.dp))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Player Embouchure life
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Embocadura",
                        tint = RpgRubyHp,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Sua Embocadura: ${campaignState.stats.hp}/${campaignState.stats.maxHp}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                LinearProgressIndicator(
                    progress = { campaignState.stats.getHpProgress() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = RpgRubyHp,
                    trackColor = Color.Black.copy(0.5f)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Boss Shield/Life
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Inimigo",
                        tint = RpgGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Barreira do Chefe",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                val bossHp = ((questions.size - activeIndex).toFloat() / questions.size.toFloat())
                LinearProgressIndicator(
                    progress = { bossHp },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = RpgGold,
                    trackColor = Color.Black.copy(0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // THE RIDDLE CORE CONTAINER
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RpgSurfaceLight),
            border = BorderStroke(1.dp, RpgCardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "📝 DESAFIO / ENIGMA DO PORTAL:",
                    color = RpgGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = currentQuestion.prompt,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.SansSerif,
                    lineHeight = 18.sp
                )
                
                Spacer(modifier = Modifier.height(10.dp))

                // Listen Helper Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(0.3f))
                        .clickable { AudioSynth.playFreq(currentQuestion.soundFreqToPlay, 600) }
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Ouvir nota",
                        tint = RpgGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ESCUTAR DESIGN DE SOM (GUIA)",
                        color = RpgGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // GUITAR HERO NOTE TRACK (A ESTEIRA DE NOTAS)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F13)),
            border = BorderStroke(2.dp, RpgCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = "ESTEIRA DE NOTAS (TRUMPET HERO)",
                    color = RpgCyanXp,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Vertical scrolling Guitar-Hero style 3-lane highway
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(220.dp)
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF0F0F15), Color(0xFF1B1B26), Color(0xFF0C0C0F))
                            )
                        )
                        .border(2.dp, RpgCardBorder, RoundedCornerShape(12.dp))
                ) {
                    // Lane dividers / Strings
                    Row(modifier = Modifier.fillMaxSize()) {
                        Spacer(modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.fillMaxHeight().width(1.5.dp).background(Color.DarkGray.copy(0.35f)))
                        Spacer(modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.fillMaxHeight().width(1.5.dp).background(Color.DarkGray.copy(0.35f)))
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    // Target Zone Strike line at the far bottom
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp)
                            .height(24.dp)
                            .background(Color(0xFF00FFCC).copy(0.12f))
                            .border(BorderStroke(1.2.dp, Color(0xFF00FFCC).copy(0.35f)))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(3) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, Color(0xFF00FFCC).copy(0.5f), CircleShape)
                                )
                            }
                        }
                    }

                    Text(
                        text = "🔻 ÁREA DE SOPRO 🔻",
                        color = Color(0xFF00FFCC).copy(0.8f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 4.dp)
                    )

                    // Render scrolling notes queue based on index!
                    val queueSpan = 3
                    for (stepOffset in 0..queueSpan) {
                        val loopIndex = activeIndex + stepOffset
                        if (loopIndex < questions.size) {
                            val loopQuestion = questions[loopIndex]
                            
                            val expectedPistonsForNode = if (loopQuestion.pistonMappingRequired.isNotEmpty()) {
                                loopQuestion.pistonMappingRequired
                            } else {
                                val matchingNote = TrumpetMusic.NOTES.find { abs(it.freq - loopQuestion.soundFreqToPlay) < 1.1f }
                                matchingNote?.pistons ?: "0"
                            }

                            val representationForNode = if (loopQuestion.pistonMappingRequired.isNotEmpty()) {
                                val matchingNote = TrumpetMusic.NOTES.find { abs(it.freq - loopQuestion.soundFreqToPlay) < 1.1f }
                                matchingNote?.namePt ?: "Nota"
                            } else {
                                val matchingNote = TrumpetMusic.NOTES.find { abs(it.freq - loopQuestion.soundFreqToPlay) < 1.1f }
                                matchingNote?.representationPt ?: "Nota Alvo"
                            }

                            val isActiveNode = (stepOffset == 0)
                            
                            val verticalYOffset = when (stepOffset) {
                                0 -> 116.dp // aligned on strike line
                                1 -> 72.dp
                                2 -> 30.dp
                                else -> (-10).dp
                            }

                            val sizeFactor = if (isActiveNode) 1f else if (stepOffset == 1) 0.82f else 0.68f
                            val transparencyFactor = if (isActiveNode) 1f else if (stepOffset == 1) 0.6f else 0.3f

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        scaleX = sizeFactor
                                        scaleY = sizeFactor
                                        alpha = transparencyFactor
                                    }
                                    .offset(y = verticalYOffset)
                            ) {
                                if (expectedPistonsForNode == "0") {
                                    // Open notes: beautiful wide cyan strip cross-lane bar
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .fillMaxWidth(0.85f)
                                            .height(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFF00E5FF).copy(0.15f), Color(0xFF00E5FF), Color(0xFF00E5FF).copy(0.15f))
                                                )
                                            )
                                            .border(2.dp, Color.White, RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isActiveNode) "ABERTO (0) 📯 $representationForNode" else "ABERTO (0) - $representationForNode",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                } else {
                                    val activeValvesForThisNode = listOf(
                                        expectedPistonsForNode.contains("1"),
                                        expectedPistonsForNode.contains("2"),
                                        expectedPistonsForNode.contains("3")
                                    )

                                    // Connect dots if single node chord
                                    val activeValvesCount = activeValvesForThisNode.count { it }
                                    if (activeValvesCount > 1) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .fillMaxWidth(0.72f)
                                                .height(5.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(Brush.horizontalGradient(listOf(Color(0xFFFFB300), Color.White, Color(0xFFFFB300))))
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Piston 1 lane node - Neon Green
                                        if (activeValvesForThisNode[0]) {
                                            Box(
                                                modifier = Modifier
                                                    .size(if (isActiveNode) 30.dp else 22.dp)
                                                    .clip(CircleShape)
                                                    .shadow(5.dp, CircleShape)
                                                    .background(Brush.radialGradient(listOf(Color.White, Color(0xFF00FF88))))
                                                    .border(2.dp, Color.White, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (isActiveNode) "1" else "",
                                                    color = Color.Black,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.size(24.dp))
                                        }

                                        // Piston 2 lane node - Neon Hot Pink/Red
                                        if (activeValvesForThisNode[1]) {
                                            Box(
                                                modifier = Modifier
                                                    .size(if (isActiveNode) 30.dp else 22.dp)
                                                    .clip(CircleShape)
                                                    .shadow(5.dp, CircleShape)
                                                    .background(Brush.radialGradient(listOf(Color.White, Color(0xFFFF1E56))))
                                                    .border(2.dp, Color.White, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (isActiveNode) "2" else "",
                                                    color = Color.Black,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.size(24.dp))
                                        }

                                        // Piston 3 lane node - Neon Golden Amber
                                        if (activeValvesForThisNode[2]) {
                                            Box(
                                                modifier = Modifier
                                                    .size(if (isActiveNode) 30.dp else 22.dp)
                                                    .clip(CircleShape)
                                                    .shadow(5.dp, CircleShape)
                                                    .background(Brush.radialGradient(listOf(Color.White, Color(0xFFFF9F00))))
                                                    .border(2.dp, Color.White, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (isActiveNode) "3" else "",
                                                    color = Color.Black,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.size(24.dp))
                                        }
                                    }

                                    // Active Note Text display centered on top of target chord
                                    if (isActiveNode) {
                                        Text(
                                            text = representationForNode,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .background(Color.Black.copy(0.72f), RoundedCornerShape(6.dp))
                                                .border(0.8.dp, RpgGold.copy(0.4f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bypass and swallow the ancient horizontal stave
                if (false) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(Color.Black)
                            .border(1.dp, RpgCardBorder, RoundedCornerShape(8.dp))
                    ) {
                    // Stave horizontal lines
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        repeat(5) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color.DarkGray.copy(0.6f))
                            )
                        }
                    }

                    // Interactive target pulse receiver on the left
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(60.dp)
                            .align(Alignment.CenterStart)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF1E2F4C).copy(0.4f), Color.Transparent)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Glowing target ring
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(
                                    3.dp,
                                    if (showExplanation) RpgForestGreen else RpgGold,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Alvo",
                                tint = if (showExplanation) RpgForestGreen else RpgGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Falling nodes sequence from right
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 75.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Note 1 (The Targeted Active Note)
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .shadow(6.dp, CircleShape)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(RpgBronze, RpgGold)
                                    )
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = targetNoteName,
                                    color = RpgDarkBg,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "🔴",
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // Upcoming Note 2
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "Próxima...",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(6.dp),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                }

                Spacer(modifier = Modifier.height(6.dp))
                
                // Instructional slogan
                Text(
                    text = "Ajuste os pistões (1, 2, 3) combinando o dedilhado correto e clique em SOPRAR para detonar a nota!",
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // INTERACTIVE RETRO TRUMPET CYLINDER PISTONS (TECLAS DO TROMPETE)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RpgSurfaceLight),
            border = BorderStroke(1.dp, RpgBronze.copy(0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CONSOLE DE VÁLVULAS DO TROMPETE",
                    color = RpgBronze,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Layout representing the 3 mechanical valves
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    playerValves.forEachIndexed { pistonIdx, isPressed ->
                        val valveLabel = (pistonIdx + 1).toString()
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp)
                        ) {
                            // Cylindrical mechanical button drawing
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isPressed) {
                                            Brush.verticalGradient(
                                                listOf(RpgBronze.copy(0.9f), RpgGold.copy(0.9f))
                                            )
                                        } else {
                                            Brush.verticalGradient(
                                                listOf(Color.DarkGray, Color.Black)
                                            )
                                        }
                                    )
                                    .border(
                                        2.dp,
                                        if (isPressed) Color.White else RpgCardBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable(enabled = !showExplanation) {
                                        playerValves[pistonIdx] = !playerValves[pistonIdx]
                                        // Short mechanical ticking sound for feedback
                                        AudioSynth.playSFX(AudioSynth.SFXType.HIT_SUCCESS)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // Physical valve visual indicators with compression spring diagram
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(if (isPressed) Color.White else Color.LightGray)
                                            .border(1.5.dp, Color.Gray, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "PISTÃO $valveLabel",
                                        color = if (isPressed) RpgDarkBg else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = if (isPressed) "DOWN" else "UP",
                                        color = if (isPressed) RpgDarkBg.copy(0.7f) else Color.Gray,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    // Squeezing compression spring diagram representative lines
                                    Text(
                                        text = if (isPressed) "░░░░" else "▒▒▒▒▒▒",
                                        color = if (isPressed) RpgDarkBg.copy(0.5f) else Color.Gray,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 2.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // DYNAMICS SHOWS RESULTS OR SOPRAR MAIN ACTIONS
        if (showExplanation) {
            // SUCCESS HIGHLIGHT EXPLANATION PANEL
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(0.4f)),
                border = BorderStroke(1.dp, RpgForestGreen)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "✨ NOTAL ALVO DETONADA!",
                        color = Color(0xFF81C784),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentQuestion.explanation,
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    onNextOrFinish()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("next_question_button"),
                colors = ButtonDefaults.buttonColors(containerColor = RpgGold)
            ) {
                Text(
                    text = if (activeIndex + 1 >= questions.size) "FINALIZAR COMBATE 🏆" else "PRÓXIMA NOTA NA ESTEIRA ➡️",
                    color = RpgDarkBg,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            // BUTTON FOR ACTIVE BLOW (SOPRAR AGORA)
            Button(
                onClick = {
                    // Assemble state string corresponding to user's valves
                    val userValves = mutableListOf<String>()
                    if (playerValves[0]) userValves.add("1")
                    if (playerValves[1]) userValves.add("2")
                    if (playerValves[2]) userValves.add("3")
                    val userPistonsStr = if (userValves.isEmpty()) "0" else userValves.joinToString("-")

                    // Play the real acoustic note produced by those valves! Excellent simulation!
                    val realNoteSound = getNoteForValvesAndTarget(playerValves, currentQuestion.soundFreqToPlay)
                    AudioSynth.playFreq(realNoteSound.freq, 500)

                    val isCorrectFingering = userPistonsStr == targetPistons

                    if (isCorrectFingering) {
                        onChoiceSelected(currentQuestion.correctAnswerIndex)
                        onSendAnswer()
                    } else {
                        // Triggers the recovery panel modal / training system
                        AudioSynth.playSFX(AudioSynth.SFXType.HIT_FAIL)
                        isRecoveryOpen = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("submit_answer_button"),
                colors = ButtonDefaults.buttonColors(containerColor = RpgGold)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Soprar",
                        tint = RpgDarkBg
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🌬️ SOPRAR EM COMBATE!",
                        color = RpgDarkBg,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }

    // THE INTEGRATED RECOVERY MECHANISM OVERLAY (RESTAURADOR DE EMBOCADURA / SOPRO REPARADOR)
    if (isRecoveryOpen) {
        val animationOffset = if (recoveryShakeTrigger) 10.dp else 0.dp
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(0.75f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .offset(x = animationOffset),
                colors = CardDefaults.cardColors(containerColor = RpgSurfaceLight),
                border = BorderStroke(2.dp, RpgRubyHp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Desafinado",
                        tint = RpgRubyHp,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "⚠️ BOCAL DESALINHADO / DESAFINADO!",
                        color = RpgRubyHp,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "O som falhou ou guinchou! Entenda por que seu dedilhado não encaixou.",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    Divider(color = RpgCardBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Explanation Feedback
                    val activeValvesText = playerValves.mapIndexedNotNull { index, b -> if (b) (index + 1).toString() else null }.let { if (it.isEmpty()) "aberto (0)" else it.joinToString("-") }
                    val activePlayedNote = getNoteForValvesAndTarget(playerValves, currentQuestion.soundFreqToPlay)

                    Text(
                        text = "O que você colocou:",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Text(
                        text = "Pistões $activeValvesText -> Soaria a nota ${activePlayedNote.representationPt}",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 8.dp)
                    )

                    Text(
                        text = "O que o Chefe exige:",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Text(
                        text = "Nota: $targetNoteName -> Exige VÁLVULA(S) [ $targetPistons ]",
                        color = RpgGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 12.dp)
                    )

                    // Slow-motion graphical valve guide helper
                    Text(
                        text = "GUIA DE CORREÇÃO (SOPRO REPARADOR):",
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("1", "2", "3").forEach { piston ->
                            val shouldBeDown = targetPistons.contains(piston)
                            val isUserPressed = playerValves[piston.toInt() - 1]
                            
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (shouldBeDown) RpgGold.copy(0.2f) else Color.DarkGray.copy(0.6f))
                                    .border(
                                        2.dp,
                                        if (shouldBeDown) RpgGold else Color.Gray.copy(0.4f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        playerValves[piston.toInt() - 1] = !playerValves[piston.toInt() - 1]
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = piston,
                                        color = if (shouldBeDown) RpgGold else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = if (shouldBeDown) "BAIXAR" else "SOLTAR",
                                        color = if (shouldBeDown) RpgGold.copy(0.8f) else Color.Gray,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Buttons: 1. Retry with correction, 2. Accept penalty and escape
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                isRecoveryOpen = false
                                // Give player a slight hp penalty because they didn't get it first try
                                campaignState.failChallenge(
                                    category = currentWorldZone?.skillCategory ?: SkillCategory.NATURAL_NOTES,
                                    customHpPenalty = if (isBoss) 15 else 10 // reduced penalty on training recovery
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 6.dp)
                        ) {
                            Text("PAGAR HP", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }

                        Button(
                            onClick = {
                                val userValves = mutableListOf<String>()
                                if (playerValves[0]) userValves.add("1")
                                if (playerValves[1]) userValves.add("2")
                                if (playerValves[2]) userValves.add("3")
                                val userPistonsStr = if (userValves.isEmpty()) "0" else userValves.joinToString("-")

                                if (userPistonsStr == targetPistons) {
                                    // Succeeded recovery training! Restore hp bonus!
                                    AudioSynth.playSFX(AudioSynth.SFXType.HIT_SUCCESS)
                                    campaignState.stats = campaignState.stats.copy(
                                        hp = (campaignState.stats.hp + 8).coerceAtMost(campaignState.stats.maxHp)
                                    )
                                    isRecoveryOpen = false
                                    onChoiceSelected(currentQuestion.correctAnswerIndex)
                                    onSendAnswer()
                                } else {
                                    // Visual shake anim and buzzer
                                    AudioSynth.playSFX(AudioSynth.SFXType.HIT_FAIL)
                                    recoveryShakeTrigger = true
                                    Thread {
                                        Thread.sleep(200)
                                        recoveryShakeTrigger = false
                                    }.start()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RpgForestGreen),
                            modifier = Modifier.weight(1.4f)
                        ) {
                            Text("🌬️ REESTABELECER SOPRO", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

// Draw custom interactive pistons representation row
@Composable
fun TrumpetVisualPistonsRow(dedilhado: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        val keys = listOf("1", "2", "3")
        keys.forEach { num ->
            val isActive = dedilhado.contains(num)
            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isActive) RpgBronze else Color.DarkGray)
                    .border(2.dp, if (isActive) Color.White else Color.Gray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = num,
                    color = if (isActive) RpgDarkBg else Color.LightGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// Map player pressed valves list to the physically closest Trumpet note harmonic partial
fun getNoteForValvesAndTarget(valves: List<Boolean>, targetFreq: Float): com.example.music.MusicalNote {
    val activeList = mutableListOf<String>()
    if (valves.getOrNull(0) == true) activeList.add("1")
    if (valves.getOrNull(1) == true) activeList.add("2")
    if (valves.getOrNull(2) == true) activeList.add("3")
    val searchPistons = if (activeList.isEmpty()) "0" else activeList.joinToString("-")

    val matchingNotes = com.example.music.TrumpetMusic.NOTES.filter { it.pistons == searchPistons }
    if (matchingNotes.isEmpty()) {
        return com.example.music.TrumpetMusic.NOTES.first()
    }
    return matchingNotes.minByOrNull { kotlin.math.abs(it.freq - targetFreq) } ?: matchingNotes.first()
}


// --- SUB-SCREEN 4: INTERACTIVE PRACTICE & SOLO SANDBOX (SCREEN 1) ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractivePracticeScreen(campaignState: GameCampaign) {
    val stats = campaignState.stats
    
    // Sandbox options state
    val scaleBases = listOf(
        Pair("Lá Menor (Am Concert)", 11),  // B minor written on Bb trumpet
        Pair("Ré Menor (Dm Concert)", 4),   // E minor written
        Pair("Sol Menor (Gm Concert)", 9)   // A minor written
    )
    var selectedScaleIndex by remember { mutableStateOf(0) }
    var useBlueNote by remember { mutableStateOf(true) }

    // Assemble phrase
    val phrase = remember { mutableStateListOf<MusicalNote>() }
    var scaleNotes = remember(selectedScaleIndex, useBlueNote, stats.trumpetType) {
        val concertRootValue = scaleBases[selectedScaleIndex].second
        // if Bb trumpet selected, we apply written scale transposition (+2 semitones or equivalent conversion)
        val writtenRoot = if (stats.trumpetType == TrumpetType.BB_TRUMPET) {
            (concertRootValue + 2) % 13
        } else {
            concertRootValue
        }

        if (useBlueNote) {
            TrumpetMusic.getPentatonicWithBlueNote(writtenRoot)
        } else {
            TrumpetMusic.getMinorPentatonic(writtenRoot)
        }
    }

    // Practice feedback evaluation state
    var activeEvaluationResult by remember { mutableStateOf<ImprovisationResult?>(null) }
    
    // Trumpet interactive pistons state (toggled when clicked visually)
    val appPistonsState = remember { mutableStateListOf(false, false, false) } // 3 valves

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
    ) {
        Text(
            text = "🎷 MESA DE SOLOS & IMPROVISAÇÃO",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Text(
            text = "Aqui você toca de verdade! Selecione um tom, improvise clicando nas notas para construir uma frase e descubra se sua melodia tem senso musical.",
            color = Color.LightGray,
            fontSize = 12.sp,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Backdrop/Track scale selector
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = RpgSurfaceLight),
            border = BorderStroke(1.dp, RpgCardBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Ajustar Base do Solo:",
                    color = RpgGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Select scale base
                scaleBases.forEachIndexed { index, pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selectedScaleIndex == index) RpgGold.copy(0.2f) else Color.Transparent)
                            .clickable {
                                selectedScaleIndex = index
                                phrase.clear()
                                activeEvaluationResult = null
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedScaleIndex == index,
                            onClick = { 
                                selectedScaleIndex = index
                                phrase.clear()
                                activeEvaluationResult = null
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = RpgGold)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = pair.first,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Divider(color = RpgCardBorder)
                Spacer(modifier = Modifier.height(4.dp))

                // Toggle Blue Note Switch Option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Liberar Blue Note (Passagem b5)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Adiciona a quarta aumentada na pentatônica.",
                            color = Color.LightGray,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                    Switch(
                        checked = useBlueNote,
                        onCheckedChange = {
                            useBlueNote = it
                            phrase.clear()
                            activeEvaluationResult = null
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = RpgGold)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // INTERACTIVE MECHANICAL TRUMPET VALVES DEVICE (Vibrations on clicks!)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(0.4f)),
            border = BorderStroke(1.dp, RpgBronze.copy(0.5f))
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "DEDILHADOR MECÂNICO INTERATIVO",
                    color = RpgBronze,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // The 3 Valve keys that sink on touch/clicks
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    listOf("1", "2", "3").forEachIndexed { valIdx, name ->
                        val isPressed = appPistonsState[valIdx]
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isPressed) RpgBronze else Color.DarkGray)
                                .border(2.dp, if (isPressed) Color.White else Color.Gray, RoundedCornerShape(8.dp))
                                .clickable {
                                    appPistonsState[valIdx] = !appPistonsState[valIdx]
                                    AudioSynth.playSFX(AudioSynth.SFXType.HIT_SUCCESS)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = name,
                                    color = if (isPressed) RpgDarkBg else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (isPressed) "DOWN" else "UP",
                                    color = if (isPressed) RpgDarkBg.copy(0.7f) else Color.Gray,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Dica: Você pode ativar os pistões livremente ou apenas clicar nos blocos de notas abaixo. Clicar nas notas automaticamente simulará o dedilhado correto nos pistões acima!",
                    color = Color.LightGray.copy(0.8f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // DISPLAY MUSIC SCALE KEYBOARD CELLS
        Text(
            text = "Notas Disponíveis do Acorde:",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // Keyboard of scale notes
        scaleNotes.forEach { note ->
            val isBlue = useBlueNote && (note.semitoneValue % 12 == (scaleBases[selectedScaleIndex].second + 6) % 12)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isBlue) Color(0xFF1E283C) else RpgSurfaceLight)
                    .border(
                        1.dp,
                        if (isBlue) RpgCyanXp.copy(0.8f) else RpgCardBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        // Play synthesized note
                        AudioSynth.playFreq(note.freq, 500)
                        
                        // Set physical mockup valves indicators to match correct fingering (pistons sequence)
                        // Clear valves then trigger correct fingers
                        appPistonsState[0] = note.pistons.contains("1")
                        appPistonsState[1] = note.pistons.contains("2")
                        appPistonsState[2] = note.pistons.contains("3")

                        // Add note to phrase sequence
                        phrase.add(note)
                        activeEvaluationResult = null
                    }
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Nota",
                        tint = if (isBlue) RpgCyanXp else RpgGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = note.representationPt,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (isBlue) "Blue Note (Tensão b5)" else "Nota da Escala",
                            color = if (isBlue) RpgCyanXp else Color.LightGray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }

                // Pistons configuration graphic display inside note row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Válvulas: [ ${note.pistons} ]",
                        color = RpgBronze,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Tocar",
                        tint = RpgGold,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // MUSIC PLAYBACK RIBBON (Shows composed melodies!)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(0.4f)),
            border = BorderStroke(1.dp, RpgCardBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SUA FRASE CRIADA (${phrase.size} notas):",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    if (phrase.isNotEmpty()) {
                        Text(
                            text = "Limpar 🗑️",
                            color = RpgRubyHp,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.clickable { phrase.clear(); activeEvaluationResult = null }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Ribbon
                if (phrase.isEmpty()) {
                    Text(
                        text = "Toque nos blocos de notas acima para montar seu solo de trompete...",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 6.dp)
                    ) {
                        phrase.forEach { note ->
                            Box(
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(RpgBronze)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = note.id,
                                    color = RpgDarkBg,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Buttons: 1. Touch/Play All phrase, 2. Evaluate Solo
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            // play in cascade sequence
                            Thread {
                                phrase.forEach { note ->
                                    AudioSynth.playFreq(note.freq, 350)
                                    Thread.sleep(400)
                                }
                            }.start()
                        },
                        enabled = phrase.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RpgSurfaceLight)
                    ) {
                        Text(
                            "REPRODUZIR",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = {
                            val concertRootValue = scaleBases[selectedScaleIndex].second
                            val writtenRoot = if (stats.trumpetType == TrumpetType.BB_TRUMPET) {
                                (concertRootValue + 2) % 13
                            } else {
                                concertRootValue
                            }

                            val evaluation = TrumpetMusic.evaluateImprovisation(
                                phrase = phrase,
                                rootSemitone = writtenRoot,
                                withBlueNote = useBlueNote
                            )
                            activeEvaluationResult = evaluation

                            // Award gold or XP based on successful solos!
                            if (evaluation.score >= 60) {
                                AudioSynth.playSFX(AudioSynth.SFXType.LEVEL_UP)
                                campaignState.completeChallenge(
                                    category = SkillCategory.GUIDED_IMPROVISATION,
                                    wasPerfect = evaluation.score >= 85,
                                    speedBonus = false
                                )
                            } else {
                                AudioSynth.playSFX(AudioSynth.SFXType.HIT_FAIL)
                            }
                        },
                        enabled = phrase.isNotEmpty(),
                        modifier = Modifier
                            .weight(1.2f)
                            .padding(start = 4.dp)
                            .testTag("solo_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = RpgGold)
                    ) {
                        Text(
                            "SOLAR E AVALIAR",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = RpgDarkBg
                        )
                    }
                }
            }
        }

        // EVALUATION PARCHMENT DIALOG DISPLAY
        activeEvaluationResult?.let { eval ->
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF0)), // Parchment look
                border = BorderStroke(2.dp, RpgGold)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📜 PERGAMINHO DO MAESTRO",
                        color = Color(0xFF5D4037),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = eval.summaryPt,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Detalhamento e Críticas:",
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    eval.feedbacks.forEach { feedback ->
                        Text(
                            text = feedback,
                            color = if (feedback.startsWith("+")) Color(0xFF2E7D32) else Color(0xFFC2185B),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.SansSerif,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Para melhorar seu solo: Escolha uma base menor, monte sequências variando a altura das notas e garanta resolver em uma nota aberta estável do trompete como Dó ou Sol no final!",
                        color = Color(0xFF5D4037),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }
    }
}


// --- SUB-SCREEN 5: TAVERNA DO FERREIRO GEAR & SHOP (SCREEN 2) ---

@Composable
fun TavernShopScreen(campaignState: GameCampaign) {
    val stats = campaignState.stats

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
    ) {
        Text(
            text = "🍻 TAVERNA DO SOPRO & LOJA",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Text(
            text = "Negocie suas Moedas d'Ouro ganhas em desafios pedagógicos para comprar consertos, óleos ou trompetes lendários com bônus passivos mágicos!",
            color = Color.LightGray,
            fontSize = 12.sp,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Health recovery card inside tavern
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1010)),
            border = BorderStroke(1.dp, RpgRubyHp.copy(0.6f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "🧪 Elixir de Recuperação Plena",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "Restaura todo seu HP desgastado de imediato por um pequeno tributo.",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                Button(
                    onClick = {
                        if (stats.gold >= 30) {
                            campaignState.healPlayerFull()
                            AudioSynth.playSFX(AudioSynth.SFXType.ITEM_BUY)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RpgRubyHp),
                    shape = RoundedCornerShape(6.dp),
                    enabled = stats.gold >= 30 && stats.hp < stats.maxHp,
                    modifier = Modifier.testTag("potion_heal_button")
                ) {
                    Text(
                        "30 GP",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Equipamentos & Consumíveis Disponíveis:",
            color = RpgGold,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        GameShop.ITEMS.forEach { item ->
            val hasTrumpet = !item.isConsumable && stats.purchasedTrumpets.contains(item.id)
            val ownedText = when (item.id) {
                "valve_oil" -> " (Possui: ${stats.valveOilCount})"
                "jazz_mute" -> " (Possui: ${stats.cupMuteCount})"
                "tuning_slide" -> " (Possui: ${stats.tuningSlideCount})"
                else -> ""
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .shadow(4.dp, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = RpgSurfaceLight),
                border = BorderStroke(
                    1.dp,
                    if (hasTrumpet) RpgForestGreen.copy(0.6f) else RpgCardBorder
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name + ownedText,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Bônus: ${item.effectDesc}",
                                color = RpgGold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = {
                                if (campaignState.buyItem(item)) {
                                    AudioSynth.playSFX(AudioSynth.SFXType.ITEM_BUY)
                                }
                            },
                            enabled = stats.gold >= item.cost && !hasTrumpet,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasTrumpet) RpgForestGreen else RpgBronze
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.testTag("buy_${item.id}")
                        ) {
                            Text(
                                text = if (hasTrumpet) "ADQUIRIDO" else "${item.cost} GP",
                                color = if (hasTrumpet) Color.White else RpgDarkBg,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.description,
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }
    }
}


// --- SUB-SCREEN 6: CHARACTER PROFILE & HQ STATS (SCREEN 3) ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CharacterHQScreen(campaignState: GameCampaign) {
    val stats = campaignState.stats
    val masteries = campaignState.masteries

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
    ) {
        Text(
            text = "🛡️ QUARTEL-GENERAL (QG)",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Text(
            text = "Analise o domínio dinâmico do seu cérebro sobre as habilidades essenciais do trompete e equipe os instrumentos adquiridos.",
            color = Color.LightGray,
            fontSize = 12.sp,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Equipping style selection trigger
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = RpgSurfaceLight),
            border = BorderStroke(1.dp, RpgCardBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "SELECIONAR SEU TROMPETE ATIVO:",
                    color = RpgGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                val availableStyles = listOf(
                    Triple("standard_brass", "Trompete de Latão Tradicional (Livre)", "Sem bônus passivo"),
                    Triple("silver_star", "Trompete Estelar de Prata", "+15% Moedas d'Ouro por vitória"),
                    Triple("cosmic_gold", "Trompete Dourado Cósmico", "Dobro de ganhos de XP (+100%)")
                )

                availableStyles.forEach { (styleId, labelName, passiveDesc) ->
                    val isPurchased = stats.purchasedTrumpets.contains(styleId)
                    val isActive = stats.activeTrumpetStyle == styleId

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when {
                                    isActive -> RpgGold.copy(0.2f)
                                    !isPurchased -> Color.Black.copy(0.2f)
                                    else -> Color.Transparent
                                }
                            )
                            .clickable(enabled = isPurchased && !isActive) {
                                campaignState.selectTrumpet(styleId)
                                AudioSynth.playSFX(AudioSynth.SFXType.HIT_SUCCESS)
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!isPurchased) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Bloqueado",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = labelName,
                                    color = if (isPurchased) Color.White else Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = passiveDesc,
                                color = if (isPurchased) RpgGold else Color.Gray,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }

                        if (isActive) {
                            Text(
                                text = "EQUIPADO",
                                color = RpgGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.testTag("equipped_item_$styleId")
                            )
                        } else if (isPurchased) {
                            Text(
                                text = "EQUIPAR",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Mastery breakdown bars list
        Text(
            text = "📈 DOMÍNIO POR CATEGORIA PEDAGÓGICA",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        SkillCategory.values().forEach { category ->
            val scoreVal = masteries[category.id] ?: 0

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = RpgSurfaceLight),
                border = BorderStroke(1.dp, RpgCardBorder)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = category.displayName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = category.desc,
                                color = Color.LightGray,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }

                        Text(
                            text = "$scoreVal%",
                            color = RpgGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { scoreVal / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = RpgForestGreen,
                        trackColor = Color.Black.copy(0.4f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // RECOVERY EXPEDITIONS DECK (from spaced repetition mistakes!)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B263B)),
            border = BorderStroke(1.dp, RpgCyanXp.copy(0.6f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🗃️ DECK DE RECUPERAÇÃO ESPAÇADA",
                        color = RpgCyanXp,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    val activeDeckCards = campaignState.deck.values.filter { it.errorCount > 0 && it.successStreak < 3 }
                    Text(
                        text = "${activeDeckCards.size} Pendentes",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                val activeDeckCards = campaignState.deck.values.filter { it.errorCount > 0 && it.successStreak < 3 }
                if (activeDeckCards.isEmpty()) {
                    Text(
                        text = "Nenhum erro registrado recentemente! Excelente! A memória dos pistões e das regras enarmônicas está limpa e dominada.",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                } else {
                    activeDeckCards.forEach { card ->
                        val targetNote = TrumpetMusic.getById(card.noteId) ?: return@forEach

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(0.3f))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Revisar Nota: " + targetNote.representationPt,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Erros nesta nota: ${card.errorCount} | Acertos seguidos: ${card.successStreak}/3",
                                    color = Color.LightGray,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }

                            Button(
                                onClick = {
                                    // Play the target correct sound so they memorize it actively, clear the card streak and restore health
                                    AudioSynth.playFreq(targetNote.freq, 700)
                                    campaignState.registerNoteSuccess(card.noteId)
                                    // Award partial HP recover on memory reconstruction
                                    campaignState.stats = campaignState.stats.copy(
                                        hp = (campaignState.stats.hp + 10).coerceAtMost(campaignState.stats.maxHp),
                                        gold = campaignState.stats.gold + 5
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RpgCyanXp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "REVER (SOM)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = RpgDarkBg
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


// --- SUB-SCREEN 7: FULL HEALTH / DEFEATED RESTORATOR (STATE SEPARATION SCREEN) ---

@Composable
fun CharacterDefeatedScreen(campaignState: GameCampaign) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF210808))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RpgSurfaceLight)
                .border(2.dp, RpgRubyHp, RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Derrotado",
                tint = RpgRubyHp,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                "💀 VOCÊ FOI DERROTADO!",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Sua embocadura cansou e seu HP musical zerou. O aprendizado requer oxigênio e descanso prático para evitar lesões musculares dos lábios!",
                color = Color.LightGray,
                fontSize = 13.sp,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Option 1: Buy health potion if has gold
            val stats = campaignState.stats
            Button(
                onClick = {
                    campaignState.healPlayerFull()
                    AudioSynth.playSFX(AudioSynth.SFXType.LEVEL_UP)
                },
                enabled = stats.gold >= 30,
                colors = ButtonDefaults.buttonColors(containerColor = RpgRubyHp),
                modifier = Modifier.fillMaxWidth().testTag("revive_buy_button")
            ) {
                Text(
                    "COMPRAR POTION DE CURA (30 GP)",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Option 2: Active study recovery task (free but requires doing spaced repetition sound memorization!)
            Button(
                onClick = {
                    // Help them recover HP for free by restoring status
                    campaignState.stats = campaignState.stats.copy(hp = 50)
                    campaignState.triggerChange()
                },
                colors = ButtonDefaults.buttonColors(containerColor = RpgForestGreen),
                modifier = Modifier.fillMaxWidth().testTag("revive_study_button")
            ) {
                Text(
                    "DESCANSO DA EMBOCADURA (+50 HP GRÁTIS)",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
