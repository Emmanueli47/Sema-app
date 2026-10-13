package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SemaRepository(private val semaDao: SemaDao) {

    val allContacts: Flow<List<Contact>> = semaDao.getAllContacts()
    val allStatusUpdates: Flow<List<StatusUpdate>> = semaDao.getAllStatusUpdates()
    val allCallLogs: Flow<List<CallLog>> = semaDao.getAllCallLogs()

    fun getMessagesBetween(userA: String, userB: String): Flow<List<Message>> {
        return semaDao.getMessagesBetweenUsers(userA, userB)
    }

    fun getAllMessagesFlow(): Flow<List<Message>> {
        return semaDao.getAllMessagesFlow()
    }

    suspend fun getContactById(id: String): Contact? = withContext(Dispatchers.IO) {
        semaDao.getContactById(id)
    }

    suspend fun insertContact(contact: Contact) = withContext(Dispatchers.IO) {
        semaDao.insertContact(contact)
    }

    suspend fun insertStatusUpdate(status: StatusUpdate) = withContext(Dispatchers.IO) {
        semaDao.insertStatusUpdate(status)
    }

    suspend fun markStatusAsViewed(statusId: Long) = withContext(Dispatchers.IO) {
        semaDao.markStatusAsViewed(statusId)
    }

    suspend fun insertCallLog(callLog: CallLog) = withContext(Dispatchers.IO) {
        semaDao.insertCallLog(callLog)
    }

    suspend fun clearChat(userA: String, userB: String) = withContext(Dispatchers.IO) {
        semaDao.clearChatBetweenUsers(userA, userB)
    }

    suspend fun clearAllCallLogs() = withContext(Dispatchers.IO) {
        semaDao.clearAllCallLogs()
    }

    suspend fun clearAllStatuses() = withContext(Dispatchers.IO) {
        semaDao.clearAllStatuses()
    }

    suspend fun insertMessageDirectly(message: Message): Long = withContext(Dispatchers.IO) {
        semaDao.insertMessage(message)
    }

    /**
     * Sends a message and triggers simulated typing and response if needed.
     */
    suspend fun sendMessage(
        senderId: String,
        receiverId: String,
        content: String,
        type: String = "TEXT",
        mediaUrl: String? = null
    ): Message = withContext(Dispatchers.IO) {
        val message = Message(
            senderId = senderId,
            receiverId = receiverId,
            content = content,
            type = type,
            mediaUrl = mediaUrl,
            status = "READ" // Messages sent by active view are read by active view
        )
        val id = semaDao.insertMessage(message)
        message.copy(id = id)
    }

    /**
     * Generates a smart automated Swahili response for a given contact.
     */
    suspend fun generateAutomatedResponse(contactId: String, userMessage: String): String = withContext(Dispatchers.IO) {
        val contact = semaDao.getContactById(contactId) ?: return@withContext "Samahani, sikuelewa."
        
        // If AI is enabled and Gemini API Key is configured, attempt to use Gemini
        if (contact.isAiEnabled || contactId == "ai_rafiki") {
            val systemInstruction = when (contactId) {
                "mama" -> "Wewe ni Mama wa mtumiaji, unampenda sana mwanao na unamjali kila sekunde. Jibu ujumbe huu wa mwanao kwa Kiswahili kitamu chenye upendo, busara na huruma ya Mama wa Kitanzania. Jibu liwe fupi, lisizidi sentensi mbili au tatu, na utumie emoji za mioyo na tabasamu."
                "baba" -> "Wewe ni Baba wa mtumiaji, mwanaume mzito wa busara wa Kitanzania mwenye maneno mafupi lakini yenye baraka na miongozo thabiti. Jibu ujumbe wa mwanao kwa Kiswahili kifupi cha heshima. Usiongee sana, neno moja au mawili yanatosha (Mfano: 'Sawa mwanangu, nimekupata. Kazi njema', 'Ubarikiwe sana'). Jibu lisizidi sentensi mbili."
                "john" -> "Wewe ni John, mshkaji wa karibu (rafiki wa kiunzi) wa mtumiaji. Unapenda mziki wa Bongo Flava na utani wa soka. Jibu ujumbe huu wa mshkaji wako kwa kutumia Kiswahili cha mtaani/vijana cha sasa cha Dar es Salaam (kama vile 'oya', 'mwanangu', 'kichizi', 'fresh', 'tisha sana', 'kamata', 'babe'). Jibu liwe fupi na la ucheshi."
                "aisha" -> "Wewe ni Aisha, mfanyakazi mwenza (software engineer) wa mtumiaji kazini. Wewe ni mtaalamu na mwenye adabu sana. Jibu ujumbe huu kwa adabu, weledi na ufasaha kwa Kiswahili rasmi cha ofisini au biashara. Jibu liwe fupi na la kusaidia."
                else -> "Wewe ni Sema AI, msaidizi mwerevu wa karibu (Chatbot) kwenye programu ya Sema (WhatsApp clone). Ongea kwa Kiswahili kizuri, cha ufasaha na cha kirafiki cha Tanzania. Jibu maswali ya mtumiaji kwa ufupi na kwa ufasaha (lisizidi sentensi tatu). Tumia emoji zinazofaa mazungumzo."
            }

            val reply = RetrofitClient.generateSwahiliReply(userMessage, systemInstruction)
            if (!reply.startsWith("ERROR:")) {
                return@withContext reply
            }
        }

        // Offline / Fallback Reply System based on persona
        return@withContext generateOfflineReply(contactId, userMessage)
    }

    private fun generateOfflineReply(contactId: String, userMessage: String): String {
        val cleaned = userMessage.lowercase().trim()
        return when (contactId) {
            "mama" -> {
                when {
                    cleaned.contains("mambo") || cleaned.contains("habari") || cleaned.contains("habari yako") -> 
                        "Habari za asubuhi mwanangu? Natumai umeamka salama na una afya njema. Mimi niko salama kabisa hapa nyumbani, nakupenda sana! ❤️"
                    cleaned.contains("kazi") || cleaned.contains("masomo") || cleaned.contains("shule") -> 
                        "Sawa mwanangu, fanya kazi kwa bidii na kwa heshima zote. Kumbuka kupumzika na unywe maji mengi. Mungu akubariki sana!"
                    cleaned.contains("pesa") || cleaned.contains("hela") || cleaned.contains("tuma") -> 
                        "Sawa mwanangu, nitajitahidi kukutumia kidogo kwenye simu jioni hii uendelee na mahitaji yako. Uwe makini na matumizi, sawa?"
                    cleaned.contains("naumwa") || cleaned.contains("sijisikii vizuri") || cleaned.contains("ugonjwa") -> 
                        "Ooh mwanangu pole sana! Umeanza kunywa dawa? Nitakuombea kwa Mungu upone haraka sana. Tafadhali lala upumzike."
                    else -> "Ubarikiwe sana mwanangu. Niambie kama unahitaji chochote kutoka nyumbani hapa. Nakuombea baraka kila siku! 🥰"
                }
            }
            "baba" -> {
                when {
                    cleaned.contains("mambo") || cleaned.contains("habari") -> "Habari yako. Kila kitu kiko salama."
                    cleaned.contains("kazi") || cleaned.contains("masomo") -> "Vizuri sana. Endelea kupambana, maisha yanahitaji nidhamu na juhudi kubwa."
                    cleaned.contains("pesa") || cleaned.contains("hela") || cleaned.contains("tuma") -> "Nimekusikia. Nitajitahidi nikutumie jioni nikirudi nyumbani."
                    cleaned.contains("sawa") || cleaned.contains("ok") || cleaned.contains("haya") -> "Sawa."
                    else -> "Sawa mwanangu. Jilinde vizuri na uwe na nidhamu kila mahali."
                }
            }
            "john" -> {
                when {
                    cleaned.contains("mambo") || cleaned.contains("habari") || cleaned.contains("oya") || cleaned.contains("mambo vipi") -> 
                        "Oyaaa mwanangu! Fresh sana. Inakuwaje pande hizo? Nambie mambo mapya, niko hapa nawaza mchongo wa leo jioni! 😎"
                    cleaned.contains("pesa") || cleaned.contains("hela") || cleaned.contains("tuma") -> 
                        "Dah mwanangu mfuko umekauka mbaya leo! Tuvumiliane tu mpaka mambo yakikaa sawa au kama una vocha unitupie hapa mzee, nitakurudishia kesho. 😂"
                    cleaned.contains("kiwanja") || cleaned.contains("jioni") || cleaned.contains("michezo") || cleaned.contains("game") -> 
                        "Inabidi leo tucheki lile gemu la Arsenal pale klabuni, au vipi mwanangu? Nitakushtua jioni ikifika saa kumi na mbili."
                    else -> "Tisha sana mwanangu! Salama kabisa. Baadaye tunashtuana mambo yakikaa fresh. Stay cool! 🔥"
                }
            }
            "aisha" -> {
                when {
                    cleaned.contains("mambo") || cleaned.contains("habari") || cleaned.contains("habari za kazi") -> 
                        "Habari yako. Mimi niko salama, namalizia kufanya mapitio ya ripoti ya mradi wa sasa. Natumai upande wako kila kitu kiko sawa."
                    cleaned.contains("kazi") || cleaned.contains("faili") || cleaned.contains("ripoti") -> 
                        "Asante kwa taarifa. Nimepokea na ninaanza kufanyia kazi mara moja. Nitakujulisha nikimaliza kuunganisha ripoti zote."
                    cleaned.contains("mkutano") || cleaned.contains("kikao") -> 
                        "Ndio, kikao chetu kimepangwa kuanza saa kumi jioni kwenye chumba cha mikutano au kwa njia ya Zoom/Meet. Tafadhali jiandae."
                    else -> "Asante sana kwa ujumbe wako. Tafadhali wasiliana nami kama kuna jambo lingine lolote linalohitaji msaada wangu kazini."
                }
            }
            else -> { // Sema AI
                when {
                    cleaned.contains("mambo") || cleaned.contains("habari") || cleaned.contains("mambo vipi") || cleaned.contains("mambo?") -> 
                        "Habari yako! Mimi ni Sema AI, msaidizi wako mwerevu wa karibu hapa Sema. Nimefurahi sana kuongea na wewe leo! Unaweza kuniuliza habari yoyote ya mtaani au kunipa kazi nikusaidie kuandika. Niambie, unataka nini leo? 😊"
                    cleaned.contains("jina") -> 
                        "Jina langu ni Sema AI! Nimeundwa hapa kwenye programu ya Sema ili kuwa msaidizi wako mwerevu wa soga (AI Chatbot) katika Kiswahili fasaha."
                    cleaned.contains("whatsapp") || cleaned.contains("sema") -> 
                        "Programu hii inaitwa **Sema**, ni WhatsApp clone nzuri sana ya Kiswahili iliyotengenezwa kwa ajili ya kutuma ujumbe, kuona stasuti na kufanya majaribio ya mawasiliano thabiti ya ana kwa ana na marafiki, na hata akili mnemba (AI)!"
                    cleaned.contains("asante") || cleaned.contains("shukrani") -> 
                        "Karibu sana! Ni furaha yangu kukusaidia wakati wowote. Una lingine lolote unalotaka nikusaidie nalo?"
                    else -> "Ujumbe wako ni mzuri sana! Kumbuka mimi ni Sema AI, msaidizi wako mwerevu. Ninaweza kukusaidia kujibu maswali yako yote kwa Kiswahili safi kabisa. Tafadhali niambie una swali lingine au unataka nikuandikie nini sasa hivi? 🚀"
                }
            }
        }
    }

    /**
     * Seeds the local Room database with beautiful Kiswahili starter datasets on first boot.
     */
    suspend fun initializeDefaultDataIfNeeded() = withContext(Dispatchers.IO) {
        val existing = semaDao.getAllContacts().first()
        if (existing.isNotEmpty()) return@withContext

        // 1. Setup default contacts
        val defaultContacts = listOf(
            Contact("user_me", "Mimi (Mimi)", "+255 712 345 678", "#00A884", "Niko hapa kutumia Sema!", "Mawasiliano rahisi na yenye furaha 📱", true, "Online"),
            Contact("mama", "Mama ❤️", "+255 754 111 222", "#E91E63", "Familia ni nguzo ya kila kitu.", "Mungu akubariki sana mwanangu 🙏", true, "Online"),
            Contact("baba", "Baba", "+255 765 333 444", "#3F51B5", "Kazi na Sala ndio nguzo.", "Kupambana mpaka mwisho.", false, "ilikuwa leo saa 08:15"),
            Contact("john", "John (Mshkaji) 🤙", "+255 688 555 666", "#00BCD4", "Maisha ni mafupi, furahia kila sekunde!", "Inakuwaje mwanangu? 😎", true, "Online"),
            Contact("aisha", "Aisha (Kazini) 📁", "+255 711 777 888", "#9C27B0", "Software Engineer | IT Consultant", "Kwenye mkutano wa timu 💻", false, "ilikuwa jana saa 18:30"),
            Contact("ai_rafiki", "Sema AI 🤖", "Sema AI Assistant", "#6200EE", "Msaidizi wako mwerevu wa karibu hapa Sema. Naongea Kiswahili fasaha!", "Tayari kukujibu sasa hivi...", true, "Online", isAiEnabled = true)
        )
        semaDao.insertContacts(defaultContacts)

        // 2. Setup default conversation message history
        val now = System.currentTimeMillis()
        val defaultMessages = listOf(
            // Mama History
            Message(senderId = "mama", receiverId = "user_me", content = "Habari za asubuhi mwanangu, umeshakula chakula cha asubuhi?", timestamp = now - 3600000 * 4, status = "READ"),
            Message(senderId = "user_me", receiverId = "mama", content = "Habari za asubuhi Mama, ndio nimekula tayari! Habari za huko nyumbani?", timestamp = now - 3600000 * 3, status = "READ"),
            Message(senderId = "mama", receiverId = "user_me", content = "Salama kabisa mwanangu. Mungu akubariki sana na akulinde kwenye kazi zako za leo. ❤️", timestamp = now - 3600000 * 2, status = "READ"),
            
            // Baba History
            Message(senderId = "user_me", receiverId = "baba", content = "Habari za mchana Baba, natumai upo salama.", timestamp = now - 3600000 * 10, status = "READ"),
            Message(senderId = "baba", receiverId = "user_me", content = "Salama. Kazi njema mwanangu.", timestamp = now - 3600000 * 9, status = "READ"),

            // John History
            Message(senderId = "john", receiverId = "user_me", content = "Oya mwanangu! Leo jioni kuna lile gemu kali la Real Madrid na Barcelona, vipi tunaenda kuangalia pamoja?", timestamp = now - 3600000 * 5, status = "READ"),
            Message(senderId = "user_me", receiverId = "john", content = "Dah! Lazima tu mzee wangu, hilo gemu halikwepeki kabisa! Nitakupigia tukimaliza kupiga chata hapa.", timestamp = now - 3600000 * 4, status = "READ"),

            // Aisha History
            Message(senderId = "aisha", receiverId = "user_me", content = "Habari, naomba utume faili la mahesabu ya mradi uliyofanyia kazi jana jioni ili niliunganishe kwenye ripoti.", timestamp = now - 3600000 * 24, status = "READ"),
            Message(senderId = "user_me", receiverId = "aisha", content = "Habari Aisha, sawa tayari nilishalikamilisha. Nakutumia sasa hivi.", timestamp = now - 3600000 * 23, status = "READ"),

            // AI Rafiki History
            Message(senderId = "ai_rafiki", receiverId = "user_me", content = "Habari gani rafiki yangu wa karibu! Karibu kwenye Sema app. Mimi ni Sema AI, unaweza kuniuliza chochote na nitakujibu kwa Kiswahili safi kabisa. Tuanze sasa? 😊🚀", timestamp = now - 1800000, status = "READ")
        )
        for (m in defaultMessages) {
            semaDao.insertMessage(m)
        }

        // 3. Setup default statuses
        val defaultStatuses = listOf(
            StatusUpdate(contactId = "mama", contactName = "Mama ❤️", avatarColorHex = "#E91E63", textContent = "Mungu ni mwema mwanangu. Nakutakia siku yenye amani na baraka nyingi! 🥰🙏", backgroundColorHex = "#E91E63", timestamp = now - 3600000 * 2, viewed = false),
            StatusUpdate(contactId = "john", contactName = "John (Mshkaji) 🤙", avatarColorHex = "#00BCD4", textContent = "Muziki mzuri ndio dawa pekee ya akili... Leo tunaanza hivi! 🔥🎧", backgroundColorHex = "#3F51B5", timestamp = now - 3600000 * 5, viewed = false),
            StatusUpdate(contactId = "aisha", contactName = "Aisha (Kazini) 📁", avatarColorHex = "#9C27B0", textContent = "Hatimaye mfumo wangu wote umerun vizuri na kufanikiwa kufanya kazi bila error yoyote! Code is beautiful. 🚀💻📊", backgroundColorHex = "#4CAF50", timestamp = now - 3600000 * 8, viewed = false)
        )
        for (s in defaultStatuses) {
            semaDao.insertStatusUpdate(s)
        }

        // 4. Setup default Call Logs
        val defaultCallLogs = listOf(
            CallLog(contactId = "mama", contactName = "Mama ❤️", avatarColorHex = "#E91E63", timestamp = now - 3600000 * 2, isIncoming = true, isMissed = true),
            CallLog(contactId = "baba", contactName = "Baba", avatarColorHex = "#3F51B5", timestamp = now - 3600000 * 15, isIncoming = false, isMissed = false, durationText = "Dakika 3"),
            CallLog(contactId = "john", contactName = "John (Mshkaji) 🤙", avatarColorHex = "#00BCD4", timestamp = now - 3600000 * 1, isIncoming = true, isMissed = false, durationText = "Dakika 12")
        )
        for (c in defaultCallLogs) {
            semaDao.insertCallLog(c)
        }
    }
}
