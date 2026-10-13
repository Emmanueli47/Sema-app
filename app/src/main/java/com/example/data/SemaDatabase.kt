package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SemaDao {
    // Contacts
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getContactById(id: String): Contact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<Contact>)

    @Query("DELETE FROM contacts")
    suspend fun deleteAllContacts()

    // Messages
    @Query("SELECT * FROM messages WHERE (senderId = :userA AND receiverId = :userB) OR (senderId = :userB AND receiverId = :userA) ORDER BY timestamp ASC")
    fun getMessagesBetweenUsers(userA: String, userB: String): Flow<List<Message>>

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessagesFlow(): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message): Long

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: Long, status: String)

    @Query("UPDATE messages SET status = 'READ' WHERE senderId = :senderId AND receiverId = :receiverId")
    suspend fun markMessagesAsRead(senderId: String, receiverId: String)

    @Query("DELETE FROM messages WHERE (senderId = :userA AND receiverId = :userB) OR (senderId = :userB AND receiverId = :userA)")
    suspend fun clearChatBetweenUsers(userA: String, userB: String)

    // Status Updates
    @Query("SELECT * FROM status_updates ORDER BY timestamp DESC")
    fun getAllStatusUpdates(): Flow<List<StatusUpdate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatusUpdate(status: StatusUpdate)

    @Query("UPDATE status_updates SET viewed = 1 WHERE id = :statusId")
    suspend fun markStatusAsViewed(statusId: Long)

    @Query("DELETE FROM status_updates")
    suspend fun clearAllStatuses()

    // Call Logs
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getAllCallLogs(): Flow<List<CallLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(callLog: CallLog)

    @Query("DELETE FROM call_logs")
    suspend fun clearAllCallLogs()
}

@Database(
    entities = [Contact::class, Message::class, StatusUpdate::class, CallLog::class],
    version = 1,
    exportSchema = false
)
abstract class SemaDatabase : RoomDatabase() {
    abstract fun semaDao(): SemaDao
}
