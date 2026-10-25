package app.horae.data

import androidx.room.withTransaction
import app.horae.domain.DayRule
import app.horae.domain.Entry
import app.horae.domain.Feast
import app.horae.domain.Gloss
import app.horae.domain.Hour
import app.horae.domain.Kept
import app.horae.domain.MetaRow
import app.horae.domain.Preset
import app.horae.domain.Presets
import app.horae.domain.Season
import java.time.LocalDate
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The whole book at one moment; small enough to keep in memory. */
class State(
    val hours: List<Hour>,
    entries: List<Entry>,
    glosses: List<Gloss>,
    val feasts: List<Feast>,
    val seasons: List<Season>,
    val rules: List<DayRule>,
    kept: List<Kept>,
    val meta: Map<String, String>,
) {
    val entries: Map<Long, List<Entry>> = entries.groupBy { it.day }
    val glosses: Map<Long, List<Gloss>> = glosses.groupBy { it.day }
    private val kept: Set<Kept> = kept.toSet()
    val salt: Long = meta[SALT]?.toLongOrNull() ?: 0
    val firstDay: Long? = this.entries.keys.minOrNull()

    fun on(day: Long): List<Entry> = entries[day].orEmpty()
    fun kept(day: Long, rule: Long) = Kept(day, rule) in kept
    fun keptRules(day: Long) = rules.count { kept(day, it.id) }
    fun feast(date: LocalDate): Feast? = feasts.firstOrNull { it.month == date.monthValue && it.dom == date.dayOfMonth && (it.since == 0 || date.year >= it.since) }
    fun season(day: Long): Season? = seasons.lastOrNull { day in it }
    fun silenceBroken(day: Long) = meta["$BROKEN$day"] != null

    companion object {
        const val SALT = "salt"
        const val BROKEN = "broken:"
    }
}

class Repo(private val db: HoraeDb) {
    private val dao = db.dao()
    private val lock = Mutex()
    private val _state = MutableStateFlow<State?>(null)
    val state: StateFlow<State?> = _state

    suspend fun refresh(): State = lock.withLock { load() }

    private suspend fun load(): State {
        var meta = dao.meta().associate { it.k to it.v }
        if (meta[State.SALT] == null) {
            dao.meta(listOf(MetaRow(State.SALT, Random.nextLong().toString())))
            meta = dao.meta().associate { it.k to it.v }
        }
        val s = State(dao.hours(), dao.entries(), dao.glosses(), dao.feasts(), dao.seasons(), dao.rules(), dao.kept(), meta)
        _state.value = s
        return s
    }

    private suspend fun edit(block: suspend () -> Unit): State = lock.withLock { db.withTransaction { block() }; load() }

    suspend fun preset(p: Preset, ru: Boolean) = edit { dao.clearHours(); dao.insertHours(Presets.hours(p, ru)) }

    suspend fun save(h: Hour): Long {
        var id = h.id
        edit { if (h.id == 0L) id = dao.insert(h.copy(ord = (dao.hours().maxOfOrNull { it.ord } ?: -1) + 1)) else dao.update(h) }
        return id
    }

    suspend fun delete(h: Hour) = edit { dao.delete(h) }

    /** Moves an hour up or down the list. */
    suspend fun move(h: Hour, by: Int) = edit {
        val list = dao.hours().toMutableList()
        val i = list.indexOfFirst { it.id == h.id }
        val j = i + by
        if (i < 0 || j !in list.indices) return@edit
        list.add(j, list.removeAt(i))
        dao.updateHours(list.mapIndexed { k, x -> x.copy(ord = k) })
    }

    suspend fun keep(e: Entry) = edit { dao.insert(e) }
    suspend fun gloss(g: Gloss) = edit { dao.insert(g) }

    suspend fun save(f: Feast) = edit { dao.upsert(f) }
    suspend fun delete(f: Feast) = edit { dao.delete(f) }
    suspend fun save(s: Season) = edit { dao.upsert(s) }
    suspend fun delete(s: Season) = edit { dao.delete(s) }
    suspend fun save(r: DayRule) = edit { dao.upsert(r) }
    suspend fun delete(r: DayRule) = edit { dao.forgetRule(r.id); dao.delete(r) }

    suspend fun toggle(day: Long, rule: Long) = edit {
        val k = Kept(day, rule)
        if (dao.kept().contains(k)) dao.unkeep(k) else dao.keep(k)
    }

    suspend fun breakSilence(day: Long) = edit { dao.meta(listOf(MetaRow("${State.BROKEN}$day", "1"))) }

    /** Fills an empty book in one go (the demo). */
    suspend fun seed(entries: List<Entry>, glosses: List<Gloss>, kept: List<Kept>) = edit {
        dao.insertEntries(entries); dao.insertGlosses(glosses); dao.keepAll(kept)
    }
}
