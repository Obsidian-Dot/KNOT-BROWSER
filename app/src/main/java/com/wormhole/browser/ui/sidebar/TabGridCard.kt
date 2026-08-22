package com.wormhole.browser.ui.sidebar

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wormhole.browser.core.browser.Space
import com.wormhole.browser.core.browser.Tab
import com.wormhole.browser.core.webview.TabThumbnailCache
import com.wormhole.browser.ui.theme.WormHoleMotion
import com.wormhole.browser.ui.theme.bouncyClickable

@Composable
fun TabGridCard(
    tab: Tab,
    isActive: Boolean,
    spaceAccent: Space?,
    onClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val entranceScale = remember { Animatable(0.72f) }
    LaunchedEffect(Unit) {
        entranceScale.animateTo(1f, animationSpec = WormHoleMotion.bouncy())
    }

    Column(modifier = modifier.scale(entranceScale.value)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = if (isActive) {
                BorderStroke(2.dp, spaceAccent?.accent?.color ?: MaterialTheme.colorScheme.primary)
            } else {
                null
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .bouncyClickable(role = Role.Tab, onClick = onClick),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val thumbnail = TabThumbnailCache.get(tab.id)
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .bouncyClickable(role = Role.Button, onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close tab",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, start = 2.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FaviconDot(accentColor = spaceAccent?.accent?.color)
            Text(
                text = tab.title.ifBlank { "New Tab" },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

@Composable
private fun FaviconDot(accentColor: Color?) {
    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(accentColor?.copy(alpha = 0.15f) ?: MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.Language,
            contentDescription = null,
            tint = accentColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(9.dp),
        )
    }
}
