package com.google.mediapipe.examples.llminference

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Add
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.StateFlow

@Composable
internal fun ChatRoute(
    onClose: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val chatViewModel: ChatViewModel = viewModel(factory = ChatViewModel.getFactory(context))

    // Reset InferenceModel when entering ChatScreen
    LaunchedEffect(Unit) {
        val inferenceModel = InferenceModel.getInstance(context)
        chatViewModel.resetInferenceModel(inferenceModel)
    }

    val uiState by chatViewModel.uiState.collectAsStateWithLifecycle()
    val textInputEnabled by chatViewModel.isTextInputEnabled.collectAsStateWithLifecycle()
    val isContextLoaded by chatViewModel.isContextLoaded.collectAsStateWithLifecycle()

    ChatScreen(
        context,
        uiState,
        textInputEnabled,
        isContextLoaded = isContextLoaded,
        remainingTokens = chatViewModel.tokensRemaining,
        resetTokenCount = {
            chatViewModel.recomputeSizeInTokens("")
        },
        onSendMessage = { message ->
            chatViewModel.sendMessage(message)
        },
        onClearContext = {
            chatViewModel.clearContext()
        },
        onChangedMessage = { message ->
            chatViewModel.recomputeSizeInTokens(message)
        },
        onClose = onClose
    )
}

@Composable
fun ChatScreen(
    context: Context,
    uiState: UiState,
    textInputEnabled: Boolean,
    isContextLoaded: Boolean,
    remainingTokens: StateFlow<Int>,
    resetTokenCount: () -> Unit,
    onSendMessage: (String) -> Unit,
    onClearContext: () -> Unit,
    onChangedMessage: (String) -> Unit,
    onClose: () -> Unit
) {
    var userMessage by rememberSaveable { mutableStateOf("") }
    val tokens by remainingTokens.collectAsState(initial = -1)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { it.readText() }
            if (content != null) {
                onSendMessage("Context from file: $content")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Bottom
    ) {
        // Top bar with close button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = InferenceModel.model.toString().replace("_", " "),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (tokens >= 0) "$tokens tokens left" else "Calculating...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            IconButton(
                onClick = {
                    InferenceModel.getInstance(context).close()
                    uiState.clearMessages()
                    resetTokenCount()
                    onClose()
                },
                enabled = textInputEnabled
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        if (tokens == 0) {
            // Show warning label that context is full
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.LightGray)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.context_full_message),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.Red,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            reverseLayout = true
        ) {
            items(uiState.messages) { chat ->
                ChatItem(chat)
            }
        }

        if (isContextLoaded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "📄 Document Loaded as Context",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onClearContext,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear Context",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column { }

            IconButton(
                onClick = {
                    launcher.launch("text/plain")
                },
                enabled = textInputEnabled
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Attach File"
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            TextField(
                value = userMessage,
                onValueChange = { userMessage = it
                    // Only recompute on first word or when we get a new word
                    if (!userMessage.contains(" ") || userMessage.trim() != userMessage)  {
                        onChangedMessage(userMessage)
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
                label = {
                    Text(stringResource(R.string.chat_label))
                },
                modifier = Modifier
                    .weight(0.85f)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            onChangedMessage(userMessage)
                        }
                    },
                enabled = textInputEnabled
            )

            IconButton(
                onClick = {
                    if (userMessage.isNotBlank()) {
                        onSendMessage(userMessage)
                        userMessage = ""
                    }
                },
                modifier = Modifier
                    .padding(start = 16.dp)
                    .align(Alignment.CenterVertically)
                    .fillMaxWidth()
                    .weight(0.15f),
                enabled = textInputEnabled && tokens > 0
            ) {
                Icon(
                    Icons.AutoMirrored.Default.Send,
                    contentDescription = stringResource(R.string.action_send),
                    modifier = Modifier
                )
            }
        }
    }
}

@Composable
fun ChatItem(
    chatMessage: ChatMessage
) {
    val isUser = chatMessage.isFromUser
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val bubbleShape = if (isUser) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    val horizontalAlignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        horizontalAlignment = horizontalAlignment,
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = if (isUser) "You" else if (chatMessage.isThinking) "DeepSeek (Thinking...)" else "AI Assistant",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 2.dp, start = if (isUser) 0.dp else 4.dp, end = if (isUser) 4.dp else 0.dp)
        )
        Row {
            BoxWithConstraints {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = bubbleColor),
                    shape = bubbleShape,
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.widthIn(0.dp, maxWidth * 0.85f)
                ) {
                    if (chatMessage.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(12.dp).size(24.dp),
                            strokeWidth = 2.dp,
                            color = contentColor
                        )
                    } else {
                        Text(
                            text = chatMessage.message,
                            color = contentColor,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}
