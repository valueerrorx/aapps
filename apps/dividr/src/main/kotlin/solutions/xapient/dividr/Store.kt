package solutions.xapient.dividr

import android.content.Context
import androidx.core.content.edit
import solutions.xapient.dividr.domain.CodecException
import solutions.xapient.dividr.domain.Group
import solutions.xapient.dividr.domain.Ids
import solutions.xapient.dividr.domain.NewerFormatException
import solutions.xapient.dividr.domain.WireCodec
import java.io.File
import java.security.SecureRandom

// Eine Datei je Gruppe (WireCodec, unkomprimiert), lokale Angaben in SharedPreferences; Backup ist aus
class Store(context: Context, private val dir: File = File(context.filesDir, "groups")) {
    private val sp = context.getSharedPreferences("dividr", Context.MODE_PRIVATE)
    private val random = SecureRandom()

    // Zufaellige Geraete-ID, einmal je Installation, nie 0
    val device: Int = sp.getInt("device", 0).takeIf { it != 0 } ?: run {
        var d = 0
        while (d == 0) d = random.nextInt()
        sp.edit(commit = true) { putInt("device", d) }
        d
    }

    fun newId(): Long {
        val seq = sp.getInt("seq", 0) + 1
        sp.edit(commit = true) { putInt("seq", seq) }
        return Ids.of(device, seq)
    }

    fun newGroupId(): Long {
        var id = 0L
        while (id == 0L) id = random.nextLong()
        return id
    }

    fun keyFor(g: Group): String = java.lang.Long.toHexString(g.ids.min())

    fun loadAll(): Map<String, Group> {
        val files = dir.listFiles { f -> f.name.endsWith(EXT) } ?: return emptyMap()
        val keys = files.map { it.name.removeSuffix(EXT) }.toSet() +
            (dir.listFiles { f -> f.name.endsWith(BAK) }?.map { it.name.removeSuffix(BAK) } ?: emptyList())
        return keys.mapNotNull { k -> load(k)?.let { k to it } }.toMap()
    }

    // Kaputte Datei: letzte gueltige Sicherung
    fun load(key: String): Group? = read(File(dir, key + EXT)) ?: read(File(dir, key + BAK))

    private fun read(f: File): Group? = try {
        if (f.isFile) WireCodec.decode(f.readBytes()) else null
    } catch (_: CodecException) {
        null
    } catch (_: NewerFormatException) {
        null
    } catch (_: java.io.IOException) {
        null
    }

    // Atomar: Temp-Datei, alte Fassung wird Sicherung, dann umbenennen
    fun save(key: String, g: Group) {
        dir.mkdirs()
        val tmp = File(dir, key + TMP)
        val cur = File(dir, key + EXT)
        val bak = File(dir, key + BAK)
        tmp.writeBytes(WireCodec.encode(g))
        if (cur.exists()) {
            bak.delete()
            cur.renameTo(bak)
        }
        if (!tmp.renameTo(cur)) {
            cur.delete()
            tmp.renameTo(cur)
        }
    }

    fun delete(key: String) {
        listOf(EXT, BAK, TMP).forEach { File(dir, key + it).delete() }
        sp.edit { remove("me_$key"); if (lastKey == key) remove("last") }
    }

    var lastKey: String?
        get() = sp.getString("last", null)
        set(v) = sp.edit { if (v == null) remove("last") else putString("last", v) }

    fun me(key: String): Long? = if (sp.contains("me_$key")) sp.getLong("me_$key", 0) else null

    fun setMe(key: String, id: Long?) = sp.edit { if (id == null) remove("me_$key") else putLong("me_$key", id) }

    fun lastRate(currency: String): Long? = sp.getLong("rate_$currency", 0).takeIf { it > 0 }

    fun setLastRate(currency: String, micros: Long) = sp.edit { putLong("rate_$currency", micros) }

    var haptics: Boolean
        get() = sp.getBoolean("haptics", true)
        set(v) = sp.edit { putBoolean("haptics", v) }

    var cameraBlocked: Boolean
        get() = sp.getBoolean("cameraBlocked", false)
        set(v) = sp.edit { putBoolean("cameraBlocked", v) }

    private companion object {
        const val EXT = ".dvr"
        const val BAK = ".bak"
        const val TMP = ".tmp"
    }
}
