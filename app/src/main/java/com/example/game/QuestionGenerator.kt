package com.example.game

data class GameQuestion(
    val id: String,
    val worldId: Int,
    val title: String,
    val prompt: String,
    val choices: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String,
    val soundFreqToPlay: Float = 0f, // play dynamic sound helper
    val isPistonQuestion: Boolean = false,
    val pistonMappingRequired: String = "" // e.g. "1-2"
)

object QuestionGenerator {
    fun generateQuestionsForWorld(worldId: Int): List<GameQuestion> {
        return when (worldId) {
            1 -> listOf(
                GameQuestion(
                    "w1_q1", 1,
                    "Sequenciador Natural",
                    "Qual nota natural vem imediatamente DEPOIS do Fá (F)?",
                    listOf("Sol (G)", "Lá (A)", "Mi (E)", "Si (B)"),
                    0,
                    "Subindo de forma natural: Fá -> Sol. A sequência é C-D-E-F-G-A-B.",
                    392.00f
                ),
                GameQuestion(
                    "w1_q2", 1,
                    "Sequenciador Natural Reverso",
                    "Qual nota natural vem imediatamente ANTES do Ré (D)?",
                    listOf("Si (B)", "Ré# (D#)", "Dó (C)", "Mi (E)"),
                    2,
                    "Descendo na ordem natural: Ré -> Dó.",
                    261.63f
                ),
                GameQuestion(
                    "w1_q3", 1,
                    "O Elo Perdido",
                    "Complete a sequência natural ascendente: Ré (D) -> Mi (E) -> ________ ?",
                    listOf("Sol (G)", "Fá (F)", "Fá# (F#)", "Dó (C)"),
                    1,
                    "Fá (F) é a nota natural vizinha direta de Mi (E). Elas estão separadas por apenas 1 semitom!",
                    349.23f
                ),
                GameQuestion(
                    "w1_q4", 1,
                    "Identificação Auditiva",
                    "Qual é a nota de partida natural emitida por este som fundamental?",
                    listOf("Lá (A)", "Si (B)", "Dó (C)", "Sol (G)"),
                    2,
                    "O Dó Central (C4) soa com freqüência de 261Hz e é a 1ª nota do nosso mapa estelar.",
                    261.63f
                ),
                GameQuestion(
                    "w1_q5", 1,
                    "O Final do Ciclo",
                    "Após a nota Si (B) na escala natural ascendente, para onde retornamos?",
                    listOf("Retornamos ao Dó (C)", "Lá (A)", "Fá (F)", "A escala acaba ali"),
                    0,
                    "A escala musical é circular. Após o Si, as notas se repetem em uma oitava mais alta começando pelo Dó.",
                    523.25f
                )
            )
            2 -> listOf(
                GameQuestion(
                    "w2_q1", 2,
                    "A Regra das Coladas",
                    "Quais pares de notas não possuem nenhuma alteração (sustenido/bemol) entre elas?",
                    listOf("Mi-Fá e Si-Dó", "Dó-Ré e Sol-Lá", "Ré-Mi e Fá-Sol", "Lá-Si e Ré-Mi"),
                    0,
                    "Mi-Fá (E-F) e Si-Dó (B-C) são coladas por apenas meio tom (semitom). Não existe E# ou Cb por padrão!",
                    329.63f
                ),
                GameQuestion(
                    "w2_q2", 2,
                    "Draco das Enarmonias",
                    "A nota Ré# (D#) é equivalente no dedilhado e no som a qual dessas notas?",
                    listOf("Mib (Eb)", "Réb (Db)", "Fáb (Fb)", "Mi# (E#)"),
                    0,
                    "Enarmonia é o fenômeno onde notas têm nomes diferentes mas produzem exatamente o mesmo som e dedilhado, como Ré# e Mib.",
                    311.13f
                ),
                GameQuestion(
                    "w2_q3", 2,
                    "Ascensão Cromática",
                    "Ao subir a escala cromática partindo de Sol (G), qual é o próximo passo exato?",
                    listOf("Sol# / Láb (G# / Ab)", "Mi (E)", "Lá (A)", "Fá# (F#)"),
                    0,
                    "O sustenido (#) eleva a nota natural em meio tom. Logo, depois de Sol temos Sol#.",
                    415.30f
                ),
                GameQuestion(
                    "w2_q4", 2,
                    "A Distância Máxima",
                    "De Dó (C) até Ré (D) temos uma distância de quantos tons?",
                    listOf("1 Tom Inteiro (2 Semitons)", "Meio Tom (1 Semitom)", "3 Tons", "Nenhum tomo"),
                    0,
                    "Há Dó# entre eles, logo são 2 passos de semitom, que somam 1 Tom Inteiro.",
                    293.66f
                ),
                GameQuestion(
                    "w2_q5", 2,
                    "O Labirinto Enarmônico",
                    "A nota Fá# (F#) é o mesmo que:",
                    listOf("Solb (Gb)", "Láb (Ab)", "Fab (Fb)", "Mib (Eb)"),
                    0,
                    "Fá# (subir meio tom de Fá) e Solb (descer meio tom de Sol) são a mesma nota na prática do trompete.",
                    369.99f
                )
            )
            3 -> listOf(
                GameQuestion(
                    "w3_q1", 3,
                    "As Três Chaves Abertas",
                    "Ao soprar o trompete sem apertar NENHUM pistão (dedilhado zero/aberto), qual nota natural de repouso tocamos?",
                    listOf("Dó (C)", "Si (B)", "Fá (F)", "Mi (E)"),
                    0,
                    "Dó aberto (0) é a nota principal do iniciante. Na oitava acima, o Sol (G) também usa o pistão aberto (0).",
                    261.63f,
                    true, "0"
                ),
                GameQuestion(
                    "w3_q2", 3,
                    "O Comando de Ré",
                    "Para tocar a nota Ré (D) com afinação correta, quais pistões devem ser pressionados?",
                    listOf("Pistões 1 e 3", "Pistão 2", "Pistão 1", "Pistões 1, 2 e 3"),
                    0,
                    "O Ré (D4) usa os pistões 1 e 3 para alongar a coluna de ar interna corretamente.",
                    293.66f,
                    true, "1-3"
                ),
                GameQuestion(
                    "w3_q3", 3,
                    "Pistão Único",
                    "A nota Fá (F) usa um único pistão principal. Qual é?",
                    listOf("Pistão 1", "Pistão 2", "Pistão 3", "Nenhum, Fá é aberto"),
                    0,
                    "Fá (F4) utiliza apenas o pistão 1 pressionado.",
                    349.23f,
                    true, "1"
                ),
                GameQuestion(
                    "w3_q4", 3,
                    "O Dedilhado Cromático Extremo",
                    "Qual nota exige apertar TODOS os três pistões (1, 2 e 3) de uma vez?",
                    listOf("Dó# / Réb (C# / Db)", "Dó (C)", "Fá# (F#)", "Mi (E)"),
                    0,
                    "Dó# é a nota mais longa do registro grave inicial, exigindo os pistões 1-2-3.",
                    277.18f,
                    true, "1-2-3"
                ),
                GameQuestion(
                    "w3_q5", 3,
                    "A Válvula Solitária",
                    "Com apenas o pistão 2 pressionado, qual nota abaixo soamos?",
                    listOf("Si (B)", "Ré (D)", "Lá (A)", "Sol (G)"),
                    0,
                    "O Si (B4) e o Fá# (F#4) utilizam tradicionalmente o pistão 2.",
                    493.88f,
                    true, "2"
                )
            )
            4 -> listOf(
                GameQuestion(
                    "w4_q1", 4,
                    "A Chave Pentatônica",
                    "Qual é a fórmula de intervalos de semitons usada para extrair a Pentatônica Menor?",
                    listOf("0, 3, 5, 7, 10 semitons", "0, 2, 4, 7, 9 semitons", "0, 1, 2, 3, 4 semitons", "0, 4, 7, 11, 12 semitons"),
                    0,
                    "A Pentatônica Menor segue os semitons 0 (fundamental), 3 (terça menor), 5 (quarta justa), 7 (quinta justa) e 10 (sétima menor).",
                    440.00f
                ),
                GameQuestion(
                    "w4_q2", 4,
                    "Solo em Lá Menor",
                    "Na escala de Lá menor pentatônica escrita (Am), quais são as 5 notas que a compõem?",
                    listOf("Lá - Dó - Ré - Mi - Sol", "Lá - Si - Dó# - Mi - Fá#", "Lá - Ré - Mi - Fá - Si", "Lá - Si - Do - Ré - Mi"),
                    0,
                    "Partindo do Lá (+3, +2, +2, +3 semitons): Lá, Dó, Ré, Mi, Sol. É o desenho mais famoso e intuitivo para solos!",
                    440.00f
                ),
                GameQuestion(
                    "w4_q3", 4,
                    "Grau Desafiador",
                    "Por que a Pentatônica Menor é tão famosa e segura para improvisar mesmo alterando os acordes de base?",
                    listOf("Porque não possui o 2º e o 6º grau, evitando a dissonância de semitons vizinhos", "Porque só usa notas abertas", "Porque foi inventada na lua", "Porque só tem notas graves"),
                    0,
                    "Ao remover os intervalos de semitom naturais da escala menor (segunda menor e sexta menor), qualquer nota clicada soará incrivelmente harmônica e segura sobre a base!",
                    293.66f
                ),
                GameQuestion(
                    "w4_q4", 4,
                    "O Atalho de Ré Pentatônico",
                    "Se você começa um solo pentatônico partindo do Ré (D), qual será a sua terça menor (3ª nota do desenho)?",
                    listOf("Fá (F)", "Sol (G)", "Si (B)", "Ré# (D#)"),
                    0,
                    "Ré (+3 semitons de distância cromática) = Ré -> Ré# -> Mi -> Fá. Então a terça menor de Ré é o Fá!",
                    349.23f
                ),
                GameQuestion(
                    "w4_q5", 4,
                    "Intervalo de Bravura",
                    "O intervalo entre a 4ª e a 5ª nota da pentatônica menor natural regular de Lá (Ré para Mi) mede:",
                    listOf("2 semitons (1 tom inteiro)", "5 semitons", "Nenhum tom", "Uma oitava"),
                    0,
                    "Ré (semitone 5) para Mi (semitone 7) dá exatamente 2 semitons (um tom inteiro). É nesse espaço que o Blues esconde seu segredo!",
                    329.63f
                )
            )
            5 -> listOf(
                GameQuestion(
                    "w5_q1", 5,
                    "O Segredo do Blues",
                    "A famosa 'Blue Note' na escala Pentatônica Menor localiza-se matematicamente onde?",
                    listOf("No 4º grau aumentado (b5) - entre o 4º e o 5º grau", "Logo no 1º grau repetido", "Entre o 1º e o 3º grau", "Fora de qualquer oitava"),
                    0,
                    "A Blue Note é a quarta aumentada (ou quinta diminuta, b5). Ela adiciona aquela tensão fantástica, sombria e melancólica clássica do blues e do jazz.",
                    369.99f
                ),
                GameQuestion(
                    "w5_q2", 5,
                    "A Localização na Prática",
                    "Em uma Pentatônica Menor de Lá (A), qual nota exata age como a Blue Note?",
                    listOf("Ré# / Mib (D# / Eb)", "Fá# (F#)", "Fá (F)", "Dó# (C#)"),
                    0,
                    "Lá Pentatônica tem Ré (4º grau) e Mi (5º grau). A nota espremida entre eles é o Ré# ou Mib, a fantástica Blue Note de Lá!",
                    311.13f
                ),
                GameQuestion(
                    "w5_q3", 5,
                    "A Regra da Passagem",
                    "Como a Blue Note deve ser dedilhada e executada para manter o bom gosto musical e pedagógico?",
                    listOf("Com notas rápidas de passagem resolvidas na nota vizinha", "Sustentando-a por 10 segundos estáticos no fim", "Usando apenas o pistão do meio aleatoriamente", "Tocada de olhos fechados sem som"),
                    0,
                    "Como a Blue Note é muito instável, sustentá-la cria dissonância desagradável. O segredo é usá-la para ornamentar e correr para repousar no Dó, Ré ou Mi!",
                    329.63f
                ),
                GameQuestion(
                    "w5_q4", 5,
                    "O Dedilhado da Sombra",
                    "Se a Blue Note na pentatônica de Lá é o Mib (Eb), qual é o dedilhado de pistões correspondente no trompete?",
                    listOf("Pistões 2 e 3", "Pistões 1 e 2", "Pistão aberto (0)", "Pistão 1"),
                    0,
                    "Ré#/Mib usa o dedilhado clássico de pistões 2 e 3.",
                    311.13f,
                    true, "2-3"
                ),
                GameQuestion(
                    "w5_q5", 5,
                    "Vibração de Atitude",
                    "Verdadeiro ou Falso: No Trompete de Si♭, a transposição altera a sensibilidade da Blue Note.",
                    listOf("Falso, o conceito de 4ª aumentada passagem é universal", "Verdadeiro, vira nota natural", "Falso, as duas desaparecem", "Verdadeiro, vira um Ré menor"),
                    0,
                    "Embora a escrita mude por causa da transposição (+2 semitons), a física acústica e o efeito expressivo do semitom de tensão (b5) permanecem universais!",
                    440.00f
                )
            )
            else -> listOf() // Will trigger sandbox bosstype
        }
    }
}
