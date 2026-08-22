package com.wormhole.browser.ui.sidebar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wormhole.browser.core.browser.Space
import com.wormhole.browser.core.browser.Tab
import com.wormhole.browser.ui.theme.WormHoleIconBadge
import com.wormhole.browser.ui.theme.WormHoleMotion
import com.wormhole.browser.ui.theme.WormHoleSurface
import com.wormhole.browser.ui.theme.bouncyClickable
import com.wormhole.browser.ui.theme.spinBounceClickable

private val TAB_GRID_MIN_COLUMN_WIDTH = 140.dp

@Composable
fun WormHoleSidebar(
    isExpanded: Boolean,
    spaces: List<Space>,
    activeSpaceId: String,
    tabsInActiveSpace: List<Tab>,
    activeTabId: String?,
    onSpaceSelected: (String) -> Unit,
    onAddSpace: () -> Unit,
    onTabSelected: (String) -> Unit,
    onTabClosed: (String) -> Unit,
    onNewTab: () -> Unit,
    onSettingsClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeSpace = spaces.firstOrNull { it.id == activeSpaceId }

    AnimatedVisibility(
        visible = isExpanded,
        enter = fadeIn(animationSpec = WormHoleMotion.overlay()) + scaleIn(initialScale = 0.97f, animationSpec = WormHoleMotion.popup()),
        exit = fadeOut(animationSpec = WormHoleMotion.fadeOut()) + scaleOut(targetScale = 0.98f, animationSpec = WormHoleMotion.snappy()),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SpaceSwitcher(
                        spaces = spaces,
                        activeSpaceId = activeSpaceId,
                        onSpaceSelected = onSpaceSelected,
                        onAddSpace = onAddSpace,
                        modifier = Modifier.weight(1f),
                    )

                    WormHoleIconBadge(
                        icon = Icons.Default.Close,
                        contentDescription = "Close tabs",
                        size = 36.dp,
                        modifier = Modifier.padding(start = 12.dp),
                        onClick = onClose,
                    )
                }

                Text(
                    text = activeSpace?.name.orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
                )

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = TAB_GRID_MIN_COLUMN_WIDTH),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(tabsInActiveSpace, key = { it.id }) { tab ->
                        TabGridCard(
                            tab = tab,
                            isActive = tab.id == activeTabId,
                            spaceAccent = activeSpace,
                            onClick = {
                                onTabSelected(tab.id)
                                onClose()
                            },
                            onClose = { onTabClosed(tab.id) },

                            modifier = Modifier.animateItem(
                                placementSpec = WormHoleMotion.bouncy(),
                            ),
                        )
                    }
                }

                SidebarFooter(onNewTab = onNewTab, onSettingsClick = onSettingsClick)
            }
        }
    }
}

@Composable
private fun SidebarFooter(onNewTab: () -> Unit, onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WormHoleIconBadge(
            icon = Icons.Default.Settings,
            contentDescription = "Settings",
            size = 40.dp,
            onClick = onSettingsClick,
        )

        Box(
            modifier = Modifier
                .clip(WormHoleSurface.PillShape)
                .background(MaterialTheme.colorScheme.primary)
                .spinBounceClickable(onClick = onNewTab)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(end = 4.dp),
                )
                Text(
                    "New Tab",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}
