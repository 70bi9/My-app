package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.MangaDexClient
import com.example.data.models.ColoredManga
import com.example.data.models.MangaPageEntity
import com.example.ui.theme.*
import com.example.viewmodel.MangaViewModel
import com.example.viewmodel.Screen
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContainer()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer() {
    val viewModel: MangaViewModel = viewModel()
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsState()

    // Handle standard physical back button
    BackHandler(enabled = currentScreen != Screen.Home) {
        when (currentScreen) {
            Screen.MangaReader -> viewModel.navigateTo(Screen.Library)
            Screen.ProcessingTerminal -> {
                if (!viewModel.isProcessingActive.value) {
                    viewModel.navigateTo(Screen.Home)
                } else {
                    Toast.makeText(context, "جاري المعالجة... يرجى الانتظار", Toast.LENGTH_SHORT).show()
                }
            }
            else -> viewModel.navigateTo(Screen.Home)
        }
    }

    Scaffold(
        bottomBar = {
            if (currentScreen != Screen.MangaReader && currentScreen != Screen.ProcessingTerminal) {
                MangaBottomNavigation(
                    currentScreen = currentScreen,
                    onNavigate = { screen -> viewModel.navigateTo(screen) }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    Screen.Home -> HomeScreen(viewModel)
                    Screen.Library -> LibraryScreen(viewModel)
                    Screen.ImportDex -> ImportDexScreen(viewModel)
                    Screen.Settings -> SettingsScreen(viewModel)
                    Screen.ProcessingTerminal -> ProcessingTerminalScreen(viewModel)
                    Screen.MangaReader -> MangaReaderScreen(viewModel)
                }
            }
        }
    }
}

// --- Custom Shapes for Split Swipe UI ---
class SplitClipShape(private val fraction: Float) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Rectangle(
            Rect(0f, 0f, size.width * fraction, size.height)
        )
    }
}

// --- Navigation component ---
@Composable
fun MangaBottomNavigation(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        NavigationBarItem(
            selected = currentScreen == Screen.Home,
            onClick = { onNavigate(Screen.Home) },
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
            label = { Text("الرئيسية", fontSize = 11.sp, fontWeight = if (currentScreen == Screen.Home) FontWeight.Bold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                indicatorColor = SlateButtonContainer
            ),
            modifier = Modifier.testTag("nav_home")
        )
        NavigationBarItem(
            selected = currentScreen == Screen.Library,
            onClick = { onNavigate(Screen.Library) },
            icon = { Icon(Icons.Filled.List, contentDescription = "Library") },
            label = { Text("المكتبة", fontSize = 11.sp, fontWeight = if (currentScreen == Screen.Library) FontWeight.Bold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                indicatorColor = SlateButtonContainer
            ),
            modifier = Modifier.testTag("nav_library")
        )
        NavigationBarItem(
            selected = currentScreen == Screen.ImportDex,
            onClick = { onNavigate(Screen.ImportDex) },
            icon = { Icon(Icons.Filled.CloudDownload, contentDescription = "Import Dex") },
            label = { Text("استيراد", fontSize = 11.sp, fontWeight = if (currentScreen == Screen.ImportDex) FontWeight.Bold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                indicatorColor = SlateButtonContainer
            ),
            modifier = Modifier.testTag("nav_import")
        )
        NavigationBarItem(
            selected = currentScreen == Screen.Settings,
            onClick = { onNavigate(Screen.Settings) },
            icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
            label = { Text("الإعدادات", fontSize = 11.sp, fontWeight = if (currentScreen == Screen.Settings) FontWeight.Bold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                indicatorColor = SlateButtonContainer
            ),
            modifier = Modifier.testTag("nav_settings")
        )
    }
}

// ==========================================
// 1. HOME SCREEN
// ==========================================
@Composable
fun HomeScreen(viewModel: MangaViewModel) {
    val context = LocalContext.current
    val allManga by viewModel.allMangaList.collectAsState()

    // File Picker for PDFs
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                // Get title from file name
                val cr = context.contentResolver
                var title = "مستند مانغا مستورد PDF"
                cr.query(it, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        title = cursor.getString(nameIndex).substringBeforeLast(".")
                    }
                }
                viewModel.uploadPdf(context, it, title)
            }
        }
    )

    // Multiple Images Picker
    val imagesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                val imageInputs = uris.mapIndexed { index, uri ->
                    val stream = context.contentResolver.openInputStream(uri)
                    "image_$index.jpg" to (stream ?: context.assets.open("placeholder.jpg"))
                }
                viewModel.uploadImages("فصل صور مخصص • Custom Chapter", imageInputs)
            }
        }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Sleek Sophisticated Top Header Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MangaColor ",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "AI",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary // #A8C7FF
                        )
                    }
                    Text(
                        text = "MATERIAL ENGINE V4.0",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant // #8E9199
                    )
                }
                
                // Profile Avatar Circle with Gradient Accent
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,      // #A8C7FF
                                        MaterialTheme.colorScheme.secondary    // #D0BCFF
                                    )
                                )
                            )
                    )
                }
            }
        }

        // 2. Recognized Work Featured Card (Sophisticated Slate Card)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(32.dp))
                    .padding(20.dp)
            ) {
                // Top-Right Pro Mode Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(100.dp))
                        .background(SlateButtonContainer)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "PRO MODE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    // Pulsing Model status
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Small accent dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = "AI Engine: Stable Diffusion XL",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Recognized Work:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Blue Lock",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tags row
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(100.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "Character: Isagi",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(100.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "Palette: Official Anime",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Progress indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Progress bar container
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(0.75f)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }

                        Text(
                            text = "75%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Action Buttons Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "تلوين واستيراد جديد • Colorize Manga",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { pdfLauncher.launch(arrayOf("application/pdf")) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_pdf")
                            .height(112.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary, // #A8C7FF
                            contentColor = DarkTextBlue // #003062
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "📥",
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "رفع مانغا PDF",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Upload PDF",
                                fontSize = 11.sp,
                                color = DarkTextBlue.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Button(
                        onClick = { imagesLauncher.launch("image/*") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_images")
                            .height(112.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SlateButtonContainer, // #3A4759
                            contentColor = SlateButtonContent // #D1E4FF
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "🖼️",
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "تلوين صور مجمعة",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Batch Images",
                                fontSize = 11.sp,
                                color = SlateButtonContent.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Button(
                    onClick = { viewModel.navigateTo(Screen.ImportDex) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_api")
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SlateButtonContainer.copy(alpha = 0.4f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        Icons.Filled.TravelExplore, 
                        contentDescription = "Online",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "بحث واستيراد فصول (MangaDex Cloud Server)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Recent Work Activity Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آخر الفصول المعالجة • Recent Colorizations",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "View Library",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { viewModel.navigateTo(Screen.Library) }
                )
            }
        }

        if (allManga.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = "Logo",
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "مكتبتك فارغة حالياً. قم برفع مستند PDF أو استيراد فصل للبدء!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(allManga.take(5)) { manga ->
                RecentMangaCard(
                    manga = manga,
                    onOpen = { viewModel.openMangaReader(manga) },
                    onDelete = { viewModel.deleteManga(manga.id) }
                )
            }
        }
    }
}

@Composable
fun RecentMangaCard(
    manga: ColoredManga,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Volume thumbnail cover photo
            Box(
                modifier = Modifier
                    .size(68.dp, 94.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            ) {
                if (manga.coverUrl.isNotEmpty()) {
                    AsyncImage(
                        model = if (manga.coverUrl.startsWith("http")) manga.coverUrl else File(manga.coverUrl),
                        contentDescription = "Cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Filled.Book,
                        contentDescription = "Cover",
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.Center),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = manga.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(SlateButtonContainer)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            manga.source,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        "${manga.pageCount} صفحة • Pages",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LinearProgressIndicator(
                        progress = manga.progress / 100f,
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape),
                        color = if (manga.status == "FAILED") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.background
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "${manga.progress}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (manga.status == "FAILED") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
                if (manga.status == "PROCESSING") {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ==========================================
// 2. LIBRARY SCREEN
// ==========================================
@Composable
fun LibraryScreen(viewModel: MangaViewModel) {
    val allMangaList by viewModel.allMangaList.collectAsState()
    val favoriteList by viewModel.favoriteMangaList.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val displayedManga = if (selectedTab == 0) allMangaList else favoriteList

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "مكتبتي الملونة • Colorized Library",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("الكل (${allMangaList.size})", fontSize = 14.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("المفضلة (${favoriteList.size})", fontSize = 14.sp) }
            )
        }

        if (displayedManga.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.PhotoFilter,
                        contentDescription = "Empty",
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "لا توجد عناصر لعرضها في هذا التبويب.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(displayedManga) { manga ->
                    MangaGridCard(
                        manga = manga,
                        onOpen = { viewModel.openMangaReader(manga) },
                        onFavoriteToggle = { viewModel.toggleFavorite(manga.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun MangaGridCard(
    manga: ColoredManga,
    onOpen: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (manga.coverUrl.isNotEmpty()) {
                    AsyncImage(
                        model = if (manga.coverUrl.startsWith("http")) manga.coverUrl else File(manga.coverUrl),
                        contentDescription = "Cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Filled.Book,
                        contentDescription = "No Cover",
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                }

                // Favorite Star Icon Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable { onFavoriteToggle() }
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = if (manga.isFavorite) Icons.Filled.Star else Icons.Filled.StarOutline,
                        contentDescription = "Fav",
                        modifier = Modifier.size(18.dp),
                        tint = if (manga.isFavorite) Color.Yellow else Color.White
                    )
                }

                // Source Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SlateButtonContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = manga.source, 
                        fontSize = 9.sp, 
                        color = MaterialTheme.colorScheme.primary, 
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = manga.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${manga.pageCount} صفحة ملونة | Pages",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ==========================================
// 3. IMPORT ONLINE (MANGADEX SCREEN & FEED)
// ==========================================
@Composable
fun ImportDexScreen(viewModel: MangaViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResult by viewModel.searchResult.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val selectedManga by viewModel.selectedManga.collectAsState()
    val chaptersList by viewModel.chaptersList.collectAsState()
    val isLoadingChapters by viewModel.isLoadingChapters.collectAsState()

    var showChaptersDialog by remember { mutableStateOf(false) }

    // Synchronize Dialog state with Selected Manga
    LaunchedEffect(selectedManga) {
        showChaptersDialog = selectedManga != null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "بحث عن مــانـغـا • MangaDex Cloud Import",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.searchManga(it) },
                placeholder = { Text("ادخل اسم المانغا (مثل Blue Lock, Naruto)") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_manga_input"),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (searchResult.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "Search placeholder",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "ابدأ البحث بالإنجليزية لتوسيع قاعدة نتائج المانغا ...",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(searchResult) { manga ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectMangaDex(manga) },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(14.dp)
                                .fillMaxWidth()
                        ) {
                            AsyncImage(
                                model = MangaDexClient.getCoverUrl(manga),
                                contentDescription = "Manga Cover",
                                modifier = Modifier
                                    .size(68.dp, 94.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.DarkGray)
                                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = manga.attributes.title["en"] ?: (manga.attributes.title.values.firstOrNull() ?: "Unknown Name"),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = manga.attributes.description?.get("en") ?: "لا يوجد وصف متوفر لهذا العمل.",
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(SlateButtonContainer)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "الحالة: ${manga.attributes.status ?: "غير محدد"}", 
                                        fontSize = 10.sp, 
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Chapters Feed Dialog
    if (showChaptersDialog && selectedManga != null) {
        val manga = selectedManga!!
        AlertDialog(
            onDismissRequest = { viewModel.selectMangaDex(manga) /* Toggle or clear state */ },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.searchManga(searchQuery) }) {
                    Text("إغلاق • Close", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = "تنزيل وتلوين الفصول للعمل:\n${manga.attributes.title["en"] ?: "Manga"}", 
                    fontSize = 16.sp, 
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                ) {
                    if (isLoadingChapters) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary)
                    } else if (chaptersList.isEmpty()) {
                        Text("لا توجد فصول مترجمة متوفرة باللغات المحددة [AR, EN]", modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(chaptersList) { chapter ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(12.dp)
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                            Text(
                                                "الفصل • Chapter ${chapter.attributes.chapter ?: "1"}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                "العنوان: ${chapter.attributes.title ?: "بدون عنوان"}\nالصفحات: ${chapter.attributes.pages} | اللغة: ${chapter.attributes.translatedLanguage}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                lineHeight = 15.sp
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.downloadMangaDexChapter(manga, chapter)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = DarkTextBlue
                                            ),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.AutoAwesome, 
                                                contentDescription = "colorize", 
                                                modifier = Modifier.size(14.dp),
                                                tint = DarkTextBlue
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("تلوين", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

// ==========================================
// 4. SETTINGS SCREEN
// ==========================================
@Composable
fun SettingsScreen(viewModel: MangaViewModel) {
    val isUltraMode by viewModel.isUltraAccuracy.collectAsState()
    val exportQuality by viewModel.exportQuality.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "إعدادات تلوين المحرك • Setup AI Engine",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black
        )

        // Accuracy Toggle Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "الترميم الدقيق للوجوه وهياكل الملابس • Ultra Accuracy Mode",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "يقوم بإنشاء قناع تتبع دقيق (Masking Segment) لملامح الأنمي ومنع تسرب الألوان خارج خطوط الرسم الأصلي.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                    Switch(
                        checked = isUltraMode,
                        onCheckedChange = { viewModel.setUltraAccuracy(it) },
                        modifier = Modifier.testTag("switch_ultra_mode")
                    )
                }
            }
        }

        // Quality Select Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "دقة تلوين وتصدير المستند النهائي • Max Resolution Mode",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))

                val qualityOptions = listOf("1080p", "2K", "4K")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    qualityOptions.forEach { opt ->
                        val selected = exportQuality == opt
                        Button(
                            onClick = { viewModel.setExportQuality(opt) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primary else SlateButtonContainer,
                                contentColor = if (selected) DarkTextBlue else MaterialTheme.colorScheme.onSurface
                            ),
                            border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Text(opt, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Server diagnostics
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "📊 تشخيص الأداء الذكي • Performance Diagnostics",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Text("• مُعالج الرسوميات المتصل: RTX GPU (CUDA Core Acceleration Active)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("• محاكي نموذج التعرف الحسي: Florence-2-L + CLIP Vision Multi-Agent", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("• حجم المذكرة التخزينية الموقتة للإنبات: Qwen Cache 128MB System Active", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("• حالة استجابة مفتاح Gemini API: مؤمن ومتصل محلياً عبر BuildConfig", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ==========================================
// 5. PROCESSING TERMINAL SCREEN
// ==========================================
@Composable
fun ProcessingTerminalScreen(viewModel: MangaViewModel) {
    val logs by viewModel.pipelineLogs.collectAsState()
    val isActive by viewModel.isProcessingActive.collectAsState()
    val scrollState = rememberScrollState()

    // Autoscroll to bottom when new logs arrive
    LaunchedEffect(logs) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active visual progress indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "أنبوب تلوين المانغا الذكي • AI Color Pipeline",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    if (isActive) "جاري الاستدلال وتجميع الطبقات الملونة..." else "اكتملت عملية المعالجة وتجميع ملفات الفصل!",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isActive) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                Icon(
                    Icons.Filled.Verified,
                    contentDescription = "Success",
                    modifier = Modifier.size(32.dp),
                    tint = Color.Green
                )
            }
        }

        // Output Terminal Window
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = logs,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color.Green,
                    lineHeight = 16.sp
                )
            }
        }

        // Actions Bottom Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.navigateTo(Screen.Home) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SlateButtonContainer)
            ) {
                Text("العودة للرئيسية • Home", color = SlateButtonContent, fontWeight = FontWeight.Bold)
            }

            if (!isActive) {
                Button(
                    onClick = { viewModel.navigateTo(Screen.Library) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = DarkTextBlue
                    )
                ) {
                    Text("فتح المعارض والمكتبة • Open", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// 6. MANGA READER (BEFORE-AFTER SLIDER UI!)
// ==========================================
@Composable
fun MangaReaderScreen(viewModel: MangaViewModel) {
    val context = LocalContext.current
    val manga by viewModel.activeManga.collectAsState()
    val pages by viewModel.activePages.collectAsState()
    val isLoading by viewModel.isLoadingPages.collectAsState()

    var currentPageIndex by remember { mutableStateOf(0) }
    var splitFraction by remember { mutableStateOf(0.5f) } // Before-After Split fraction (0.0 to 1.0)

    // Reset current index when manga changes
    LaunchedEffect(manga) {
        currentPageIndex = 0
        splitFraction = 0.5f
    }

    if (manga == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Custom elegant Reader header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.navigateTo(Screen.Library) }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        manga!!.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "الصفحة • Page ${currentPageIndex + 1} من ${pages.size}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row {
                // PDF Export & Share Button
                IconButton(
                    onClick = {
                        val path = manga!!.localPdfPath
                        if (!path.isNullOrBlank()) {
                            val file = File(path)
                            if (file.exists()) {
                                try {
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        file
                                    )
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "تصدير ومشاركة الفصل PDF"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "فشل المشاركة: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "PDF الملون غير متوفر لهذه التشكيلة.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "يتم إنشاء ملف PDF بعد اكتمال تلوين كل الصور.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(Icons.Filled.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                }

                // Favorite Toggle
                IconButton(onClick = { viewModel.toggleFavorite(manga!!.id) }) {
                    Icon(
                        imageVector = if (manga!!.isFavorite) Icons.Filled.Star else Icons.Filled.StarOutline,
                        contentDescription = "Favorite",
                        tint = if (manga!!.isFavorite) Color.Yellow else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (isLoading || pages.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val page = pages[currentPageIndex]

            // Main display area with stacked before/after interactive clipping!
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black)
            ) {
                // Layer 1: Raw Original B&W (drawn as base stack)
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = File(page.originalImagePath).takeIf { File(page.originalImagePath).exists() } ?: page.originalImagePath,
                        contentDescription = "Original BW Drawing",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                // Layer 2: Colored image masked by SplitClipShape(fraction)
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = File(page.colorizedImagePath).takeIf { File(page.colorizedImagePath).exists() } ?: page.colorizedImagePath,
                        contentDescription = "Colored Image Layer",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(SplitClipShape(splitFraction)),
                        contentScale = ContentScale.Fit
                    )
                }

                // Visual labels left & right
                Text(
                    "خط الرسم الأصلي (B&W)",
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )

                Text(
                    "تلوين الأنمي الذكي (AI COLORED)",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Scrubbing control & layout guidance
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "اسحب الشريط للمقارنة بين الرسم الأصلي وتلوين الأنمي الذكي:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Slider(
                    value = splitFraction,
                    onValueChange = { splitFraction = it },
                    modifier = Modifier.fillMaxWidth().testTag("split_slider")
                )

                // Bottom Page Controllers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { if (currentPageIndex > 0) currentPageIndex-- },
                        enabled = currentPageIndex > 0,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SlateButtonContainer,
                            contentColor = SlateButtonContent
                        )
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Prev", tint = SlateButtonContent)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("الصفحة السابقة", fontWeight = FontWeight.Bold)
                    }

                    Text(
                        "${currentPageIndex + 1} / ${pages.size}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Button(
                        onClick = { if (currentPageIndex < pages.size - 1) currentPageIndex++ },
                        enabled = currentPageIndex < pages.size - 1,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = DarkTextBlue
                        )
                    ) {
                        Text("الصفحة التالية", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Filled.ArrowForward, contentDescription = "Next", tint = DarkTextBlue)
                    }
                }
            }
        }
    }
}
