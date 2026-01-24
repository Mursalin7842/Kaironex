package com.mursaline.kaironex.features.study.viewers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.ui.theme.KaironexColors

/**
 * ============================================================
 * IN-APP SECURE BROWSER (PLACEHOLDER)
 * ============================================================
 *
 * Secure browser for study materials with:
 * - No URL bar for untrusted content
 * - Permitted domain list only
 * - Tab switcher for preloaded study tabs
 * - "Request new tab" modal for whitelist additions
 * - Browsing tracked for Proof-of-Work
 *
 * TODO: Implement actual WebView with domain restrictions
 */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InAppSecureBrowser(
    currentUrl: String,
    pageTitle: String,
    permittedDomains: List<String> = listOf(
        "wikipedia.org",
        "khanacademy.org",
        "coursera.org",
        "edx.org",
        "scholar.google.com",
        "arxiv.org"
    ),
    onNavigate: (String) -> Unit = {},
    onRequestNewDomain: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showDomainRequest by remember { mutableStateOf(false) }
    var requestedDomain by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        // Browser Header
        BrowserHeader(
            pageTitle = pageTitle,
            currentDomain = extractDomain(currentUrl),
            isSecure = true,
            onBack = { /* TODO */ },
            onForward = { /* TODO */ },
            onRefresh = { /* TODO */ },
            onRequestDomain = { showDomainRequest = true }
        )

        // Browser Content (Placeholder)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "Browser",
                    tint = KaironexColors.ElectricBlue,
                    modifier = Modifier.size(80.dp)
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Secure Study Browser",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = KaironexColors.InkBlack
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = currentUrl,
                    style = MaterialTheme.typography.bodyMedium,
                    color = KaironexColors.SlateGray,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(32.dp))

                // Implementation note
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = KaironexColors.AttentionOrange.copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Construction,
                            contentDescription = null,
                            tint = KaironexColors.AttentionOrange,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "WebView Implementation Pending",
                            style = MaterialTheme.typography.titleSmall,
                            color = KaironexColors.AttentionOrange,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Will implement domain-restricted WebView\nwith browsing history tracking",
                            style = MaterialTheme.typography.bodySmall,
                            color = KaironexColors.AttentionOrange.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Permitted domains
                Text(
                    text = "Permitted Domains",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = KaironexColors.InkBlack
                )
                Spacer(Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    permittedDomains.forEach { domain ->
                        DomainChip(domain = domain)
                    }
                }
            }
        }

        // Proof of Work indicator
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = KaironexColors.SuccessGreen.copy(alpha = 0.1f)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = "Secure",
                    tint = KaironexColors.SuccessGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Browsing restricted to study-approved domains • Time tracked for PoW",
                    style = MaterialTheme.typography.labelSmall,
                    color = KaironexColors.SuccessGreen
                )
            }
        }
    }

    // Domain request dialog
    if (showDomainRequest) {
        AlertDialog(
            onDismissRequest = { showDomainRequest = false },
            title = { Text("Request New Domain") },
            text = {
                Column {
                    Text(
                        "Enter the domain you need for your studies. It will be reviewed before approval.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = requestedDomain,
                        onValueChange = { requestedDomain = it },
                        label = { Text("Domain (e.g., docs.python.org)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRequestNewDomain(requestedDomain)
                        showDomainRequest = false
                        requestedDomain = ""
                    }
                ) {
                    Text("Submit Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDomainRequest = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun BrowserHeader(
    pageTitle: String,
    currentDomain: String,
    isSecure: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onRequestDomain: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = KaironexColors.CanvasWhite,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Navigation buttons
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = KaironexColors.SlateGray
                )
            }
            IconButton(onClick = onForward, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Forward",
                    tint = KaironexColors.SlateGray
                )
            }
            IconButton(onClick = onRefresh, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = KaironexColors.SlateGray
                )
            }

            Spacer(Modifier.width(8.dp))

            // URL/Title bar (read-only for security)
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                color = KaironexColors.CloudGray
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSecure) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = if (isSecure) "Secure" else "Not Secure",
                        tint = if (isSecure) KaironexColors.SuccessGreen else KaironexColors.AlertRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pageTitle,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = KaironexColors.InkBlack,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentDomain,
                            style = MaterialTheme.typography.labelSmall,
                            color = KaironexColors.SlateGray,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // Request new domain
            IconButton(onClick = onRequestDomain, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.AddCircle,
                    contentDescription = "Request Domain",
                    tint = KaironexColors.ElectricBlue
                )
            }
        }
    }
}

@Composable
private fun DomainChip(domain: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = KaironexColors.ElectricBlue.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Approved",
                tint = KaironexColors.SuccessGreen,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = domain,
                style = MaterialTheme.typography.labelMedium,
                color = KaironexColors.ElectricBlue
            )
        }
    }
}

/**
 * Tab switcher for multiple study tabs
 */
@Composable
fun BrowserTabSwitcher(
    tabs: List<BrowserTab>,
    currentTabIndex: Int,
    onTabSelect: (Int) -> Unit,
    onTabClose: (Int) -> Unit,
    onNewTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = KaironexColors.CanvasWhite
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                TabChip(
                    tab = tab,
                    isSelected = index == currentTabIndex,
                    onClick = { onTabSelect(index) },
                    onClose = { onTabClose(index) },
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            // Add new tab button
            IconButton(
                onClick = onNewTab,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "New Tab",
                    tint = KaironexColors.SlateGray
                )
            }
        }
    }
}

@Composable
private fun TabChip(
    tab: BrowserTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
        color = if (isSelected) KaironexColors.CanvasWhite else KaironexColors.CloudGray
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tab.title,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) KaironexColors.InkBlack else KaironexColors.SlateGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(Modifier.width(4.dp))
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close Tab",
                    tint = KaironexColors.SlateGray,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

data class BrowserTab(
    val id: String,
    val title: String,
    val url: String,
    val favicon: String? = null
)

private fun extractDomain(url: String): String {
    return try {
        url.removePrefix("https://")
            .removePrefix("http://")
            .substringBefore("/")
    } catch (e: Exception) {
        url
    }
}
