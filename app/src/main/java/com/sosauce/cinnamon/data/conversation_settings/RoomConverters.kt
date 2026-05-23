package com.sosauce.cinnamon.data.conversation_settings

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json

class RoomConverters {

    @TypeConverter
    fun convertListToString(list: List<String>): String {
        return Json.encodeToString(list)
    }

    @TypeConverter
    fun convertStringToList(string: String): List<String> {
        return Json.decodeFromString(string)
    }

    @TypeConverter
    fun convertListIntToString(list: List<Int>): String {
        return Json.encodeToString(list)
    }

    @TypeConverter
    fun convertStringToListInt(string: String): List<Int> {
        return Json.decodeFromString(string)
    }
}