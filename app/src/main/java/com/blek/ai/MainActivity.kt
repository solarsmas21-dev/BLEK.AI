package com.blek.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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

/*
 * Ganti URL ini dengan alamat backend BLEK AI milikmu.
 * Jangan menaruh API key penyedia AI di aplikasi Android.
 * Backend menerima JSON {"message":"..."} dan mengembalikan {"reply":"..."}.
 */
private const val CHAT_ENDPOINT = "https://YOUR_BACKEND_URL/chat"

private data class ChatMessage(
    val text: String,
    val fromUser: Boolean
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BlekAiApp() }
    }
}

@Composable
private fun BlekAiApp() {
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var draft by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    val bg = Color(0xFF171717)
    val panel = Color(0xFF242424)
    val textColor = Color(0xFFF5F5F5)
    val muted = Color(0xFFAAAAAA)
    val accent = Color(0xFFB8B8B8)

    suspend fun sendToBlekAi(prompt: String): String = withContext(Dispatchers.IO) {
        require(!CHAT_ENDPOINT.contains("YOUR_BACKEND_URL")) {
            "Backend BLEK AI belum dipasang. Kita perlu membuat server AI dan memasukkan alamatnya."
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
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use {
                it.write(body)
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream
                         else connection.errorStream
            val response = stream?.let {
                BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            }.orEmpty()

            if (status !in 200..299) {
                throw IllegalStateException("Server mengembalikan HTTP $status: $response")
            }

            val json = JSONObject(response)
            json.optString("reply").takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Respons server tidak memiliki field 'reply'.")
        } finally {
            connection.disconnect()
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = accent,
            onPrimary = Color(0xFF171717),
            background = bg,
            surface = panel,
            onBackground = textColor,
            onSurface = textColor
        )
    ) {
        Scaffold(
            containerColor = bg,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp)
                        .background(bg)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "BLEK.AI",
                            color = textColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Asisten AI pribadi", color = muted, fontSize = 11.sp)
                    }
                    IconButton(
                        onClick = {
                            messages.clear()
                            errorText = null
                            draft = ""
                        }
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Percakapan baru",
                            tint = textColor
                        )
                    }
                }
            },
            bottomBar = {
                Surface(color = bg, shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Kirim pesan ke BLEK.AI…", color = muted) },
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 5,
                            enabled = !isLoading,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    // Tombol kirim di samping tetap bisa digunakan.
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedBorderColor = accent,
                                unfocusedBorderColor = Color(0xFF555555),
                                cursorColor = textColor
                            )
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
                            enabled = draft.isNotBlank() && !isLoading,
                            modifier = Modifier.size(52.dp),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Kirim pesan")
                        }
                    }
                }
            }
        ) { padding ->
            LaunchedEffect(messages.size, isLoading) {
                val count = messages.size + if (isLoading) 1 else 0
                if (count > 0) listState.animateScrollToItem(count - 1)
            }

            // Kirim pesan terakhir dari pengguna ke backend.
            LaunchedEffect(messages.size) {
                val last = messages.lastOrNull()
                if (last?.fromUser == true && isLoading) {
                    try {
                        val answer = sendToBlekAi(last.text)
                        messages.add(ChatMessage(answer, false))
                    } catch (e: Exception) {
                        errorText = e.message ?: "Tidak dapat menghubungi server BLEK AI."
                    } finally {
                        isLoading = false
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(bg)
            ) {
                if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "BLEK.AI",
                                color = textColor,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Apa yang ingin kamu bahas hari ini?",
                                color = muted,
                                fontSize = 15.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "AI pribadi yang sedang dikembangkan.",
                                color = Color(0xFF777777),
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(messages) { message ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (message.fromUser)
                                    Arrangement.End else Arrangement.Start
                            ) {
                                Surface(
                                    color = if (message.fromUser) Color(0xFF343434) else panel,
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.widthIn(max = 320.dp)
                                ) {
                                    Text(
                                        text = message.text,
                                        modifier = Modifier.padding(14.dp),
                                        color = textColor,
                                        fontSize = 15.sp,
                                        lineHeight = 22.sp
                                    )
                                }
                            }
                        }

                        if (isLoading) {
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = accent
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text("BLEK.AI sedang berpikir…", color = muted, fontSize = 13.sp)
                                }
                            }
                        }

                        if (errorText != null) {
                            item {
                                Text(
                                    text = errorText!!,
                                    color = Color(0xFFFF9B9B),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
