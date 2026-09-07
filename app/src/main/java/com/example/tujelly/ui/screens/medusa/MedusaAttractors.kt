package com.example.tujelly.ui.screens.medusa

import androidx.compose.ui.geometry.Offset

/**
 * Dimensión ontológica según RFC-004.
 */
enum class AttractorDimension(val id: Int, val title: String) {
    CONTEXT(1, "Contexto / Compañía"),
    ENERGY(2, "Energía / Ritmo"),
    TONE(3, "Tono Emocional"),
    WORLD(4, "Mundo / Textura"),
    COGNITIVE(5, "Carga Cognitiva")
}

/**
 * Representación de un atractor gravitacional en el espacio semántico.
 */
data class Attractor(
    val id: String,
    val dimension: AttractorDimension,
    val displayName: String,
    val description: String,
    val position: Offset, // Posición normalizada en el lienzo cósmico (0.0f - 1.0f)
    val genres: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val excludeGenres: List<String> = emptyList(),
    val excludeTags: List<String> = emptyList(),
    val ratingMax: String? = null,
    val boostFactor: Float = 1.0f
)

/**
 * Catálogo de los 24 atractores base definidos en RFC-004.
 * Distribuidos armónicamente a lo largo de la anatomía de la Medusa Nebulosa Cósmica:
 * - Campana superior: Contexto
 * - Centro y núcleo luminoso: Tono Emocional y Energía
 * - Filamentos y velos inferiores: Mundo y Carga Cognitiva
 */
object MedusaAttractorCatalog {

    val allAttractors: List<Attractor> = listOf(
        // =========================================================================
        // DIMENSIÓN 1: CONTEXTO / COMPAÑÍA (DIM_CONTEXT) - Sombrilla Superior
        // =========================================================================
        Attractor(
            id = "CTX_KIDS_SOLO",
            dimension = AttractorDimension.CONTEXT,
            displayName = "Infantil Solo",
            description = "Exclusivo para niños pequeños sin supervisión continua",
            position = Offset(0.12f, 0.16f),
            genres = listOf("Animation", "Children", "Family", "Animación", "Infantil"),
            tags = listOf("kids", "preschool", "dibujos", "infantil", "children"),
            excludeTags = listOf("dark", "violence", "horror", "gore", "sexual", "terror"),
            ratingMax = "G",
            boostFactor = 1.25f
        ),
        Attractor(
            id = "CTX_FAMILY_ALL",
            dimension = AttractorDimension.CONTEXT,
            displayName = "Toda la Familia",
            description = "Apto para niños y adultos en simultáneo",
            position = Offset(0.24f, 0.12f),
            genres = listOf("Animation", "Family", "Adventure", "Animación", "Familiar", "Aventura"),
            tags = listOf("family-friendly", "disney", "pixar", "familiar", "aventura", "niños"),
            ratingMax = "PG",
            boostFactor = 1.25f
        ),
        Attractor(
            id = "CTX_FAMILY_TEENS",
            dimension = AttractorDimension.CONTEXT,
            displayName = "Familia con Teens",
            description = "Tensión o acción moderada sin contenido explícito adulto",
            position = Offset(0.36f, 0.15f),
            genres = listOf("Action", "Sci-Fi", "Comedy", "Adventure", "Acción", "Ciencia Ficción"),
            tags = listOf("coming-of-age", "superhero", "adolescencia", "juvenil", "instituto"),
            ratingMax = "PG-13",
            boostFactor = 1.15f
        ),
        Attractor(
            id = "CTX_COUPLE",
            dimension = AttractorDimension.CONTEXT,
            displayName = "En Pareja",
            description = "Química entre personajes, romance o drama conversacional",
            position = Offset(0.10f, 0.28f),
            genres = listOf("Romance", "Drama", "Comedy", "Comedia"),
            tags = listOf("relationship", "romantic", "love-story", "pareja", "amor", "romántico"),
            boostFactor = 1.15f
        ),
        Attractor(
            id = "CTX_FRIENDS_PARTY",
            dimension = AttractorDimension.CONTEXT,
            displayName = "Noche con Amigos",
            description = "Entretenimiento directo, ritmo alto, comedia o terror social",
            position = Offset(0.23f, 0.24f),
            genres = listOf("Comedy", "Horror", "Action", "Comedia", "Terror", "Acción"),
            tags = listOf("popcorn-movie", "cult-classic", "slasher", "fiesta", "diversión", "palomitera"),
            boostFactor = 1.15f
        ),
        Attractor(
            id = "CTX_SOLO_INTIMATE",
            dimension = AttractorDimension.CONTEXT,
            displayName = "Solo / Inmersión",
            description = "Experiencias íntimas, complejas o psicológicas",
            position = Offset(0.35f, 0.27f),
            genres = listOf("Drama", "Mystery", "Thriller", "Misterio", "Suspense"),
            tags = listOf("psychological", "arthouse", "character-study", "íntimo", "autor", "inmersión"),
            boostFactor = 1.15f
        ),

        // =========================================================================
        // DIMENSIÓN 2: ENERGÍA / RITMO (DIM_ENERGY) - Arco Derecho de la Campana
        // =========================================================================
        Attractor(
            id = "ENG_CONTEMPLATIVE",
            dimension = AttractorDimension.ENERGY,
            displayName = "Pausado / Calma",
            description = "Planos largos, silencios, desarrollo lento de escena",
            position = Offset(0.44f, 0.18f),
            tags = listOf("slow-burn", "atmospheric", "meditative", "pausado", "calma", "lento", "silencioso"),
            excludeGenres = listOf("Action", "Acción"),
            boostFactor = 1.10f
        ),
        Attractor(
            id = "ENG_BUILDING",
            dimension = AttractorDimension.ENERGY,
            displayName = "Tensión Creciente",
            description = "Suspenso acumulativo que escala progresivamente",
            position = Offset(0.52f, 0.28f),
            genres = listOf("Thriller", "Mystery", "Suspense", "Misterio"),
            tags = listOf("suspense", "tension", "escalating", "countdown", "cuenta atrás", "asfixiante"),
            boostFactor = 1.10f
        ),
        Attractor(
            id = "ENG_HIGH_OCTANE",
            dimension = AttractorDimension.ENERGY,
            displayName = "Trepidante",
            description = "Dinamismo continuo, persecuciones, movimiento rápido",
            position = Offset(0.45f, 0.38f),
            genres = listOf("Action", "Adventure", "Acción", "Aventura"),
            tags = listOf("fast-paced", "car-chase", "martial-arts", "adrenalina", "velocidad", "persecución"),
            boostFactor = 1.15f
        ),
        Attractor(
            id = "ENG_LIGHT_BREEZY",
            dimension = AttractorDimension.ENERGY,
            displayName = "Ligero",
            description = "Fluidez sin esfuerzo, escenas ágiles y desenfadadas",
            position = Offset(0.55f, 0.40f),
            genres = listOf("Comedy", "Animation", "Comedia", "Animación"),
            tags = listOf("feel-good", "breezy", "lighthearted", "optimista", "ligero", "desenfadado"),
            boostFactor = 1.10f
        ),
        Attractor(
            id = "ENG_FRENETIC",
            dimension = AttractorDimension.ENERGY,
            displayName = "Frenético",
            description = "Edición caótica, ritmo desenfrenado, saturación",
            position = Offset(0.48f, 0.52f),
            tags = listOf("non-stop", "hyperactive", "chaotic", "surreal", "caótico", "frenético", "adrenalina"),
            boostFactor = 1.10f
        ),

        // =========================================================================
        // DIMENSIÓN 3: TONO EMOCIONAL (DIM_TONE) - Núcleo y Órganos de Luz
        // =========================================================================
        Attractor(
            id = "TON_WONDER",
            dimension = AttractorDimension.TONE,
            displayName = "Asombro",
            description = "Estimula el sentido de maravilla y descubrimiento",
            position = Offset(0.25f, 0.38f),
            genres = listOf("Fantasy", "Adventure", "Documentary", "Fantasía", "Aventura", "Documental"),
            tags = listOf("sense-of-wonder", "magical", "maravilla", "asombro", "descubrimiento", "espectáculo"),
            boostFactor = 1.15f
        ),
        Attractor(
            id = "TON_WARM_COSY",
            dimension = AttractorDimension.TONE,
            displayName = "Confort / Cálido",
            description = "Refugio emocional, sensación de seguridad y optimismo",
            position = Offset(0.35f, 0.40f),
            tags = listOf("comfort-movie", "wholesome", "cozy", "confort", "cálido", "hogar", "esperanza"),
            boostFactor = 1.10f
        ),
        Attractor(
            id = "TON_SMART_HUMOR",
            dimension = AttractorDimension.TONE,
            displayName = "Humor Ingenioso",
            description = "Sátira, ironía, comedia de diálogo inteligente",
            position = Offset(0.16f, 0.42f),
            genres = listOf("Comedy", "Comedia"),
            tags = listOf("satire", "dark-comedy", "witty", "sátira", "ironía", "ingenio", "inteligente"),
            boostFactor = 1.10f
        ),
        Attractor(
            id = "TON_UNSETTLING",
            dimension = AttractorDimension.TONE,
            displayName = "Inquietante",
            description = "Sensación de peligro inminente, atmósfera turbia",
            position = Offset(0.28f, 0.50f),
            genres = listOf("Horror", "Mystery", "Thriller", "Terror", "Misterio", "Suspense"),
            tags = listOf("creepy", "eerie", "claustrophobic", "inquietante", "perturbador", "siniestro"),
            boostFactor = 1.10f
        ),
        Attractor(
            id = "TON_EPIC",
            dimension = AttractorDimension.TONE,
            displayName = "Épico / Triunfo",
            description = "Escala monumental, superación personal o colectiva",
            position = Offset(0.38f, 0.52f),
            genres = listOf("History", "Action", "War", "Historia", "Bélica", "Acción"),
            tags = listOf("epic", "heroic", "triumph", "inspirational", "épico", "hazaña", "victoria", "gloria"),
            boostFactor = 1.15f
        ),
        Attractor(
            id = "TON_CATHARTIC",
            dimension = AttractorDimension.TONE,
            displayName = "Catarsis",
            description = "Emoción intensa, liberación mediante la tristeza o el drama",
            position = Offset(0.22f, 0.58f),
            genres = listOf("Drama"),
            tags = listOf("tear-jerker", "emotional", "tragedy", "catarsis", "lágrima", "tristeza", "emocionante"),
            boostFactor = 1.10f
        ),

        // =========================================================================
        // DIMENSIÓN 4: MUNDO / TEXTURA (DIM_WORLD) - Velos de Luz Inferiores Izquierdos
        // =========================================================================
        Attractor(
            id = "MND_FANTASY",
            dimension = AttractorDimension.WORLD,
            displayName = "Fantasía / Magia",
            description = "Reglas mágicas, folclore, seres extraordinarios",
            position = Offset(0.12f, 0.68f),
            genres = listOf("Fantasy", "Fantasía"),
            tags = listOf("magic", "mythology", "folklore", "fantasía", "magia", "criaturas", "hadas"),
            boostFactor = 1.15f
        ),
        Attractor(
            id = "MND_SCI_FI",
            dimension = AttractorDimension.WORLD,
            displayName = "Ciencia Ficción",
            description = "Tecnología futura, viajes espaciales, especulación",
            position = Offset(0.22f, 0.70f),
            genres = listOf("Sci-Fi", "Science Fiction", "Ciencia Ficción"),
            tags = listOf("space", "artificial-intelligence", "future", "espacio", "ciber", "ia", "futuro", "alien"),
            boostFactor = 1.15f
        ),
        Attractor(
            id = "MND_HISTORICAL",
            dimension = AttractorDimension.WORLD,
            displayName = "Época / Historia",
            description = "Reconstrucción del pasado, rigor de época",
            position = Offset(0.10f, 0.82f),
            genres = listOf("History", "Drama", "Historia"),
            tags = listOf("period-piece", "costume-drama", "biography", "época", "histórico", "siglo", "biografía"),
            boostFactor = 1.10f
        ),
        Attractor(
            id = "MND_REALIST",
            dimension = AttractorDimension.WORLD,
            displayName = "Cotidiano",
            description = "El mundo real contemporáneo, situaciones creíbles",
            position = Offset(0.19f, 0.84f),
            genres = listOf("Drama", "Crime", "Crimen"),
            tags = listOf("contemporary", "realistic", "slice-of-life", "cotidiano", "realista", "urbano"),
            boostFactor = 1.10f
        ),
        Attractor(
            id = "MND_DYSTOPIA",
            dimension = AttractorDimension.WORLD,
            displayName = "Distopía / Cyber",
            description = "Sociedades colapsadas, neón, estética futurista",
            position = Offset(0.28f, 0.80f),
            genres = listOf("Sci-Fi", "Thriller", "Ciencia Ficción", "Suspense"),
            tags = listOf("cyberpunk", "dystopian", "post-apocalyptic", "distopía", "apocalipsis", "neón"),
            boostFactor = 1.15f
        ),

        // =========================================================================
        // DIMENSIÓN 5: CARGA COGNITIVA (DIM_COGNITIVE) - Velos de Luz Inferiores Derechos
        // =========================================================================
        Attractor(
            id = "COG_DISCONNECT",
            dimension = AttractorDimension.COGNITIVE,
            displayName = "Desconexión",
            description = "Apagar el cerebro; fórmula predecible y entretenida",
            position = Offset(0.38f, 0.66f),
            tags = listOf("turn-off-brain", "simple", "mindless-fun", "entretenimiento", "fácil", "desconexión"),
            boostFactor = 1.05f
        ),
        Attractor(
            id = "COG_PUZZLE",
            dimension = AttractorDimension.COGNITIVE,
            displayName = "Rompecabezas",
            description = "Pistas, giros de guion, exige atención a los detalles",
            position = Offset(0.48f, 0.68f),
            genres = listOf("Mystery", "Crime", "Misterio", "Crimen"),
            tags = listOf("plot-twist", "whodunit", "mind-bending", "giro", "acertijo", "rompecabezas", "detective"),
            boostFactor = 1.15f
        ),
        Attractor(
            id = "COG_PHILOSOPHICAL",
            dimension = AttractorDimension.COGNITIVE,
            displayName = "Dilema Moral",
            description = "Preguntas éticas, existencia, libre albedrío",
            position = Offset(0.36f, 0.78f),
            tags = listOf("philosophical", "moral-dilemma", "existential", "filosófico", "moral", "existencial"),
            boostFactor = 1.10f
        ),
        Attractor(
            id = "COG_VISUAL_SPECTACLE",
            dimension = AttractorDimension.COGNITIVE,
            displayName = "Espectáculo Visual",
            description = "Prioridad al diseño de sonido, fotografía y efectos",
            position = Offset(0.48f, 0.80f),
            tags = listOf("visual-spectacle", "cinematography", "vfx", "fotografía", "visual", "espectáculo"),
            boostFactor = 1.15f
        )
    )

    private val byIdMap: Map<String, Attractor> = allAttractors.associateBy { it.id }

    fun getById(id: String): Attractor? = byIdMap[id]
}
