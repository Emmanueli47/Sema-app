package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.CallLog
import com.example.data.Contact
import com.example.data.Message
import com.example.data.StatusUpdate
import java.text.SimpleDateFormat
import java.util.*

// Styling tokens for Sema (Bento Grid Theme)
val WhatsappGreen = Color(0xFF6750A4)
val WhatsappTealAccent = Color(0xFF9A82DB)
val WhatsappDarkBg = Color(0xFF1D1B20)
val WhatsappLightBg = Color(0xFFFEF7FF)
val SentBubbleLight = Color(0xFFEADDFF)
val ReceivedBubbleLight = Color(0xFFF3EDF7)
val SentBubbleDark = Color(0xFF4F378B)
val ReceivedBubbleDark = Color(0xFF2B2930)

@Composable
fun SemaApp(viewModel: SemaViewModel) {
    val activeUserId by viewModel.activeUserId.collectAsState()
    val activeContactId by viewModel.activeContactId.collectAsState()
    val activeStatusToView by viewModel.activeStatusToView.collectAsState()
    val activeCallingContactId by viewModel.activeCallingContactId.collectAsState()
    
    val contacts by viewModel.contacts.collectAsState()
    val activeContact = contacts.find { it.id == activeContactId }
    val currentUser = contacts.find { it.id == activeUserId } ?: Contact("user_me", "Mimi", "+255 712 345 678", "#00A884", "Niko hapa kutumia Sema!", "Sema Sasa!", true, "Online")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Main Screen Router
        Crossfade(targetState = activeContactId, label = "ScreenTransition") { contactId ->
            if (contactId == null) {
                // Main Multi-tab Dashboard
                DashboardScreen(viewModel = viewModel, currentUser = currentUser)
            } else {
                // Open Conversational Chat Feed
                ChatScreen(
                    viewModel = viewModel,
                    activeContact = activeContact ?: Contact(contactId, "Mawasiliano", "", "#9C27B0", "", "", false, ""),
                    currentUser = currentUser
                )
            }
        }

        // 1. Status Overlay story viewer
        activeStatusToView?.let { status ->
            StatusViewer(status = status, onDismiss = { viewModel.setStatusToView(null) })
        }

        // 2. Outgoing calling popup
        activeCallingContactId?.let { callingId ->
            val callingContact = contacts.find { it.id == callingId }
            callingContact?.let { contact ->
                SimulatedCallDialog(
                    contact = contact,
                    isVideo = viewModel.isVideoCall.collectAsState().value,
                    onDismiss = { viewModel.cancelCall() }
                )
            }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: SemaViewModel, currentUser: Contact) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val activeUserId by viewModel.activeUserId.collectAsState()

    var showAddContactDialog by remember { mutableStateOf(false) }
    var showPostStatusDialog by remember { mutableStateOf(false) }
    var showProfileSelectorDialog by remember { mutableStateOf(false) }

    // Filter contacts to remove active simulation identity so we don't display ourselves in chat lists
    val chatContacts = contacts.filter { it.id != activeUserId }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFEF7FF))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sema",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        color = Color(0xFF1D1B20)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Current simulation identity display card
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFEADDFF))
                                .clickable { showProfileSelectorDialog = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("profile_switcher_badge"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF21005D))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Mimi: ${if (currentUser.id == "user_me") "Mimi" else currentUser.name}",
                                fontSize = 12.sp,
                                color = Color(0xFF21005D),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Rounded.SwapHoriz,
                                contentDescription = "Switch profile",
                                tint = Color(0xFF21005D),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { viewModel.selectTab(3) },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF3EDF7))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Settings",
                                tint = Color(0xFF49454F)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .border(width = 1.dp, color = Color(0xFFE7E0EC), shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                containerColor = Color(0xFFF3EDF7),
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = { Icon(imageVector = Icons.Rounded.Chat, contentDescription = "Chats") },
                    label = { Text("Soga", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1D192B),
                        unselectedIconColor = Color(0xFF49454F).copy(alpha = 0.7f),
                        selectedTextColor = Color(0xFF1D192B),
                        unselectedTextColor = Color(0xFF49454F).copy(alpha = 0.7f),
                        indicatorColor = Color(0xFFE8DEF8)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = { Icon(imageVector = Icons.Rounded.CircleNotifications, contentDescription = "Status") },
                    label = { Text("Status", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1D192B),
                        unselectedIconColor = Color(0xFF49454F).copy(alpha = 0.7f),
                        selectedTextColor = Color(0xFF1D192B),
                        unselectedTextColor = Color(0xFF49454F).copy(alpha = 0.7f),
                        indicatorColor = Color(0xFFE8DEF8)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = { Icon(imageVector = Icons.Rounded.Call, contentDescription = "Calls") },
                    label = { Text("Simu", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1D192B),
                        unselectedIconColor = Color(0xFF49454F).copy(alpha = 0.7f),
                        selectedTextColor = Color(0xFF1D192B),
                        unselectedTextColor = Color(0xFF49454F).copy(alpha = 0.7f),
                        indicatorColor = Color(0xFFE8DEF8)
                    )
                )
            }
        },
        floatingActionButton = {
            when (selectedTab) {
                0 -> {
                    FloatingActionButton(
                        onClick = { showAddContactDialog = true },
                        containerColor = Color(0xFFD0BCFF),
                        contentColor = Color(0xFF21005D),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("add_contact_fab")
                    ) {
                        Icon(imageVector = Icons.Rounded.PersonAdd, contentDescription = "Add contact")
                    }
                }
                1 -> {
                    FloatingActionButton(
                        onClick = { showPostStatusDialog = true },
                        containerColor = Color(0xFFD0BCFF),
                        contentColor = Color(0xFF21005D),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("add_status_fab")
                    ) {
                        Icon(imageVector = Icons.Rounded.Edit, contentDescription = "Post Status")
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(WhatsappLightBg)
        ) {
            when (selectedTab) {
                0 -> ChatsTab(viewModel = viewModel, chatContacts = chatContacts)
                1 -> StatusTab(viewModel = viewModel, contacts = contacts)
                2 -> CallsTab(viewModel = viewModel)
                3 -> SettingsTab(viewModel = viewModel, contacts = contacts, currentUser = currentUser)
            }
        }
    }

    // Modal dialogue panels
    if (showAddContactDialog) {
        AddContactDialog(
            onDismiss = { showAddContactDialog = false },
            onConfirm = { name, phone, bio, isAi ->
                viewModel.createContact(name, phone, bio, isAi)
                showAddContactDialog = false
            }
        )
    }

    if (showPostStatusDialog) {
        PostStatusDialog(
            onDismiss = { showPostStatusDialog = false },
            onConfirm = { text, color ->
                viewModel.postStatus(text, color)
                showPostStatusDialog = false
            }
        )
    }

    if (showProfileSelectorDialog) {
        ProfileSelectorDialog(
            contacts = contacts,
            activeUserId = activeUserId,
            onDismiss = { showProfileSelectorDialog = false },
            onSelect = { selectedId ->
                viewModel.switchActiveUserId(selectedId)
                showProfileSelectorDialog = false
            }
        )
    }
}

// ---------------------- TABS ----------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatsTab(viewModel: SemaViewModel, chatContacts: List<Contact>) {
    val allMessages by viewModel.allMessages.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val typingContacts by viewModel.typingContacts.collectAsState()
    val activeUserId by viewModel.activeUserId.collectAsState()

    // Filter contacts based on search query
    val filteredContacts = if (searchQuery.isBlank()) {
        chatContacts
    } else {
        chatContacts.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize().background(WhatsappLightBg)) {
        // Search Bar styled precisely from Bento theme `#F3EDF7` bg and `#49454F` text
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("search_field"),
            placeholder = { Text("Tafuta mazungumzo...", color = Color(0xFF49454F)) },
            leadingIcon = { Icon(imageVector = Icons.Rounded.Search, contentDescription = "Search", tint = Color(0xFF49454F)) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(imageVector = Icons.Rounded.Close, contentDescription = "Clear", tint = Color(0xFF49454F))
                    }
                }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF3EDF7),
                unfocusedContainerColor = Color(0xFFF3EDF7),
                focusedBorderColor = WhatsappGreen,
                unfocusedBorderColor = Color.Transparent
            )
        )

        if (filteredContacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChatBubbleOutline,
                        contentDescription = "No chats",
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Hakuna soga zozote zilizopatikana.\nBofya kitufe cha chini kulia kuanzisha soga!",
                        textAlign = TextAlign.Center,
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            // Bento calculation: prioritize AI Rafiki on the left, then first and second other contacts on the right.
            val aiContact = filteredContacts.find { it.id == "ai_rafiki" }
            val otherContacts = filteredContacts.filter { it.id != "ai_rafiki" }
            
            val leftContact = aiContact ?: filteredContacts.firstOrNull()
            val rightTopContact = if (leftContact == aiContact) otherContacts.getOrNull(0) else otherContacts.getOrNull(1)
            val rightBottomContact = if (leftContact == aiContact) otherContacts.getOrNull(1) else otherContacts.getOrNull(2)
            
            val bentoIds = listOfNotNull(leftContact?.id, rightTopContact?.id, rightBottomContact?.id)
            
            // If we are searching, we display everything in a flat list. If not, we display Bento + remaining.
            val recentContacts = if (searchQuery.isBlank()) {
                filteredContacts.filter { it.id !in bentoIds }
            } else {
                filteredContacts
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                if (searchQuery.isBlank()) {
                    // Bento Grid Section
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(175.dp)
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Left tall card (AI / Featured) in Bento style `#E8DEF8` with rounded 28dp
                            if (leftContact != null) {
                                val leftIsTyping = typingContacts.contains(leftContact.id)
                                val leftHistory = allMessages.filter {
                                    (it.senderId == activeUserId && it.receiverId == leftContact.id) ||
                                            (it.senderId == leftContact.id && it.receiverId == activeUserId)
                                }.sortedBy { it.timestamp }
                                val leftLatest = leftHistory.lastOrNull()

                                Box(
                                    modifier = Modifier
                                        .weight(1.15f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(28.dp))
                                        .background(Color(0xFFE8DEF8))
                                        .clickable { viewModel.selectContact(leftContact.id) }
                                        .padding(14.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (leftContact.id == "ai_rafiki") "🤖" else "🔥",
                                                    fontSize = 18.sp
                                                )
                                            }
                                            
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color(0xFF6750A4))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = if (leftIsTyping) "typing" else "3 Mpya",
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = leftContact.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF21005D),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (leftIsTyping) "anaandika..." else leftLatest?.content ?: leftContact.statusText,
                                                fontSize = 11.sp,
                                                color = Color(0xFF49454F),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Right Stack Column
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Right Top (Lavender Pill `#D0BCFF` with rounded 24dp)
                                if (rightTopContact != null) {
                                    val rightTopIsTyping = typingContacts.contains(rightTopContact.id)
                                    val rightTopHistory = allMessages.filter {
                                        (it.senderId == activeUserId && it.receiverId == rightTopContact.id) ||
                                                (it.senderId == rightTopContact.id && it.receiverId == activeUserId)
                                    }.sortedBy { it.timestamp }
                                    val rightTopLatest = rightTopHistory.lastOrNull()

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(Color(0xFFD0BCFF))
                                            .clickable { viewModel.selectContact(rightTopContact.id) }
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.5f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = "❤️", fontSize = 12.sp)
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = rightTopContact.name,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF21005D),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = if (rightTopIsTyping) "typing..." else rightTopLatest?.content ?: rightTopContact.statusText,
                                                    fontSize = 9.sp,
                                                    color = Color(0xFF21005D).copy(alpha = 0.7f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }

                                // Right Bottom (White/Border Pill with rounded 24dp)
                                if (rightBottomContact != null) {
                                    val rightBottomIsTyping = typingContacts.contains(rightBottomContact.id)
                                    val rightBottomHistory = allMessages.filter {
                                        (it.senderId == activeUserId && it.receiverId == rightBottomContact.id) ||
                                                (it.senderId == rightBottomContact.id && it.receiverId == activeUserId)
                                    }.sortedBy { it.timestamp }
                                    val rightBottomLatest = rightBottomHistory.lastOrNull()

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(Color.White)
                                            .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(24.dp))
                                            .clickable { viewModel.selectContact(rightBottomContact.id) }
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFF3EDF7)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = "💼", fontSize = 12.sp)
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = rightBottomContact.name,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1D1B20),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = if (rightBottomIsTyping) "typing..." else rightBottomLatest?.content ?: rightBottomContact.statusText,
                                                    fontSize = 9.sp,
                                                    color = Color(0xFF49454F),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section Label "Zilizopita"
                    item {
                        Text(
                            text = "ZILIZOPITA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF49454F),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            letterSpacing = 1.5.sp
                        )
                    }
                }

                // Recent chats list (styled as white cards with rounded 16.dp and background)
                items(recentContacts, key = { it.id }) { contact ->
                    val isTyping = typingContacts.contains(contact.id)
                    val chatHistory = allMessages.filter {
                        (it.senderId == activeUserId && it.receiverId == contact.id) ||
                                (it.senderId == contact.id && it.receiverId == activeUserId)
                    }.sortedBy { it.timestamp }
                    val latestMessage = chatHistory.lastOrNull()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                    ) {
                        ChatListItem(
                            contact = contact,
                            latestMessage = latestMessage,
                            isTyping = isTyping,
                            onClick = { viewModel.selectContact(contact.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatListItem(
    contact: Contact,
    latestMessage: Message?,
    isTyping: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile Circle Avatar
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(android.graphics.Color.parseColor(contact.avatarColorHex))),
            contentAlignment = Alignment.Center
        ) {
            if (contact.id == "ai_rafiki") {
                Icon(
                    imageVector = Icons.Rounded.SmartToy,
                    contentDescription = "AI",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Text(
                    text = contact.name.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Name and details
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = contact.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
                // Timestamp
                val timeStr = latestMessage?.let {
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it.timestamp))
                } ?: ""
                Text(
                    text = timeStr,
                    fontSize = 12.sp,
                    color = if (isTyping) WhatsappTealAccent else Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Last message message or typing status
            if (isTyping) {
                Text(
                    text = "anaandika ujumbe...",
                    color = WhatsappTealAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            } else if (latestMessage != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Check status blue ticks
                    if (latestMessage.senderId == "user_me") {
                        Icon(
                            imageVector = Icons.Rounded.DoneAll,
                            contentDescription = "Read status",
                            modifier = Modifier.size(16.dp),
                            tint = if (latestMessage.status == "READ") Color(0xFF53BDEB) else Color.Gray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = latestMessage.content,
                        color = Color.Gray,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    text = contact.statusText,
                    color = Color.Gray,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun StatusTab(viewModel: SemaViewModel, contacts: List<Contact>) {
    val statusUpdates by viewModel.statusUpdates.collectAsState()
    val activeUserId by viewModel.activeUserId.collectAsState()

    // Separate statuses: My Statuses vs Others Statuses
    val myStatus = statusUpdates.filter { it.contactId == activeUserId }.maxByOrNull { it.timestamp }
    val otherStatuses = statusUpdates.filter { it.contactId != activeUserId }
    val groupedStatuses = otherStatuses.groupBy { it.contactId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Status Zangu",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = WhatsappGreen,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // My status item card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .clickable {
                    myStatus?.let { viewModel.setStatusToView(it) }
                }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(WhatsappTealAccent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = "My profile",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Status Yangu",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = myStatus?.let {
                        "Uliweka " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it.timestamp))
                    } ?: "Gusa kuweka status yako sasa",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Sasisho za Hivi Karibuni",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = WhatsappGreen,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (groupedStatuses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Hakuna sasisho mpya za marafiki zako bado.",
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(vertical = 4.dp)
            ) {
                groupedStatuses.forEach { (_, updates) ->
                    val latestUpdate = updates.maxByOrNull { it.timestamp }!!
                    val contactObj = contacts.find { it.id == latestUpdate.contactId }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setStatusToView(latestUpdate) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Ring ring around profile
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .drawBehind {
                                    val ringColor = if (latestUpdate.viewed) Color.LightGray else WhatsappTealAccent
                                    drawCircle(
                                        color = ringColor,
                                        style = Stroke(width = 3.dp.toPx())
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(latestUpdate.avatarColorHex))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = latestUpdate.contactName.take(1).uppercase(),
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = latestUpdate.contactName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(latestUpdate.timestamp))
                            Text(
                                text = "Leo saa $timeStr",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                    HorizontalDivider(color = Color(0xFFF9F9F9), modifier = Modifier.padding(horizontal = 12.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { viewModel.deleteStatuses() },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f), contentColor = Color.Red),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(imageVector = Icons.Rounded.DeleteSweep, contentDescription = "Clear statuses")
            Spacer(modifier = Modifier.width(6.dp))
            Text("Futa Status Zote")
        }
    }
}

@Composable
fun CallsTab(viewModel: SemaViewModel) {
    val callLogs by viewModel.callLogs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Kumbukumbu za Simu",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = WhatsappGreen
            )
            if (callLogs.isNotEmpty()) {
                TextButton(onClick = { viewModel.deleteCallHistory() }) {
                    Text("Futa zote", color = Color.Red, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (callLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.CallEnd,
                        contentDescription = "No calls",
                        modifier = Modifier.size(56.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Hakuna kumbukumbu za simu bado.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
            ) {
                items(callLogs) { log ->
                    CallLogItem(log = log)
                    HorizontalDivider(color = Color(0xFFF9F9F9))
                }
            }
        }
    }
}

@Composable
fun CallLogItem(log: CallLog) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(log.avatarColorHex))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = log.contactName.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = log.contactName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (log.isIncoming) Icons.AutoMirrored.Rounded.CallReceived else Icons.Rounded.CallMade,
                        contentDescription = "Direction",
                        modifier = Modifier.size(16.dp),
                        tint = if (log.isMissed) Color.Red else Color.Green
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val dateStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(log.timestamp))
                    Text(
                        text = "Leo, $dateStr • ${log.durationText ?: "Muda haujulikani"}",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Icon(
            imageVector = Icons.Rounded.Phone,
            contentDescription = "Call",
            tint = WhatsappGreen
        )
    }
}

@Composable
fun SettingsTab(viewModel: SemaViewModel, contacts: List<Contact>, currentUser: Contact) {
    var editBio by remember { mutableStateOf(currentUser.bio) }
    var editName by remember { mutableStateOf(currentUser.name) }
    var editPhone by remember { mutableStateOf(currentUser.phoneNumber) }
    
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Active identity details
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Wasifu wa Simulation Yangu",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = WhatsappGreen
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Jina langu la sasa") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = editPhone,
                    onValueChange = { editPhone = it },
                    label = { Text("Nambari yangu ya simu") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = editBio,
                    onValueChange = { editBio = it },
                    label = { Text("Kuhusu mimi (Bio)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.createContact(editName, editPhone, editBio)
                        focusManager.clearFocus()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsappGreen),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Hifadhi Wasifu")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Guide / Instruction panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Miongozo ya Simulator ya Sema 🎮",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "1. Gusa kadi ya juu kulia (picha ya sasa) wakati wowote ili kubadili mtumaji wa ujumbe (Switch active identity).\n\n" +
                            "2. Kama unataka kutuma ujumbe kutoka kwa John kwenda kwa Mimi, badili wasifu kuwa John, fungua soga ya 'Mimi', na utume ujumbe.\n\n" +
                            "3. Ukibadili kuwa Mimi, utaona ujumbe huo umeingia vizuri kutoka kwa John!\n\n" +
                            "4. Ukituma ujumbe kama Mimi kwenda kwa SEMA AI, utajibiwa na Akili Mnemba (Gemini AI) kwa Kiswahili!",
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    lineHeight = 18.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Majaribio ya API na Gemini Key 🔑",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                val isKeyConfigured = com.example.BuildConfig.GEMINI_API_KEY.isNotEmpty() && com.example.BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isKeyConfigured) Color.Green else Color.Red)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isKeyConfigured) "Gemini API Key Ipo Sawa (Tayari)" else "Hakuna Key ya Gemini (Utafanya soga kwa offline)",
                        fontSize = 13.sp,
                        color = if (isKeyConfigured) Color.Green else Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Unaweza kuweka key yako kwenye 'Secrets panel' upande wa kushoto wa AI Studio ili kufanya mazungumzo ya kiakili na roboti na contacts wengine yajibu kiotomatiki kwa upeo mkubwa sana!",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// ---------------------- CHAT DETAILED SCREEN ----------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: SemaViewModel,
    activeContact: Contact,
    currentUser: Contact
) {
    val activeMessages by viewModel.activeMessages.collectAsState()
    val typingContacts by viewModel.typingContacts.collectAsState()
    val showDetails by viewModel.showContactDetails.collectAsState()

    var messageText by remember { mutableStateOf("") }
    val isTyping = typingContacts.contains(activeContact.id)

    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto scroll to bottom when a new message arrives
    LaunchedEffect(activeMessages.size) {
        if (activeMessages.isNotEmpty()) {
            listState.animateScrollToItem(activeMessages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .clickable { viewModel.toggleContactDetails(true) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(activeContact.avatarColorHex))),
                            contentAlignment = Alignment.Center
                        ) {
                            if (activeContact.id == "ai_rafiki") {
                                Icon(
                                    imageVector = Icons.Rounded.SmartToy,
                                    contentDescription = "AI",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(
                                    text = activeContact.name.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = activeContact.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isTyping) "anaandika..." else if (activeContact.isOnline) "Yuko hewani (Online)" else activeContact.lastSeenText,
                                fontSize = 11.sp,
                                color = if (isTyping) Color.Green else Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.selectContact(null) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.startCall(activeContact.id, false) }) {
                        Icon(imageVector = Icons.Rounded.Phone, contentDescription = "Voice Call", tint = Color.White)
                    }
                    IconButton(onClick = { viewModel.startCall(activeContact.id, true) }) {
                        Icon(imageVector = Icons.Rounded.Videocam, contentDescription = "Video Call", tint = Color.White)
                    }
                    IconButton(onClick = { viewModel.toggleContactDetails(true) }) {
                        Icon(imageVector = Icons.Rounded.MoreVert, contentDescription = "More", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WhatsappGreen)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // Drawing dynamic WhatsApp chat theme canvas background pattern
                .drawBehind {
                    drawRect(color = Color(0xFFE5DDD5))
                    // Subtle dynamic dots and grid elements as drawing wallpaper
                    val dotSpacing = 30.dp.toPx()
                    for (x in 0..size.width.toInt() step dotSpacing.toInt()) {
                        for (y in 0..size.height.toInt() step dotSpacing.toInt()) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.15f),
                                radius = 1.5.dp.toPx(),
                                center = Offset(x.toFloat(), y.toFloat())
                            )
                        }
                    }
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Messages List Feed
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp)
                ) {
                    item {
                        // Encryption disclaimer card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4).copy(alpha = 0.9f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "🔒 Ujumbe wako umelindwa mwisho-hadi-mwisho (End-to-End Encrypted). Hakuna mtu mwingine yeyote anayeweza kusoma.",
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    color = Color(0xFF5D4037),
                                    modifier = Modifier.padding(8.dp),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    items(activeMessages, key = { it.id }) { msg ->
                        val isMe = msg.senderId == currentUser.id
                        MessageBubble(msg = msg, isMe = isMe)
                    }
                }

                // Input box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SentimentSatisfiedAlt,
                            contentDescription = "Emojis",
                            tint = Color.Gray,
                            modifier = Modifier.clickable { }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        BasicTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 12.dp)
                                .testTag("message_input_field"),
                            textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Send
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (messageText.isNotBlank()) {
                                        viewModel.sendMessage(messageText)
                                        messageText = ""
                                    }
                                }
                            ),
                            decorationBox = { innerTextField ->
                                if (messageText.isEmpty()) {
                                    Text("Andika ujumbe...", color = Color.Gray, fontSize = 15.sp)
                                }
                                innerTextField()
                            }
                        )

                        Icon(
                            imageVector = Icons.Rounded.Attachment,
                            contentDescription = "Attach",
                            tint = Color.Gray,
                            modifier = Modifier.clickable { }
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Floating circular green action button
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(WhatsappTealAccent)
                            .clickable {
                                if (messageText.isNotBlank()) {
                                    viewModel.sendMessage(messageText)
                                    messageText = ""
                                    keyboardController?.hide()
                                }
                            }
                            .testTag("send_message_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // Modal detailed specifications of contact
    if (showDetails) {
        ContactDetailsDialog(
            contact = activeContact,
            onDismiss = { viewModel.toggleContactDetails(false) },
            onClearChat = {
                viewModel.clearActiveChat()
                viewModel.toggleContactDetails(false)
            }
        )
    }
}

@Composable
fun MessageBubble(msg: Message, isMe: Boolean) {
    val alignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isMe) {
        if (isSystemInDarkTheme()) SentBubbleDark else SentBubbleLight
    } else {
        if (isSystemInDarkTheme()) ReceivedBubbleDark else ReceivedBubbleLight
    }
    val textColor = if (isSystemInDarkTheme()) Color.White else Color.Black

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isMe) 12.dp else 0.dp,
                        bottomEnd = if (isMe) 0.dp else 12.dp
                    )
                )
                .background(bubbleColor)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = msg.content,
                color = textColor,
                fontSize = 15.sp,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp))
                Text(
                    text = timeStr,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
                if (isMe) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Rounded.DoneAll,
                        contentDescription = "SentStatus",
                        modifier = Modifier.size(14.dp),
                        tint = if (msg.status == "READ") Color(0xFF53BDEB) else Color.Gray
                    )
                }
            }
        }
    }
}

// ---------------------- DIALOGS ----------------------

@Composable
fun AddContactDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var isAi by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Weka Mawasiliano Mapya", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Jina kamili") },
                    modifier = Modifier.fillMaxWidth().testTag("add_contact_name_input"),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Namba ya simu") },
                    modifier = Modifier.fillMaxWidth().testTag("add_contact_phone_input"),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Wasifu mfupi (Bio)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = isAi, onCheckedChange = { isAi = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Wezesha Msaidizi wa AI (Roleplay Mode)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, phone, bio, isAi) },
                colors = ButtonDefaults.buttonColors(containerColor = WhatsappGreen)
            ) {
                Text("Hifadhi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Ghairi", color = Color.Red)
            }
        }
    )
}

@Composable
fun PostStatusDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val colors = listOf("#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#00BCD4", "#4CAF50", "#FF9800", "#FF5722")
    var selectedColor by remember { mutableStateOf(colors.random()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unda Status ya Kisasa", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(android.graphics.Color.parseColor(selectedColor)))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = { if (it.length <= 120) text = it },
                        modifier = Modifier.fillMaxWidth().testTag("status_text_input"),
                        textStyle = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        ),
                        decorationBox = { innerTextField ->
                            if (text.isEmpty()) {
                                Text(
                                    "Andika nini kiko akilini mwako leo... ✍️",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Chagua rangi ya mandharinyuma:", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(color)))
                                .border(
                                    width = if (selectedColor == color) 2.dp else 0.dp,
                                    color = Color.Black,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onConfirm(text, selectedColor) },
                colors = ButtonDefaults.buttonColors(containerColor = WhatsappGreen)
            ) {
                Text("Chapisha")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Ghairi", color = Color.Red)
            }
        }
    )
}

@Composable
fun ProfileSelectorDialog(
    contacts: List<Contact>,
    activeUserId: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Chagua Jina Unalotaka Kuiga (Switch Identity)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = WhatsappGreen,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(contacts) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (contact.id == activeUserId) WhatsappGreen.copy(alpha = 0.1f) else Color.Transparent)
                                .clickable { onSelect(contact.id) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(contact.avatarColorHex))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = contact.name.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (contact.id == "user_me") "Mimi (Wewe)" else contact.name,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = contact.phoneNumber,
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                            if (contact.id == activeUserId) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Active",
                                    tint = WhatsappGreen
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Funga", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun ContactDetailsDialog(
    contact: Contact,
    onDismiss: () -> Unit,
    onClearChat: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(contact.avatarColorHex))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.name.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = contact.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.Black
                )
                Text(
                    text = contact.phoneNumber,
                    color = Color.Gray,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Kuhusu (Bio)", fontWeight = FontWeight.Bold, color = WhatsappGreen, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = contact.bio.ifEmpty { "Hakuna wasifu" }, color = Color.DarkGray, fontSize = 14.sp)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Hali (Status Text)", fontWeight = FontWeight.Bold, color = WhatsappGreen, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = contact.statusText, color = Color.DarkGray, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onClearChat,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Rounded.Delete, contentDescription = "Clear Chat")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Futa Ujumbe Wote (Clear Chat)")
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Funga", color = Color.Gray)
                }
            }
        }
    }
}

// ---------------------- STATUS VIEWER STORY SCREEN ----------------------

@Composable
fun StatusViewer(
    status: StatusUpdate,
    onDismiss: () -> Unit
) {
    var progress by remember { mutableStateOf(0f) }

    // Automatic progress loader
    LaunchedEffect(key1 = status.id) {
        val duration = 5000 // 5 seconds
        val interval = 50
        val steps = duration / interval
        for (i in 1..steps) {
            delay(interval.toLong())
            progress = i.toFloat() / steps
        }
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(android.graphics.Color.parseColor(status.backgroundColorHex)))
                .clickable { onDismiss() }
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Segmented timer progress indicators
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Sender Details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = status.contactName.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = status.contactName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(status.timestamp))
                        Text(
                            text = "Leo saa $timeStr",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }

                // Main Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = status.textContent,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 34.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Text(
                    text = "Gusa skrini mahali popote ili kufunga",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ---------------------- SIMULATED CALL SCREEN ----------------------

@Composable
fun SimulatedCallDialog(
    contact: Contact,
    isVideo: Boolean,
    onDismiss: () -> Unit
) {
    // Simulated ringing sound / delay state
    var callTimerText by remember { mutableStateOf("Inapiga simu (Ringing)...") }

    LaunchedEffect(Unit) {
        delay(2000)
        callTimerText = "Simu imekatika (Mteja hapatikani)"
        delay(1500)
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WhatsappDarkBg)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxHeight()
            ) {
                // Header details
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 48.dp)
                ) {
                    Icon(
                        imageVector = if (isVideo) Icons.Rounded.Videocam else Icons.Rounded.Phone,
                        contentDescription = "Call type",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = contact.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = callTimerText,
                        color = WhatsappTealAccent,
                        fontSize = 14.sp
                    )
                }

                // Profile Image Circle
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(contact.avatarColorHex))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.name.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 54.sp
                    )
                }

                // Cancel call round button
                Box(
                    modifier = Modifier
                        .padding(bottom = 48.dp)
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
