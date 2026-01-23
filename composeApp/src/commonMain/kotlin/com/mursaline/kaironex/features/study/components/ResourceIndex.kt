package com.mursaline.kaironex.features.study.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.ui.theme.KaironexColors

data class Resource(
    val id: String,
    val title: String,
    val type: ResourceType,
    val duration: String? = null
)

enum class ResourceType {
    PDF, VIDEO, CODE
}

@Composable
fun ResourceIndex(
    resources: List<Resource>,
    selectedResourceId: String?,
    onResourceSelect: (Resource) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "RESOURCES",
            style = MaterialTheme.typography.labelMedium,
            color = KaironexColors.SlateGray,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(resources) { resource ->
                ResourceItem(
                    resource = resource,
                    isSelected = resource.id == selectedResourceId,
                    onClick = { onResourceSelect(resource) }
                )
            }
        }
    }
}

@Composable
fun ResourceItem(
    resource: Resource,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val icon = when(resource.type) {
        ResourceType.PDF -> Icons.Filled.Article
        ResourceType.VIDEO -> Icons.Filled.VideoLibrary
        ResourceType.CODE -> Icons.Filled.Code
    }
    
    val backgroundColor = if (isSelected) KaironexColors.CloudGray else KaironexColors.CanvasWhite
    val borderColor = if (isSelected) KaironexColors.ElectricBlue else KaironexColors.BorderGray
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) KaironexColors.ElectricBlue else KaironexColors.BorderGray
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) KaironexColors.ElectricBlue else KaironexColors.SlateGray,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = resource.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = KaironexColors.InkBlack
                )
                resource.duration?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = KaironexColors.SlateGray
                    )
                }
            }
        }
    }
}
