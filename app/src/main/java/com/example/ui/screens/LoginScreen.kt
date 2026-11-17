package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import com.example.ui.components.AppIcons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.UserRole
import com.example.ui.theme.BrandRed
import com.example.ui.theme.BrandRedBright
import com.example.ui.theme.BrandRedDark
import com.example.ui.theme.CharcoalBrand
import com.example.viewmodel.LoginFormState

@Composable
fun LoginScreen(
    loginState: LoginFormState,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onSelectRole: (UserRole?) -> Unit,
    onLoginSubmit: (UserRole, String) -> Unit,
    onClearError: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isWideScreen = maxWidth >= 760.dp

        if (isWideScreen) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Hero Panel (Merah Putih / Crimson Dark)
                Box(
                    modifier = Modifier
                        .weight(1.05f)
                        .fillMaxHeight()
                ) {
                    LoginHeroSection(
                        isDarkTheme = isDarkTheme,
                        compact = false,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                // Right Login Controls
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .safeDrawingPadding()
                        .imePadding()
                        .padding(32.dp)
                ) {
                    ThemeModeQuickButton(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = onToggleTheme,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LoginInteractivePanel(
                            loginState = loginState,
                            isDarkTheme = isDarkTheme,
                            onSelectRole = onSelectRole,
                            onLoginSubmit = onLoginSubmit,
                            onClearError = onClearError,
                            modifier = Modifier.widthIn(max = 460.dp)
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(255.dp)
                ) {
                    LoginHeroSection(
                        isDarkTheme = isDarkTheme,
                        compact = true,
                        modifier = Modifier.fillMaxSize()
                    )
                    ThemeModeQuickButton(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = onToggleTheme,
                        overHero = true,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .safeDrawingPadding()
                            .padding(top = 12.dp, end = 16.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    LoginInteractivePanel(
                        loginState = loginState,
                        isDarkTheme = isDarkTheme,
                        onSelectRole = onSelectRole,
                        onLoginSubmit = onLoginSubmit,
                        onClearError = onClearError,
                        modifier = Modifier.widthIn(max = 520.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeModeQuickButton(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    overHero: Boolean = false,
    modifier: Modifier = Modifier
) {
    FilledTonalIconButton(
        onClick = onToggleTheme,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = if (overHero) Color.White.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (overHero) Color.White else MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
            .size(48.dp)
            .testTag("theme_toggle_button")
    ) {
        Icon(
            imageVector = if (isDarkTheme) AppIcons.LightMode else AppIcons.DarkMode,
            contentDescription = if (isDarkTheme) "Aktifkan Mode Terang" else "Aktifkan Mode Gelap"
        )
    }
}

@Composable
private fun LoginHeroSection(
    isDarkTheme: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val gradientColors = if (isDarkTheme) {
        listOf(Color(0xFF42080E), Color(0xFF1D0609), Color(0xFF10131A))
    } else {
        listOf(BrandRed, BrandRedDark, Color(0xFF7A0B14))
    }

    Box(
        modifier = modifier.background(Brush.verticalGradient(colors = gradientColors))
    ) {
        // Subtle Merah Putih decorative arcs
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = if (isDarkTheme) 0.05f else 0.10f),
                radius = size.maxDimension * 0.42f,
                center = Offset(size.width * 0.92f, size.height * 0.14f)
            )
            drawCircle(
                color = Color.White.copy(alpha = if (isDarkTheme) 0.04f else 0.08f),
                radius = size.maxDimension * 0.30f,
                center = Offset(size.width * 0.10f, size.height * 0.92f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = if (compact) 24.dp else 40.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth(if (compact) 0.82f else 0.90f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_farnad_logo),
                    contentDescription = "Logo FARNAD CELL",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (compact) 52.dp else 72.dp)
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "SISTEM MANAJEMEN FARNAD CELL",
                style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (!compact) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Kelola kasir penjualan barang, pencatatan pulsa & layanan digital, absensi foto GPS karyawan, stok barang, dan laporan keuangan usaha dalam satu sistem terintegrasi.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.88f)
                )
            }
        }
    }
}

@Composable
private fun LoginInteractivePanel(
    loginState: LoginFormState,
    isDarkTheme: Boolean,
    onSelectRole: (UserRole?) -> Unit,
    onLoginSubmit: (UserRole, String) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = loginState.selectedRole,
        label = "login_role_transition",
        modifier = modifier.fillMaxWidth()
    ) { selectedRole ->
        if (selectedRole == null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Pilih Akses Masuk",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Silakan pilih peran Anda untuk masuk ke dashboard operasional.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))

                RoleSelectionCard(
                    title = "1. OWNER",
                    subtitle = "Kontrol penuh stok, harga, karyawan, laporan keuangan & audit aktivitas",
                    icon = AppIcons.AdminPanelSettings,
                    iconBoxColor = BrandRed,
                    badgeBgColor = BrandRed.copy(alpha = if (isDarkTheme) 0.22f else 0.12f),
                    badgeTextColor = if (isDarkTheme) BrandRedBright else BrandRed,
                    badgeText = "Manajemen Penuh",
                    testTag = "role_card_owner",
                    onClick = { onSelectRole(UserRole.OWNER) }
                )

                RoleSelectionCard(
                    title = "2. KARYAWAN",
                    subtitle = "Absensi foto GPS, kasir penjualan barang, dan transaksi pulsa & digital",
                    icon = AppIcons.Badge,
                    iconBoxColor = if (isDarkTheme) Color(0xFF2D3342) else CharcoalBrand,
                    badgeBgColor = if (isDarkTheme) Color.White.copy(alpha = 0.12f) else CharcoalBrand.copy(alpha = 0.10f),
                    badgeTextColor = MaterialTheme.colorScheme.onSurface,
                    badgeText = "Operasional Harian",
                    testTag = "role_card_karyawan",
                    onClick = { onSelectRole(UserRole.KARYAWAN) }
                )
            }
        } else {
            var password by remember(selectedRole) { mutableStateOf("") }
            var showPassword by remember { mutableStateOf(false) }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { onSelectRole(null) },
                            modifier = Modifier.testTag("login_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                            Spacer(Modifier.width(4.dp))
                            Text("Ganti Peran")
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedRole == UserRole.OWNER) BrandRed else CharcoalBrand,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (selectedRole == UserRole.OWNER) {
                                        AppIcons.AdminPanelSettings
                                    } else {
                                        AppIcons.Badge
                                    },
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Login ${selectedRole.label.uppercase()}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (selectedRole == UserRole.OWNER) {
                                    "Masukkan kata sandi Owner"
                                } else {
                                    "Masukkan kata sandi Karyawan"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            onClearError()
                        },
                        label = { Text("Kata Sandi ${selectedRole.label}") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) AppIcons.VisibilityOff else AppIcons.Visibility,
                                    contentDescription = if (showPassword) "Sembunyikan sandi" else "Tampilkan sandi"
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (selectedRole == UserRole.KARYAWAN) KeyboardType.NumberPassword else KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { onLoginSubmit(selectedRole, password) }
                        ),
                        isError = loginState.errorMessage != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input")
                    )

                    if (loginState.errorMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = loginState.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = { onLoginSubmit(selectedRole, password) },
                        enabled = !loginState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("login_submit_button")
                    ) {
                        if (loginState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = "MASUK SEBAGAI ${selectedRole.label.uppercase()}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleSelectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBoxColor: Color,
    badgeBgColor: Color,
    badgeTextColor: Color,
    badgeText: String,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = iconBoxColor,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = badgeBgColor,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = badgeTextColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
