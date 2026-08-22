package com.wormhole.browser.ui.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.wormhole.browser.core.browser.Space
import com.wormhole.browser.ui.theme.WormHoleSurface
import com.wormhole.browser.ui.theme.bouncyClickable

@Composable
fun SpaceSwitcher(
    spaces: List<Space>,
    activeSpaceId: String,
    onSpaceSelected: (String) -> Unit,
    onAddSpace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(spaces.sortedBy { it.order }, key = { it.id }) { space ->
            SpaceAvatar(
                space = space,
                isActive = space.id == activeSpaceId,
                onClick = { onSpaceSelected(space.id) },
            )
        }
        item {
            AddSpaceButton(onClick = onAddSpace)
        }
    }
}

@Composable
private fun SpaceAvatar(space: Space, isActive: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(space.accent.color)
            .then(
                if (isActive) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                } else {
                    Modifier
                },
            )
            .bouncyClickable(role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = space.name.take(1).uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
        )
    }
}

@Composable
private fun AddSpaceButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(WormHoleSurface.Fill)
            .border(1.dp, WormHoleSurface.HairlineBorder, CircleShape)
            .bouncyClickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = "New Space",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}
