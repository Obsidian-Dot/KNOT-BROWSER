package com.wormhole.browser.ui.onboarding

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.wormhole.browser.core.settings.SearchEngine
import com.wormhole.browser.ui.theme.WormHoleSurface
import com.wormhole.browser.ui.theme.bouncyClickable

@Composable
fun OnboardingScreen(
    currentEngine: SearchEngine,
    onEngineSelected: (SearchEngine) -> Unit,
    onFinished: () -> Unit,
    onPrivacyPolicyClick: () -> Unit = {},
    onTermsClick: () -> Unit = {},
) {
    var page by remember { mutableIntStateOf(0) }
    val pageCount = 4

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            if (page < pageCount - 1) {
                Text(
                    text = "Skip",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .bouncyClickable(onClick = { page = pageCount - 1 })
                        .padding(8.dp),
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (page) {
                0 -> WelcomePage()
                1 -> FeaturesPage()
                2 -> SearchEnginePage(currentEngine = currentEngine, onEngineSelected = onEngineSelected)
                else -> DefaultBrowserPage()
            }
        }

        PageIndicator(count = pageCount, current = page)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = { if (page < pageCount - 1) page++ else onFinished() },
                shape = WormHoleSurface.PillShape,
                modifier = Modifier.weight(1f).height(54.dp),
            ) {
                Text(
                    text = if (page < pageCount - 1) "Continue" else "Start browsing",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (page < pageCount - 1) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.padding(start = 2.dp))
                }
            }
        }

        if (page == pageCount - 1) {
            val linkColor = MaterialTheme.colorScheme.primary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "By continuing, you agree to WormHole's",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Terms of Service",
                    style = MaterialTheme.typography.labelSmall,
                    color = linkColor,
                    modifier = Modifier.bouncyClickable(onClick = onTermsClick),
                )
                Text(
                    text = "  and  ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Privacy Policy",
                    style = MaterialTheme.typography.labelSmall,
                    color = linkColor,
                    modifier = Modifier.bouncyClickable(onClick = onPrivacyPolicyClick),
                )
            }
        }
    }
}

@Composable
private fun WelcomePage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = WormHoleSurface.CardShape,
            color = WormHoleSurface.Fill,
            border = WormHoleSurface.border(),
            modifier = Modifier.size(88.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Public,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        Text(
            text = "Welcome to WormHole",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "A fast, private browser with an AI assistant built in. Let's get you set up -- it only takes a moment.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun FeaturesPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Built for how you actually browse",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 22.dp, start = 4.dp),
        )
        FeatureRow(Icons.Default.SmartToy, "AI Assistant", "Summarize pages, translate, or ask WormHole to do things for you.")
        FeatureRow(Icons.Default.Tab, "Spaces & tabs", "Keep work, personal, and incognito browsing cleanly separated.")
        FeatureRow(Icons.Default.Bolt, "Built for speed", "Tabs stay warm in memory so switching between them is instant.")
        FeatureRow(Icons.Default.Shield, "Privacy-minded", "Incognito tabs, and you're always in control of what's shared.")
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            modifier = Modifier.size(44.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            }
        }
        Column(modifier = Modifier.padding(start = 14.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SearchEnginePage(
    currentEngine: SearchEngine,
    onEngineSelected: (SearchEngine) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Pick your search engine",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Used whenever you type a search instead of a web address. Change this anytime in Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
        )
        SearchEngine.entries.forEach { engine ->
            val selected = engine == currentEngine
            Surface(
                shape = WormHoleSurface.PillShape,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                } else {
                    WormHoleSurface.Fill
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .bouncyClickable(onClick = { onEngineSelected(engine) }),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(engine.displayName, style = MaterialTheme.typography.bodyLarge)
                    if (selected) {
                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun DefaultBrowserPage() {
    val context = LocalContext.current

    var permissionsRequested by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {  }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Finish setup",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Two optional steps that make WormHole work smoothly from the start.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
        )

        SetupActionCard(
            icon = Icons.Default.SwapHoriz,
            title = "Set WormHole as default browser",
            subtitle = "Open links from other apps directly in WormHole.",
            actionLabel = "Set default",
            onClick = { requestDefaultBrowser(context) },
        )

        Spacer(Modifier.height(12.dp))

        SetupActionCard(
            icon = Icons.Default.Notifications,
            title = "App permissions",
            subtitle = "Camera, microphone, and location -- only used when a site asks and you approve.",
            actionLabel = if (permissionsRequested) "Requested" else "Allow",
            onClick = {
                permissionsRequested = true
                permissionLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.CAMERA,
                        android.Manifest.permission.RECORD_AUDIO,
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                    ).filter {
                        ContextCompat.checkSelfPermission(context, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
                    }.toTypedArray(),
                )
            },
        )
    }
}

@Composable
private fun SetupActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = WormHoleSurface.CardShape,
        color = WormHoleSurface.Fill,
        border = WormHoleSurface.border(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = WormHoleSurface.Fill,
                border = WormHoleSurface.border(),
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp, end = 8.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
                shape = WormHoleSurface.PillShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .height(36.dp)
                    .bouncyClickable(onClick = onClick),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(actionLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun PageIndicator(count: Int, current: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(count) { index ->
            val active = index == current
            val width by animateFloatAsState(if (active) 22f else 7f, label = "dotWidth")
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .height(7.dp)
                    .width(width.dp)
                    .background(
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(50),
                    ),
            )
        }
    }
}

private fun requestDefaultBrowser(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
        if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER)) {
            if (!roleManager.isRoleHeld(RoleManager.ROLE_BROWSER)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER)
                context.startActivity(intent)
                return
            }
        }
    }

    val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    runCatching { context.startActivity(intent) }.onFailure {
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}
