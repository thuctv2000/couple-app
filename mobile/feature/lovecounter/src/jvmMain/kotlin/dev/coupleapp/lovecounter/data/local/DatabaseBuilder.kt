package dev.coupleapp.lovecounter.data.local
import androidx.room.Room
fun openCounterDatabase(path: String): CounterDatabase = buildCounterDatabase(Room.databaseBuilder<CounterDatabase>(name = path))
