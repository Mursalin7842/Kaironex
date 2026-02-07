package com.mursaline.kaironex.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.ui.unit.sp
import com.mursaline.kaironex.ui.theme.KaironexColors

/**
 * Scheduled Tasks Card for Dashboard
 * Shows all user's scheduled study blocks and tasks
 */

data class ScheduledTask(
    val id: String,
    val taskId: String? = null,
    val userId: String? = null,
    val title: String,
    val subject: String, // mapped from type or subject
    val startTime: String,
    val endTime: String,
    val duration: String,
    val status: TaskStatus,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val topics: String? = null,
    val isFlexible: Boolean = true,
    val linkedDeadline: String? = null,
    val type: String = "study",
    val location: String? = null,
    val difficulty: String? = null,
    val contentMode: String? = null
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
    onViewAllClick: () -> Unit = {},
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
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            onClick = onViewAllClick,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("View all ${tasks.size} tasks")
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else if (tasks.isNotEmpty()) {
                         // Always show "View All" even if list is short, to navigate to calendar
                        TextButton(
                            onClick = onViewAllClick,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("View Full Schedule")
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScheduledTaskItem(
    task: ScheduledTask,
    isMobile: Boolean,
    isDetailed: Boolean = true, // Defaulting to detailed for new design
    showShadow: Boolean = true, // New param to control floating/flat
    showFullContext: Boolean = false, // New param for full content
    onClick: () -> Unit
) {
    // Redesigned Card with Conditional Shadow
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = KaironexColors.CanvasWhite,
        shadowElevation = if (showShadow) 4.dp else 0.dp, // Conditional Shadow
        border = if (!showShadow) androidx.compose.foundation.BorderStroke(1.dp, KaironexColors.CloudGray) else null, // Border if flat
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 1. TOP: Title & Status/Priority
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = KaironexColors.InkBlack,
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(Modifier.width(8.dp))
                
                // Priority Dot or Badge
                if (task.priority == TaskPriority.HIGH || task.priority == TaskPriority.CRITICAL) {
                     Surface(
                        shape = CircleShape,
                        color = Color(task.priority.color).copy(alpha = 0.1f)
                     ) {
                         Text(
                             task.priority.label,
                             modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                             style = MaterialTheme.typography.labelSmall,
                             color = Color(task.priority.color),
                             fontWeight = FontWeight.Bold
                         )
                     }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            // 2. TIME DURATION ROW
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Schedule, 
                    contentDescription = null, 
                    tint = KaironexColors.ElectricBlue,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${task.startTime} - ${task.endTime} (${task.duration})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = KaironexColors.ElectricBlue
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            // 3. DETAILS ROW (Subject, Topics, Flexible, Leadline)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                 // Subject Chip
                 Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = KaironexColors.CloudGray
                 ) {
                     Text(
                         task.subject,
                         modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                         style = MaterialTheme.typography.labelSmall,
                         color = KaironexColors.SlateGray
                     )
                 }

                 // Location
                 if (!task.location.isNullOrBlank()) {
                      Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = KaironexColors.CloudGray
                     ) {
                         Row(
                             verticalAlignment = Alignment.CenterVertically,
                             modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                         ) {
                             Icon(Icons.Default.Place, null, tint = KaironexColors.SlateGray, modifier = Modifier.size(10.dp))
                             Spacer(Modifier.width(2.dp))
                             Text(
                                 task.location,
                                 style = MaterialTheme.typography.labelSmall,
                                 color = KaironexColors.SlateGray
                             )
                         }
                     }
                 }
                 
                 // Difficulty
                 if (!task.difficulty.isNullOrBlank()) {
                      Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if(task.difficulty.equals("hard", true)) KaironexColors.ErrorRed.copy(alpha=0.1f) else KaironexColors.CloudGray
                     ) {
                         Text(
                             task.difficulty.replaceFirstChar { it.titlecase() },
                             modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                             style = MaterialTheme.typography.labelSmall,
                             color = if(task.difficulty.equals("hard", true)) KaironexColors.ErrorRed else KaironexColors.SlateGray
                         )
                     }
                 }
                 
                 // Content Mode
                 if (!task.contentMode.isNullOrBlank()) {
                      Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = KaironexColors.ElectricBlue.copy(alpha = 0.1f)
                     ) {
                         Text(
                             task.contentMode.replaceFirstChar { it.titlecase() },
                             modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                             style = MaterialTheme.typography.labelSmall,
                             color = KaironexColors.ElectricBlue
                         )
                     }
                 }
                 
                 // Flexible Badge
                 if (task.isFlexible) {
                      Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = KaironexColors.SuccessGreen.copy(alpha = 0.1f)
                     ) {
                         Row(
                             verticalAlignment = Alignment.CenterVertically,
                             modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                         ) {
                             Icon(Icons.Default.Autorenew, null, tint = KaironexColors.SuccessGreen, modifier = Modifier.size(10.dp))
                             Spacer(Modifier.width(2.dp))
                             Text(
                                 "Flexible",
                                 style = MaterialTheme.typography.labelSmall,
                                 color = KaironexColors.SuccessGreen
                             )
                         }
                     }
                 }
                 
                 // Deadline
                 if (!task.linkedDeadline.isNullOrBlank()) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                          Icon(Icons.Default.Event, null, tint = KaironexColors.ErrorRed, modifier = Modifier.size(12.dp))
                          Spacer(Modifier.width(2.dp))
                          Text(
                              "Due: ${task.linkedDeadline.take(10)}",
                              style = MaterialTheme.typography.labelSmall,
                              color = KaironexColors.ErrorRed
                          )
                      }
                 }
            }
            
            // Topics details
            if (!task.topics.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = task.topics,
                    style = MaterialTheme.typography.bodySmall,
                    color = KaironexColors.SlateGray,
                    maxLines = if (showFullContext) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
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
        priority = TaskPriority.HIGH,
        topics = "Sorting, Searching, Dynamic Programming"
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
        priority = TaskPriority.CRITICAL,
        linkedDeadline = "2026-02-10"
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
