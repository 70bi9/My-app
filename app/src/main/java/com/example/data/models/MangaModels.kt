package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "colored_manga")
@JsonClass(generateAdapter = true)
data class ColoredManga(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val source: String, // "PDF", "IMAGES", "MANGADEX"
    val chapterNumber: String = "1",
    val status: String, // "PROCESSING", "COMPLETED", "FAILED"
    val progress: Int = 100,
    val pageCount: Int,
    val coverUrl: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val localPdfPath: String? = null
)

@Entity(tableName = "manga_pages")
@JsonClass(generateAdapter = true)
data class MangaPageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mangaId: Long,
    val pageIndex: Int,
    val originalImagePath: String,
    val colorizedImagePath: String,
    val charactersJson: String? = null,
    val width: Int = 1200,
    val height: Int = 1600
)

// Simple dataclass for detected anime characters
@JsonClass(generateAdapter = true)
data class AnimeCharacter(
    val name: String,
    val confidence: Double,
    val hairColor: String,
    val eyeColor: String,
    val clothingHex: String,
    val boundingBox: String? = null
)
