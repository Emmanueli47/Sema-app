package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey val id: String,
    val name: String,
    val phoneNumber: String,
    val avatarColorHex: String,
    val bio: String,
    val statusText: String,
    val isOnline: Boolean,
    val lastSeenText: String,
    val isAiEnabled: Boolean = false // If true, Gemini can respond automatically as this contact
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "SENT", // SENT, DELIVERED, READ
    val type: String = "TEXT", // TEXT, IMAGE, AUDIO
    val mediaUrl: String? = null
)

@Entity(tableName = "status_updates")
data class StatusUpdate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: String,
    val contactName: String,
    val avatarColorHex: String,
    val textContent: String,
    val backgroundColorHex: String,
    val timestamp: Long = System.currentTimeMillis(),
    val viewed: Boolean = false
)

@Entity(tableName = "call_logs")
data class CallLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: String,
    val contactName: String,
    val avatarColorHex: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isIncoming: Boolean,
    val isMissed: Boolean,
    val durationText: String? = null
)
