package com.spotzee.android.network

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.spotzee.android.TrackUser

/**
 * TypeAdapterFactory for TrackUser that skips null fields during serialization.
 *
 * This is necessary because the global Gson configuration uses .serializeNulls(),
 * but the backend interprets null as "set field to NULL" rather than "ignore field".
 * We only want to send fields that the user explicitly set.
 */
class TrackUserAdapterFactory : TypeAdapterFactory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (type.rawType != TrackUser::class.java) {
            return null
        }
        return TrackUserAdapter(gson) as TypeAdapter<T>
    }

    private class TrackUserAdapter(private val gson: Gson) : TypeAdapter<TrackUser>() {

        override fun write(out: JsonWriter, value: TrackUser?) {
            if (value == null) {
                out.nullValue()
                return
            }

            out.beginObject()

            value.email?.let {
                out.name("email")
                out.value(it)
            }

            value.phone?.let {
                out.name("phone")
                out.value(it)
            }

            value.timezone?.let {
                out.name("timezone")
                out.value(it)
            }

            value.locale?.let {
                out.name("locale")
                out.value(it)
            }

            value.data?.let { map ->
                out.name("data")
                gson.toJson(map, Map::class.java, out)
            }

            out.endObject()
        }

        override fun read(reader: JsonReader): TrackUser? {
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull()
                return null
            }

            var email: String? = null
            var phone: String? = null
            var timezone: String? = null
            var locale: String? = null
            var data: Map<String, Any>? = null

            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "email" -> email = if (reader.peek() == JsonToken.NULL) {
                        reader.nextNull(); null
                    } else reader.nextString()
                    "phone" -> phone = if (reader.peek() == JsonToken.NULL) {
                        reader.nextNull(); null
                    } else reader.nextString()
                    "timezone" -> timezone = if (reader.peek() == JsonToken.NULL) {
                        reader.nextNull(); null
                    } else reader.nextString()
                    "locale" -> locale = if (reader.peek() == JsonToken.NULL) {
                        reader.nextNull(); null
                    } else reader.nextString()
                    "data" -> {
                        @Suppress("UNCHECKED_CAST")
                        data = gson.fromJson(reader, Map::class.java) as? Map<String, Any>
                    }
                    else -> reader.skipValue()
                }
            }
            reader.endObject()

            return TrackUser(email, phone, timezone, locale, data)
        }
    }
}
