package dev.coupleapp.lovecounter.data.local
import android.content.Context
import androidx.room.Room
fun openCounterDatabase(context: Context): CounterDatabase = buildCounterDatabase(
    Room.databaseBuilder<CounterDatabase>(context.applicationContext, context.applicationContext.getDatabasePath("couple.db").absolutePath)
)

fun openCounterStore(context: Context): LocalCounterStore = LocalCounterStore(openCounterDatabase(context))
