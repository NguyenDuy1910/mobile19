package com.example.minlish

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        VocabularyEntity::class,
        DeckEntity::class,
        StudySessionEntity::class
    ],
    version = 5
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun vocabularyDao(): VocabularyDao
    abstract fun deckDao(): DeckDao
    abstract fun studySessionDao(): StudySessionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vocabulary_table ADD COLUMN pronunciation TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE vocabulary_table ADD COLUMN descriptionEn TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE vocabulary_table ADD COLUMN example TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE vocabulary_table ADD COLUMN collocation TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE vocabulary_table ADD COLUMN relatedWords TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE vocabulary_table ADD COLUMN note TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE vocabulary_table ADD COLUMN deckId INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS deck_table (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        tags TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS study_session_table (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        vocabularyId INTEGER NOT NULL,
                        result TEXT NOT NULL,
                        reviewedAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Không có thay đổi schema
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "minlish_database"
                )
                    // Thêm dòng này vào để Room tự động xóa db cũ khi cấu trúc thay đổi
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}