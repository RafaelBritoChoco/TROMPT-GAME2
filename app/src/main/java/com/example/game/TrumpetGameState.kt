package com.example.game

import com.example.music.MusicalNote
import com.example.music.TrumpetMusic

// --- Game Models ---

data class PlayerStats(
    val level: Int = 1,
    val xp: Int = 0,
    val maxXp: Int = 100,
    val hp: Int = 100,
    val maxHp: Int = 100,
    val gold: Int = 120, // Initial gold pocket money
    val trumpetType: TrumpetType = TrumpetType.BB_TRUMPET,
    val activeGear: List<ShopItem> = emptyList(),
    // Keep track of bought permanent instruments
    val purchasedTrumpets: List<String> = listOf("standard_brass"),
    val activeTrumpetStyle: String = "standard_brass", // standard_brass, silver_star, cosmic_gold
    // Active consumables count
    val valveOilCount: Int = 0, // Golden Valve Oil protects from high damage
    val cupMuteCount: Int = 0,  // Classic Jazz Cup Mute: increases XP on solos
    val tuningSlideCount: Int = 0 // Tuning Slide: auto pass bypass items
) {
    fun getXpProgress(): Float = xp.toFloat() / maxXp.toFloat()
    fun getHpProgress(): Float = hp.toFloat() / maxHp.toFloat()
}

enum class TrumpetType(val displayName: String, val offsetSemitones: Int) {
    BB_TRUMPET("Trumpet Si♭ (Bb)", 2),
    C_TRUMPET("Trumpet Dó (C)", 0)
}

data class ShopItem(
    val id: String,
    val name: String,
    val description: String,
    val cost: Int,
    val isConsumable: Boolean,
    val effectDesc: String,
    val iconName: String
)

object GameShop {
    val ITEMS = listOf(
        ShopItem(
            "silver_star",
            "Trompete de Prata Estelar",
            "Um instrumento brilhante e leve. Aumenta todo ouro ganho em +15%!",
            150,
            false,
            "+15% de Moedas d'Ouro por vitória em missões.",
            "silver"
        ),
        ShopItem(
            "cosmic_gold",
            "Trompete Dourado Cósmico",
            "Instrumento lendário banhado a ouro estelar. Dobra o XP adquirido!",
            350,
            false,
            "+100% de bônus de XP em todas as atividades.",
            "gold_trumpet"
        ),
        ShopItem(
            "valve_oil",
            "Óleo de Pistão Especial",
            "Consumível lubrificante. Reduz o dano de respostas erradas pela metade!",
            30,
            true,
            "Dura 1 batalha. -50% dano sofrido por perdas de HP.",
            "potion"
        ),
        ShopItem(
            "jazz_mute",
            "Surdina Cup de Jazz",
            "Surdina clássica de alumínio. O som sussurrado dobra a diversão do improviso!",
            45,
            true,
            "Dura 1 solo. Dobra o XP ganho na área de Improvisação.",
            "mute"
        ),
        ShopItem(
            "tuning_slide",
            "Volta de Afinação Rápida",
            "Permite contornar e pular uma questão difícil em chefes recuperando vida.",
            60,
            true,
            "Pula questão difícil com sucesso instantâneo de combate.",
            "slide"
        )
    )
}

enum class SkillCategory(val id: String, val displayName: String, val desc: String) {
    NATURAL_NOTES("natural_notes", "Vales das Naturais", "Sequenciamento e identificação de Dó a Si"),
    CHROMATIC_SEQUENCE("chromatic_sequence", "Caverna do Cromático", "Domínio de enarmônicas e exceções (E-F, B-C)"),
    VALVE_COMBINATIONS("valve_combinations", "Templo dos Pistões", "Agilidade visual de comandos/dedilhados"),
    PENTATONIC_DEGREES("pentatonic_degrees", "Pico Pentatônico", "Fórmula menor e intervalos semitons (0-3-5-7-10)"),
    BLUE_NOTE_MAGIC("blue_note_magic", "Pântano da Blue Note", "A instabilidade dramática da 4ª aumentada passagem"),
    GUIDED_IMPROVISATION("guided_improvisation", "Guilda do Solo", "Criação de melodias coerentes sobre bases reais")
}

data class SkillMastery(
    val category: SkillCategory,
    val progressPercent: Int = 0 // Ranges 0 to 100
)

data class WorldZone(
    val id: Int,
    val name: String,
    val description: String,
    val skillCategory: SkillCategory,
    val requiredMastery: Int,
    val stepsCount: Int = 4,
    val bossName: String,
    val bossDescription: String,
    val difficultyClass: String
)

object GameWorlds {
    val WORLDS = listOf(
        WorldZone(
            1,
            "Mundo 1: O Mapa das Notas",
            "Domine as 7 notas naturais (Dó a Si). A ponte inicial essencial que liga o sopro do ar (som fundamental) ao dedilhado estruturado de oitavas.",
            SkillCategory.NATURAL_NOTES,
            0,
            5,
            "O Guardião da Clave",
            "Desafio de Circularidade: O Guardião exige que você toque a escala natural ascendente e descendente de forma sequencial. Mostre precisão absoluta nos pistões abertos e fechados!",
            "Fácil"
        ),
        WorldZone(
            2,
            "Mundo 2: O Cromático",
            "Suba semitons com sustenidos, bemóis e as regras secretas das notas coladas (E-F, B-C). Conecta as decolagens de meio tom com a mecânica dos pistões do trompete.",
            SkillCategory.CHROMATIC_SEQUENCE,
            35,
            5,
            "Draco o Enarmônico",
            "Duelo Enarmônico: Draco emite uma frequência e se transforma mudando de nome (ex: Ré# para Mib). Você deve descobrir a nota equivalente fónica e tocar a combinação exata de válvulas!",
            "Médio"
        ),
        WorldZone(
            3,
            "Mundo 3: O Trompete",
            "Domínio mecânico puro de agilidade. Conecte o timing visual dos dedilhados 1, 2, 3 às notas da escala. Transiciona o aluno da teoria das notas isoladas para a agilidade melódica.",
            SkillCategory.VALVE_COMBINATIONS,
            40,
            6,
            "O Mestre dos Três Pistões",
            "Combate de Memorização Rítmica: O Mestre faz sequências de alta velocidade de pistões mudando o dedilhado. imite e toque cada nota no tempo exato para desarmar a sua armadilha mecânica!",
            "Médio"
        ),
        WorldZone(
            4,
            "Mundo 4: Pentatônica",
            "Estudo dos intervalos e semitons da Pentatônica Menor (0-3-5-7-10). Conecta o dedilhado mecânico do instrumento à lógica melódica para a improvisação livre.",
            SkillCategory.PENTATONIC_DEGREES,
            45,
            5,
            "Esfinge dos Cinco Graus",
            "Teste de Graus da Escala: A Esfinge dita ordens intervalares como 'toque a terça menor' ou 'toque a quinta justa' em Lá Menor. Só o domínio desse mapa de intervalos te deixará passar!",
            "Difícil"
        ),
        WorldZone(
            5,
            "Mundo 5: Blue Note",
            "Domine a dramática quarta aumentada (b5) como uma instável nota de passagem. Transiciona a harmonia pentatônica pura para o sentimento expressivo e melancólico do Blues.",
            SkillCategory.BLUE_NOTE_MAGIC,
            45,
            5,
            "O Fantasma de New Orleans",
            "Desafio da Resolução de Tensão: O Fantasma entoa uma base de Blues lento e cobra o uso correto da Blue Note como passagem, resolvendo nas notas estáveis (4ª ou 5ª). Erre a resolução e ele sugará seu HP!",
            "Lendário"
        ),
        WorldZone(
            6,
            "Mundo 6: Improvisação Guiada",
            "O clímax do seu aprendizado. Crie frases coerentes e com bom senso melódico sob backing tracks. Conecta os mundos anteriores em um espetáculo de solo autoral de trompete.",
            SkillCategory.GUIDED_IMPROVISATION,
            50,
            4,
            "O Maestro de Ouro",
            "O Julgamento do Solo Perfeito: Crie um solo autoral de 8 notas seguindo os preceitos de notas estáveis (como tônica/quinta) e bom senso rítmico. O Maestro avaliará sua alma musical!",
            "Mestre"
        )
    )
}

// Spaced repetition flashcard model for tracking tricky note associations
data class NoteFlashcard(
    val noteId: String,
    val typeOfCard: String, // "PISTON" or "CHROMATIC" or "BEFORE_AFTER"
    val errorCount: Int = 0,
    val successStreak: Int = 0,
    val nextReviewTimestamp: Long = 0L, // 0 means review immediately
    val difficultyWeight: Float = 1.0f // Scale modifier
)

// --- Game Campaign State Organizer ---

class GameCampaign {
    var stats = PlayerStats()
    var masteries = mutableMapOf<String, Int>().apply {
        SkillCategory.values().forEach { put(it.id, 0) }
    }
    
    // Spaced repetition cards
    var deck = mutableMapOf<String, NoteFlashcard>()

    // Current screen context
    var activeWorldId: Int = 1
    var activeStepIndex: Int = 0 // 0 means world map lobby, 1..6 are challenges
    
    // Custom state callbacks
    var onStateChanged: (() -> Unit)? = null

    fun triggerChange() {
        onStateChanged?.invoke()
    }

    // Spend gold
    fun buyItem(item: ShopItem): Boolean {
        if (stats.gold >= item.cost) {
            val updated = when (item.id) {
                "silver_star" -> stats.copy(
                    gold = stats.gold - item.cost,
                    purchasedTrumpets = (stats.purchasedTrumpets + "silver_star").distinct()
                )
                "cosmic_gold" -> stats.copy(
                    gold = stats.gold - item.cost,
                    purchasedTrumpets = (stats.purchasedTrumpets + "cosmic_gold").distinct()
                )
                "valve_oil" -> stats.copy(
                    gold = stats.gold - item.cost,
                    valveOilCount = stats.valveOilCount + 1
                )
                "jazz_mute" -> stats.copy(
                    gold = stats.gold - item.cost,
                    cupMuteCount = stats.cupMuteCount + 1
                )
                "tuning_slide" -> stats.copy(
                    gold = stats.gold - item.cost,
                    tuningSlideCount = stats.tuningSlideCount + 1
                )
                else -> stats
            }
            stats = updated
            triggerChange()
            return true
        }
        return false
    }

    fun selectTrumpet(styleId: String) {
        if (styleId in stats.purchasedTrumpets) {
            stats = stats.copy(activeTrumpetStyle = styleId)
            triggerChange()
        }
    }

    fun setTrumpetKey(type: TrumpetType) {
        stats = stats.copy(trumpetType = type)
        triggerChange()
    }

    // Award XP, Gold and update dynamic mastery safely
    fun completeChallenge(category: SkillCategory, wasPerfect: Boolean, speedBonus: Boolean) {
        var baseGold = if (wasPerfect) 25 else 15
        var baseXP = if (wasPerfect) 35 else 20
        if (speedBonus) {
            baseGold += 5
            baseXP += 10
        }

        // Apply instrument buffs
        if (stats.activeTrumpetStyle == "silver_star") {
            baseGold = (baseGold * 1.15).toInt()
        }
        if (stats.activeTrumpetStyle == "cosmic_gold") {
            baseXP = (baseXP * 2.0).toInt()
        }

        // Add consumables modifiers
        if (category == SkillCategory.GUIDED_IMPROVISATION && stats.cupMuteCount > 0) {
            baseXP *= 2
            stats = stats.copy(cupMuteCount = stats.cupMuteCount - 1)
        }

        // Log states
        var newXp = stats.xp + baseXP
        var newLevel = stats.level
        var newMaxXp = stats.maxXp
        if (newXp >= newMaxXp) {
            newLevel++
            newXp -= newMaxXp
            newMaxXp = (newMaxXp * 1.3).toInt()
        }

        stats = stats.copy(
            level = newLevel,
            xp = newXp,
            maxXp = newMaxXp,
            gold = stats.gold + baseGold,
            hp = (stats.hp + 15).coerceAtMost(stats.maxHp) // Healing on success
        )

        // Mastery adjustment
        val currentMastery = masteries[category.id] ?: 0
        val masteryGain = if (wasPerfect) 10 else 6
        masteries[category.id] = (currentMastery + masteryGain).coerceAtMost(100)

        triggerChange()
    }

    // Wrong answer consequences
    fun failChallenge(category: SkillCategory, customHpPenalty: Int = 20) {
        var calculatedDamage = customHpPenalty
        if (stats.valveOilCount > 0) {
            calculatedDamage = (calculatedDamage * 0.5f).toInt()
            stats = stats.copy(valveOilCount = stats.valveOilCount - 1)
        }

        stats = stats.copy(
            hp = (stats.hp - calculatedDamage).coerceAtLeast(0)
        )

        // Mastery decay slight drop to represent decay on failed items
        val currentMastery = masteries[category.id] ?: 0
        masteries[category.id] = (currentMastery - 3).coerceAtLeast(0)

        triggerChange()
    }

    fun healPlayerFull() {
        stats = stats.copy(hp = stats.maxHp, gold = (stats.gold - 30).coerceAtLeast(0))
        triggerChange()
    }

    // Spaced repetition: Feed errors back to review cards stack
    fun registerNoteError(noteId: String, type: String) {
        val currentCard = deck[noteId] ?: NoteFlashcard(noteId, type)
        deck[noteId] = currentCard.copy(
            errorCount = currentCard.errorCount + 1,
            successStreak = 0,
            difficultyWeight = currentCard.difficultyWeight + 0.3f,
            nextReviewTimestamp = System.currentTimeMillis() + 15000L // reappear extremely soon (15s)
        )
        triggerChange()
    }

    fun registerNoteSuccess(noteId: String) {
        val currentCard = deck[noteId] ?: return
        val newStreak = currentCard.successStreak + 1
        val safeInterval = 30000L * newStreak
        deck[noteId] = currentCard.copy(
            successStreak = newStreak,
            difficultyWeight = (currentCard.difficultyWeight - 0.2f).coerceAtLeast(0.5f),
            nextReviewTimestamp = System.currentTimeMillis() + safeInterval
        )
        triggerChange()
    }
}
