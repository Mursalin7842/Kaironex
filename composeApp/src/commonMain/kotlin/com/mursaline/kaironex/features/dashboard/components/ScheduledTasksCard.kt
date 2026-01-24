package com.mursaline.kaironex.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.ui.theme.KaironexColors

/**
 * Scheduled Tasks Card for Dashboard
 * Shows all user's scheduled study blocks and tasks
 */

data class ScheduledTask(
    val id: String,
    val title: String,
    val subject: String,
    val startTime: String,
    val endTime: String,
    val duration: String,
    val status: TaskStatus,
    val priority: TaskPriority = TaskPriority.NORMAL
)

enum class TaskStatus(val label: String, val color: Long) {
    UPCOMING("Upcoming", 0xFF2196F3),
    IN_PROGRESS("In Progress", 0xFF4CAF50),
    COMPLETED("Completed", 0xFF9E9E9E),
    OVERDUE("Overdue", 0xFFF44336),
    SCHEDULED("Scheduled", 0xFF9C27B0)
}

enum class TaskPriority(val label: String, val color: Long) {
    LOW("Low", 0xFF4CAF50),
    NORMAL("Normal", 0xFF2196F3),
    HIGH("High", 0xFFFF9800),
    CRITICAL("Critical", 0xFFF44336)
}

@Composable
fun ScheduledTasksCard(
    tasks: List<ScheduledTask>,
    isMobile: Boolean,
    onTaskClick: (ScheduledTask) -> Unit = {},
    onAddTask: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = KaironexColors.CanvasWhite,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(if (isMobile) 12.dp else 16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = KaironexColors.ElectricBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Today's Schedule",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = KaironexColors.InkBlack
                    )
                }

                // Add task button
                IconButton(
                    onClick = onAddTask,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Task",
                        tint = KaironexColors.ElectricBlue
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            if (tasks.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = KaironexColors.SlateGray.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No tasks scheduled",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KaironexColors.SlateGray
                        )
                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick = onAddTask) {
                            Text("Add your first task")
                        }
                    }
                }
            } else {
                // Task list
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tasks.take(if (isMobile) 3 else 5).forEach { task ->
                        ScheduledTaskItem(
                            task = task,
                            isMobile = isMobile,
                            onClick = { onTaskClick(task) }
                        )
                    }

                    // Show more button if there are more tasks
                    if (tasks.size > (if (isMobile) 3 else 5)) {
                        TextButton(
                            onClick = { /* Navigate to full schedule */ },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("View all ${tasks.size} tasks")
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduledTaskItem(
    task: ScheduledTask,
    isMobile: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color(task.status.color).copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(if (isMobile) 50.dp else 60.dp)
            ) {
                Text(
                    task.startTime,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(task.status.color)
                )
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(12.dp)
                        .background(Color(task.status.color).copy(alpha = 0.3f))
                )
                Text(
                    task.endTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = KaironexColors.SlateGray
                )
            }

            Spacer(Modifier.width(12.dp))

            // Task details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = KaironexColors.InkBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (task.priority == TaskPriority.HIGH || task.priority == TaskPriority.CRITICAL) {
                        Spacer(Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(task.priority.color))
                        )
                    }
                }
                Text(
                    task.subject,
                    style = MaterialTheme.typography.labelSmall,
                    color = KaironexColors.SlateGray
                )
            }

            // Duration badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = KaironexColors.CloudGray
            ) {
                Text(
                    task.duration,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = KaironexColors.SlateGray
                )
            }
        }
    }
}

// Sample data provider
fun getSampleScheduledTasks(): List<ScheduledTask> = listOf(
    ScheduledTask(
        id = "1",
        title = "Data Structures Review",
        subject = "Computer Science",
        startTime = "9:00 AM",
        endTime = "10:30 AM",
        duration = "90 min",
        status = TaskStatus.COMPLETED
    ),
    ScheduledTask(
        id = "2",
        title = "Algorithm Practice",
        subject = "DSA",
        startTime = "11:00 AM",
        endTime = "12:00 PM",
        duration = "60 min",
        status = TaskStatus.IN_PROGRESS,
        priority = TaskPriority.HIGH
    ),
    ScheduledTask(
        id = "3",
        title = "System Design Study",
        subject = "Architecture",
        startTime = "2:00 PM",
        endTime = "3:30 PM",
        duration = "90 min",
        status = TaskStatus.UPCOMING
    ),
    ScheduledTask(
        id = "4",
        title = "Mock Interview Prep",
        subject = "Interview",
        startTime = "4:00 PM",
        endTime = "5:00 PM",
        duration = "60 min",
        status = TaskStatus.SCHEDULED,
        priority = TaskPriority.CRITICAL
    ),
    ScheduledTask(
        id = "5",
        title = "Database Fundamentals",
        subject = "SQL",
        startTime = "6:00 PM",
        endTime = "7:00 PM",
        duration = "60 min",
        status = TaskStatus.SCHEDULED
    )
)
