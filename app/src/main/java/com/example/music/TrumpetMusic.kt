package com.example.music

import kotlin.math.abs

data class MusicalNote(
    val id: String,
    val namePt: String,
    val nameEn: String,
    val freq: Float,
    val pistons: String, // E.g. "0", "1-2-3", "1-3", "2-3", "1-2", "1", "2"
    val isNatural: Boolean,
    val semitoneValue: Int, // Relative of C4=0, C#4=1, D4=2 ... C5=12
    val representationPt: String // Portuguese display like "C (Dó)"
)

object TrumpetMusic {
    val NOTES = listOf(
        MusicalNote("C4", "Dó", "C", 261.63f, "0", true, 0, "Dó (C)"),
        MusicalNote("C#4", "Dó# / Réb", "C# / Db", 277.18f, "1-2-3", false, 1, "Dó# / Réb (C# / Db)"),
        MusicalNote("D4", "Ré", "D", 293.66f, "1-3", true, 2, "Ré (D)"),
        MusicalNote("D#4", "Ré# / Mib", "D# / Eb", 311.13f, "2-3", false, 3, "Ré# / Mib (D# / Eb)"),
        MusicalNote("E4", "Mi", "E", 329.63f, "1-2", true, 4, "Mi (E)"),
        MusicalNote("F4", "Fá", "F", 349.23f, "1", true, 5, "Fá (F)"),
        MusicalNote("F#4", "Fá# / Solb", "F# / Gb", 369.99f, "2", false, 6, "Fá# / Solb (F# / Gb)"),
        MusicalNote("G4", "Sol", "G", 392.00f, "0", true, 7, "Sol (G)"),
        MusicalNote("G#4", "Sol# / Láb", "G# / Ab", 415.30f, "2-3", false, 8, "Sol# / Láb (G# / Ab)"),
        MusicalNote("A4", "Lá", "A", 440.00f, "1-2", true, 9, "Lá (A)"),
        MusicalNote("A#4", "Lá# / Sib", "A# / Bb", 466.16f, "1", false, 10, "Lá# / Sib (A# / Bb)"),
        MusicalNote("B4", "Si", "B", 493.88f, "2", true, 11, "Si (B)"),
        MusicalNote("C5", "Dó (Médio)", "C (Mid)", 523.25f, "0", true, 12, "Dó (C5)")
    )

    fun getById(id: String): MusicalNote? = NOTES.find { it.id == id }
    fun getBySemitone(semitone: Int): MusicalNote? {
        val normalized = if (semitone < 0) {
            0
        } else if (semitone > 12) {
            12
        } else {
            semitone
        }
        return NOTES.find { it.semitoneValue == normalized }
    }

    // A minor Concert = B minor on Bb Trumpet (+2 semitones transposition)
    // Transpose real sound to Bb written notation
    fun transposeConcertToBbWritten(concertSemitone: Int): Int {
        return (concertSemitone + 2) % 12
    }

    // Build minor pentatonic scale given a root note semitone
    // Scale degrees: 1, b3, 4, 5, b7
    // Semitone steps from root: 0, 3, 5, 7, 10
    fun getMinorPentatonic(rootSemitone: Int): List<MusicalNote> {
        val steps = listOf(0, 3, 5, 7, 10)
        return steps.mapNotNull { step ->
            val targetSemi = (rootSemitone + step)
            // find nearest wrap note in our 0..12 register
            NOTES.find { it.semitoneValue == targetSemi % 13 }
        }
    }

    // Build Minor Pentatonic with Blue Note
    // Scale degrees: 1, b3, 4, blue_note(b5), 5, b7
    // Semitone steps from root: 0, 3, 5, 6, 7, 10
    fun getPentatonicWithBlueNote(rootSemitone: Int): List<MusicalNote> {
        val steps = listOf(0, 3, 5, 6, 7, 10)
        return steps.mapNotNull { step ->
            val targetSemi = (rootSemitone + step)
            NOTES.find { it.semitoneValue == targetSemi % 13 }
        }
    }

    // Evaluates an improvised phrase
    // Expected root semitone of scale (e.g. A4 = 9, so A minor pentatonic)
    fun evaluateImprovisation(
        phrase: List<MusicalNote>,
        rootSemitone: Int,
        withBlueNote: Boolean = true
    ): ImprovisationResult {
        if (phrase.isEmpty()) {
            return ImprovisationResult(0, "Toque notas para montar seu solo!", false)
        }

        val scaleSemitones = if (withBlueNote) {
            listOf(0, 3, 5, 6, 7, 10).map { (rootSemitone + it) % 13 }
        } else {
            listOf(0, 3, 5, 7, 10).map { (rootSemitone + it) % 13 }
        }

        val blueNoteSemitone = (rootSemitone + 6) % 13

        var score = 100
        val feedbacks = mutableListOf<String>()

        // 1. Check scale correctness
        val offScaleNotes = phrase.filter { it.semitoneValue !in scaleSemitones }
        if (offScaleNotes.isNotEmpty()) {
            val lost = offScaleNotes.size * 20
            score -= lost
            feedbacks.add("-${lost}pts: Algumas notas tocadas não pertencem à escala pentatônica menor.")
        } else {
            feedbacks.add("+10pts: Todas as notas fazem parte da escala correta!")
        }

        // 2. Ending/Resolution note check
        // Root (0), or 5th (7) relative to root
        val lastNote = phrase.last()
        val relativeLast = (lastNote.semitoneValue - rootSemitone + 13) % 13
        if (relativeLast == 0 || relativeLast == 7) {
            score += 15
            feedbacks.add("+15pts: Finalização perfeita! Você terminou em uma nota de repouso muito estável (${lastNote.namePt}).")
        } else if (relativeLast == 6) { // ended on blue note
            score -= 25
            feedbacks.add("-25pts: Cuidado! Terminar o solo na Blue Note deixa a melodia muito instável e sem resolução.")
        } else {
            score -= 10
            feedbacks.add("-10pts: Término aceitável, mas terminar na Fundamental ou na Quinta daria mais estabilidade.")
        }

        // 3. Blue Note passing tone evaluation
        var blueNotesCount = 0
        var correctlyPassed = 0
        for (i in phrase.indices) {
            if (phrase[i].semitoneValue == blueNoteSemitone) {
                blueNotesCount++
                // check if neighbors are 4th (root + 5) or 5th (root + 7)
                val prev = if (i > 0) (phrase[i-1].semitoneValue - rootSemitone + 13) % 13 else -1
                val next = if (i < phrase.size - 1) (phrase[i+1].semitoneValue - rootSemitone + 13) % 13 else -1
                if ((prev == 5 || prev == 7) && (next == 5 || next == 7)) {
                    correctlyPassed++
                }
            }
        }

        if (blueNotesCount > 0) {
            if (correctlyPassed == blueNotesCount) {
                score += 20
                feedbacks.add("+20pts: Excelente senso de Blue Note! Usou-a perfeitamente como nota de passagem entre a 4ª e 5ª.")
            } else {
                score -= 15
                feedbacks.add("-15pts: Você usou a Blue Note, mas tente fazê-la apenas de passagem rápida para resolver na 4ª ou na 5ª.")
            }
        }

        // 4. Variety check
        val uniqueNotes = phrase.map { it.id }.distinct().size
        if (uniqueNotes < 3 && phrase.size >= 3) {
            score -= 15
            feedbacks.add("-15pts: Melodia repetitiva. Experimente usar mais notas do desenho pentatônico.")
        }

        val clampedScore = score.coerceIn(10, 100)
        val rank = when {
            clampedScore >= 90 -> "Sinfônico Celestial"
            clampedScore >= 75 -> "Solista do Coreto"
            clampedScore >= 50 -> "Estudante Esforçado"
            else -> "Embocadura Torta"
        }

        return ImprovisationResult(
            score = clampedScore,
            summaryPt = "Classificação: $rank. Nota Final: $clampedScore/100",
            feedbacks = feedbacks
        )
    }
}

data class ImprovisationResult(
    val score: Int,
    val summaryPt: String,
    val feedbackSuccess: Boolean = score >= 60,
    val feedbacks: List<String> = emptyList()
)
