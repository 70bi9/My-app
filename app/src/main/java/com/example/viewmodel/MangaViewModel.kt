package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.*
import com.example.data.models.ColoredManga
import com.example.data.models.MangaPageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream

class MangaViewModel(application: Application) : AndroidViewModel(application) {

    // Database Setup
    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            application.applicationContext,
            AppDatabase::class.java,
            "manga_color_ai_db"
        ).fallbackToDestructiveMigration().build()
    }

    private val repository: MangaRepository by lazy {
        MangaRepository(application.applicationContext, database)
    }

    // --- UI Navigation State ---
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // --- Settings States ---
    private val _isUltraAccuracy = MutableStateFlow(true)
    val isUltraAccuracy: StateFlow<Boolean> = _isUltraAccuracy.asStateFlow()

    private val _exportQuality = MutableStateFlow("2K") // "1080p", "2K", "4K"
    val exportQuality: StateFlow<String> = _exportQuality.asStateFlow()

    // --- DB Flow Data ---
    val allMangaList: StateFlow<List<ColoredManga>> = repository.mangaList
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteMangaList: StateFlow<List<ColoredManga>> = repository.favoriteMangaList
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Processing Pipeline States ---
    private val _pipelineLogs = MutableStateFlow("")
    val pipelineLogs: StateFlow<String> = _pipelineLogs.asStateFlow()

    private val _isProcessingActive = MutableStateFlow(false)
    val isProcessingActive: StateFlow<Boolean> = _isProcessingActive.asStateFlow()

    private val _processingMangaId = MutableStateFlow<Long?>(null)
    val processingMangaId: StateFlow<Long?> = _processingMangaId.asStateFlow()

    // --- MangaDex Search States ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResult = MutableStateFlow<List<MangaData>>(emptyList())
    val searchResult: StateFlow<List<MangaData>> = _searchResult.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _selectedManga = MutableStateFlow<MangaData?>(null)
    val selectedManga: StateFlow<MangaData?> = _selectedManga.asStateFlow()

    private val _chaptersList = MutableStateFlow<List<ChapterData>>(emptyList())
    val chaptersList: StateFlow<List<ChapterData>> = _chaptersList.asStateFlow()

    private val _isLoadingChapters = MutableStateFlow(false)
    val isLoadingChapters: StateFlow<Boolean> = _isLoadingChapters.asStateFlow()

    // --- Active Reader Details ---
    private val _activeManga = MutableStateFlow<ColoredManga?>(null)
    val activeManga: StateFlow<ColoredManga?> = _activeManga.asStateFlow()

    private val _activePages = MutableStateFlow<List<MangaPageEntity>>(emptyList())
    val activePages: StateFlow<List<MangaPageEntity>> = _activePages.asStateFlow()

    private val _isLoadingPages = MutableStateFlow(false)
    val isLoadingPages: StateFlow<Boolean> = _isLoadingPages.asStateFlow()

    // --- Functions ---

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun setUltraAccuracy(enabled: Boolean) {
        _isUltraAccuracy.value = enabled
    }

    fun setExportQuality(quality: String) {
        _exportQuality.value = quality
    }

    fun toggleFavorite(mangaId: Long) {
        viewModelScope.launch {
            repository.toggleFavorite(mangaId)
        }
    }

    fun deleteManga(mangaId: Long) {
        viewModelScope.launch {
            repository.deleteManga(mangaId)
            if (_activeManga.value?.id == mangaId) {
                _activeManga.value = null
                _activePages.value = emptyList()
                _currentScreen.value = Screen.Home
            }
        }
    }

    // Upload / Import PDF Action
    fun uploadPdf(context: Context, uri: Uri, title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val mangaId = repository.importMangaPdf(uri.toString(), inputStream, title)
                    launchColorization(mangaId)
                }
            } catch (e: Exception) {
                Log.e("MangaViewModel", "Error uploading PDF: ${e.message}")
            }
        }
    }

    // Upload Images Action
    fun uploadImages(title: String, imageList: List<Pair<String, InputStream>>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val mangaId = repository.importMangaImages(title, imageList)
                launchColorization(mangaId)
            } catch (e: Exception) {
                Log.e("MangaViewModel", "Error uploading Images: ${e.message}")
            }
        }
    }

    // Trigger Colorization Pipeline
    fun launchColorization(mangaId: Long) {
        _processingMangaId.value = mangaId
        _isProcessingActive.value = true
        _pipelineLogs.value = ""
        _currentScreen.value = Screen.ProcessingTerminal

        viewModelScope.launch(Dispatchers.IO) {
            repository.runColorizationPipeline(
                mangaId = mangaId,
                isUltraAccuracy = _isUltraAccuracy.value,
                resolutionMode = _exportQuality.value,
                onLogsUpdated = { newLog ->
                    _pipelineLogs.update { it + newLog }
                }
            )
            _isProcessingActive.value = false
        }
    }

    // MangaDex Search API Trigger
    fun searchManga(query: String) {
        _searchQuery.value = query
        if (query.trim().isEmpty()) {
            _searchResult.value = emptyList()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isSearching.value = true
            try {
                val response = MangaDexClient.service.searchManga(title = query)
                _searchResult.value = response.data
            } catch (e: Exception) {
                Log.e("MangaViewModel", "MangaDex Search error: ${e.message}")
                _searchResult.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    // Select Manga from Dex to view Chapters
    fun selectMangaDex(manga: MangaData) {
        _selectedManga.value = manga
        _chaptersList.value = emptyList()
        _isLoadingChapters.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = MangaDexClient.service.getMangaFeed(id = manga.id)
                _chaptersList.value = response.data
            } catch (e: Exception) {
                Log.e("MangaViewModel", "MangaDex Feed error: ${e.message}")
                _chaptersList.value = emptyList()
            } finally {
                _isLoadingChapters.value = false
            }
        }
    }

    // Download/Import and Colorize specific MangaDex Chapter
    fun downloadMangaDexChapter(manga: MangaData, chapter: ChapterData) {
        onMangaDexChapterDownload(manga, chapter)
    }

    private fun onMangaDexChapterDownload(manga: MangaData, chapter: ChapterData) {
        _isProcessingActive.value = true
        _pipelineLogs.value = "📥 بدء استيراد الفصل من سيرفرات MangaDex API...\n"
        _currentScreen.value = Screen.ProcessingTerminal

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = MangaDexClient.service.getAtHomeServer(chapter.id)
                val serverBaseUrl = response.baseUrl
                val hash = response.chapter.hash
                val pagesFilenames = response.chapter.dataSaver.takeIf { it.isNotEmpty() } ?: response.chapter.data

                _pipelineLogs.update { it + "📡 تم الاتصال! جاري تنزيل ${pagesFilenames.size} صفحة مانغا...\n" }

                // Create local DB item
                val coverUrl = MangaDexClient.getCoverUrl(manga)
                val title = "${manga.attributes.title["en"] ?: "Manga"} - Ch.${chapter.attributes.chapter ?: "1"}"
                
                val dbMangaId = repository.insertManga(
                    ColoredManga(
                        title = title,
                        source = "MANGADEX",
                        chapterNumber = chapter.attributes.chapter ?: "1",
                        status = "PROCESSING",
                        progress = 10,
                        pageCount = pagesFilenames.size,
                        coverUrl = coverUrl
                    )
                )

                // Save original pages with remote URL or downloaded cached paths
                val mangaDir = File(getApplication<Application>().cacheDir, "manga_dex_${dbMangaId}")
                mangaDir.mkdirs()

                val pageEntities = mutableListOf<MangaPageEntity>()
                pagesFilenames.forEachIndexed { index, fileName ->
                    val fullUrl = "$serverBaseUrl/data-saver/$hash/$fileName".takeIf { response.chapter.dataSaver.isNotEmpty() }
                        ?: "$serverBaseUrl/data/$hash/$fileName"

                    val pathOriginal = File(mangaDir, "orig_${index}.jpg")
                    val pathColored = File(mangaDir, "color_${index}.jpg")

                    // Download image to local file so we can colorize it and base64 it!
                    try {
                        val client = okhttp3.OkHttpClient()
                        val req = okhttp3.Request.Builder().url(fullUrl).build()
                        client.newCall(req).execute().use { res ->
                            res.body?.byteStream()?.use { stream ->
                                java.io.FileOutputStream(pathOriginal).use { out ->
                                    stream.copyTo(out)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MangaViewModel", "Failed downloading page image: ${e.message}")
                    }

                    pageEntities.add(
                        MangaPageEntity(
                            mangaId = dbMangaId,
                            pageIndex = index,
                            originalImagePath = pathOriginal.absolutePath,
                            colorizedImagePath = pathColored.absolutePath
                        )
                    )
                }

                _pipelineLogs.update { it + "✅ تم تنزيل وحفظ كل الصفحات محلياً.\n" }
                // Insert pages to database
                database.mangaDao().insertPages(pageEntities)

                // Now launch standard colorization pipeline!
                repository.runColorizationPipeline(
                    mangaId = dbMangaId,
                    isUltraAccuracy = _isUltraAccuracy.value,
                    resolutionMode = _exportQuality.value,
                    onLogsUpdated = { newLog ->
                        _pipelineLogs.update { it + newLog }
                    }
                )
            } catch (e: Exception) {
                _pipelineLogs.update { it + "❌ فشل تحميل الفصل: ${e.localizedMessage}\n" }
                Log.e("MangaViewModel", "Failed download & colorize: ${e.message}")
            } finally {
                _isProcessingActive.value = false
            }
        }
    }

    // Open reader view for selected item from Library
    fun openMangaReader(manga: ColoredManga) {
        _activeManga.value = manga
        _isLoadingPages.value = true
        _currentScreen.value = Screen.MangaReader

        viewModelScope.launch {
            repository.getPages(manga.id).collectLatest { pagesList ->
                _activePages.value = pagesList
                _isLoadingPages.value = false
            }
        }
    }
}

// Represent Different Screens in stateful navigation
sealed class Screen {
    object Home : Screen()
    object Library : Screen()
    object ImportDex : Screen()
    object Settings : Screen()
    object ProcessingTerminal : Screen()
    object MangaReader : Screen()
}
