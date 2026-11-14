package com.blek.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

private const val CHAT_ENDPOINT = "https://YOUR_BACKEND_URL/chat"

private data class ChatMessage(val text: String, val fromUser: Boolean)

private enum class AppTab { CHAT, IMAGE }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BlekAiApp() }
    }
}

@Composable
private fun BlekAiApp() {
    var selectedTab by remember { mutableStateOf(AppTab.CHAT) }
    val bg = Color(0xFF171717)
    val panel = Color(0xFF242424)
    val textColor = Color(0xFFF5F5F5)
    val muted = Color(0xFFAAAAAA)
    val accent = Color(0xFFB8B8B8)

    MaterialTheme(colorScheme = darkColorScheme(
        primary = accent,
        onPrimary = bg,
        background = bg,
        surface = panel,
        onBackground = textColor,
        onSurface = textColor
    )) {
        Scaffold(
            containerColor = bg,
            topBar = {
                Row(
                    modifier = Modifier.fillMaxWidth().height(64.dp)
                        .background(bg).padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("BLEK.AI", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(if (selectedTab == AppTab.CHAT) "Asisten AI pribadi" else "Image Studio", color = muted, fontSize = 11.sp)
                    }
                    Icon(Icons.Default.Image, contentDescription = "Image Studio", tint = textColor)
                }
            },
            bottomBar = {
                NavigationBar(containerColor = panel) {
                    NavigationBarItem(
                        selected = selectedTab == AppTab.CHAT,
                        onClick = { selectedTab = AppTab.CHAT },
                        icon = { Icon(Icons.Default.Send, contentDescription = null) },
                        label = { Text("Chat") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == AppTab.IMAGE,
                        onClick = { selectedTab = AppTab.IMAGE },
                        icon = { Icon(Icons.Default.Image, contentDescription = null) },
                        label = { Text("Image Studio") }
                    )
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).background(bg)) {
                when (selectedTab) {
                    AppTab.CHAT -> ChatScreen(textColor, muted, panel)
                    AppTab.IMAGE -> ImageStudioScreen(textColor, muted, panel, accent)
                }
            }
        }
    }
}

@Composable
private fun ChatScreen(textColor: Color, muted: Color, panel: Color) {
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var draft by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    suspend fun sendToBlekAi(prompt: String): String = withContext(Dispatchers.IO) {
        require(!CHAT_ENDPOINT.contains("YOUR_BACKEND_URL")) {
            "Backend BLEK.AI belum dipasang. Server AI perlu dibuat terlebih dahulu."
        }
        val connection = (URL(CHAT_ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 60000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }
        try {
            val body = JSONObject().put("message", prompt).toString()
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.let {
                BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { reader -> reader.readText() }
            }.orEmpty()
            if (status !in 200..299) error("Server HTTP $status: $response")
            JSONObject(response).optString("reply").takeIf { it.isNotBlank() }
                ?: error("Respons server tidak memiliki field 'reply'.")
        } finally {
            connection.disconnect()
        }
    }

    LaunchedEffect(messages.size, isLoading) {
        val last = messages.lastOrNull()
        if (last?.fromUser == true && isLoading) {
            try {
                messages.add(ChatMessage(sendToBlekAi(last.text), false))
            } catch (e: Exception) {
                errorText = e.message ?: "Tidak dapat menghubungi server."
            } finally {
                isLoading = false
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (messages.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("BLEK.AI", color = textColor, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Apa yang ingin kamu bahas hari ini?", color = muted)
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(messages) { msg ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (msg.fromUser) Arrangement.End else Arrangement.Start) {
                        Surface(color = if (msg.fromUser) Color(0xFF343434) else panel, shape = RoundedCornerShape(16.dp)) {
                            Text(msg.text, Modifier.padding(13.dp), color = textColor)
                        }
                    }
                }
                if (isLoading) item { CircularProgressIndicator(Modifier.size(20.dp), color = textColor) }
                if (errorText != null) item { Text(errorText!!, color = Color(0xFFFF9B9B), fontSize = 12.sp) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft, onValueChange = { draft = it }, modifier = Modifier.weight(1f),
                placeholder = { Text("Kirim pesan…", color = muted) }, maxLines = 4,
                shape = RoundedCornerShape(22.dp), enabled = !isLoading
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = {
                    val prompt = draft.trim()
                    if (prompt.isNotEmpty() && !isLoading) {
                        messages.add(ChatMessage(prompt, true))
                        draft = ""
                        errorText = null
                        isLoading = true
                    }
                },
                enabled = draft.isNotBlank() && !isLoading
            ) { Icon(Icons.Default.Send, contentDescription = "Kirim") }
        }
    }
}

@Composable
private fun ImageStudioScreen(textColor: Color, muted: Color, panel: Color, accent: Color) {
    var style by remember { mutableStateOf("Anime") }
    var prompt by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Mesin gambar belum dihubungkan.") }

    Column(
        modifier = Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("AI Image Studio", color = textColor, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text("Buat atau edit gambar dengan dua gaya pilihan.", color = muted, fontSize = 13.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("Anime", "Realistis").forEach { option ->
                FilterChip(
                    selected = style == option,
                    onClick = { style = option },
                    label = { Text(option) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accent,
                        selectedLabelColor = Color(0xFF171717),
                        labelColor = textColor
                    )
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth().height(190.dp),
            color = panel,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Image, contentDescription = null, tint = muted, modifier = Modifier.size(42.dp))
                Spacer(Modifier.height(8.dp))
                Text("Area hasil gambar", color = textColor, fontWeight = FontWeight.SemiBold)
                Text("Hasil generasi akan tampil di sini.", color = muted, fontSize = 12.sp)
            }
        }

        OutlinedButton(
            onClick = { status = "Pemilih gambar akan diaktifkan saat fitur unggah dibuat." },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Pilih gambar untuk diedit")
        }

        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            label = { Text("Deskripsi gambar atau edit") },
            placeholder = { Text("Contoh: karakter anime di taman saat senja…") },
            minLines = 3,
            maxLines = 5,
            shape = RoundedCornerShape(16.dp)
        )

        Button(
            onClick = {
                status = if (prompt.isBlank()) "Tulis deskripsi gambar terlebih dahulu."
                else "Antarmuka siap. Backend generator gambar belum dihubungkan."
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Buat / Edit Gambar")
        }
        Text(status, color = muted, fontSize = 12.sp)
    }
}
