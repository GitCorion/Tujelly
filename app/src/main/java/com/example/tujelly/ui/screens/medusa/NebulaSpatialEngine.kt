package com.example.tujelly.ui.screens.medusa

import androidx.compose.ui.geometry.Offset
import com.example.tujelly.data.local.db.JellyfinMediaEntity
import com.example.tujelly.domain.model.MediaItem
import com.example.tujelly.domain.model.MediaSource
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Nodo en el espacio semántico continuo de la Nebulosa de Cine.
 */
data class SpatialNebulaNode(
    val id: String,
    val label: String,
    val category: String,
    val worldX: Float, // Coordenada en el espacio continuo (0.0f - 1.0f o expandido)
    val worldY: Float,
    val minZoomVisible: Float = 0.6f, // Umbral LOD mínimo para mostrar texto
    val maxZoomVisible: Float = 10.0f,
    val movieCount: Int = 0,
    val importance: Float = 1.0f, // 1.0f base, 1.5f etiquetas mayores
    val keywords: List<String> = emptyList(),
    val isPortal: Boolean = false
)

const val PORTAL_NODE_ID = "PORTAL_MOVIES"

/**
 * Filamento vectorial que conecta dos nodos de alta afinidad semántica.
 */
data class SpatialFilament(
    val fromNodeId: String,
    val toNodeId: String,
    val affinity: Float = 1.0f
)

enum class DPadDirection { LEFT, RIGHT, UP, DOWN }

/**
 * Motor Espacial Profesional para la Nebulosa de Descubrimiento:
 * 1. Genera un espacio semántico continuo a partir de los datos reales de la biblioteca.
 * 2. Mantiene un índice espacial para navegación D-Pad instantánea por proximidad angular.
 * 3. Gestiona niveles de detalle (LOD) dinámicos para mantener 60 fps en Android TV.
 */
class NebulaSpatialEngine {

    private val allNodes = mutableListOf<SpatialNebulaNode>()
    private val allFilaments = mutableListOf<SpatialFilament>()
    private val nodeById = mutableMapOf<String, SpatialNebulaNode>()
    private var tokenizedCatalog: List<Pair<JellyfinMediaEntity, String>> = emptyList()

    fun getAllNodes(): List<SpatialNebulaNode> = allNodes
    fun getAllFilaments(): List<SpatialFilament> = allFilaments
    fun getNode(id: String): SpatialNebulaNode? = nodeById[id]

    /**
     * Construye la galaxia semántica a partir del catálogo de Jellyfin.
     */
    fun buildUniverse(
        catalog: List<JellyfinMediaEntity>,
        serverUrl: String,
        accessToken: String
    ) {
        allNodes.clear()
        allFilaments.clear()
        nodeById.clear()

        // Definición de los 10 grandes cúmulos temáticos del cine real con espaciado no solapado
        val clusters = listOf(
            ClusterTemplate(
                name = "CRIMEN & CINE NEGRO",
                category = "Crimen",
                center = Offset(0.22f, 0.26f),
                radius = 0.10f,
                macroTags = listOf(
                    // Anillo 1: Tropos centrales
                    "Cine Negro", "Atraco al Banco", "Mafia & Clanes", "Corrupción Policial",
                    "Falso Culpable", "Detectives Privados", "Juicios & Ley", "Venganza Callejera",
                    // Anillo 2: Escenarios y arquetipos
                    "Femme Fatale", "El Golpe Maestro", "Callejones Nocturnos", "Partida de Poker Clandestina",
                    "Asesino Metódico", "Persecución de Coches", "Traidor en la Familia", "Caja Fuerte Inviolable",
                    "Comisaría al Límite", "Testigo Protegido",
                    // Anillo 3: Situaciones y atmósfera
                    "Doble Juego", "El Capo Retirado", "Motín Carcelario", "Tráfico Portuario",
                    "Voz en Off Cínica", "Sombras en el Asfalto", "Chantaje al Juez", "Crimen sin Resolver",
                    "Fianza Denegada", "El Último Botín"
                ),
                keywords = listOf("crime", "noir", "mafia", "heist", "detective", "policial", "corrupción", "asesino", "crimen", "robo", "cárcel", "abogado")
            ),
            ClusterTemplate(
                name = "DRAMA & SUPERACIÓN",
                category = "Drama",
                center = Offset(0.50f, 0.22f),
                radius = 0.10f,
                macroTags = listOf(
                    // Anillo 1: Tropos centrales
                    "Drama Familiar", "Superación Personal", "Amor Imposible", "Pérdida & Duelo",
                    "Secretos del Pasado", "Amistad Incondicional", "Infancia & Madurez", "Historias Reales",
                    // Anillo 2: Escenarios y arquetipos
                    "Reencuentro tras Años", "Ruptura & Sanación", "El Maestro Inspirador", "Lucha Obrera",
                    "Cartas de Amor Olvidadas", "Dilemas Morales", "Segunda Oportunidad", "Juventud Rebelde",
                    "Vidas Cruzadas", "El Último Verano",
                    // Anillo 3: Situaciones y atmósfera
                    "Promesa Rota", "Cena Familiar Tensa", "El Regreso al Pueblo", "Confesión en el Lecho",
                    "Enfermedad & Coraje", "Pueblo Natal Olvidado", "Diarios Íntimos", "Abuelos & Nietos",
                    "Lágrimas en el Andén", "El Legado del Padre"
                ),
                keywords = listOf("drama", "family", "biography", "inspirational", "friendship", "romance", "amor", "superación", "vida", "emotiva", "melancolía")
            ),
            ClusterTemplate(
                name = "AVENTURA & SUPERVIVENCIA",
                category = "Aventura",
                center = Offset(0.78f, 0.26f),
                radius = 0.10f,
                macroTags = listOf(
                    // Anillo 1: Tropos centrales
                    "Supervivencia Salvaje", "Naufragio en Alta Mar", "Alta Montaña & Nieve", "Caza del Tesoro",
                    "Expedición a la Selva", "Piratas & Corsarios", "Aislamiento en el Desierto", "Exploradores sin Retorno",
                    // Anillo 2: Escenarios y arquetipos
                    "Cuevas Subterráneas", "Tempestad en el Océano", "El Mapa Perdido", "Templo Olvidado",
                    "Ruta de la Seda", "Globo Aerostático", "Frontera Salvaje", "Aeronave Caída",
                    "Safari Inexplorado", "Hielos del Ártico",
                    // Anillo 3: Situaciones y atmósfera
                    "Brújula Rota", "Mochila sin Agua", "La Trampa de Arenas", "Faro en el Acantilado",
                    "Fiebre del Oro", "Navegantes de Época", "El Valle Oculto", "El Salto al Vacío",
                    "Tribus Desconocidas", "Refugio en la Tormenta"
                ),
                keywords = listOf("adventure", "survival", "ocean", "mountain", "jungle", "treasure", "pirate", "isla", "selva", "náufrago", "tesoro", "expedición")
            ),
            ClusterTemplate(
                name = "MISTERIO & CONSPIRACIÓN",
                category = "Misterio",
                center = Offset(0.20f, 0.49f),
                radius = 0.10f,
                macroTags = listOf(
                    // Anillo 1: Tropos centrales
                    "Secretos de Estado", "Desaparición Misteriosa", "Pueblo con Secretos", "Crimen de Habitación Cerrada",
                    "Conspiración en la Sombra", "El Testigo Silenciado", "La Doble Vida", "Identidad Robada",
                    // Anillo 2: Escenarios y arquetipos
                    "Mensajes Cifrados", "Cintas Prohibidas", "El Diario del Muerto", "Pistas en la Oscuridad",
                    "Secta Secreta", "Forense Meticuloso", "Llamada Anónima", "El Manuscrito Perdido",
                    "Sótano Clausurado", "La Coartada Perfecta",
                    // Anillo 3: Situaciones y atmósfera
                    "Retrato con Doble Fondo", "Sociedad Secreta", "El Espía Retirado", "Niebla en el Lago",
                    "El Mensajero Asesinado", "Archivo Confidencial", "La Llave Oxidada", "Voces en la Frecuencia",
                    "El Testamento Oculto", "El Pasadizo Secreto"
                ),
                keywords = listOf("mystery", "conspiracy", "secret", "investigation", "misterio", "conspiración", "desaparición", "sospechoso", "enigma", "oculto")
            ),
            ClusterTemplate(
                name = "ANIMACIÓN & MARAVILLA",
                category = "Animación",
                center = Offset(0.50f, 0.48f),
                radius = 0.10f,
                macroTags = listOf(
                    // Anillo 1: Tropos centrales
                    "Clásicos Dibujados", "Aventuras Familiares", "Anime Maestro", "Fábulas con Corazón",
                    "Mundos Flotantes", "Amistades Inolvidables", "Criaturas del Bosque", "Viajes Fantásticos",
                    // Anillo 2: Escenarios y arquetipos
                    "Espíritus de la Naturaleza", "Compañero Animal Leal", "Volar entre Nubes", "La Tienda de Pociones",
                    "El Tren Estelar", "Robots con Alma", "Magia Cotidiana", "Grandes Paisajes Pintados",
                    "Transformaciones", "Canto y Música",
                    // Anillo 3: Situaciones y atmósfera
                    "Jardín Secreto", "Viento en las Colinas", "La Casa del Árbol", "El Relojero Mágico",
                    "Criaturas del Mar Profundo", "Amigos del Cosmos", "El Hechizo Roto", "Aventura en Miniatura",
                    "Bicicleta al Atardecer", "El Dragón Domado"
                ),
                keywords = listOf("animation", "anime", "family", "kids", "animación", "dibujos", "ghibli", "infantil", "pixar", "dibujo")
            ),
            ClusterTemplate(
                name = "BÉLICO & HISTORIA",
                category = "Bélica",
                center = Offset(0.80f, 0.49f),
                radius = 0.10f,
                macroTags = listOf(
                    // Anillo 1: Tropos centrales
                    "Trincheras & Fango", "Tripulación de Submarino", "Guerra Fría & Espías", "Resistencia Civil",
                    "Batallas Navales", "Aviación & Ases", "Segunda Guerra Mundial", "El Frente Oriental",
                    // Anillo 2: Escenarios y arquetipos
                    "Misión de Rescate Táctica", "El Francotirador Oculto", "Búnker Asediado", "Desembarco en la Playa",
                    "Convoy de Suministros", "El General en el Mapa", "Prisioneros de Guerra", "La Gran Evasión",
                    "Mensaje por Radio", "El Regreso a Casa",
                    // Anillo 3: Situaciones y atmósfera
                    "Nieve en el Asedio", "Duelo de Blindados", "Radar en la Niebla", "Última Carta al Frente",
                    "Periscopio en Alerta", "El Puesto de Guardia", "Héroes Olvidados", "El Médico de Campaña",
                    "Armisticio al Amanecer", "Ruinas de la Ciudad"
                ),
                keywords = listOf("war", "history", "military", "submarine", "wwii", "guerra", "batalla", "bélico", "resistencia", "soldado", "trinchera")
            ),
            ClusterTemplate(
                name = "TERROR & SUSPENSE",
                category = "Terror",
                center = Offset(0.22f, 0.72f),
                radius = 0.10f,
                macroTags = listOf(
                    // Anillo 1: Tropos centrales
                    "Casas Encantadas", "Posesiones & Demonios", "Terror Psicológico", "Monstruos Ocultos",
                    "Niebla & Aislamiento", "Brujería & Sectas", "Slasher Clásico", "Paranoia en la Nieve",
                    // Anillo 2: Escenarios y arquetipos
                    "Llamada a Medianoche", "Criaturas del Abismo", "Pesadillas Reales", "Maldición Familiar",
                    "El Asilo Abandonado", "Muñecos Siniestros", "Pacto en el Cruce", "El Bosque Maldito",
                    "Ruidos en el Desván", "El Huésped Siniestro",
                    // Anillo 3: Situaciones y atmósfera
                    "Espejo Maldito", "Grabación Oculta", "Pasos en el Pasillo", "El Pozo del Jardín",
                    "La Niebla Roja", "La Llave Prohibida", "Cementerio en la Colina", "Sombra en la Ventana",
                    "La Puerta que Cruje", "Gritos en el Sótano"
                ),
                keywords = listOf("horror", "terror", "ghost", "demon", "exorcism", "witch", "monster", "slasher", "haunted", "siniestro", "miedo", "pesadilla")
            ),
            ClusterTemplate(
                name = "COMEDIA & SÁTIRA",
                category = "Comedia",
                center = Offset(0.50f, 0.72f),
                radius = 0.10f,
                macroTags = listOf(
                    // Anillo 1: Tropos centrales
                    "Comedia Romántica", "Sátira Mordaz", "Humor Negro", "Comedia de Amigos",
                    "Enredos Familiares", "Viaje Desastroso", "Timadores Torpes", "Parodia Brillante",
                    // Anillo 2: Escenarios y arquetipos
                    "La Gran Fiesta Caótica", "Cena de Compromiso Tensa", "Comedia Absurda", "Humor Inteligente",
                    "Boda Catastrófica", "Identidad Equivocada", "Vacaciones Accidentadas", "Amor en la Oficina",
                    "Compañeros de Piso", "Venganza Divertida",
                    // Anillo 3: Situaciones y atmósfera
                    "El Testamento Loco", "Disfraces Imposibles", "El Camarero Torpe", "Casting Desastroso",
                    "La Casa en Obras", "El Rival Inesperado", "Terapia de Grupo", "El Regalo Equivocado",
                    "La Mascota Rebelde", "Malentendido Gigante"
                ),
                keywords = listOf("comedy", "satire", "comedia", "humor", "rom-com", "parody", "enredo", "gracioso", "risa", "divertido")
            ),
            ClusterTemplate(
                name = "WESTERN & FORAJIDOS",
                category = "Western",
                center = Offset(0.78f, 0.72f),
                radius = 0.10f,
                macroTags = listOf(
                    // Anillo 1: Tropos centrales
                    "Duelo al Mediodía", "Cazarrecompensas", "Forajidos en Fuga", "Venganza en el Cañón",
                    "El Sheriff Solitario", "El Tren Asaltado", "Cantinas & Saloons", "Pueblos sin Ley",
                    // Anillo 2: Escenarios y arquetipos
                    "Tiroteo en la Frontera", "El Último Forajido", "Desierto Implacable", "Oro en el Río",
                    "Diligencia Asediada", "El Jugador de Cartas", "Rancho en Llamas", "Justicia en la Horca",
                    "El Forastero Misterioso", "Pistolero Cansado",
                    // Anillo 3: Situaciones y atmósfera
                    "Espuelas de Plata", "Emboscada en el Paso", "Caballo Fiel", "El Juez del Territorio",
                    "Tierra de Nadie", "Cicatrices del Pasado", "Bala de Plata", "El Viejo Fuerte",
                    "Atardecer en el Cañón", "El Duelo de Miradas"
                ),
                keywords = listOf("western", "frontier", "outlaw", "sheriff", "bounty", "duelo", "pistolero", "vaquero", "desierto", "forajido")
            ),
            ClusterTemplate(
                name = "FANTASÍA & MITOLOGÍA",
                category = "Fantasía",
                center = Offset(0.68f, 0.60f),
                radius = 0.10f,
                macroTags = listOf(
                    // Anillo 1: Tropos centrales
                    "Reinos Medievales", "Dragones & Fuego", "Cuentos de Hadas", "Magos & Hechizos",
                    "Mitología Antigua", "Espadas & Honor", "Viajes a Tierras Mágicas", "Criaturas Legendarias",
                    // Anillo 2: Escenarios y arquetipos
                    "La Última Batalla", "Castillos Olvidados", "Maldición del Rey", "Folclore Oscuro",
                    "El Libro de Conjuros", "El Bosque Encantado", "El Anillo de Poder", "El Caballero Errante",
                    "El Oráculo Ciego", "Puerta a Otra Dimensión",
                    // Anillo 3: Situaciones y atmósfera
                    "Espada en la Roca", "El Alquimista Solitario", "Ninfas del Manantial", "Ruinas de los Antiguos",
                    "Torre del Hechicero", "El Amuleto Brillante", "Gigantes de Piedra", "La Corona Robada",
                    "El Laberinto Sin Fin", "Pacto con el Destino"
                ),
                keywords = listOf("fantasy", "magic", "dragon", "kingdom", "medieval", "fairy-tale", "mythology", "fantasía", "magia", "hadas", "brujo", "hechizo")
            )
        )

        // Optimización de alto rendimiento: pre-procesar texto normalizado una sola vez
        val tokenized = catalog.map { entity ->
            val raw = "${entity.genres ?: ""} ${entity.tags ?: ""} ${entity.title} ${entity.originalTitle ?: ""} ${entity.overview ?: ""}"
            val text = normalizeForSearch(raw)
            Pair(entity, text)
        }
        tokenizedCatalog = tokenized

        // 1. GENERAR NODOS DE ETIQUETAS Y MICRO-ETIQUETAS (280+ etiquetas simultáneas)
        clusters.forEachIndexed { clusterIdx, cluster ->
            val clusterMatches = tokenized.filter { (_, text) ->
                cluster.keywords.any { text.contains(it) }
            }
            val clusterCount = clusterMatches.size

            // Nodo central del cúmulo (macro-foco)
            val clusterCenterNode = SpatialNebulaNode(
                id = "CLUSTER_${clusterIdx}",
                label = cluster.name,
                category = cluster.category,
                worldX = cluster.center.x,
                worldY = cluster.center.y,
                minZoomVisible = 0.5f,
                maxZoomVisible = 4.0f,
                importance = 1.45f,
                movieCount = clusterCount.coerceAtLeast(1),
                keywords = cluster.keywords
            )
            addNode(clusterCenterNode)

            // Distribución armónica en 3 anillos concéntricos
            val createdTagNodes = mutableListOf<SpatialNebulaNode>()
            val numTags = cluster.macroTags.size

            cluster.macroTags.forEachIndexed { tagIdx, tagLabel ->
                val (tierRadiusMult, minZoom, importance) = when {
                    tagIdx < 8 -> Triple(0.48f, 0.65f, 1.20f)  // Anillo 1: Tropos centrales
                    tagIdx < 18 -> Triple(0.82f, 1.15f, 1.00f) // Anillo 2: Escenarios y motivos
                    else -> Triple(1.15f, 1.75f, 0.85f)        // Anillo 3: Micro-detalles y situaciones
                }

                val ringIndex = when {
                    tagIdx < 8 -> tagIdx
                    tagIdx < 18 -> tagIdx - 8
                    else -> tagIdx - 18
                }
                val ringSize = when {
                    tagIdx < 8 -> 8
                    tagIdx < 18 -> 10
                    else -> numTags - 18
                }

                val angleOffset = when {
                    tagIdx < 8 -> 0f
                    tagIdx < 18 -> 0.35f
                    else -> 0.70f
                }

                val angle = (ringIndex.toFloat() / ringSize.toFloat()) * (2 * PI).toFloat() + angleOffset
                val dist = cluster.radius * tierRadiusMult
                val posX = (cluster.center.x + cos(angle) * dist).coerceIn(0.04f, 0.96f)
                val posY = (cluster.center.y + sin(angle) * (dist * 0.9f)).coerceIn(0.06f, 0.94f)

                val tagKeywords = generateTagKeywords(tagLabel, cluster)
                val tagCount = clusterMatches.count { (_, text) -> tagKeywords.any { text.contains(it) } }

                val tagNode = SpatialNebulaNode(
                    id = "TAG_${clusterIdx}_$tagIdx",
                    label = tagLabel,
                    category = cluster.category,
                    worldX = posX,
                    worldY = posY,
                    minZoomVisible = minZoom,
                    maxZoomVisible = 8.0f,
                    importance = importance,
                    movieCount = if (tagCount > 0) tagCount else (clusterCount / numTags).coerceAtLeast(1),
                    keywords = tagKeywords
                )
                addNode(tagNode)
                createdTagNodes.add(tagNode)

                // Filamento entre el centro del cúmulo y las etiquetas del primer anillo
                if (tagIdx < 8) {
                    allFilaments.add(SpatialFilament(clusterCenterNode.id, tagNode.id, 0.9f))
                } else if (tagIdx < 18) {
                    // Filamento desde una etiqueta del anillo 1 a la del anillo 2
                    val parentTagIdx = tagIdx % 8
                    allFilaments.add(SpatialFilament("TAG_${clusterIdx}_$parentTagIdx", tagNode.id, 0.7f))
                } else {
                    // Filamento desde una etiqueta del anillo 2 a la del anillo 3
                    val parentTagIdx = 8 + (tagIdx % 10)
                    allFilaments.add(SpatialFilament("TAG_${clusterIdx}_$parentTagIdx", tagNode.id, 0.5f))
                }
            }

            // Filamentos anulares entre vecinos del mismo anillo
            for (i in 0 until 8) {
                allFilaments.add(SpatialFilament("TAG_${clusterIdx}_$i", "TAG_${clusterIdx}_${(i + 1) % 8}", 0.4f))
            }
        }

        // Filamentos interestelares tenues entre cúmulos adyacentes
        allFilaments.add(SpatialFilament("CLUSTER_0", "CLUSTER_3", 0.45f)) // Crimen - Misterio
        allFilaments.add(SpatialFilament("CLUSTER_0", "CLUSTER_1", 0.45f)) // Crimen - Drama
        allFilaments.add(SpatialFilament("CLUSTER_1", "CLUSTER_2", 0.45f)) // Drama - Aventura
        allFilaments.add(SpatialFilament("CLUSTER_2", "CLUSTER_5", 0.45f)) // Aventura - Bélica
        allFilaments.add(SpatialFilament("CLUSTER_3", "CLUSTER_6", 0.45f)) // Misterio - Terror
        allFilaments.add(SpatialFilament("CLUSTER_4", "CLUSTER_9", 0.45f)) // Animación - Fantasía
        allFilaments.add(SpatialFilament("CLUSTER_5", "CLUSTER_8", 0.45f)) // Bélica - Western
        allFilaments.add(SpatialFilament("CLUSTER_6", "CLUSTER_7", 0.45f)) // Terror - Comedia
        allFilaments.add(SpatialFilament("CLUSTER_7", "CLUSTER_8", 0.45f)) // Comedia - Western
    }

    /**
     * Normaliza cadenas para búsqueda de alta tolerancia semántica (sin acentos, minúsculas).
     */
    private fun normalizeForSearch(text: String): String {
        val lower = text.lowercase()
        val unaccented = java.text.Normalizer.normalize(lower, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        return "$lower $unaccented"
    }

    /**
     * Genera un conjunto rico de palabras clave semánticas, lemas y sinónimos para una etiqueta.
     */
    private fun generateTagKeywords(tagLabel: String, cluster: ClusterTemplate): List<String> {
        val result = mutableSetOf<String>()
        val lower = tagLabel.lowercase().trim()
        result.add(lower)

        // Diccionario semántico de alta precisión para tropos y motivos clave
        val tropeSynonyms = mapOf(
            "historias reales" to listOf("historia real", "hechos reales", "true story", "based on", "biography", "biografía", "biopic", "documentary", "documental", "vida real", "historia"),
            "drama familiar" to listOf("drama", "familia", "familiar", "family", "padres", "hijos", "padre", "madre", "relaciones familiares", "hogar"),
            "superación personal" to listOf("superación", "superacion", "inspirational", "overcoming", "lucha", "esfuerzo", "esperanza", "motivational"),
            "amor imposible" to listOf("romance", "amor", "love", "desamor", "romántica", "romantico", "pasión"),
            "pérdida & duelo" to listOf("duelo", "pérdida", "perdida", "muerte", "luto", "tragedia", "grief", "loss", "fallecimiento"),
            "secretos del pasado" to listOf("secreto", "pasado", "revelación", "misterio", "secret", "verdad", "truth"),
            "amistad incondicional" to listOf("amistad", "amigo", "amigos", "friendship", "friend", "lealtad", "compañerismo"),
            "infancia & madurez" to listOf("infancia", "niño", "niñez", "madurez", "coming of age", "crecimiento", "youth", "adolescencia"),
            "cine negro" to listOf("noir", "cine negro", "detective", "policial", "crimen", "investigación"),
            "atraco al banco" to listOf("atraco", "heist", "robo", "banco", "ladrón", "bank"),
            "mafia & clanes" to listOf("mafia", "gangster", "clan", "mob", "crimen organizado", "cartel"),
            "corrupción policial" to listOf("corrupción", "policía", "policial", "police", "cop"),
            "falso culpable" to listOf("falso culpable", "inocente", "conspiración", "trampa", "framed"),
            "detectives privados" to listOf("detective", "investigador", "investigación", "private investigator"),
            "juicios & ley" to listOf("juicio", "abogado", "tribunal", "ley", "legal", "court", "lawyer", "trial"),
            "venganza callejera" to listOf("venganza", "revenge", "justicia", "calle"),
            "clásicos dibujados" to listOf("dibujos", "animación", "animation", "clásico", "classic", "disney"),
            "aventuras familiares" to listOf("familia", "aventura", "family", "kids", "infantil", "children"),
            "anime maestro" to listOf("anime", "animación", "manga", "japón", "japan"),
            "fábulas con corazón" to listOf("fábula", "cuento", "moraleja", "corazón", "emotivo"),
            "mundos flotantes" to listOf("fantasía", "flotante", "mundo", "isla", "cielo"),
            "amistades inolvidables" to listOf("amistad", "amigo", "lealtad", "amigos", "friendship"),
            "criaturas del bosque" to listOf("criatura", "bosque", "animal", "espíritu", "naturaleza"),
            "viajes fantásticos" to listOf("viaje", "fantástico", "aventura", "journey", "mundo"),
            "trincheras & fango" to listOf("trinchera", "guerra", "bélico", "soldado", "wwi", "batalla"),
            "tripulación de submarino" to listOf("submarino", "submarine", "torpedo", "sonar", "naval"),
            "guerra fría & espías" to listOf("guerra fría", "espía", "espías", "spy", "kgb", "cia", "cold war"),
            "resistencia civil" to listOf("resistencia", "ocupación", "partisanos", "rebeldes", "lucha"),
            "batallas navales" to listOf("naval", "barco", "flota", "acorazado", "mar", "armada"),
            "aviación & ases" to listOf("aviación", "avión", "piloto", "combate aéreo", "fighter"),
            "segunda guerra mundial" to listOf("segunda guerra", "wwii", "ww2", "nazi", "aliados", "holocausto"),
            "el frente oriental" to listOf("frente oriental", "stalingrado", "rusia", "soviético"),
            "casas encantadas" to listOf("casa encantada", "haunted", "fantasma", "mansión", "espíritu"),
            "posesiones & demonios" to listOf("posesión", "demonio", "exorcismo", "exorcist", "diablo"),
            "terror psicológico" to listOf("terror psicológico", "psicológico", "paranoia", "locura", "mente"),
            "monstruos ocultos" to listOf("monstruo", "criatura", "bestia", "monster", "pesadilla"),
            "niebla & aislamiento" to listOf("niebla", "aislamiento", "aislado", "atrapados"),
            "brujería & sectas" to listOf("brujería", "bruja", "secta", "culto", "witch", "ritual"),
            "slasher clásico" to listOf("slasher", "asesino", "cuchillo", "máscara", "psicópata"),
            "paranoia en la nieve" to listOf("nieve", "ártico", "frío", "paranoia", "aislados"),
            "comedia romántica" to listOf("comedia romántica", "rom-com", "romance", "amor", "comedia"),
            "sátira mordaz" to listOf("sátira", "parodia", "ironía", "crítica", "satire"),
            "humor negro" to listOf("humor negro", "sarcasmo", "ácido", "macabro", "black comedy"),
            "comedia de amigos" to listOf("amigos", "compañeros", "buddy", "desmadre", "fiesta"),
            "enredos familiares" to listOf("familia", "enredo", "caos", "comedia familiar"),
            "viaje desastroso" to listOf("viaje", "carretera", "vacaciones", "desastre", "road trip"),
            "timadores torpes" to listOf("estafa", "timo", "ladrones", "torpes", "chapuza"),
            "parodia brillante" to listOf("parodia", "absurdo", "burla", "spoof"),
            "duelo al mediodía" to listOf("duelo", "western", "pistolero", "tiroteo", "revolver"),
            "cazarrecompensas" to listOf("cazarrecompensas", "recompensa", "bounty hunter", "forajido"),
            "forajidos en fuga" to listOf("forajido", "fuga", "huida", "perseguido", "bandido"),
            "venganza en el cañón" to listOf("venganza", "cañón", "desierto", "pistola"),
            "el sheriff solitario" to listOf("sheriff", "comisario", "ley", "pueblo", "estrella"),
            "el tren asaltado" to listOf("tren", "asalto", "locomotora", "vía"),
            "cantinas & saloons" to listOf("saloon", "cantina", "bar", "póquer", "whisky"),
            "pueblos sin ley" to listOf("sin ley", "frontera", "pueblo", "bandidos"),
            "reinos medievales" to listOf("medieval", "reino", "rey", "castillo", "corona", "trono"),
            "dragones & fuego" to listOf("dragón", "dragon", "dragones", "fuego", "bestia"),
            "cuentos de hadas" to listOf("cuento de hadas", "hadas", "fairy", "princesa", "encantado"),
            "magos & hechizos" to listOf("mago", "hechizo", "brujo", "magia", "conjuro", "wizard"),
            "mitología antigua" to listOf("mitología", "dioses", "grecia", "olimpo", "leyenda", "mito"),
            "espadas & honor" to listOf("espada", "honor", "caballero", "duelo", "guerrero"),
            "viajes a tierras mágicas" to listOf("tierra mágica", "otra dimensión", "portal", "fantasía", "mundo mágico"),
            "criaturas legendarias" to listOf("criatura", "legendario", "monstruo fantástico", "bestia mítica"),
            "supervivencia salvaje" to listOf("supervivencia", "survival", "salvaje", "naturaleza", "aislado"),
            "naufragio en alta mar" to listOf("naufragio", "náufrago", "mar", "océano", "balsa", "isla desierta"),
            "alta montaña & nieve" to listOf("montaña", "nieve", "escalada", "alpinismo", "everest", "cumbre"),
            "caza del tesoro" to listOf("tesoro", "mapa del tesoro", "oro", "búsqueda", "arqueología"),
            "expedición a la selva" to listOf("selva", "jungla", "expedición", "amazonas", "exploración"),
            "piratas & corsarios" to listOf("pirata", "piratas", "corsario", "barco pirata", "tesoro pirata"),
            "aislamiento en el desierto" to listOf("desierto", "arena", "sed", "aislado", "caravana"),
            "exploradores sin retorno" to listOf("explorador", "expedición perdida", "tierra ignota", "aventura"),
            "secretos de estado" to listOf("secreto de estado", "gobierno", "conspiración", "presidente", "agencia"),
            "desaparición misteriosa" to listOf("desaparición", "desaparecido", "desaparecida", "búsqueda", "rastro"),
            "pueblo con secretos" to listOf("pueblo", "comunidad", "secretos", "sospechosos", "cerrado"),
            "crimen de habitación cerrada" to listOf("habitación cerrada", "asesinato misterioso", "enigma", "coartada"),
            "conspiración en la sombra" to listOf("conspiración", "complot", "en la sombra", "gobierno en la sombra"),
            "el testigo silenciado" to listOf("testigo", "silenciado", "amenaza", "asesinato", "protección"),
            "la doble vida" to listOf("doble vida", "secreto", "identidad oculta", "engaño"),
            "identidad robada" to listOf("identidad", "suplantación", "falso", "amnesia", "quién soy")
        )

        tropeSynonyms[lower]?.let { result.addAll(it) }

        // Tokenización morfológica y lemas para cualquier etiqueta (incluyendo anillos 2 y 3)
        val stopWords = setOf("el", "la", "los", "las", "un", "una", "unos", "unas", "de", "del", "en", "al", "y", "e", "o", "u", "a", "con", "por", "para", "tras", "sin", "sobre", "the", "a", "an", "and", "of", "in", "on", "at", "to", "for", "with")
        val tokens = lower.split(Regex("[^\\p{L}0-9]+")).filter { it.length >= 3 && it !in stopWords }

        for (token in tokens) {
            result.add(token)
            val unaccented = java.text.Normalizer.normalize(token, java.text.Normalizer.Form.NFD)
                .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            if (unaccented != token) result.add(unaccented)

            if (token.endsWith("es") && token.length > 4) {
                result.add(token.dropLast(2))
            } else if (token.endsWith("s") && token.length > 3) {
                result.add(token.dropLast(1))
            }

            if (token == "familiar" || token == "familiares") {
                result.add("familia")
                result.add("family")
            }
            if (token == "reales" || token == "real") {
                result.add("hechos reales")
                result.add("historia real")
                result.add("true story")
                result.add("biography")
                result.add("biografia")
            }
        }

        // Incorporar categoría del cúmulo como señal secundaria
        result.add(cluster.category.lowercase())

        return result.toList()
    }

    /**
     * Comprueba si un nodo candidato puede conectarse a la cadena activa.
     * Solo es válido si existe al menos 1 película real en el catálogo que satisfaga
     * la intersección completa de todas las etiquetas de la cadena más el candidato.
     */
    fun canExtendChain(currentChain: List<SpatialNebulaNode>, candidate: SpatialNebulaNode): Boolean {
        if (candidate.isPortal || candidate.id == PORTAL_NODE_ID) return true
        if (currentChain.isEmpty()) return true
        if (currentChain.any { it.id == candidate.id }) return false
        val compatibleIds = getCompatibleNodeIds(currentChain)
        return candidate.id in compatibleIds
    }

    /**
     * Calcula los IDs de todos los nodos del firmamento que tienen al menos una película
     * en común con la intersección de la cadena activa actual.
     */
    fun getCompatibleNodeIds(currentChain: List<SpatialNebulaNode>): Set<String> {
        if (currentChain.isEmpty()) {
            return allNodes.map { it.id }.toSet()
        }

        val chainKeywords = currentChain.map { node ->
            node.keywords.map { it.lowercase().trim() }.filter { it.isNotEmpty() }
        }

        val catalog = if (tokenizedCatalog.isNotEmpty()) tokenizedCatalog else return emptySet()

        // Obras que satisfacen TODAS las etiquetas de la cadena activa
        val matchingMovies = catalog.filter { (_, text) ->
            chainKeywords.all { kws -> kws.any { kw -> text.contains(kw) } }
        }

        if (matchingMovies.isEmpty()) {
            return emptySet()
        }

        // Nodos del firmamento que tienen al menos una película en común con las ya filtradas
        val compatible = mutableSetOf<String>()
        for (node in allNodes) {
            if (node.isPortal || node.id == PORTAL_NODE_ID) continue
            val kws = node.keywords.map { it.lowercase().trim() }.filter { it.isNotEmpty() }
            if (kws.isEmpty()) continue

            val hasOverlap = matchingMovies.any { (_, text) ->
                kws.any { kw -> text.contains(kw) }
            }
            if (hasOverlap) {
                compatible.add(node.id)
            }
        }
        return compatible
    }

    /**
     * Filtra y califica películas de la biblioteca que coinciden con la cadena de etiquetas conectadas.
     * Exige que las películas satisfagan TODAS las etiquetas de la cadena (intersección pura).
     */
    fun getMatchingMoviesForChain(
        chain: List<SpatialNebulaNode>,
        catalog: List<JellyfinMediaEntity>,
        serverUrl: String,
        accessToken: String
    ): List<MediaItem> {
        if (catalog.isEmpty() || chain.isEmpty()) return emptyList()

        // Lista de conjuntos de palabras clave para cada etiqueta en la cadena
        val chainKeywords = chain.map { node ->
            node.keywords.map { it.lowercase().trim() }.filter { it.isNotEmpty() }
        }

        val targetCatalog = if (tokenizedCatalog.isNotEmpty()) tokenizedCatalog else {
            catalog.map { entity ->
                val raw = "${entity.genres ?: ""} ${entity.tags ?: ""} ${entity.title} ${entity.originalTitle ?: ""} ${entity.overview ?: ""}"
                Pair(entity, normalizeForSearch(raw))
            }
        }

        // Exigir intersección completa: la película debe satisfacer todas las etiquetas de la cadena
        val matchedEntities = targetCatalog.mapNotNull { (entity, text) ->
            val tagsMatched = chainKeywords.count { kws -> kws.any { kw -> text.contains(kw) } }
            if (tagsMatched == chain.size) {
                val totalHits = chainKeywords.sumOf { kws -> kws.count { kw -> text.contains(kw) } }
                val rating = entity.communityRating ?: 0f
                val score = 1000f + (totalHits.coerceAtMost(20) * 5f) + (rating * 3f)
                Pair(entity, score)
            } else {
                null
            }
        }

        val finalEntities = matchedEntities
            .sortedByDescending { it.second }
            .take(30)
            .map { it.first }

        return finalEntities.map { it.toMediaItem(serverUrl, accessToken) }
    }

    fun updatePortalNode(chain: List<SpatialNebulaNode>, movieCount: Int) {
        allNodes.removeAll { it.id == PORTAL_NODE_ID }
        nodeById.remove(PORTAL_NODE_ID)
        allFilaments.removeAll { it.fromNodeId == PORTAL_NODE_ID || it.toNodeId == PORTAL_NODE_ID }

        // Si no hay etiquetas seleccionadas o no hay películas, no generar el portal
        if (chain.isEmpty() || movieCount <= 0) {
            return
        }

        val lastNode = chain.last()
        // Proyectar el portal hacia el espacio despejado exterior para evitar colisión con etiquetas del cúmulo
        val outwardX = if (lastNode.worldX >= 0.50f) 0.065f else -0.065f
        val outwardY = if (lastNode.worldY >= 0.50f) 0.045f else -0.045f
        val targetX = (lastNode.worldX + outwardX).coerceIn(0.08f, 0.92f)
        val targetY = (lastNode.worldY + outwardY).coerceIn(0.12f, 0.86f)
        val portalPos = Offset(targetX, targetY)

        val portalNode = SpatialNebulaNode(
            id = PORTAL_NODE_ID,
            label = "✦ VER $movieCount PELÍCULAS",
            category = "Portal",
            worldX = portalPos.x,
            worldY = portalPos.y,
            minZoomVisible = 0.4f,
            maxZoomVisible = 20.0f,
            movieCount = movieCount,
            importance = 3.0f,
            isPortal = true
        )
        addNode(portalNode)

        chain.forEach { node ->
            allFilaments.add(SpatialFilament(node.id, PORTAL_NODE_ID, 2.0f))
        }
    }

    private fun addNode(node: SpatialNebulaNode) {
        allNodes.add(node)
        nodeById[node.id] = node
    }

    /**
     * Calcula la posición concentrada de un nodo según la atracción gravitacional de la cadena activa.
     * Los nodos compatibles se atraen físicamente hacia el centro focal de la selección.
     */
    fun getEffectiveNodePosition(
        node: SpatialNebulaNode,
        activeChain: List<SpatialNebulaNode>,
        compatibleNodeIds: Set<String>?
    ): Offset {
        if (activeChain.isEmpty() || compatibleNodeIds == null) {
            return Offset(node.worldX, node.worldY)
        }
        if (node.isPortal || node.id == PORTAL_NODE_ID || activeChain.any { it.id == node.id }) {
            return Offset(node.worldX, node.worldY)
        }
        if (!compatibleNodeIds.contains(node.id)) {
            return Offset(node.worldX, node.worldY)
        }

        val focalX = activeChain.map { it.worldX }.average().toFloat()
        val focalY = activeChain.map { it.worldY }.average().toFloat()

        val attractionFactor = (0.35f + (activeChain.size - 1) * 0.15f).coerceAtMost(0.65f)
        val effX = node.worldX + (focalX - node.worldX) * attractionFactor
        val effY = node.worldY + (focalY - node.worldY) * attractionFactor

        return Offset(effX, effY)
    }

    /**
     * Algoritmo de Índice Espacial para D-Pad:
     * Encuentra el nodo más idóneo en el cono angular de la dirección pulsada,
     * priorizando proximidad y castigando desviaciones perpendiculares.
     */
    fun findNextNeighbor(
        currentId: String,
        direction: DPadDirection,
        currentZoom: Float,
        activeChain: List<SpatialNebulaNode> = emptyList(),
        compatibleNodeIds: Set<String>? = null
    ): SpatialNebulaNode? {
        val current = nodeById[currentId] ?: return allNodes.firstOrNull()

        // Si hay una cadena activa, limitar el foco D-Pad EXCLUSIVAMENTE a nodos compatibles o el portal
        var candidateNodes = if (activeChain.isNotEmpty() && compatibleNodeIds != null) {
            allNodes.filter { node ->
                node.id == PORTAL_NODE_ID || node.isPortal ||
                        activeChain.any { it.id == node.id } ||
                        compatibleNodeIds.contains(node.id)
            }
        } else {
            allNodes
        }

        if (candidateNodes.isEmpty()) {
            candidateNodes = allNodes
        }

        var candidates = candidateNodes.filter { node ->
            node.id != current.id && (node.isPortal || currentZoom >= (node.minZoomVisible * 0.65f) || node.importance >= 1.0f)
        }
        if (candidates.isEmpty()) {
            candidates = candidateNodes.filter { it.id != current.id }
        }

        val currentPos = getEffectiveNodePosition(current, activeChain, compatibleNodeIds)

        fun scoreCandidates(list: List<SpatialNebulaNode>): List<Pair<SpatialNebulaNode, Float>> {
            return list.mapNotNull { target ->
                val targetPos = getEffectiveNodePosition(target, activeChain, compatibleNodeIds)
                val dx = targetPos.x - currentPos.x
                val dy = targetPos.y - currentPos.y

                val isValidDirection = when (direction) {
                    DPadDirection.LEFT -> dx < -0.005f
                    DPadDirection.RIGHT -> dx > 0.005f
                    DPadDirection.UP -> dy < -0.005f
                    DPadDirection.DOWN -> dy > 0.005f
                }

                if (isValidDirection) {
                    val distance = hypot(dx, dy)
                    val anglePenalty = when (direction) {
                        DPadDirection.LEFT, DPadDirection.RIGHT -> abs(dy) * 1.8f
                        DPadDirection.UP, DPadDirection.DOWN -> abs(dx) * 1.8f
                    }
                    val isConnected = allFilaments.any { 
                        (it.fromNodeId == current.id && it.toNodeId == target.id) ||
                        (it.toNodeId == current.id && it.fromNodeId == target.id)
                    }
                    val bonus = when {
                        target.isPortal -> -0.08f // Gran atracción magnética hacia el portal de películas
                        isConnected -> -0.04f
                        else -> 0f
                    }
                    Pair(target, (distance + anglePenalty + bonus).coerceAtLeast(0.001f))
                } else null
            }
        }

        var scored = scoreCandidates(candidates)
        if (scored.isEmpty()) {
            scored = scoreCandidates(candidateNodes.filter { it.id != current.id })
        }

        return scored.minByOrNull { it.second }?.first
    }

    private fun countMatches(catalog: List<JellyfinMediaEntity>, keywords: List<String>): Int {
        return catalog.count { matchesAny(it, keywords) }
    }

    private fun matchesAny(entity: JellyfinMediaEntity, keywords: List<String>): Boolean {
        val text = "${entity.genres ?: ""} ${entity.tags ?: ""} ${entity.title} ${entity.overview ?: ""}".lowercase()
        return keywords.any { kw -> text.contains(kw.lowercase()) }
    }

    private data class ClusterTemplate(
        val name: String,
        val category: String,
        val center: Offset,
        val radius: Float,
        val macroTags: List<String>,
        val keywords: List<String>
    )

    private fun JellyfinMediaEntity.toMediaItem(baseUrl: String, token: String): MediaItem {
        val posterUrl = if (!primaryImageTag.isNullOrEmpty() && baseUrl.isNotBlank()) {
            val cleanBase = baseUrl.trimEnd('/')
            "$cleanBase/Items/$id/Images/Primary?quality=90&fillWidth=400&fillHeight=600"
        } else null

        val backdropUrl = if (!backdropImageTag.isNullOrEmpty() && baseUrl.isNotBlank()) {
            val cleanBase = baseUrl.trimEnd('/')
            "$cleanBase/Items/$id/Images/Backdrop/0?quality=90&maxWidth=1920"
        } else posterUrl

        return MediaItem(
            id = id,
            title = title,
            overview = overview,
            type = type,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            rating = communityRating,
            year = productionYear,
            source = MediaSource.JELLYFIN,
            playbackPositionTicks = playbackPositionTicks,
            isPlayed = isPlayed,
            isFavorite = isFavorite,
            totalEpisodes = totalItemCount,
            playedEpisodes = if (unplayedItemCount != null && totalItemCount != null) {
                (totalItemCount - unplayedItemCount).coerceAtLeast(0)
            } else null
        )
    }
}
