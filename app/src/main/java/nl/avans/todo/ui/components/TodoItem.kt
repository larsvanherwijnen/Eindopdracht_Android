package nl.avans.todo.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nl.avans.todo.data.model.Todo
import nl.avans.todo.ui.theme.Warning
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun TodoItem(
    todo: Todo,
    onToggleCompleted: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, HH:mm") }
    val now = remember { LocalDateTime.now() }
    val scrollState = rememberScrollState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(todo.id) {
                detectTapGestures(
                    onLongPress = { onLongClick() },
                    onTap = { onClick() }
                )
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = todo.completed,
                onCheckedChange = { onToggleCompleted() }
            )

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = todo.name,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (todo.completed) TextDecoration.LineThrough else TextDecoration.None
                )

                if (todo.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = todo.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (todo.completed) TextDecoration.LineThrough else TextDecoration.None
                    )
                }

                todo.dueDate?.let { dueDate ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Due date",
                            modifier = Modifier.size(16.dp),
                            tint = when {
                                todo.completed -> MaterialTheme.colorScheme.onSurfaceVariant
                                dueDate < now -> MaterialTheme.colorScheme.error
                                dueDate.minusHours(24) < now -> Warning
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = dueDate.format(dateFormatter),
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                todo.completed -> MaterialTheme.colorScheme.onSurfaceVariant
                                dueDate < now -> MaterialTheme.colorScheme.error
                                dueDate.minusHours(24) < now -> Warning
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )

                        if (todo.notificationEnabled) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications enabled",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (todo.addedToCalendar) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = "Added to calendar",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = { onDelete() },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete todo"
                )
            }
        }
    }
}