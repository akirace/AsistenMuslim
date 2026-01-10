package com.aghatis.asmal.ui.assistant

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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aghatis.asmal.data.model.ChatMessage
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text

@Composable
fun AiChatScreen(
    onNavigateBack: () -> Unit,
    viewModel: AiChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val chips by viewModel.suggestionChips.collectAsState()
    val prayerTimeName by viewModel.prayerTimeCountdown.collectAsState()
    val prayerTimeLeft by viewModel.nextPrayerTime.collectAsState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size + 1) // +1 for spacer/chips
        }
    }

    // Custom Dark Theme Colors for this screen (as requested)
    val darkGreenBg = Color(0xFF0E1A14) // Dark Greenish Black
    val bubbleGreen = Color(0xFF1B3B2B) // Dark Green Bubble
    val userGreen = Color(0xFF25D366) // Bright Green (WhatsApp-like/Islamic modern)
    val textLight = Color(0xFFE0ECE6)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = darkGreenBg,
        topBar = {
            ChatHeader(
                onNavClick = { /* Open Drawer */ },
                prayerTimeName = prayerTimeName,
                prayerTimeLeft = prayerTimeLeft
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Chat List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Date Separator
                item {
                    DateSeparator()
                }

                items(messages) { message ->
                    ChatMessageBubble(
                        message = message,
                        aiBubbleColor = bubbleGreen,
                        userBubbleColor = userGreen,
                        textColor = textLight
                    )
                }

                if (isLoading) {
                    item { TypingIndicator() }
                }
                
                // Suggestion Chips (Only show if history is short or always at bottom? User said "first chat give example with chips")
                // We'll place them at the bottom of the list for now
                if (messages.size <= 2 && !isLoading) {
                   item {
                       SuggestionChipsLayout(chips) { chipText ->
                           viewModel.sendMessage(chipText)
                       }
                   }
                }
            }

            // Input Area
            ChatInputArea(
                onSend = { text ->
                    viewModel.sendMessage(text)
                    keyboardController?.hide()
                },
                enabled = !isLoading
            )
        }
    }
}

@Composable
fun ChatHeader(
    onNavClick: () -> Unit,
    prayerTimeName: String,
    prayerTimeLeft: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Nav & AI Info
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Deenia AI",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.CheckCircle, // Verified Icon
                        contentDescription = "Verified",
                        tint = Color(0xFF25D366),
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = "ONLINE",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF25D366)
                )
            }
        }

        // Right: Prayer Timer
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = "Time",
                    tint = Color(0xFF25D366),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = prayerTimeName,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF25D366)
                )
            }
            Text(
                text = prayerTimeLeft,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun DateSeparator() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color.White.copy(alpha = 0.1f),
            shape = RoundedCornerShape(50),
        ) {
            Text(
                text = "TODAY, 14 RAMADAN", // Hardcoded for demo/design match
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    aiBubbleColor: Color,
    userBubbleColor: Color,
    textColor: Color
) {
    val isUser = message.isUser
    val alignment = if (isUser) Alignment.End else Alignment.Start
    
    // User text is usually dark on bright green, AI text is light on dark green
    val contentColor = if (isUser) Color.Black else Color(0xFFE0ECE6)
    val bubbleColor = if (isUser) userBubbleColor else aiBubbleColor

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.Top // Align top for avatars
        ) {
            if (!isUser) {
                // AI Avatar (Sparkle)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2E4F3E)), // Slightly lighter green for avatar bg
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = "AI",
                        tint = Color(0xFF25D366),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Surface(
                color = bubbleColor,
                shape = if (isUser) RoundedCornerShape(16.dp, 2.dp, 16.dp, 16.dp) 
                        else RoundedCornerShape(2.dp, 16.dp, 16.dp, 16.dp),
                shadowElevation = 0.dp,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.text,
                        color = contentColor,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp)
                    )
                }
            }
            
            if (isUser) {
                 Spacer(modifier = Modifier.width(8.dp))
                 // User Avatar
                 Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0ECE6)),
                    contentAlignment = Alignment.Center
                ) {
                     // Placeholder User Icon
                     Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color.Gray))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuggestionChipsLayout(chips: List<String>, onChipClick: (String) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (text in chips) {
            SuggestionChip(
                onClick = { onChipClick(text) },
                label = { 
                    Text(
                        text = text, 
                        color = Color.White.copy(alpha = 0.9f)
                    ) 
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = Color.White.copy(alpha = 0.1f),
                    labelColor = Color.White
                ),
                shape = RoundedCornerShape(50)
            )
        }
    }
}

@Composable
fun ChatInputArea(
    onSend: (String) -> Unit,
    enabled: Boolean
) {
    var text by remember { mutableStateOf("") }
    val darkInputBg = Color(0xFF1B3B2B)

    Surface(
        color = Color(0xFF0E1A14), // Match screen bg
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(darkInputBg, RoundedCornerShape(26.dp))
                .padding(horizontal = 16.dp, vertical = 4.dp), // Adjust padding
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Left (Library/Books) - Optional from image UI
            Icon(
                imageVector = Icons.Default.Menu, // Placeholder for library
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))

            Box(modifier = Modifier.weight(1f)) {
                 if (text.isEmpty()) {
                    Text(
                        text = "Ask a question...",
                        color = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (text.isNotBlank()) {
                                onSend(text)
                                text = ""
                            }
                        }
                    )
                )
            }

            TextButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onSend(text)
                        text = ""
                    }
                },
                enabled = enabled && text.isNotBlank()
            ) {
                Text(
                    text = "Send",
                    color = if (text.isNotBlank()) Color(0xFF25D366) else Color.White.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 40.dp, top = 8.dp)
    ) {
        Text(
            text = "Deenia sedang mengetik...",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}
