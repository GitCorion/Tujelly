package com.example.tujelly.ui.screens.medusa

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Diccionario de traducción dinámico para etiquetas cinematográficas (TMDB y Jellyfin).
 * Incluye diccionario base estático, persistencia en caché local y sincronización
 * en vivo desde el repositorio de GitHub sin necesidad de actualizar la APK.
 */
object TagTranslations {

    private const val GITHUB_DICTIONARY_URL =
        "https://raw.githubusercontent.com/GitCorion/Tujelly/main/data/medusa_tags_es.json"
    private const val CACHE_FILE_NAME = "medusa_tags_cache.json"

    private val dynamicMap = ConcurrentHashMap<String, String>()

    private val baseMap = mapOf(
        "action" to "Acción",
        "accion" to "Acción",
        "adventure" to "Aventura",
        "animation" to "Animación",
        "animacion" to "Animación",
        "anime" to "Anime",
        "comedy" to "Comedia",
        "crime" to "Crimen",
        "documentary" to "Documental",
        "drama" to "Drama",
        "family" to "Familia",
        "fantasy" to "Fantasía",
        "fantasia" to "Fantasía",
        "history" to "Historia",
        "horror" to "Terror",
        "music" to "Música",
        "musica" to "Música",
        "mystery" to "Misterio",
        "romance" to "Romance",
        "science fiction" to "Ciencia Ficción",
        "sci-fi" to "Ciencia Ficción",
        "ciencia ficcion" to "Ciencia Ficción",
        "thriller" to "Suspense",
        "suspense" to "Suspense",
        "war" to "Bélica",
        "belica" to "Bélica",
        "western" to "Western",

        "romcom" to "Comedia Romántica",
        "romantic comedy" to "Comedia Romántica",
        "amor" to "Amor",
        "love" to "Amor",
        "manga" to "Basado en Manga",
        "based on manga" to "Basado en Manga",
        "navidad" to "Navidad",
        "christmas" to "Navidad",
        "musical" to "Musical",
        "school" to "Escolar",
        "high school" to "Instituto",
        "college" to "Universidad",

        "supernatural" to "Sobrenatural",
        "murder" to "Asesinato",
        "serial killer" to "Asesino en Serie",
        "monster" to "Monstruos",
        "monsters" to "Monstruos",
        "zombie" to "Zombis",
        "zombies" to "Zombis",
        "vampire" to "Vampiros",
        "vampires" to "Vampiros",
        "ghost" to "Fantasmas",
        "ghosts" to "Fantasmas",
        "demon" to "Demonios",
        "demons" to "Demonios",
        "possession" to "Posesión",
        "haunted house" to "Casa Encantada",
        "slasher" to "Slasher",
        "creature" to "Criatura",
        "shark" to "Tiburón",
        "animal attack" to "Ataque Animal",
        "alien" to "Alienígenas",
        "aliens" to "Alienígenas",
        "space" to "Espacio",
        "space opera" to "Space Opera",
        "robot" to "Robots",
        "robots" to "Robots",
        "artificial intelligence" to "Inteligencia Artificial",
        "ai" to "Inteligencia Artificial",
        "cyberpunk" to "Cyberpunk",
        "steampunk" to "Steampunk",
        "dystopia" to "Distopía",
        "dystopian" to "Distopía",
        "utopia" to "Utopía",
        "post-apocalyptic" to "Post-Apocalíptico",
        "apocalypse" to "Apocalipsis",
        "survival" to "Supervivencia",
        "time travel" to "Viajes en el Tiempo",
        "time loop" to "Bucle Temporal",
        "alternate reality" to "Realidad Alternativa",
        "multiverse" to "Multiverso",
        "parallel world" to "Mundo Paralelo",
        "virtual reality" to "Realidad Virtual",
        "cyberspace" to "Ciberespacio",
        "hacker" to "Hacker",
        "clone" to "Clones",

        "magic" to "Magia",
        "dragon" to "Dragón",
        "dragons" to "Dragones",
        "witch" to "Brujería",
        "witches" to "Brujas",
        "werewolf" to "Hombre Lobo",
        "wizard" to "Mago",
        "kingdom" to "Reino",
        "swordplay" to "Esgrima",
        "samurai" to "Samurái",
        "ninja" to "Ninja",
        "martial arts" to "Artes Marciales",
        "kung fu" to "Kung Fu",
        "mythology" to "Mitología",
        "greek mythology" to "Mitología Griega",

        "friendship" to "Amistad",
        "dating" to "Citas",
        "wedding" to "Boda",
        "marriage" to "Matrimonio",
        "breakup" to "Ruptura",
        "coming of age" to "Crecimiento y Madurez",
        "slice of life" to "Vida Cotidiana",
        "kids" to "Infantil",
        "children" to "Infantil",
        "sitcom" to "Sitcom",
        "parody" to "Parodia",
        "satire" to "Sátira",
        "dark comedy" to "Comedia Negra",
        "black comedy" to "Comedia Negra",

        "police" to "Policial",
        "police corruption" to "Corrupción Policial",
        "detective" to "Detective",
        "investigation" to "Investigación",
        "murder mystery" to "Misterio de Asesinato",
        "heist" to "Atraco",
        "bank robbery" to "Robo de Banco",
        "robbery" to "Robo",
        "mafia" to "Mafia",
        "gangster" to "Gánster",
        "mob" to "Crimen Organizado",
        "organized crime" to "Crimen Organizado",
        "cartel" to "Cártel",
        "drugs" to "Narcotráfico",
        "drug cartel" to "Cártel de la Droga",
        "undercover" to "Infiltrado",
        "prison" to "Prisión y Cárcel",
        "prison escape" to "Fuga de la Cárcel",
        "courtroom" to "Juicios y Ley",
        "lawyer" to "Abogados",
        "revenge" to "Venganza",
        "vigilante" to "Justiciero",
        "hitman" to "Sicario",
        "assassin" to "Asesino a Sueldo",
        "hostage" to "Rehenes",
        "kidnapping" to "Secuestro",
        "conspiracy" to "Conspiración",
        "political" to "Política",
        "spy" to "Espías",
        "espionage" to "Espionaje",
        "cold war" to "Guerra Fría",
        "true crime" to "Crimen Real",
        "based on true story" to "Hechos Reales",
        "biography" to "Biográfico",
        "based on novel or book" to "Basado en Novela",
        "based on comic" to "Basado en Cómic",
        "based on video game" to "Basado en Videojuego",
        "based on light novel" to "Novela Ligera",
        "based on movie" to "Basado en Película",

        "miniseries" to "Miniserie",
        "anthology" to "Antología",
        "period drama" to "Drama de Época",
        "costume drama" to "Drama de Época",
        "medieval" to "Medieval",
        "world war ii" to "Segunda Guerra Mundial",
        "wwii" to "Segunda Guerra Mundial",
        "world war i" to "Primera Guerra Mundial",
        "vietnam war" to "Guerra de Vietnam",
        "submarine" to "Submarinos",
        "aviation" to "Aviación y Pilotos",
        "1980s" to "Años 80",
        "1990s" to "Años 90",
        "1970s" to "Años 70",
        "1960s" to "Años 60",
        "1950s" to "Años 50",
        "retro" to "Retro",
        "classic" to "Clásico",
        "cult classic" to "Película de Culto",
        "tragedy" to "Tragedia",
        "melancholy" to "Melancolía",
        "nostalgia" to "Nostalgia",
        "psychological" to "Psicológico",
        "mindfuck" to "Giro Psicológico",
        "plot twist" to "Giro Inesperado",
        "isolation" to "Aislamiento",
        "claustrophobic" to "Claustrofobia",

        "shounen" to "Shounen",
        "seinen" to "Seinen",
        "shoujo" to "Shoujo",
        "isekai" to "Isekai",
        "ecchi" to "Ecchi",
        "mecha" to "Mecha",
        "magical girl" to "Chica Mágica",
        "3d animation" to "Animación 3D",
        "stop motion" to "Stop Motion",
        "cgi" to "Efectos Visuales",

        "superhero" to "Superhéroes",
        "marvel" to "Marvel",
        "dc comics" to "DC Comics",
        "sports" to "Deportes",
        "football" to "Fútbol",
        "boxing" to "Boxeo",
        "racing" to "Carreras",
        "cars" to "Coches",
        "car chase" to "Persecución en Coche",
        "road trip" to "Viaje por Carretera",
        "rock" to "Música Rock",

        "lgbt" to "LGBTQ+",
        "gay" to "Gay",
        "lesbian" to "Lésbico",
        "queer" to "Queer",
        "transgender" to "Transgénero",

        "halloween" to "Halloween",
        "nature" to "Naturaleza",
        "ocean" to "Océano y Alta Mar",
        "shipwreck" to "Naufragio",
        "beach" to "Playa",
        "desert" to "Desierto",
        "mountain" to "Montaña y Nieve",
        "forest" to "Bosque",
        "island" to "Isla",
        "jungle" to "Selva",
        "small town" to "Pueblo Pequeño",
        "rural" to "Rural",
        "urban" to "Urbano",
        "new york" to "Nueva York",
        "los angeles" to "Los Ángeles",
        "tokyo" to "Tokio",
        "london" to "Londres",

        // Tropos adicionales de cine, narrativa y TMDB
        "psychopath" to "Psicopatía",
        "psychopaths" to "Psicópatas",
        "neo-noir" to "Neo-Noir",
        "neo noir" to "Neo-Noir",
        "film noir" to "Cine Negro",
        "noir" to "Cine Negro",
        "black and white" to "Blanco y Negro",
        "black-and-white" to "Blanco y Negro",
        "doctor" to "Medicina",
        "medicine" to "Medicina",
        "hospital" to "Hospital",
        "detective" to "Detectives",
        "detectives" to "Detectives",
        "amused" to "Humor",
        "hilarious" to "Comedia Desternillante",
        "suspenseful" to "Suspense",
        "suspense" to "Suspense",
        "intense" to "Intriga Intensa",
        "adoring" to "Pasión y Devoción",
        "infidelity" to "Infidelidad",
        "jealousy" to "Celos",
        "1940s" to "Años 40",
        "1930s" to "Años 30",
        "1920s" to "Años 20",
        "19th century" to "Siglo XIX",
        "20th century" to "Siglo XX",
        "dog" to "Perros y Mascotas",
        "dogs" to "Perros y Mascotas",
        "cat" to "Gatos",
        "cats" to "Gatos",
        "pet" to "Mascotas",
        "pets" to "Mascotas",
        "gay theme" to "LGBTQ+",
        "tematica lgbtq+" to "LGBTQ+",
        "temática lgbtq+" to "LGBTQ+",
        "cyborg" to "Cyborgs",
        "android" to "Androides",
        "bounty hunter" to "Cazarrecompensas",
        "fugitive" to "Fugitivo",
        "cult" to "Sectas y Cultos",
        "dark fantasy" to "Fantasía Oscura",
        "fairy tale" to "Cuentos de Hadas",
        "curse" to "Maldiciones",
        "exorcism" to "Exorcismos",
        "paranormal" to "Paranormal",
        "survival horror" to "Terror y Supervivencia",
        "psychological thriller" to "Thriller Psicológico",
        "courtroom drama" to "Drama Judicial",
        "political thriller" to "Thriller Político",
        "found footage" to "Metraje Encontrado",
        "mockumentary" to "Falso Documental",
        "buddy cop" to "Policías Compañeros",
        "road movie" to "Cine de Carretera",
        "disaster" to "Catástrofes",
        "pandemic" to "Pandemias",
        "apocalyptic" to "Apocalíptico",
        "pirate" to "Piratas",
        "pirates" to "Piratas",
        "ship" to "Barcos y Navegación",
        "train" to "Trenes",
        "airplane" to "Aviones",
        "subway" to "Metro Subterráneo",
        "amnesia" to "Amnesia",
        "secret agent" to "Agente Secreto",
        "yakuza" to "Yakuza",
        "corruption" to "Corrupción",
        "journalism" to "Periodismo",
        "theater" to "Teatro",
        "opera" to "Ópera",
        "religion" to "Religión",
        "witchcraft" to "Brujería",
        "gothic horror" to "Terror Gótico",
        "kaiju" to "Kaiju",
        "dinosaur" to "Dinosaurios",
        "dinosaurs" to "Dinosaurios",
        "ancient rome" to "Antigua Roma",
        "ancient greece" to "Antigua Grecia",
        "ancient egypt" to "Antiguo Egipto",
        "gladiator" to "Gladiadores",
        "viking" to "Vikingos",
        "vikings" to "Vikingos",
        "wild west" to "Salvaje Oeste",
        "outlaw" to "Forajidos",
        "sheriff" to "Sheriff",
        "gunslinger" to "Pistoleros",
        "civil war" to "Guerra Civil",
        "chernobyl" to "Chernóbil",
        "resistance" to "Resistencia",
        "spies" to "Espías",
        "redemption" to "Redención",
        "maze" to "Laberinto",
        "labyrinth" to "Laberintos",
        "gang" to "Bandas Callejeras",
        "mobster" to "Gánsteres",
        "secret society" to "Sociedades Secretas",
        "space travel" to "Viaje Espacial",
        "astronaut" to "Astronautas",
        "alien invasion" to "Invasión Extraterrestre",
        "first contact" to "Primer Contacto",
        "super powers" to "Superpoderes",
        "superpower" to "Superpoderes",
        "super power" to "Superpoderes",
        "supervillain" to "Supervillanos",
        "anti hero" to "Antihéroes",

        // Animación, Anime, Fantasía y Manga
        "fantasy world" to "Mundo Fantástico",
        "urban fantasy" to "Fantasía Urbana",
        "dark fantasy" to "Fantasía Oscura",
        "high fantasy" to "Alta Fantasía",
        "epic fantasy" to "Fantasía Épica",
        "magical world" to "Mundo Mágico",
        "sword and sorcery" to "Espada y Brujería",
        "japanese mythology" to "Mitología Japonesa",
        "japanese high school" to "Instituto Japonés",
        "high school student" to "Estudiantes",
        "high school" to "Instituto",
        "school life" to "Vida Escolar",
        "student" to "Estudiantes",
        "students" to "Estudiantes",
        "teacher" to "Profesores",
        "giant robot" to "Robots Gigantes",
        "giant robots" to "Robots Gigantes",
        "super robot" to "Super Robots",
        "real robot" to "Robots Mecha",
        "talking animal" to "Animales Parlantes",
        "talking animals" to "Animales Parlantes",
        "anthropomorphic" to "Antropomórfico",
        "anthropomorphic animal" to "Animales Antropomórficos",
        "demon slayer" to "Cazadores de Demonios",
        "exorcist" to "Exorcistas",
        "reincarnation" to "Reencarnación",
        "parallel world" to "Mundo Paralelo",
        "alternate universe" to "Universo Alternativo",
        "alternate reality" to "Realidad Alternativa",
        "video game" to "Videojuegos",
        "video games" to "Videojuegos",
        "gaming" to "Videojuegos",
        "gamer" to "Videojuegos",
        "board game" to "Juegos de Mesa",
        "card game" to "Juegos de Cartas",
        "toys" to "Juguetes",
        "toy" to "Juguetes",
        "doll" to "Muñecos",
        "dolls" to "Muñecos",
        "puppet" to "Marionetas",
        "puppets" to "Marionetas",
        "claymation" to "Plastilina",
        "2d animation" to "Animación 2D",
        "traditional animation" to "Animación Tradicional",
        "hand drawn" to "Dibujo a Mano",
        "folklore" to "Folclore",
        "norse mythology" to "Mitología Nórdica",
        "chinese mythology" to "Mitología China",
        "egyptian mythology" to "Mitología Egipcia",
        "giant monster" to "Monstruos Gigantes",
        "giant monsters" to "Monstruos Gigantes",
        "space exploration" to "Exploración Espacial",
        "deep space" to "Espacio Profundo",
        "outer space" to "Espacio Exterior",
        "alien planet" to "Planetas Extraterrestres",
        "space colony" to "Colonias Espaciales",
        "space station" to "Estación Espacial",
        "spaceship" to "Naves Espaciales",
        "treasure hunt" to "Búsqueda del Tesoro",
        "treasure" to "Tesoros",
        "lost city" to "Ciudades Perdidas",
        "lost civilization" to "Civilizaciones Perdidas",
        "wilderness" to "Naturaleza Salvaje",
        "desert island" to "Isla Desierta",
        "haunted forest" to "Bosque Encantado",
        "enchanted forest" to "Bosque Mágico",
        "fairy" to "Hadas",
        "fairies" to "Hadas",
        "elf" to "Elfos",
        "elves" to "Elfos",
        "dwarf" to "Enanos",
        "dwarves" to "Enanos",
        "orc" to "Orcos",
        "orcs" to "Orcos",
        "goblin" to "Goblins",
        "goblins" to "Goblins",
        "spirit" to "Espíritus",
        "spirits" to "Espíritus",
        "sorcery" to "Hechicería",
        "alchemist" to "Alquimia",
        "alchemy" to "Alquimia",
        "spell" to "Hechizos",
        "spells" to "Hechizos",
        "undead" to "No-Muertos",
        "shapeshifter" to "Cambiaformas",
        "body swap" to "Intercambio de Cuerpos",
        "slapstick" to "Comedia Física"
    )

    init {
        dynamicMap.putAll(baseMap)
        baseMap.forEach { (_, value) ->
            val unaccentedValueKey = stripAccents(value.trim().lowercase(Locale.ROOT))
            dynamicMap[unaccentedValueKey] = value
        }
    }

    /**
     * Inicializa y carga la caché local guardada previamente.
     */
    fun initCache(context: Context) {
        val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
        if (cacheFile.exists()) {
            runCatching {
                val jsonStr = cacheFile.readText()
                val json = JSONObject(jsonStr)
                json.keys().forEach { key ->
                    val value = json.optString(key)
                    if (value.isNotBlank()) {
                        dynamicMap[key.lowercase().trim()] = value
                    }
                }
            }
        }
    }

    /**
     * Sincroniza en segundo plano el diccionario remoto de GitHub.
     * Si descarga nuevas traducciones, las guarda en caché local.
     */
    suspend fun syncRemoteDictionary(context: Context) {
        withContext(Dispatchers.IO) {
            runCatching {
                val url = URL(GITHUB_DICTIONARY_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 4000
                connection.readTimeout = 4000
                connection.requestMethod = "GET"

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    var addedCount = 0

                    json.keys().forEach { key ->
                        val value = json.optString(key)
                        if (value.isNotBlank()) {
                            dynamicMap[key.lowercase().trim()] = value
                            addedCount++
                        }
                    }

                    if (addedCount > 0) {
                        val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
                        cacheFile.writeText(response)
                    }
                }
                connection.disconnect()
            }
        }
    }

    fun stripAccents(str: String): String {
        return java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }

    fun translate(tag: String): String? {
        val clean = tag.lowercase().trim()
        dynamicMap[clean]?.let { return it }
        baseMap[clean]?.let { return it }

        val unaccented = stripAccents(clean)
        if (unaccented != clean) {
            dynamicMap[unaccented]?.let { return it }
            baseMap[unaccented]?.let { return it }
        }
        return null
    }

    fun hasTranslation(tag: String): Boolean {
        val clean = tag.lowercase().trim()
        if (dynamicMap.containsKey(clean) || baseMap.containsKey(clean)) return true
        val unaccented = stripAccents(clean)
        return dynamicMap.containsKey(unaccented) || baseMap.containsKey(unaccented)
    }

    private val spanishAccents = setOf('á', 'é', 'í', 'ó', 'ú', 'ñ', 'ü')
    private val spanishStopWords = setOf("de", "del", "en", "el", "la", "los", "las", "y", "con", "para", "por", "un", "una")
    private val commonSpanishCinemaWords = setOf(
        "accion", "animacion", "aventura", "belica", "ciencia ficcion", "comedia", "crimen", "documental",
        "drama", "familia", "fantasia", "historia", "misterio", "musica", "musical", "romance", "terror",
        "suspense", "policial", "infantil", "venganza", "amistad", "hechos reales", "biografico", "cortometraje",
        "sobrenatural", "asesinato", "distopia", "supervivencia", "monstruo", "zombi", "vampiro", "fantasma",
        "magia", "brujeria", "artes marciales", "robot", "extraterrestre", "alienigena", "espacio", "epica",
        "clasico", "tragedia", "adolescencia", "juventud", "vejez", "atraco", "mafia", "narcotrafico",
        "carcel", "prision", "abogados", "politica", "espias", "espionaje", "belico", "militar", "deportes",
        "futbol", "boxeo", "carreras", "coches", "viajes", "carretera", "naufragio", "playa", "montaña",
        "bosque", "selva", "desierto", "isla", "pueblo", "ciudad", "submarino", "navidad", "halloween",
        "cine", "negro", "cine negro", "tiempo", "inteligencia artificial",
        "mundo", "mitologia", "oriental", "japones", "japonesa", "urbana", "urbano", "fantastico", "fantastica",
        "amor", "animales", "perros", "gatos", "juguetes", "espada", "brujas", "hadas", "dragones", "monstruos",
        "instituto", "colegio", "escuela", "jovenes", "naves", "reino", "castillo", "extraterrestres",
        "superpoderes", "heroes", "villanos", "ladrones", "robos", "misterios", "crimenes", "asesinos"
    )

    fun isSpanishText(tag: String): Boolean {
        val clean = tag.lowercase().trim()
        if (clean.any { it in spanishAccents }) return true
        val words = clean.split(" ", "-", "_").filter { it.isNotBlank() }
        if (words.any { it in spanishStopWords }) return true
        if (words.any { it in commonSpanishCinemaWords }) return true
        return false
    }

    fun getDisplayName(tag: String): String {
        val clean = tag.trim().lowercase(Locale.ROOT)
        val translated = translate(clean)
        if (translated != null) return translated

        // Si es una palabra en español o limpia, formatear capitalizando
        return clean.split("-", "_", " ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            }
    }
}
