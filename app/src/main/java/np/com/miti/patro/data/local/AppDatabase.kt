package np.com.miti.patro.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [EventEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun eventDao(): EventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE bs_repeating_events ADD COLUMN colorKey TEXT NOT NULL DEFAULT 'GREEN'"
                )
                db.execSQL(
                    "UPDATE bs_repeating_events SET colorKey = 'RED' WHERE isPersonalHoliday = 1"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE bs_repeating_events ADD COLUMN isShraddha INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE bs_repeating_events ADD COLUMN shraddhaTithi INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "miti_bs_calendar.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
