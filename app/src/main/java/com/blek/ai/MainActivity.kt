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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class ChatMessage(val text: String, val fromUser: Boolean)

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
    var showMenu by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = lightColorScheme(
        background = Color.White, surface = Color.White,
        primary = Color(0xFF202123), onPrimary = Color.White
    )) {
        Scaffold(
            containerColor = Color.White,
            topBar = {
                Row(
                    Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                    Text("BLEK.AI", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { messages.clear() }) {
                        Icon(Icons.Default.Add, contentDescription = "Percakapan baru")
                    }
                }
            },
            bottomBar = {
                Surface(shadowElevation = 4.dp, color = Color.White) {
                    Row(
                        Modifier.fillMaxWidth().navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        OutlinedTextField(
                            value = draft, onValueChange = { draft = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Tanyakan sesuatu...") },
                            shape = RoundedCornerShape(26.dp), maxLines = 5
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = {
                                val text = draft.trim()
                                if (text.isNotEmpty()) {
                                    messages.add(ChatMessage(text, true))
                                    draft = ""
                                    messages.add(ChatMessage(
                                        "Antarmuka BLEK.AI sudah siap. Koneksi ke model AI akan ditambahkan pada tahap berikutnya.",
                                        false
                                    ))
                                }
                            },
                            modifier = Modifier.size(52.dp), shape = CircleShape
                        ) { Icon(Icons.Default.Send, contentDescription = "Kirim") }
                    }
                }
            }
        ) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).background(Color.White)
            ) {
                if (showMenu) {
                    Surface(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        color = Color(0xFFF7F7F8), shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Riwayat percakapan akan tersedia pada pembaruan berikutnya.",
                            Modifier.padding(16.dp), color = Color(0xFF55565A), fontSize = 13.sp
                        )
                    }
                }
                if (messages.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("BLEK.AI", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text("Apa yang ingin kamu bahas hari ini?",
                                color = Color(0xFF77787C), fontSize = 15.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        items(messages) { message ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = if (message.fromUser)
                                    Arrangement.End else Arrangement.Start
                            ) {
                                Surface(
                                    color = if (message.fromUser) Color(0xFFF4F4F4) else Color.White,
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.widthIn(max = 310.dp)
                                ) {
                                    Text(message.text, Modifier.padding(14.dp),
                                        color = Color(0xFF202123), fontSize = 15.sp, lineHeight = 22.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
