package io.tokend.template.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import org.tokend.sdk.utils.BigDecimalUtil
import java.math.BigDecimal
import java.util.*

@Database(
    entities = [
        DummyEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(AppDatabase.Converters::class)
abstract class AppDatabase : RoomDatabase() {
    class Converters {
        @TypeConverter
        fun dateToUnix(value: Date?): Long? {
            return value?.let { it.time / 1000 }
        }

        @TypeConverter
        fun dateFromUnix(value: Long?): Date? {
            return value?.let { Date(it * 1000) }
        }

        @TypeConverter
        fun bigDecimalToString(value: BigDecimal?): String? {
            return value?.let { BigDecimalUtil.toPlainString(it) }
        }

        @TypeConverter
        fun stringToBigDecimal(value: String?): BigDecimal? {
            return value?.let { BigDecimalUtil.valueOf(it) }
        }
    }

    /**
     * Declare abstract DAOs here
     */

    companion object {
        /**
         * Available migrations to apply.
         * Write a migration if you change DB version and structure.
         */
        val MIGRATIONS: Array<Migration> = arrayOf()
    }
}