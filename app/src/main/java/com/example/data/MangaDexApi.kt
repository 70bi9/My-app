package com.example.data

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- MangaDex JSON Models ---

@JsonClass(generateAdapter = true)
data class MangaDexSearchResponse(
    val data: List<MangaData>
)

@JsonClass(generateAdapter = true)
data class MangaData(
    val id: String,
    val attributes: MangaAttributes,
    val relationships: List<Relationship>? = null
)

@JsonClass(generateAdapter = true)
data class MangaAttributes(
    val title: Map<String, String>,
    val description: Map<String, String>? = null,
    val status: String? = null
)

@JsonClass(generateAdapter = true)
data class Relationship(
    val id: String,
    val type: String,
    val attributes: CoverAttributes? = null
)

@JsonClass(generateAdapter = true)
data class CoverAttributes(
    val fileName: String? = null
)

@JsonClass(generateAdapter = true)
data class MangaDexFeedResponse(
    val data: List<ChapterData>
)

@JsonClass(generateAdapter = true)
data class ChapterData(
    val id: String,
    val attributes: ChapterAttributes
)

@JsonClass(generateAdapter = true)
data class ChapterAttributes(
    val volume: String? = null,
    val chapter: String? = null,
    val title: String? = null,
    val pages: Int,
    val translatedLanguage: String
)

@JsonClass(generateAdapter = true)
data class MangaDexAtHomeResponse(
    val baseUrl: String,
    val chapter: AtHomeChapter
)

@JsonClass(generateAdapter = true)
data class AtHomeChapter(
    val hash: String,
    val data: List<String>,
    val dataSaver: List<String>
)

// --- Retrofit Interface ---

interface MangaDexService {
    @GET("manga")
    suspend fun searchManga(
        @Query("title") title: String,
        @Query("limit") limit: Int = 15,
        @Query("includes[]") includes: List<String> = listOf("cover_art")
    ): MangaDexSearchResponse

    @GET("manga/{id}/feed")
    suspend fun getMangaFeed(
        @Path("id") id: String,
        @Query("translatedLanguage[]") langs: List<String> = listOf("en", "ar"),
        @Query("limit") limit: Int = 30,
        @Query("order[chapter]") chapterOrder: String = "asc"
    ): MangaDexFeedResponse

    @GET("at-home/server/{chapterId}")
    suspend fun getAtHomeServer(
        @Path("chapterId") chapterId: String
    ): MangaDexAtHomeResponse
}

// --- Retrofit Client & Helpers ---

object MangaDexClient {
    private const val BASE_URL = "https://api.mangadex.org/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    val service: MangaDexService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(MangaDexService::class.java)
    }

    // Helper to get cover image URL for a given MangaData
    fun getCoverUrl(mangaData: MangaData): String {
        val coverId = mangaData.relationships?.firstOrNull { it.type == "cover_art" }?.attributes?.fileName
            ?: mangaData.relationships?.firstOrNull { it.type == "cover_art" }?.id
        
        return if (coverId != null && coverId.contains(".")) {
            "https://uploads.mangadex.org/covers/${mangaData.id}/$coverId"
        } else {
            // Placeholder standard cover
            "https://mangadex.org/avatar.png"
        }
    }
}
