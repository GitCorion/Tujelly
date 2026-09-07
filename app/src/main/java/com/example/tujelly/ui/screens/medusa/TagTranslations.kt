package com.example.tujelly.ui.screens.medusa

import java.util.Locale

/**
 * Diccionario de traducción estricto al español para etiquetas de catálogo.
 * Garantiza cero términos en inglés sueltos en la interfaz.
 */
object TagTranslations {

    private val map = mapOf(
        "action" to "Acción",
        "adventure" to "Aventura",
        "animation" to "Animación",
        "anime" to "Anime",
        "comedy" to "Comedia",
        "crime" to "Crimen",
        "documentary" to "Documental",
        "drama" to "Drama",
        "family" to "Familia",
        "fantasy" to "Fantasía",
        "history" to "Historia",
        "horror" to "Terror",
        "music" to "Música",
        "mystery" to "Misterio",
        "romance" to "Romance",
        "science fiction" to "Ciencia Ficción",
        "sci-fi" to "Ciencia Ficción",
        "thriller" to "Thriller",
        "war" to "Bélica",
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

        "action & adventure" to "Acción y Aventura",
        "sci-fi & fantasy" to "Ciencia Ficción y Fantasía",
        "war & politics" to "Guerra y Política",

        "supernatural" to "Sobrenatural",
        "murder" to "Asesinato",
        "serial killer" to "Asesino en Serie",
        "monster" to "Monstruo",
        "zombie" to "Zombi",
        "vampire" to "Vampiro",
        "ghost" to "Fantasma",
        "demon" to "Demonio",
        "possession" to "Posesión",
        "haunted house" to "Casa Encantada",
        "slasher" to "Slasher",
        "creature" to "Criatura",
        "shark" to "Tiburón",
        "animal attack" to "Ataque Animal",
        "alien" to "Alienígena",
        "space" to "Espacio",
        "robot" to "Robot",
        "cyberpunk" to "Cyberpunk",
        "dystopia" to "Distopía",
        "post-apocalyptic" to "Post-Apocalíptico",
        "apocalypse" to "Apocalipsis",
        "survival" to "Supervivencia",
        "time travel" to "Viajes en el Tiempo",
        "virtual reality" to "Realidad Virtual",
        "cyberspace" to "Ciberespacio",

        "magic" to "Magia",
        "dragon" to "Dragón",
        "witch" to "Brujería",
        "werewolf" to "Hombre Lobo",
        "wizard" to "Mago",
        "kingdom" to "Reino",
        "swordplay" to "Esgrima",
        "samurai" to "Samurái",
        "ninja" to "Ninja",
        "martial arts" to "Artes Marciales",

        "friendship" to "Amistad",
        "dating" to "Citas",
        "wedding" to "Boda",
        "marriage" to "Matrimonio",
        "breakup" to "Ruptura",
        "coming of age" to "Crecimiento y Madurez",
        "slice of life" to "Vida Cotidiana",
        "kids" to "Infantil",
        "children" to "Niños",
        "sitcom" to "Sitcom",
        "parody" to "Parodia",
        "satire" to "Sátira",
        "dark comedy" to "Comedia Negra",

        "police" to "Policial",
        "detective" to "Detective",
        "investigation" to "Investigación",
        "murder mystery" to "Misterio de Asesinato",
        "heist" to "Atraco y Robo",
        "mafia" to "Mafia",
        "gangster" to "Gánster",
        "organized crime" to "Crimen Organizado",
        "spy" to "Espionaje",
        "espionage" to "Espionaje",
        "conspiracy" to "Conspiración",
        "political" to "Política",
        "true crime" to "Crimen Real",
        "based on true story" to "Hechos Reales",
        "biography" to "Biográfico",
        "based on novel or book" to "Novela",
        "based on comic" to "Cómic",
        "based on video game" to "Videojuego",
        "based on light novel" to "Novela Ligera",
        "based on movie" to "Basado en Película",

        "miniseries" to "Miniserie",
        "anthology" to "Antología",
        "period drama" to "Drama de Época",
        "1980s" to "Años 80",
        "1990s" to "Años 90",
        "1970s" to "Años 70",
        "1960s" to "Años 60",
        "1950s" to "Años 50",
        "retro" to "Retro",
        "classic" to "Clásico",
        "tragedy" to "Tragedia",
        "melancholy" to "Melancolía",
        "nostalgia" to "Nostalgia",

        "shounen" to "Shounen",
        "seinen" to "Seinen",
        "shoujo" to "Shoujo",
        "isekai" to "Isekai",
        "ecchi" to "Ecchi",
        "mecha" to "Mecha",
        "magical girl" to "Chica Mágica",
        "3d animation" to "Animación 3D",
        "stop motion" to "Stop Motion",

        "superhero" to "Superhéroes",
        "marvel" to "Marvel",
        "dc comics" to "DC",
        "sports" to "Deportes",
        "football" to "Fútbol",
        "boxing" to "Boxeo",
        "racing" to "Carreras",
        "rock" to "Rock",

        "lgbt" to "LGBTQ+",
        "gay" to "Gay",
        "lesbian" to "Lésbico",
        "queer" to "Queer",
        "transgender" to "Transgénero",

        "halloween" to "Halloween",
        "nature" to "Naturaleza",
        "ocean" to "Océano",
        "beach" to "Playa",
        "desert" to "Desierto",
        "mountain" to "Montaña",
        "forest" to "Bosque",
        "island" to "Isla",
        "small town" to "Pueblo Pequeño",
        "rural" to "Rural",
        "urban" to "Urbano",
        "new york" to "Nueva York",
        "los angeles" to "Los Ángeles"
    )

    fun translate(tag: String): String? = map[tag.lowercase().trim()]

    fun getDisplayName(tag: String): String {
        val clean = tag.trim().lowercase(Locale.ROOT)
        val translated = map[clean]
        if (translated != null) return translated

        // Si es una palabra en español o limpia, formatear capitalizando
        return clean.split("-", "_", " ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            }
    }
}
