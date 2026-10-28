package com.nexus.phone

data class PhoneContact(
    val name: String,
    val number: String
)

data class RecentCall(
    val name: String,
    val number: String,
    val date: Long,
    val type: Int,
    val timeLabel: String
)
