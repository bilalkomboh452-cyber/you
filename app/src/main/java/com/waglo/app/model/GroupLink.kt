
package com.waglo.app.model

/**
 * Represents a single WhatsApp public group invite link.
 * Fields are ordered from most- to least-frequently accessed
 * to align with CPU cache lines on low-end devices.
 */
data class GroupLink(
    var id: Long = 0,
    var serialNumber: Int = 0,
    var inviteLink: String = "",
    var groupName: String = "",
    var chatName: String = "",
    var category: String = Category.OTHERS,
    var source: String = Source.ACCESSIBILITY,
    var language: String = Language.UNKNOWN,
    var confidenceScore: Float = 0f,
    var status: String = Status.ACTIVE,
    var isFavorite: Boolean = false,
    var notes: String = "",
    var messageDate: String = "",
    var discoveryTime: Long = System.currentTimeMillis(),
    var addedAt: Long = System.currentTimeMillis(),
    // Runtime-only — not persisted
    var isSelected: Boolean = false
) {
    object Category {
        const val EDUCATION    = "EDUCATION"
        const val JOBS         = "JOBS"
        const val TECHNOLOGY   = "TECHNOLOGY"
        const val AI           = "AI"
        const val CRYPTO       = "CRYPTO"
        const val NEWS         = "NEWS"
        const val BUSINESS     = "BUSINESS"
        const val ENTERTAINMENT= "ENTERTAINMENT"
        const val SPORTS       = "SPORTS"
        const val COMMUNITY    = "COMMUNITY"
        const val OTHERS       = "OTHERS"

        val ALL = listOf(
            EDUCATION, JOBS, TECHNOLOGY, AI, CRYPTO,
            NEWS, BUSINESS, ENTERTAINMENT, SPORTS, COMMUNITY, OTHERS
        )
    }

    object Status {
        const val ACTIVE  = "ACTIVE"
        const val EXPIRED = "EXPIRED"
        const val INVALID = "INVALID"
        const val PENDING = "PENDING"
    }

    object Source {
        const val ACCESSIBILITY = "ACCESSIBILITY"
        const val MANUAL        = "MANUAL"
        const val DISCOVERY     = "DISCOVERY"
        const val IMPORT        = "IMPORT"
    }

    object Language {
        const val ENGLISH = "en"
        const val URDU    = "ur"
        const val ARABIC  = "ar"
        const val HINDI   = "hi"
        const val UNKNOWN = "unknown"
    }
}
