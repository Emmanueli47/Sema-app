package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SemaViewModel(
    application: Application,
    private val repository: SemaRepository
) : AndroidViewModel(application) {

    // Active identity being simulated (normally "user_me", can switch to any contact id)
    private val _activeUserId = MutableStateFlow("user_me")
    val activeUserId: StateFlow<String> = _activeUserId.asStateFlow()

    // Currently opened chat contact ID. If null, show chat list.
    private val _activeContactId = MutableStateFlow<String?>(null)
    val activeContactId: StateFlow<String?> = _activeContactId.asStateFlow()

    // Set of contacts who are currently simulated as "Typing..."
    private val _typingContacts = MutableStateFlow<Set<String>>(emptySet())
    val typingContacts: StateFlow<Set<String>> = _typingContacts.asStateFlow()

    // Control visibility of contact details view
    private val _showContactDetails = MutableStateFlow(false)
    val showContactDetails: StateFlow<Boolean> = _showContactDetails.asStateFlow()

    // Active status story currently being viewed
    private val _activeStatusToView = MutableStateFlow<StatusUpdate?>(null)
    val activeStatusToView: StateFlow<StatusUpdate?> = _activeStatusToView.asStateFlow()

    // Active calling contact ID (when simulated call dialog is active)
    private val _activeCallingContactId = MutableStateFlow<String?>(null)
    val activeCallingContactId: StateFlow<String?> = _activeCallingContactId.asStateFlow()
    val isVideoCall = MutableStateFlow(false)

    // Search query for filtering chats/messages
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Selected Bottom Navigation Tab (0 = Chats, 1 = Status, 2 = Calls, 3 = Settings)
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Flow states from repository
    val contacts: StateFlow<List<Contact>> = repository.allContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val statusUpdates: StateFlow<List<StatusUpdate>> = repository.allStatusUpdates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val callLogs: StateFlow<List<CallLog>> = repository.allCallLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All messages combined flow (for rendering latest messages)
    val allMessages: StateFlow<List<Message>> = repository.getAllMessagesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dynamically retrieve active messages between activeUserId and activeContactId
    val activeMessages: StateFlow<List<Message>> = combine(_activeUserId, _activeContactId) { userId, contactId ->
        userId to contactId
    }.flatMapLatest { (userId, contactId) ->
        if (contactId != null) {
            repository.getMessagesBetween(userId, contactId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Run database initialization on startup
        viewModelScope.launch {
            repository.initializeDefaultDataIfNeeded()
        }
    }

    // ACTIONS

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun selectContact(contactId: String?) {
        _activeContactId.value = contactId
        _showContactDetails.value = false
        if (contactId != null) {
            // Mark messages from this contact to the active user as read
            viewModelScope.launch {
                repository.getMessagesBetween(contactId, _activeUserId.value)
            }
        }
    }

    fun switchActiveUserId(newUserId: String) {
        viewModelScope.launch {
            _activeUserId.value = newUserId
            // Auto close any active chat if it is with oneself
            if (_activeContactId.value == newUserId) {
                _activeContactId.value = null
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleContactDetails(show: Boolean) {
        _showContactDetails.value = show
    }

    fun setStatusToView(status: StatusUpdate?) {
        _activeStatusToView.value = status
        if (status != null) {
            viewModelScope.launch {
                repository.markStatusAsViewed(status.id)
            }
        }
    }

    fun startCall(contactId: String, video: Boolean) {
        _activeCallingContactId.value = contactId
        isVideoCall.value = video
        viewModelScope.launch {
            val contact = repository.getContactById(contactId)
            if (contact != null) {
                val callLog = CallLog(
                    contactId = contact.id,
                    contactName = contact.name,
                    avatarColorHex = contact.avatarColorHex,
                    isIncoming = false,
                    isMissed = false,
                    durationText = "Imekatwa"
                )
                repository.insertCallLog(callLog)
            }
        }
    }

    fun cancelCall() {
        _activeCallingContactId.value = null
    }

    fun clearActiveChat() {
        val currentContact = _activeContactId.value ?: return
        val currentUser = _activeUserId.value
        viewModelScope.launch {
            repository.clearChat(currentUser, currentContact)
        }
    }

    fun deleteCallHistory() {
        viewModelScope.launch {
            repository.clearAllCallLogs()
        }
    }

    fun deleteStatuses() {
        viewModelScope.launch {
            repository.clearAllStatuses()
        }
    }

    fun createContact(name: String, phone: String, bio: String, isAi: Boolean = false) {
        viewModelScope.launch {
            val shortId = name.lowercase().replace(" ", "_")
            val hexColors = listOf("#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4", "#009688", "#4CAF50", "#FFEB3B", "#FF9800", "#FF5722")
            val randomColor = hexColors.random()
            val newContact = Contact(
                id = shortId,
                name = name,
                phoneNumber = phone,
                avatarColorHex = randomColor,
                bio = bio,
                statusText = "Ninatumia Sema! 📱",
                isOnline = true,
                lastSeenText = "Online",
                isAiEnabled = isAi
            )
            repository.insertContact(newContact)
        }
    }

    fun postStatus(textContent: String, colorHex: String) {
        viewModelScope.launch {
            val activeUser = repository.getContactById(_activeUserId.value) ?: return@launch
            val status = StatusUpdate(
                contactId = activeUser.id,
                contactName = if (activeUser.id == "user_me") "Mimi" else activeUser.name,
                avatarColorHex = activeUser.avatarColorHex,
                textContent = textContent,
                backgroundColorHex = colorHex,
                timestamp = System.currentTimeMillis(),
                viewed = false
            )
            repository.insertStatusUpdate(status)
        }
    }

    fun sendMessage(text: String, type: String = "TEXT", mediaUrl: String? = null) {
        if (text.isBlank()) return
        
        val sender = _activeUserId.value
        val receiver = _activeContactId.value ?: return

        viewModelScope.launch {
            // 1. Send the message
            val sentMsg = repository.sendMessage(sender, receiver, text, type, mediaUrl)

            // 2. If sender is "user_me" and the receiver is NOT "user_me", trigger automated typing and reply!
            if (sender == "user_me" && receiver != "user_me") {
                triggerSimulatedAutoResponse(receiver, text)
            }
        }
    }

    private fun triggerSimulatedAutoResponse(contactId: String, userMessage: String) {
        viewModelScope.launch {
            // Wait slightly before contact starts "Typing..."
            delay(1000)
            
            // Set Typing state
            _typingContacts.value = _typingContacts.value + contactId
            
            // Wait for typing simulation (feels very organic!)
            delay(2000)
            
            // Generate response using repository (Gemini or offline fallback)
            val replyText = repository.generateAutomatedResponse(contactId, userMessage)
            
            // Remove Typing state
            _typingContacts.value = _typingContacts.value - contactId
            
            // Insert received message
            val responseMsg = Message(
                senderId = contactId,
                receiverId = "user_me",
                content = replyText,
                type = "TEXT",
                status = "READ" // Open chat, read immediately
            )
            repository.insertMessageDirectly(responseMsg)
        }
    }
}

class SemaViewModelFactory(
    private val application: Application,
    private val repository: SemaRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SemaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SemaViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
