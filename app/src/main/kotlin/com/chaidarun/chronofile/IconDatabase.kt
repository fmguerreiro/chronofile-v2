// © Art Chaidarun

package com.chaidarun.chronofile

import androidx.annotation.DrawableRes

data class IconEntry(
    @DrawableRes val iconRes: Int,
    val keywords: List<String>,
    val category: String = "general"
)

object IconDatabase {
    
    // ML-based activity classifier for intelligent icon prediction
    private val activityClassifier by lazy { ActivityClassifier() }
    
    // Self-improving learning system (initialized when context is available)
    private var learningSystem: IconLearningSystem? = null
    
    // Initialize learning system with context
    fun initialize(context: android.content.Context) {
        learningSystem = IconLearningSystem(context)
    }
    
    private val iconData = listOf(
        // Work & Professional
        IconEntry(R.drawable.ic_briefcase_line, listOf("work", "job", "office", "business", "professional", "career", "employment")),
        IconEntry(R.drawable.ic_computer, listOf("coding", "programming", "computer", "development", "software", "tech", "laptop")),
        IconEntry(R.drawable.ic_chart, listOf("analysis", "data", "chart", "report", "statistics", "analytics")),
        IconEntry(R.drawable.ic_trending_up, listOf("growth", "progress", "improvement", "success", "increase")),
        IconEntry(R.drawable.ic_note, listOf("writing", "note", "document", "planning", "task", "todo", "admin", "contract", "agreement", "paperwork", "legal")),
        IconEntry(R.drawable.ic_people, listOf("meeting", "team", "collaboration", "group", "discussion", "conference")),
        IconEntry(R.drawable.ic_phone, listOf("phone", "call", "contact", "communication", "telephone")),
        IconEntry(R.drawable.ic_email, listOf("email", "mail", "message", "correspondence", "communication")),
        IconEntry(R.drawable.ic_target, listOf("goal", "target", "objective", "focus", "aim", "achievement")),
        
        // Food & Meals
        IconEntry(R.drawable.ic_utensils_line, listOf("meal", "eat", "food", "dining", "lunch", "dinner", "breakfast")),
        IconEntry(R.drawable.ic_coffee, listOf("coffee", "break", "caffeine", "morning", "beverage", "drink")),
        IconEntry(R.drawable.ic_food, listOf("sandwich", "lunch", "snack", "quick meal")),
        IconEntry(R.drawable.ic_food, listOf("pizza", "dinner", "takeout", "casual dining")),
        IconEntry(R.drawable.ic_food, listOf("salad", "healthy", "vegetables", "diet", "nutrition")),
        IconEntry(R.drawable.ic_food, listOf("cooking", "breakfast", "preparation", "kitchen", "chef")),
        
        // Exercise & Health
        IconEntry(R.drawable.ic_run, listOf("exercise", "running", "fitness", "cardio", "workout", "sport", "training", "walk", "jog", "hike", "run")),
        IconEntry(R.drawable.ic_dumbbell, listOf("gym", "weightlifting", "strength", "muscle", "fitness", "weights", "boxing", "martial", "fight")),
        IconEntry(R.drawable.ic_bike, listOf("cycling", "bike", "cardio", "outdoor", "commute", "cycle")),
        IconEntry(R.drawable.ic_swim, listOf("swim", "swimming", "swimm", "pool", "water", "laps", "freestyle", "backstroke")),
        IconEntry(R.drawable.ic_meditation, listOf("meditation", "mindfulness", "relaxation", "zen", "wellness")),
        IconEntry(R.drawable.ic_health, listOf("health", "doctor", "medical", "checkup", "appointment")),
        IconEntry(R.drawable.ic_health, listOf("medicine", "medication", "pills", "treatment", "health")),
        
        // Transportation
        IconEntry(R.drawable.ic_car, listOf("drive", "car", "commute", "travel", "transportation", "vehicle")),
        IconEntry(R.drawable.ic_car, listOf("bus", "public transport", "commute", "transit")),
        IconEntry(R.drawable.ic_car, listOf("tram", "train", "metro", "subway", "rail")),
        IconEntry(R.drawable.ic_car, listOf("flight", "airplane", "travel", "trip", "vacation")),
        IconEntry(R.drawable.ic_run, listOf("walk", "walking", "pedestrian", "stroll", "exercise")),
        
        // Rest & Leisure
        IconEntry(R.drawable.ic_moon_line, listOf("sleep", "rest", "nap", "tired", "bed", "night")),
        IconEntry(R.drawable.ic_relax, listOf("relax", "chill", "rest", "lounge", "comfort", "leisure")),
        IconEntry(R.drawable.ic_tv, listOf("tv", "television", "watch", "entertainment", "show", "movie")),
        IconEntry(R.drawable.ic_note, listOf("read", "book", "study", "learn", "education", "literature")),
        IconEntry(R.drawable.ic_game, listOf("gaming", "game", "play", "entertainment", "video game")),
        IconEntry(R.drawable.ic_music, listOf("music", "listen", "song", "audio", "entertainment")),
        
        // Social & Relationships
        IconEntry(R.drawable.ic_people, listOf("social", "friends", "relationship", "together", "people")),
        IconEntry(R.drawable.ic_people, listOf("family", "kids", "children", "parents", "home")),
        IconEntry(R.drawable.ic_chat, listOf("chat", "conversation", "talk", "communication", "discussion")),
        IconEntry(R.drawable.ic_star, listOf("party", "celebration", "fun", "event", "festive")),
        IconEntry(R.drawable.ic_drink, listOf("drinks", "social", "bar", "alcohol", "evening")),
        
        // Household & Personal
        IconEntry(R.drawable.ic_home, listOf("home", "house", "personal", "domestic", "family")),
        IconEntry(R.drawable.ic_cleaning, listOf("cleaning", "chores", "housework", "tidy", "maintenance")),
        IconEntry(R.drawable.ic_shopping, listOf("shopping", "purchase", "buy", "store", "retail")),
        IconEntry(R.drawable.ic_cleaning, listOf("laundry", "washing", "clothes", "chores")),
        IconEntry(R.drawable.ic_calendar, listOf("repair", "fix", "maintenance", "tools", "diy")),
        IconEntry(R.drawable.ic_sprout, listOf("gardening", "plants", "nature", "outdoor", "hobby")),
        
        // Personal Care
        IconEntry(R.drawable.ic_hygiene, listOf("shower", "hygiene", "morning", "routine", "personal care")),
        IconEntry(R.drawable.ic_hygiene, listOf("brush teeth", "dental", "hygiene", "morning", "night")),
        IconEntry(R.drawable.ic_hygiene, listOf("makeup", "beauty", "grooming", "personal care")),
        IconEntry(R.drawable.ic_hygiene, listOf("haircut", "grooming", "salon", "personal care")),
        
        // Education & Learning
        IconEntry(R.drawable.ic_star, listOf("education", "graduation", "achievement", "learning", "school")),
        IconEntry(R.drawable.ic_note, listOf("textbook", "study", "research", "academic", "learning")),
        IconEntry(R.drawable.ic_note, listOf("write", "homework", "assignment", "notes", "study")),
        IconEntry(R.drawable.ic_calendar, listOf("science", "research", "experiment", "laboratory", "analysis")),
        
        // Emergency & Important
        IconEntry(R.drawable.ic_calendar, listOf("urgent", "emergency", "important", "alert", "crisis")),
        IconEntry(R.drawable.ic_calendar, listOf("energy", "power", "quick", "fast", "intense")),
        IconEntry(R.drawable.ic_fire, listOf("hot", "intense", "urgent", "priority", "important")),
        
        // Weather & Seasons
        IconEntry(R.drawable.ic_sun_line, listOf("sunny", "bright", "day", "weather", "outdoor")),
        IconEntry(R.drawable.ic_calendar, listOf("rain", "weather", "indoor", "stay in")),
        IconEntry(R.drawable.ic_calendar, listOf("cold", "winter", "snow", "weather")),
        
        // Default fallbacks
        IconEntry(R.drawable.ic_note, listOf("other", "misc", "general", "activity", "task")),
        IconEntry(R.drawable.ic_schedule, listOf("time", "schedule", "appointment", "calendar"))
    )
    
    // Create keyword to icon mapping for faster lookup
    private val keywordMap = mutableMapOf<String, Int>().apply {
        iconData.forEach { entry ->
            entry.keywords.forEach { keyword ->
                // Store both original and lemmatized versions
                this[keyword.lowercase()] = entry.iconRes
                this[keyword.lemmatize()] = entry.iconRes
            }
        }
    }
    
    @DrawableRes
    fun findByKeyword(keyword: String, userCategory: String? = null): Int {
        // 1. Category override - if activity belongs to user-defined group, use group icon
        userCategory?.let { category ->
            getCategoryIcon(category)?.let { return it }
        }
        
        return findByActivityText(keyword)
    }
    
    @DrawableRes
    private fun findByActivityText(activity: String): Int {
        android.util.Log.d("IconMapping", "Finding icon for: '$activity'")
        
        // 1. Check learned user preferences first (highest priority)
        learningSystem?.getLearnedIcon(activity)?.let { 
            android.util.Log.d("IconMapping", "$activity -> learned preference: $it")
            return it 
        }
        
        // 2. ML-based prediction (primary system)
        val prediction = activityClassifier.predictCategory(activity)
        android.util.Log.d("IconMapping", "$activity -> ML prediction: ${prediction.category} (${prediction.confidence})")
        if (prediction.confidence > 0.3f) { // Lower threshold to let ML handle more cases
            android.util.Log.d("IconMapping", "$activity -> using ML prediction: ${prediction.iconRes}")
            return prediction.iconRes
        }
        
        // 3. Direct keyword match (only for very specific terms)
        val normalized = activity.lemmatize()
        keywordMap[normalized]?.let { 
            android.util.Log.d("IconMapping", "$activity -> direct keyword match ($normalized): $it")
            return it 
        }
        
        // 4. Character-based heuristics for semantic fallbacks
        getSemanticFallback(activity)?.let { 
            android.util.Log.d("IconMapping", "$activity -> semantic fallback: $it")
            return it 
        }
        
        // 5. Very conservative partial matching (only for very close matches)
        findBestMatch(normalized).let { match ->
            if (match != R.drawable.ic_note) {
                android.util.Log.d("IconMapping", "$activity -> conservative partial match: $match")
                return match
            }
        }
        
        // 6. Universal fallback
        android.util.Log.d("IconMapping", "$activity -> universal fallback")
        return R.drawable.ic_note
    }
    
    /**
     * Records a user's manual icon selection to improve future predictions.
     * Call this when a user manually changes an icon for an activity.
     */
    fun recordUserIconSelection(activity: String, selectedIcon: Int) {
        learningSystem?.recordUserSelection(activity, selectedIcon)
    }
    
    /**
     * Gets confidence score for the icon prediction.
     * Useful for UI to show uncertainty indicators.
     */
    fun getPredictionConfidence(activity: String): Float {
        learningSystem?.getLearningConfidence(activity)?.let { 
            if (it > 0.5f) return it 
        }
        
        val prediction = activityClassifier.predictCategory(activity)
        return prediction.confidence
    }
    
    @DrawableRes
    private fun findBestMatch(text: String): Int {
        // Try partial matches - but be more strict to avoid false positives
        val partialMatch = keywordMap.entries.find { (keyword, _) ->
            // Only match if keyword is a substring of the activity (not vice versa)
            // and the match is substantial (at least 4 characters or 70% of the keyword)
            when {
                text.contains(keyword, ignoreCase = true) && keyword.length >= 4 -> true
                text.contains(keyword, ignoreCase = true) && keyword.length >= (text.length * 0.7) -> true
                else -> false
            }
        }
        
        if (partialMatch != null) {
            android.util.Log.d("IconMapping", "$text -> partial match found: '${partialMatch.key}' -> ${partialMatch.value}")
            return partialMatch.value
        }
        
        // Try fuzzy matching with edit distance - but be more conservative
        val fuzzyMatch = keywordMap.entries.minByOrNull { (keyword, _) ->
            editDistance(text.lowercase(), keyword.lowercase())
        }
        
        // Only use fuzzy match if it's very close (edit distance <= 1) and similar length
        val distance = fuzzyMatch?.let { editDistance(text.lowercase(), it.key.lowercase()) } ?: Int.MAX_VALUE
        val lengthDiff = fuzzyMatch?.let { kotlin.math.abs(text.length - it.key.length) } ?: Int.MAX_VALUE
        
        return if (fuzzyMatch != null && distance <= 1 && lengthDiff <= 2) {
            android.util.Log.d("IconMapping", "$text -> fuzzy match found: '${fuzzyMatch.key}' (distance: $distance) -> ${fuzzyMatch.value}")
            fuzzyMatch.value
        } else {
            R.drawable.ic_note // Default fallback
        }
    }
    
    private fun editDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                dp[i][j] = if (s1[i - 1] == s2[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }
        
        return dp[s1.length][s2.length]
    }
    
    // Category-based icon mapping
    private fun getCategoryIcon(category: String): Int? {
        return when (category.lowercase()) {
            "work", "professional", "career" -> R.drawable.ic_briefcase_line
            "exercise", "fitness", "sport", "movement" -> R.drawable.ic_run
            "food", "meal", "consumption", "eating" -> R.drawable.ic_utensils_line
            "sleep", "rest", "relaxation" -> R.drawable.ic_moon_line
            "social", "friends", "family", "relationship" -> R.drawable.ic_people
            "learning", "education", "study" -> R.drawable.ic_note
            "creative", "art", "hobbies" -> R.drawable.ic_star
            "travel", "transportation", "commute" -> R.drawable.ic_car
            "health", "medical", "wellness" -> R.drawable.ic_health
            "entertainment", "leisure", "fun" -> R.drawable.ic_tv
            else -> null
        }
    }
    
    // Character-based heuristics for semantic fallbacks
    private fun getSemanticFallback(activity: String): Int? {
        val text = activity.lowercase()
        
        return when {
            containsExerciseWords(text) -> R.drawable.ic_run
            containsWorkWords(text) -> R.drawable.ic_briefcase_line
            containsFoodWords(text) -> R.drawable.ic_utensils_line
            containsSleepWords(text) -> R.drawable.ic_moon_line
            containsSocialWords(text) -> R.drawable.ic_people
            containsLearningWords(text) -> R.drawable.ic_note
            containsCreativeWords(text) -> R.drawable.ic_star
            containsTravelWords(text) -> R.drawable.ic_car
            containsHealthWords(text) -> R.drawable.ic_health
            containsEntertainmentWords(text) -> R.drawable.ic_tv
            containsTimeWords(text) -> R.drawable.ic_schedule
            else -> null
        }
    }
    
    // Multilingual keyword detection helpers
    private fun containsExerciseWords(text: String): Boolean {
        val exerciseKeywords = listOf(
            // English
            "exercise", "workout", "gym", "fitness", "sport", "training", "run", "walk", "bike", "swim", "yoga", "stretch",
            // Spanish
            "ejercicio", "gimnasio", "deportes", "correr", "caminar", "nadar",
            // French
            "exercice", "sport", "course", "marche", "natation",
            // German
            "übung", "sport", "laufen", "schwimmen",
            // Portuguese
            "exercício", "academia", "esporte", "corrida", "caminhada"
        )
        return exerciseKeywords.any { text.contains(it) }
    }
    
    private fun containsWorkWords(text: String): Boolean {
        val workKeywords = listOf(
            // English
            "work", "job", "office", "meeting", "project", "task", "business", "career", "employment", "professional",
            // Spanish
            "trabajo", "oficina", "reunión", "proyecto", "negocio",
            // French
            "travail", "bureau", "réunion", "projet", "affaires",
            // German
            "arbeit", "büro", "meeting", "projekt", "geschäft",
            // Portuguese
            "trabalho", "escritório", "reunião", "projeto", "negócio"
        )
        return workKeywords.any { text.contains(it) }
    }
    
    private fun containsFoodWords(text: String): Boolean {
        val foodKeywords = listOf(
            // English
            "food", "eat", "meal", "lunch", "dinner", "breakfast", "snack", "cooking", "kitchen", "restaurant",
            // Spanish
            "comida", "comer", "almuerzo", "cena", "desayuno", "cocinar",
            // French
            "nourriture", "manger", "repas", "déjeuner", "dîner", "petit-déjeuner",
            // German
            "essen", "mahlzeit", "mittagessen", "abendessen", "frühstück",
            // Portuguese
            "comida", "comer", "almoço", "jantar", "café da manhã"
        )
        return foodKeywords.any { text.contains(it) }
    }
    
    private fun containsSleepWords(text: String): Boolean {
        val sleepKeywords = listOf(
            // English
            "sleep", "rest", "nap", "bed", "tired", "relax", "night", "dream",
            // Spanish
            "dormir", "descansar", "siesta", "cama", "noche",
            // French
            "dormir", "repos", "sieste", "lit", "nuit",
            // German
            "schlafen", "ruhe", "bett", "nacht",
            // Portuguese
            "dormir", "descansar", "soneca", "cama", "noite"
        )
        return sleepKeywords.any { text.contains(it) }
    }
    
    private fun containsSocialWords(text: String): Boolean {
        val socialKeywords = listOf(
            // English
            "social", "friends", "family", "people", "party", "gathering", "visit", "chat", "talk", "date",
            // Spanish
            "amigos", "familia", "gente", "fiesta", "visita", "charlar",
            // French
            "amis", "famille", "gens", "fête", "visite", "parler",
            // German
            "freunde", "familie", "leute", "party", "besuch", "sprechen",
            // Portuguese
            "amigos", "família", "pessoas", "festa", "visita", "conversar"
        )
        return socialKeywords.any { text.contains(it) }
    }
    
    private fun containsLearningWords(text: String): Boolean {
        val learningKeywords = listOf(
            // English
            "study", "learn", "read", "book", "education", "school", "university", "course", "research", "homework",
            // Spanish
            "estudiar", "aprender", "leer", "libro", "educación", "escuela",
            // French
            "étudier", "apprendre", "lire", "livre", "éducation", "école",
            // German
            "studieren", "lernen", "lesen", "buch", "bildung", "schule",
            // Portuguese
            "estudar", "aprender", "ler", "livro", "educação", "escola"
        )
        return learningKeywords.any { text.contains(it) }
    }
    
    private fun containsCreativeWords(text: String): Boolean {
        val creativeKeywords = listOf(
            // English
            "art", "creative", "draw", "paint", "music", "write", "craft", "hobby", "design", "photography",
            // Spanish
            "arte", "creativo", "dibujar", "pintar", "música", "escribir",
            // French
            "art", "créatif", "dessiner", "peindre", "musique", "écrire",
            // German
            "kunst", "kreativ", "zeichnen", "malen", "musik", "schreiben",
            // Portuguese
            "arte", "criativo", "desenhar", "pintar", "música", "escrever"
        )
        return creativeKeywords.any { text.contains(it) }
    }
    
    private fun containsTravelWords(text: String): Boolean {
        val travelKeywords = listOf(
            // English
            "travel", "trip", "journey", "drive", "flight", "train", "bus", "commute", "transport", "vacation",
            // Spanish
            "viaje", "conducir", "vuelo", "tren", "autobús", "vacaciones",
            // French
            "voyage", "conduire", "vol", "train", "bus", "vacances",
            // German
            "reise", "fahren", "flug", "zug", "bus", "urlaub",
            // Portuguese
            "viagem", "dirigir", "voo", "trem", "ônibus", "férias"
        )
        return travelKeywords.any { text.contains(it) }
    }
    
    private fun containsHealthWords(text: String): Boolean {
        val healthKeywords = listOf(
            // English
            "health", "medical", "doctor", "hospital", "medicine", "therapy", "wellness", "checkup",
            // Spanish
            "salud", "médico", "hospital", "medicina", "terapia",
            // French
            "santé", "médecin", "hôpital", "médecine", "thérapie",
            // German
            "gesundheit", "arzt", "krankenhaus", "medizin", "therapie",
            // Portuguese
            "saúde", "médico", "hospital", "medicina", "terapia"
        )
        return healthKeywords.any { text.contains(it) }
    }
    
    private fun containsEntertainmentWords(text: String): Boolean {
        val entertainmentKeywords = listOf(
            // English
            "entertainment", "tv", "movie", "film", "show", "video", "game", "play", "fun", "leisure",
            // Spanish
            "entretenimiento", "película", "espectáculo", "juego", "diversión",
            // French
            "divertissement", "film", "spectacle", "jeu", "amusement",
            // German
            "unterhaltung", "film", "show", "spiel", "spaß",
            // Portuguese
            "entretenimento", "filme", "show", "jogo", "diversão"
        )
        return entertainmentKeywords.any { text.contains(it) }
    }
    
    private fun containsTimeWords(text: String): Boolean {
        val timeKeywords = listOf(
            // English
            "time", "schedule", "appointment", "calendar", "meeting", "deadline", "urgent",
            // Spanish
            "tiempo", "horario", "cita", "calendario", "urgente",
            // French
            "temps", "horaire", "rendez-vous", "calendrier", "urgent",
            // German
            "zeit", "termin", "kalender", "dringend",
            // Portuguese
            "tempo", "horário", "compromisso", "calendário", "urgente"
        )
        return timeKeywords.any { text.contains(it) }
    }
}