package app.horae.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.Upsert
import app.horae.domain.Codec
import app.horae.domain.DayRule
import app.horae.domain.Done
import app.horae.domain.Entry
import app.horae.domain.Feast
import app.horae.domain.Gloss
import app.horae.domain.Hour
import app.horae.domain.Kept
import app.horae.domain.MetaRow
import app.horae.domain.Season
import app.horae.domain.Step

class Converters {
    @TypeConverter fun steps(s: List<Step>): String = Codec.steps(s)
    @TypeConverter fun steps(s: String): List<Step> = Codec.steps(s)
    @TypeConverter fun done(d: List<Done>): String = Codec.done(d)
    @TypeConverter fun done(s: String): List<Done> = Codec.done(s)
}

@Dao
interface HoraeDao {
    @Query("SELECT * FROM hour ORDER BY ord, id") suspend fun hours(): List<Hour>
    @Query("SELECT * FROM entry ORDER BY at") suspend fun entries(): List<Entry>
    @Query("SELECT * FROM gloss ORDER BY at") suspend fun glosses(): List<Gloss>
    @Query("SELECT * FROM feast ORDER BY month, dom") suspend fun feasts(): List<Feast>
    @Query("SELECT * FROM season ORDER BY start") suspend fun seasons(): List<Season>
    @Query("SELECT * FROM rule ORDER BY id") suspend fun rules(): List<DayRule>
    @Query("SELECT * FROM kept") suspend fun kept(): List<Kept>
    @Query("SELECT * FROM meta") suspend fun meta(): List<MetaRow>

    @Insert suspend fun insert(h: Hour): Long
    @Insert suspend fun insertHours(h: List<Hour>)
    @Update suspend fun update(h: Hour)
    @Update suspend fun updateHours(h: List<Hour>)
    @Delete suspend fun delete(h: Hour)
    @Query("DELETE FROM hour") suspend fun clearHours()

    @Insert suspend fun insert(e: Entry): Long
    @Insert suspend fun insertEntries(e: List<Entry>)
    @Insert suspend fun insert(g: Gloss): Long
    @Insert suspend fun insertGlosses(g: List<Gloss>)

    @Upsert suspend fun upsert(f: Feast)
    @Delete suspend fun delete(f: Feast)
    @Upsert suspend fun upsert(s: Season)
    @Delete suspend fun delete(s: Season)
    @Upsert suspend fun upsert(r: DayRule)
    @Delete suspend fun delete(r: DayRule)
    @Query("DELETE FROM kept WHERE rule = :rule") suspend fun forgetRule(rule: Long)
    @Insert suspend fun keep(k: Kept)
    @Insert suspend fun keepAll(k: List<Kept>)
    @Delete suspend fun unkeep(k: Kept)
    @Upsert suspend fun meta(m: List<MetaRow>)
}

@Database(entities = [Hour::class, Entry::class, Gloss::class, Feast::class, Season::class, DayRule::class, Kept::class, MetaRow::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class HoraeDb : RoomDatabase() {
    abstract fun dao(): HoraeDao

    companion object {
        fun create(c: Context): HoraeDb = Room.databaseBuilder(c, HoraeDb::class.java, "horae.db").build()
        fun memory(c: Context): HoraeDb = Room.inMemoryDatabaseBuilder(c, HoraeDb::class.java).build()
    }
}
