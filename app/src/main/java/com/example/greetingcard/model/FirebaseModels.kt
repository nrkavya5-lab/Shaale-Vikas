package com.example.greetingcard.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Need(
    val id: String = "",
    val school: String = "",
    val title: String = "",
    val desc: String = "",
    val target: Int = 0,
    val collected: Int = 0,
    val urgent: Boolean = false,
    val contact: String = "",
    val imageUrl: String? = null
)

@IgnoreExtraProperties
data class ImpactItem(
    val id: String = "",
    val title: String = "",
    val school: String = "",
    val desc: String = "",
    val beforeUrl: String? = null,
    val afterUrl: String? = null
)

@IgnoreExtraProperties
data class Donor(
    val id: String = "",
    val name: String = "",
    val supported: String = "",
    val amountStr: String = "",
    val rank: Int = 0
)

@IgnoreExtraProperties
data class UserProfile(
    val uid: String = "",
    val fullName: String = "",
    val role: String = "Alumni", // Admin or Alumni
    val uniqueField: String = "", // UDISE or Passing Year
    val email: String = ""
)
