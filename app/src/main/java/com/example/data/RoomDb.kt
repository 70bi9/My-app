package com.example.data

import androidx.room.*
import com.example.data.models.ColoredManga
import com.example.data.models.MangaPageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MangaDao {
    @Query("SELECT * FROM colored_manga ORDER BY timestamp DESC")
    fun getMangaList(): Flow<List<ColoredManga>>

    @Query("SELECT * FROM colored_manga WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteMangaList(): Flow<List<ColoredManga>>

    @Query("SELECT * FROM colored_manga WHERE id = :id LIMIT 1")
    suspend fun getMangaById(id: Long): ColoredManga?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertManga(manga: ColoredManga): Long

    @Update
    suspend fun updateManga(manga: ColoredManga)

    @Delete
    suspend fun deleteManga(manga: ColoredManga)

    @Query("DELETE FROM colored_manga WHERE id = :id")
    suspend fun deleteMangaById(id: Long)

    @Query("SELECT * FROM manga_pages WHERE mangaId = :mangaId ORDER BY pageIndex ASC")
    fun getMangaPages(mangaId: Long): Flow<List<MangaPageEntity>>

    @Query("SELECT * FROM manga_pages WHERE mangaId = :mangaId ORDER BY pageIndex ASC")
    suspend fun getPagesByMangaIdDirect(mangaId: Long): List<MangaPageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: MangaPageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<MangaPageEntity>)

    @Query("DELETE FROM manga_pages WHERE mangaId = :mangaId")
    suspend fun deletePagesByMangaId(mangaId: Long)
}

@Database(entities = [ColoredManga::class, MangaPageEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mangaDao(): MangaDao
}
