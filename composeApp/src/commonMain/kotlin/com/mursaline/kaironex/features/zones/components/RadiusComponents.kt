package com.mursaline.kaironex.features.zones.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.ui.theme.KaironexColors

/**
 * 📡 RADIUS ZONE COMPONENTS
 * ==========================
 * Signal Decoder, Safehouse, Local Scan, Admin Protocol
 *
 * External awareness and survival systems
 */

// =========================================================================
// SIGNAL DECODER COMPONENT (Slang & Cultural Guide)
// =========================================================================

data class SlangTerm(
    val term: String,
    val meaning: String,
    val usage: String,
    val region: String,
    val isLearned: Boolean = false
)

@Composable
fun SignalDecoderCard(
    learnedTerms: Int,
    totalTerms: Int,
    recentTerms: List<SlangTerm>,
    fluencyScore: Int,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📡", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Signal Decoder",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Cultural & Language Guide",
                            style = MaterialTheme.typography.bodySmall,
                            color = KaironexColors.SlateGray
                        )
                    }
                }

                // Fluency score
                Surface(
                    color = Color(0xFF2196F3).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🗣️", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "$fluencyScore%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Terms Learned", style = MaterialTheme.typography.labelMedium, color = KaironexColors.SlateGray)
                Text("$learnedTerms / $totalTerms", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { learnedTerms.toFloat() / totalTerms },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF2196F3),
                trackColor = KaironexColors.CloudGray
            )

            Spacer(Modifier.height(16.dp))

            // Recent terms
            Text("Recent Terms", style = MaterialTheme.typography.labelMedium, color = KaironexColors.SlateGray)
            Spacer(Modifier.height(8.dp))

            recentTerms.take(3).forEach { term ->
                SlangTermItem(term = term)
                if (term != recentTerms.last()) {
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            TextButton(
                onClick = onViewAll,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View All Terms →", color = Color(0xFF2196F3))
            }
        }
    }
}

@Composable
private fun SlangTermItem(term: SlangTerm) {
    Surface(
        color = KaironexColors.CloudGray,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "\"${term.term}\"",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (term.isLearned) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Check,
                            null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    term.meaning,
                    style = MaterialTheme.typography.bodySmall,
                    color = KaironexColors.SlateGray
                )
            }
            Surface(
                color = Color(0xFF2196F3).copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    term.region,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF2196F3)
                )
            }
        }
    }
}

// =========================================================================
// SAFEHOUSE COMPONENT (Housing & Utilities)
// =========================================================================

data class SafehouseListing(
    val id: String,
    val title: String,
    val location: String,
    val price: String,
    val rating: Float,
    val isVerified: Boolean
)

@Composable
fun SafehouseCard(
    listings: List<SafehouseListing>,
    housingStabilityScore: Int,
    utilityReadiness: Float,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🏠", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Safehouse",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Housing & Utilities",
                            style = MaterialTheme.typography.bodySmall,
                            color = KaironexColors.SlateGray
                        )
                    }
                }

                // Stability score
                Surface(
                    color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Stability: $housingStabilityScore%",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Utility readiness
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Utility Readiness", style = MaterialTheme.typography.labelMedium, color = KaironexColors.SlateGray)
                Text("${(utilityReadiness * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { utilityReadiness },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF4CAF50),
                trackColor = KaironexColors.CloudGray
            )

            if (listings.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("Saved Listings", style = MaterialTheme.typography.labelMedium, color = KaironexColors.SlateGray)
                Spacer(Modifier.height(8.dp))

                listings.take(2).forEach { listing ->
                    ListingItem(listing = listing)
                    if (listing != listings.last()) {
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onSearch,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Search Nearby")
            }
        }
    }
}

@Composable
private fun ListingItem(listing: SafehouseListing) {
    Surface(
        color = KaironexColors.CloudGray,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        listing.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (listing.isVerified) {
                        Spacer(Modifier.width(4.dp))
                        Text("✓", color = Color(0xFF4CAF50))
                    }
                }
                Text(
                    listing.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = KaironexColors.SlateGray
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    listing.price,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "⭐ ${listing.rating}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFB300)
                )
            }
        }
    }
}

// =========================================================================
// LOCAL SCAN COMPONENT (Nearby Resources)
// =========================================================================

data class LocalResource(
    val name: String,
    val type: String,
    val distance: String,
    val emoji: String
)

@Composable
fun LocalScanCard(
    resources: List<LocalResource>,
    safeZoneAwareness: Float,
    onScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔍", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Local Scan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Nearby Resources",
                        style = MaterialTheme.typography.bodySmall,
                        color = KaironexColors.SlateGray
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Safe zone awareness
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Area Knowledge", style = MaterialTheme.typography.labelMedium, color = KaironexColors.SlateGray)
                Text("${(safeZoneAwareness * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { safeZoneAwareness },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = KaironexColors.ElectricBlue,
                trackColor = KaironexColors.CloudGray
            )

            Spacer(Modifier.height(16.dp))

            // Resources grid
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(resources) { resource ->
                    ResourceChip(resource = resource)
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onScan,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.ElectricBlue)
            ) {
                Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Scan Area")
            }
        }
    }
}

@Composable
private fun ResourceChip(resource: LocalResource) {
    Surface(
        color = KaironexColors.CloudGray,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).width(100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(resource.emoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                resource.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                resource.distance,
                style = MaterialTheme.typography.labelSmall,
                color = KaironexColors.SlateGray
            )
        }
    }
}

// =========================================================================
// ADMIN PROTOCOL COMPONENT (Visa & Documentation)
// =========================================================================

@Composable
fun AdminProtocolCard(
    visaDaysRemaining: Int,
    workHoursUsed: Int,
    workHourLimit: Int,
    advisorContact: String?,
    alerts: List<String>,
    onContactAdvisor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUrgent = visaDaysRemaining < 90

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isUrgent) Color(0xFFFFF3E0) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📋", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Admin Protocol",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Visa & Documentation",
                            style = MaterialTheme.typography.bodySmall,
                            color = KaironexColors.SlateGray
                        )
                    }
                }

                if (isUrgent) {
                    Surface(
                        color = Color(0xFFE65100).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                null,
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Action Needed",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Visa countdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Visa Status", style = MaterialTheme.typography.labelMedium, color = KaironexColors.SlateGray)
                    Text(
                        "$visaDaysRemaining days remaining",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isUrgent) Color(0xFFE65100) else KaironexColors.InkBlack
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Work Hours", style = MaterialTheme.typography.labelMedium, color = KaironexColors.SlateGray)
                    Text(
                        "$workHoursUsed / $workHourLimit hrs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Work hours bar
            LinearProgressIndicator(
                progress = { workHoursUsed.toFloat() / workHourLimit },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = if (workHoursUsed >= workHourLimit - 2) Color(0xFFF44336) else Color(0xFF4CAF50),
                trackColor = KaironexColors.CloudGray
            )

            // Alerts
            if (alerts.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                alerts.forEach { alert ->
                    Surface(
                        color = Color(0xFFE65100).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚠️", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                alert,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                    if (alert != alerts.last()) {
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            // Advisor contact
            advisorContact?.let {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onContactAdvisor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Groups, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Contact International Office")
                }
            }
        }
    }
}
