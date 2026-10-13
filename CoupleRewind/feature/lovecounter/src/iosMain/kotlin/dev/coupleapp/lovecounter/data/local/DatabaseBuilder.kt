package dev.coupleapp.lovecounter.data.local
import androidx.room.Room
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*

@OptIn(ExperimentalForeignApi::class)
fun openCounterDatabase(): CounterDatabase {
    val directory = NSFileManager.defaultManager.URLForDirectory(NSApplicationSupportDirectory, NSUserDomainMask, null, true, null)
    val path = requireNotNull(directory?.path) { "Local storage unavailable" } + "/couple.db"
    return buildCounterDatabase(Room.databaseBuilder<CounterDatabase>(name = path))
}

fun openCounterStore(): LocalCounterStore = LocalCounterStore(openCounterDatabase())
