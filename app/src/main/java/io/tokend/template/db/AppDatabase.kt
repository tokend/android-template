package io.tokend.template.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import io.tokend.template.features.assets.model.Asset
import io.tokend.template.features.assets.model.AssetDbEntity
import io.tokend.template.features.assets.model.SimpleAsset
import io.tokend.template.features.assets.storage.AssetsDao
import io.tokend.template.features.balances.model.BalanceDbEntity
import io.tokend.template.features.balances.storage.BalancesDao
import org.tokend.sdk.utils.BigDecimalUtil
import java.math.BigDecimal
import java.util.*

@Database(
    entities = [
        AssetDbEntity::class,
        BalanceDbEntity::class,
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(AppDatabase.Converters::class)
abstract class AppDatabase : RoomDatabase() {
    class Converters {
        private val gsonWithoutNulls = Gson()

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

        @TypeConverter
        fun assetFromJson(value: String?): Asset? {
            return value?.let { gsonWithoutNulls.fromJson(value, SimpleAsset::class.java) }
        }

        @TypeConverter
        fun assetToJson(value: Asset?): String? {
            return value?.let { gsonWithoutNulls.toJson(SimpleAsset(it)) }
        }
    }

    abstract val balances: BalancesDao
    abstract val assets: AssetsDao

    companion object {
        val MIGRATIONS: Array<Migration> = arrayOf(
            object : Migration(3, 4) {
                override fun migrate(database: SupportSQLiteDatabase) = database.run {
                    execSQL("DROP TABLE balance")
                    execSQL("CREATE TABLE IF NOT EXISTS `balance` (`id` TEXT NOT NULL, `asset_code` TEXT NOT NULL, `available` TEXT NOT NULL, `converted_amount` TEXT, `conversion_price` TEXT, `conversion_asset` TEXT, PRIMARY KEY(`id`))")
                    execSQL("CREATE  INDEX `index_balance_asset_code` ON `balance` (`asset_code`)")
                }
            },
            object : Migration(4, 5) {
                override fun migrate(database: SupportSQLiteDatabase) = database.run {
                    execSQL("DROP TABLE company")
                }
            },
            object : Migration(5,6) {
                override fun migrate(database: SupportSQLiteDatabase) = database.run{
                    execSQL("DROP TABLE balance_change")
                }
            }
        )
    }
}