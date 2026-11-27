package solutions.xapient.dividr

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.*
import platform.posix.memcpy
import solutions.xapient.dividr.domain.CodecException
import solutions.xapient.dividr.domain.Group
import solutions.xapient.dividr.domain.Ids
import solutions.xapient.dividr.domain.NewerFormatException
import solutions.xapient.dividr.domain.WireCodec
import kotlin.random.Random

// Wie Android: eine Datei je Gruppe (Application Support/groups), alte Fassung als .bak; Rest in den UserDefaults
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosStore : DividrStore {
    private val d = NSUserDefaults.standardUserDefaults
    private val fm = NSFileManager.defaultManager
    private val dir: String = run {
        val base = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).firstOrNull() as? String
            ?: NSTemporaryDirectory()
        "$base/groups"
    }

    private fun k(key: String) = "dividr.$key"
    private fun has(key: String) = d.objectForKey(k(key)) != null
    private fun long(key: String): Long? = if (has(key)) d.integerForKey(k(key)) else null
    private fun putLong(key: String, v: Long) = d.setInteger(v, k(key))
    private fun remove(key: String) = d.removeObjectForKey(k(key))

    // Zufaellige Geraete-ID, einmal je Installation, nie 0
    override val device: Int = long("device")?.toInt()?.takeIf { it != 0 } ?: run {
        var v = 0
        while (v == 0) v = Random.nextInt()
        putLong("device", v.toLong())
        v
    }

    override fun newId(): Long {
        val seq = (long("seq") ?: 0L).toInt() + 1
        putLong("seq", seq.toLong())
        return Ids.of(device, seq)
    }

    override fun newGroupId(): Long {
        var id = 0L
        while (id == 0L) id = Random.nextLong()
        return id
    }

    private fun files(ext: String): List<String> =
        (fm.contentsOfDirectoryAtPath(dir, null) ?: emptyList<Any?>()).filterIsInstance<String>().filter { it.endsWith(ext) }.map { it.removeSuffix(ext) }

    override fun loadAll(): Map<String, Group> {
        val keys = files(EXT).toSet() + files(BAK)
        return keys.mapNotNull { key -> load(key)?.let { key to it } }.toMap()
    }

    // Kaputte Datei: letzte gueltige Sicherung
    private fun load(key: String): Group? = read("$dir/$key$EXT") ?: read("$dir/$key$BAK")

    private fun read(path: String): Group? {
        val data = NSData.dataWithContentsOfFile(path) ?: return null
        val bytes = ByteArray(data.length.toInt())
        if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
        return try {
            WireCodec.decode(bytes)
        } catch (_: CodecException) {
            null
        } catch (_: NewerFormatException) {
            null
        }
    }

    // Atomar schreiben (Temp-Datei + Umbenennen macht writeToFile), vorher alte Fassung als Sicherung
    override fun save(key: String, g: Group) {
        fm.createDirectoryAtPath(dir, true, null, null)
        val bytes = WireCodec.encode(g)
        val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
        val cur = "$dir/$key$EXT"
        val bak = "$dir/$key$BAK"
        if (fm.fileExistsAtPath(cur)) {
            fm.removeItemAtPath(bak, null)
            fm.moveItemAtPath(cur, bak, null)
        }
        data.writeToFile(cur, true)
    }

    override fun delete(key: String) {
        listOf(EXT, BAK).forEach { fm.removeItemAtPath("$dir/$key$it", null) }
        remove("me_$key")
        if (lastKey == key) lastKey = null
    }

    override var lastKey: String?
        get() = d.stringForKey(k("last"))
        set(v) = if (v == null) remove("last") else d.setObject(v, k("last"))

    override fun me(key: String): Long? = long("me_$key")

    override fun setMe(key: String, id: Long?) = if (id == null) remove("me_$key") else putLong("me_$key", id)

    override fun lastRate(currency: String): Long? = long("rate_$currency")?.takeIf { it > 0 }

    override fun setLastRate(currency: String, micros: Long) = putLong("rate_$currency", micros)

    override var haptics: Boolean
        get() = if (has("haptics")) d.boolForKey(k("haptics")) else true
        set(v) = d.setBool(v, k("haptics"))

    // iOS merkt sich die Ablehnung selbst
    override var cameraBlocked: Boolean = false

    private companion object {
        const val EXT = ".dvr"
        const val BAK = ".bak"
    }
}
