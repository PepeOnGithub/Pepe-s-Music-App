package xyz.pepe.glacierclient.musicapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [PlaylistEntity::class, PlaylistTrackEntity::class, DownloadEntity::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
  abstract fun playlistDao(): PlaylistDao
  abstract fun downloadDao(): DownloadDao

  companion object {
    /** Adds offline downloads without touching existing playlists. */
    val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          "CREATE TABLE IF NOT EXISTS `downloads` (`key` TEXT NOT NULL, `source` TEXT NOT NULL, `sourceId` TEXT NOT NULL, " +
            "`title` TEXT NOT NULL, `artist` TEXT NOT NULL, `artworkUrl` TEXT, `durationMs` INTEGER, `shareUrl` TEXT, " +
            "`fileName` TEXT NOT NULL, `sizeBytes` INTEGER NOT NULL, `downloadedAt` INTEGER NOT NULL, PRIMARY KEY(`key`))",
        )
      }
    }
  }
}
