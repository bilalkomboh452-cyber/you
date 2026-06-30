
package com.waglo.app.utils

import com.waglo.app.model.GroupLink.Category

/**
 * Rule-based classifier. Fast, zero-allocation per call (uses pre-compiled sets).
 * Returns one of the 11 categories defined in [Category].
 */
object Classifier {

    // Each entry: Pair(categoryName, keywords)
    private val RULES: List<Pair<String, Set<String>>> = listOf(
        Category.AI to setOf(
            "ai", "artificial intelligence", "machine learning", "ml", "deep learning",
            "chatgpt", "openai", "gpt", "llm", "nlp", "neural", "bard", "gemini",
            "midjourney", "stable diffusion", "computer vision", "robotics"
        ),
        Category.CRYPTO to setOf(
            "crypto", "bitcoin", "btc", "ethereum", "eth", "binance", "nft",
            "defi", "blockchain", "web3", "altcoin", "token", "wallet",
            "trading", "coin", "usdt", "solana", "polygon", "bnb"
        ),
        Category.EDUCATION to setOf(
            "education", "study", "learn", "course", "school", "university",
            "college", "student", "teacher", "class", "lecture", "exam",
            "tutorial", "training", "scholarship", "degree", "تعلیم", "طالبعلم",
            "پڑھائی", "اسکول"
        ),
        Category.JOBS to setOf(
            "job", "jobs", "career", "hiring", "vacancy", "recruitment",
            "internship", "hr", "resume", "cv", "work from home", "freelance",
            "naukri", "employment", "وظیفہ", "نوکری", "ملازمت"
        ),
        Category.TECHNOLOGY to setOf(
            "tech", "technology", "programming", "coding", "developer", "software",
            "android", "ios", "flutter", "kotlin", "python", "java", "javascript",
            "react", "nodejs", "cloud", "devops", "cybersecurity", "hacking",
            "github", "linux", "arduino", "raspberry"
        ),
        Category.NEWS to setOf(
            "news", "breaking", "headlines", "geo", "ary", "samaa", "dunya",
            "hum", "express", "urdu", "pakistan", "اردو", "پاکستان", "نیوز",
            "خبریں", "akhbar", "jang", "dawn", "bbc", "reuters", "media", "press",
            "aaj", "nawaiwaqt", "92news", "bolnews"
        ),
        Category.BUSINESS to setOf(
            "business", "entrepreneur", "startup", "ecommerce", "marketing",
            "sales", "finance", "investment", "stock", "forex", "real estate",
            "property", "amazon", "dropship", "trade", "کاروبار", "تجارت"
        ),
        Category.ENTERTAINMENT to setOf(
            "entertainment", "funny", "meme", "movie", "drama", "music", "gaming",
            "game", "pubg", "free fire", "minecraft", "roblox", "tiktok", "youtube",
            "netflix", "anime", "bollywood", "lollywood", "comedy", "fun"
        ),
        Category.SPORTS to setOf(
            "sports", "cricket", "football", "soccer", "psl", "ipl", "fifa",
            "tennis", "basketball", "hockey", "athlete", "gym", "fitness",
            "wellness", "yoga", "running", "کرکٹ", "فٹبال"
        ),
        Category.COMMUNITY to setOf(
            "community", "local", "city", "town", "village", "neighbourhood",
            "karachi", "lahore", "islamabad", "rawalpindi", "peshawar", "quetta",
            "family", "friends", "group", "social", "welfare", "charity"
        )
        // OTHERS is the fallback — no explicit set needed.
    )

    /**
     * Classify using the combined group name + chat name text.
     * Scoring: count matching keywords per category, return the best match.
     * Falls back to [Category.OTHERS] if no category scores > 0.
     */
    fun classify(groupName: String, chatName: String = "", notes: String = ""): String {
        val text = "$groupName $chatName $notes".lowercase()
        var bestCategory = Category.OTHERS
        var bestScore = 0
        for ((category, keywords) in RULES) {
            val score = keywords.count { keyword -> text.contains(keyword) }
            if (score > bestScore) {
                bestScore = score
                bestCategory = category
            }
        }
        return bestCategory
    }

    /** Classify a batch of (groupName, chatName) pairs efficiently. */
    fun classifyBatch(
        items: List<Triple<String, String, String>>
    ): List<String> = items.map { (g, c, n) -> classify(g, c, n) }
}
