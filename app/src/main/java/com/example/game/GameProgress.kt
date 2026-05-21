package com.example.game

import android.content.Context
import android.util.Log
import com.example.music.MusicalNote
import com.example.music.TrumpetMusic
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

// RPG Item
data class RpgItem(
    val id: String,
    val name: String,
    val desc: String,
    val price: Int,
    val statBoost: String,
    val isEquipped: Boolean = false
)

// Spaced Repetition SM-2 Flashcard
data class Flashcard(
    val id: String,
    val question: String,
    val answer: String,
    val category: String,
    val repetitions: Int = 0,
    val intervalDays: Int = 1,
    val easeFactor: Float = 2.5f,
    val nextReviewTimeMs: Long = 0L
)

// Monster definition for RPG battles
data class Monster(
    val id: String,
    val name: String,
    val title: String,
    val hp: Int,
    val maxHp: Int,
    val damage: Int,
    val imgColor: String, // Hex string
    val weakness: String, // Portuguese tips
    val introSpeech: String
)

// Educational Worlds/Quest Lines
data class QuestWorld(
    val id: Int,
    val name: String,
    val subtitle: String,
    val description: String,
    val unlocked: Boolean,
    val category: String,
    val monster: Monster
)

// Main Game Progress State
data class GameState(
    val level: Int = 1,
    val xp: Int = 0,
    val maxXp: Int = 100,
    val hp: Int = 100,
    val maxHp: Int = 100,
    val gold: Int = 50,
    val equippedItemName: String = "Sopro Básico",
    val equippedBoost: String = "Nenhum",
    
    // Mastery scores (0 to 100)
    val masteryNaturais: Int = 0,
    val masteryCromatico: Int = 0,
    val masteryPistoes: Int = 0,
    val masteryPentatonica: Int = 0,
    val masteryBlueNotes: Int = 0,
    val masteryTransposicao: Int = 0,

    // Deck & Inventory
    val inventory: List<RpgItem> = listOf(
        RpgItem("bocal_standard", "Bocal Standard", "Bocal de fábrica. Força básica.", 0, "Dano normal (+0)", true)
    ),
    val deck: List<Flashcard> = emptyList()
)

object GameProgressManager {
    private const val TAG = "GameProgressManager"
    private const val PREFS_NAME = "note_quest_trumpet_prefs"
    private const val STATE_KEY = "game_state_json"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    
    private val stateAdapter = moshi.adapter(GameState::class.java)

    /**
     * Set up default flashcards of Music Theory & Trumpet
     */
    fun createDefaultDeck(): List<Flashcard> {
        val now = System.currentTimeMillis()
        return listOf(
            Flashcard(
                "fc1",
                "Quais notas têm o intervalo colado (meio tom), sem sustenido entre elas?",
                "Mi para Fá (E-F) e Si para Dó (B-C). Todo o restante possui um semitom no meio!",
                "Notas Naturais", nextReviewTimeMs = now
            ),
            Flashcard(
                "fc2",
                "Quais são os pistões pressionados para tocar a nota Ré (D4)?",
                "Pistões 1 e 3!",
                "Pistões do Trompete", nextReviewTimeMs = now
            ),
            Flashcard(
                "fc3",
                "Como é a fórmula da Escala Pentatônica Menor?",
                "Fórmula: 1 (Fundamental), b3 (Terça Menor), 4 (Quarta Justa), 5 (Quinta Justa), b7 (Sétima Menor).",
                "Pentatônica", nextReviewTimeMs = now
            ),
            Flashcard(
                "fc4",
                "Onde entra e qual a função da Blue Note na pentatônica?",
                "Entra entre a 4ª e a 5ª nota (Quarta Aumentada/b5). Funciona melhor como nota rápida de passagem!",
                "Blue Note", nextReviewTimeMs = now
            ),
            Flashcard(
                "fc5",
                "Se a banda toca em Lá menor real (concert), qual Pentatônica o trompete Bb deve usar?",
                "B menor (Si menor)! Pois o trompete Bb lê tudo 1 tom (2 semitons) acima do som real.",
                "Transposição", nextReviewTimeMs = now
            ),
            Flashcard(
                "fc6",
                "Quais notas do trompete usam Pistão 0 (nenhum pistão pressionado)?",
                "Dó (C4), Sol (G4) e Dó agudo (C5) dentro da oitava principal.",
                "Pistões do Trompete", nextReviewTimeMs = now
            ),
            Flashcard(
                "fc7",
                "Qual a digitação do Fá# (F#4) no trompete?",
                "Apenas o Pistão 2!",
                "Escala Cromática", nextReviewTimeMs = now
            ),
            Flashcard(
                "fc8",
                "Qual a digitação da nota Lá (A4)?",
                "Pistões 1 e 2!",
                "Pistões do Trompete", nextReviewTimeMs = now
            ),
            Flashcard(
                "fc9",
                "O que acontece com os pistões se você tocar Mi (E4)?",
                "Usa pistões 1 e 2! (É a mesma digitação da nota Lá, mas tocado em um 'parcial' mais grave).",
                "Embocadura", nextReviewTimeMs = now
            )
        )
    }

    /**
     * Generate the RPG World Levels
     */
    fun getWorlds(state: GameState): List<QuestWorld> {
        return listOf(
            QuestWorld(
                id = 1,
                name = "Bosque das Notas Naturais",
                subtitle = "Monstros da Sequência Musical",
                description = "Supere os Goblins das Sequências para dominar a ordem circular (Ré depois de Dó, Fá depois de Mi...) e fixar que Mi-Fá e Si-Dó não têm sustenidos!",
                unlocked = true,
                category = "Naturals",
                monster = Monster(
                    id = "goblin_nat",
                    name = "Goblin do Dó",
                    title = "Guardião da Escada",
                    hp = 45, maxHp = 45, damage = 8,
                    imgColor = "#4CD964",
                    weakness = "Identifique a ordem circular correta e repousos imediatos!",
                    introSpeech = "Grrr! Do Re Mi... quem vem depois?? Diga ou vire janta!"
                )
            ),
            QuestWorld(
                id = 2,
                name = "Desfiladeiro do Cromático",
                subtitle = "Fendas de Meios Tons",
                description = "Trilhe as fendas repletas de acidentes e enarmônicos. Domine a diferença entre sustenidos e bemóis e as regras de meio tom!",
                unlocked = state.masteryNaturais >= 30,
                category = "Chromatic",
                monster = Monster(
                    id = "gargoyle_crom",
                    name = "Gárgula de Pedra Enarmônica",
                    title = "O Guardião Enigmático",
                    hp = 60, maxHp = 60, damage = 12,
                    imgColor = "#95A5A6",
                    weakness = "Selecione a nota enarmônica correspondente (ex: Dó# é Réb)!",
                    introSpeech = "Raaawr! A pedra racha entre o Mi e o Fá! Existe sustenido aí? Prove!"
                )
            ),
            QuestWorld(
                id = 3,
                name = "Santuário dos Pistões",
                subtitle = "A Engenharia do Metal",
                description = "Domine as combinações clássicas de pistões (1-3, 2-3, 1-2, 2, 1, 0) de Dó a Dó. Conecte sua visão, ouvido e os dedos!",
                unlocked = state.masteryCromatico >= 30,
                category = "Piston",
                monster = Monster(
                    id = "golem_pist",
                    name = "Golem das Três Válvulas",
                    title = "Engenheiro Ancestral",
                    hp = 80, maxHp = 80, damage = 15,
                    imgColor = "#E67E22",
                    weakness = "Aperte a combinação exata correspondente à nota perguntada!",
                    introSpeech = "Clang! Minhas articulações precisam de pressão! Qual a chave da nota Sol?!"
                )
            ),
            QuestWorld(
                id = 4,
                name = "Vale da Pentatônica",
                subtitle = "Os Cinco Selos Ancestrais",
                description = "Monstros surgem em padrões pentatônicos (1, b3, 4, 5, b7). Toque e identifique os graus da pentatônica menor de forma harmônica!",
                unlocked = state.masteryPistoes >= 40,
                category = "Pentatonic",
                monster = Monster(
                    id = "shaman_pent",
                    name = "Xamã da Escala Ritual",
                    title = "Cinco Tons de Espírito",
                    hp = 95, maxHp = 95, damage = 18,
                    imgColor = "#9B59B6",
                    weakness = "Aponte os graus (b3, b7) que dão o desenho melódico!",
                    introSpeech = "Huuuum! Sinta a vibração menor! Se você errar a terça da escala, seu poder sumirá!"
                )
            ),
            QuestWorld(
                id = 5,
                name = "Caverna da Blue Note",
                subtitle = "As Chamas Tristes do Blues",
                description = "Encontre a Blue Note secreta que reside entre a 4ª e 5ª nota. Aprenda a tensionar e resolver perfeitamente!",
                unlocked = state.masteryPentatonica >= 40,
                category = "BlueNote",
                monster = Monster(
                    id = "dragon_blue",
                    name = "Dragão do Sopro Triste",
                    title = "O Monarca da Tensão",
                    hp = 120, maxHp = 120, damage = 22,
                    imgColor = "#1F3A60",
                    weakness = "Insira a Blue Note como nota de passagem para acalmar a fera!",
                    introSpeech = "Swoosh! Minha alma queima em Blues. Me dê a oitava triste do trompete!"
                )
            ),
            QuestWorld(
                id = 6,
                name = "Câmara da Improvisação",
                subtitle = "Os Senhores do Backing Track",
                description = "Seu teste final! Seus solos serão julgados em tempo real por um júri místico. Conecte de ouvido e com feeling suas notas sob backing de acorde real!",
                unlocked = state.masteryBlueNotes >= 45,
                category = "Improvisation",
                monster = Monster(
                    id = "king_jury",
                    name = "Arqui-Maestro Cósmico",
                    title = "Banca do Conservatório Negro",
                    hp = 160, maxHp = 160, damage = 25,
                    imgColor = "#F1C40F",
                    weakness = "Crie uma frase que encerre com sustentação firme e use a Blue Note com maestria!",
                    introSpeech = "Silêncio! Erga seu trompete e apresente-me uma frase mística bem estruturada!"
                )
            )
        )
    }

    /**
     * Process SM-2 Spaced Repetition calculation
     * confidence: 1 (forgot/hardest) to 5 (perfect/easiest)
     */
    fun processSM2(card: Flashcard, confidence: Int): Flashcard {
        val q = confidence.coerceIn(1, 5)
        
        val newRepetitions: Int
        val newInterval: Int
        var newEaseFactor = card.easeFactor + (0.1f - (5 - q) * (0.08f + (5 - q) * 0.02f))
        if (newEaseFactor < 1.3f) newEaseFactor = 1.3f

        if (q < 3) {
            newRepetitions = 0
            newInterval = 1
        } else {
            newRepetitions = card.repetitions + 1
            newInterval = when (newRepetitions) {
                1 -> 1
                2 -> 6
                else -> (card.intervalDays * newEaseFactor).toInt().coerceAtLeast(1)
            }
        }

        val msInDay = 24 * 60 * 60 * 1000L
        val nextReview = System.currentTimeMillis() + (newInterval * msInDay)

        return card.copy(
            repetitions = newRepetitions,
            intervalDays = newInterval,
            easeFactor = newEaseFactor,
            nextReviewTimeMs = nextReview
        )
    }

    /**
     * Load game state from SharedPreferences
     */
    fun loadState(context: Context): GameState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(STATE_KEY, null)
        return if (json != null) {
            try {
                val loaded = stateAdapter.fromJson(json)
                if (loaded != null) {
                    // Make sure deck is populated
                    if (loaded.deck.isEmpty()) {
                        loaded.copy(deck = createDefaultDeck())
                    } else {
                        loaded
                    }
                } else {
                    GameState(deck = createDefaultDeck())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deserializing state, loading defaults", e)
                GameState(deck = createDefaultDeck())
            }
        } else {
            GameState(deck = createDefaultDeck())
        }
    }

    /**
     * Save game state to SharedPreferences
     */
    fun saveState(context: Context, state: GameState) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            val json = stateAdapter.toJson(state)
            prefs.edit().putString(STATE_KEY, json).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving state", e)
        }
    }
}
