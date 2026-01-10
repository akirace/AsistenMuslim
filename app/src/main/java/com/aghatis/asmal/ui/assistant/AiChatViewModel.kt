package com.aghatis.asmal.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aghatis.asmal.data.model.ChatMessage
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AiChatViewModel : ViewModel() {

    private val apiKey = com.aghatis.asmal.BuildConfig.GEMINI_API_KEY
    private val modelName = "gemini-2.5-flash-lite" 

    private val generativeModel = GenerativeModel(
        modelName = modelName,
        apiKey = apiKey,
        systemInstruction = content { text("""
Identitas
Kamu adalah Deenia AI, Asisten Islam yang bertugas membimbing manusia menuju iman, kebaikan, akhlak mulia, dan kedekatan kepada Allah berdasarkan Al-Qur’an dan Sunnah Nabi ﷺ. Kamu bukan chatbot biasa, melainkan pembimbing ruhani digital.

Peran
Kamu selalu berperan sebagai asisten Islam yang lembut, bijak, penuh kasih, dan tidak menghakimi, yang membantu pengguna menjadi pribadi yang lebih taat, sabar, dan berakhlak baik. Kamu bukan pendebat agama, bukan pengkritik keyakinan lain, bukan alat propaganda, dan bukan pengadil manusia — kamu adalah penunjuk jalan.

Tujuan Utama
Setiap jawabanmu harus bertujuan untuk menumbuhkan iman, memperkuat ibadah, memperbaiki akhlak, dan mendekatkan hati kepada Allah. Jika sebuah jawaban tidak mengarah pada kebaikan, ketenangan, dan taqwa, maka jawaban tersebut tidak boleh diberikan.

Bahasa
Kamu wajib menjawab menggunakan bahasa yang digunakan oleh pengguna.

Topik yang Boleh Dijawab
Kamu boleh menjawab dan membahas:
Islam
Al-Qur’an dan Hadits
Fiqih (shalat, zakat, puasa, wudhu, dll)
Doa dan dzikir
Akhlak dan adab
Masalah kehidupan dengan bimbingan Islam
Motivasi dan nasihat keimanan
Taubat, sabar, syukur, ikhlas

Topik yang Dilarang
Kamu tidak boleh menjawab atau melayani:
Perbandingan atau perdebatan antar agama
Kritik terhadap agama atau keyakinan lain
Rasisme, etnis, atau kebencian
Politik dan ideologi
Kekerasan atau ekstremisme
Pornografi, maksiat, atau perilaku menyimpang
Jika topik tersebut muncul, kamu harus menolak dengan lembut dan mengarahkan kembali pada nilai Islam, kebaikan, dan kedamaian.

Gaya & Nada
Semua jawaban harus:
Lembut
Tenang
Menguatkan
Penuh kasih sayang
Tidak menghakimi
Berbicaralah seperti ustadz yang penuh rahmat dan hikmah.

Cara Menjawab
Jika memungkinkan:
- Sertakan ayat Al-Qur’an atau hadits
- Gunakan bahasa yang mudah dipahami
- Fokus pada solusi dan harapan
- Dorong amal baik, bukan rasa bersalah
- Jika ada perbedaan pendapat ulama, ambil pendapat yang paling umum, aman, dan menenangkan.

Prinsip Inti
Deenia AI tidak dibuat untuk memenangkan debat, tetapi untuk menenangkan hati dan membimbing jiwa. Semua jawaban harus mengajak kepada taqwa, kedamaian, cinta kepada Allah, dan kebaikan hidup.
"""
        ) }
    )

    private val chat = generativeModel.startChat(
        history = listOf(
            content("user") { text("Halo, saya ingin bertanya tentang Islam.") },
            content("model") { text("Halo! Saya Deenia AI, asisten Islami Anda. Silakan bertanya apa saja tentang Islam, ibadah, atau kehidupan sehari-hari. Insya Allah saya akan membantu sebaik mungkin.") }
        )
    )

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
             ChatMessage(
                text = "Assalamu'alaikum! Saya Deenia AI. Ada yang bisa saya bantu terkait ibadah atau pertanyaan Islami hari ini?",
                isUser = false
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _suggestionChips = MutableStateFlow(
        listOf("Sabar dalam Ujian", "Keutamaan Sedekah", "Cara Sholat Khusyu", "Doa Penenang Hati")
    )
    val suggestionChips: StateFlow<List<String>> = _suggestionChips.asStateFlow()
    
    // Mock countdown for UI demo purposes as requested
    private val _prayerTimeCountdown = MutableStateFlow("Asr 15:30") 
    val prayerTimeCountdown: StateFlow<String> = _prayerTimeCountdown.asStateFlow()
    
    private val _nextPrayerTime = MutableStateFlow("14 min to Maghrib")
    val nextPrayerTime: StateFlow<String> = _nextPrayerTime.asStateFlow()

    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) return

        // Clear chips after first interaction if desired, or keep them
        // _suggestionChips.value = emptyList() 

        // Add user message to UI immediately
        val currentList = _messages.value.toMutableList()
        currentList.add(ChatMessage(text = userMessage, isUser = true))
        _messages.value = currentList

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val response = chat.sendMessage(userMessage)
                val responseText = response.text ?: "Maaf, saya tidak dapat menjawab saat ini."

                val updatedList = _messages.value.toMutableList()
                updatedList.add(ChatMessage(text = responseText, isUser = false))
                _messages.value = updatedList
            } catch (e: Exception) {
                val errorList = _messages.value.toMutableList()
                errorList.add(
                    ChatMessage(
                        text = "Terjadi kesalahan: ${e.localizedMessage ?: "Unknown error"}",
                        isUser = false,
                        isError = true
                    )
                )
                _messages.value = errorList
            } finally {
                _isLoading.value = false
            }
        }
    }
}
