package eu.anifantakis.lib.securepersist.internal


import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.math.BigDecimal
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.regex.Pattern
import kotlin.time.Duration

class UriAdapter : JsonSerializer<Uri>, JsonDeserializer<Uri> {
    override fun serialize(src: Uri?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.toString() ?: "")
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Uri? {
        return json?.asString?.let { Uri.parse(it) }
    }
}

class BigDecimalAdapter : JsonSerializer<BigDecimal>, JsonDeserializer<BigDecimal> {
    override fun serialize(src: BigDecimal?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src)
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): BigDecimal? {
        return json?.asBigDecimal
    }
}

class DateAdapter : JsonSerializer<Date>, JsonDeserializer<Date> {
    override fun serialize(src: Date?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.time)
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Date? {
        return json?.asLong?.let { Date(it) }
    }
}

class DurationAdapter : JsonSerializer<Duration>, JsonDeserializer<Duration> {
    override fun serialize(src: Duration?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.toString())
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Duration? {
        return json?.asString?.let { Duration.parse(it) }
    }
}

class UUIDAdapter : JsonSerializer<UUID>, JsonDeserializer<UUID> {
    override fun serialize(src: UUID?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.toString())
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): UUID? {
        return json?.asString?.let { UUID.fromString(it) }
    }
}

class PatternAdapter : JsonSerializer<Pattern>, JsonDeserializer<Pattern> {
    override fun serialize(src: Pattern?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.pattern())
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Pattern? {
        return json?.asString?.let { Pattern.compile(it) }
    }
}

class TimeZoneAdapter : JsonSerializer<TimeZone>, JsonDeserializer<TimeZone> {
    override fun serialize(src: TimeZone?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.id)
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): TimeZone? {
        return json?.asString?.let { TimeZone.getTimeZone(it) }
    }
}

class LocaleAdapter : JsonSerializer<Locale>, JsonDeserializer<Locale> {
    override fun serialize(src: Locale?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.toLanguageTag())
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Locale? {
        return json?.asString?.let { Locale.forLanguageTag(it) }
    }
}

class CalendarAdapter : JsonSerializer<Calendar>, JsonDeserializer<Calendar> {
    override fun serialize(src: Calendar?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.timeInMillis)
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Calendar? {
        return json?.asLong?.let {
            Calendar.getInstance().apply { timeInMillis = it }
        }
    }
}

fun createGson(): Gson {
    return GsonBuilder()
        .registerTypeAdapter(Uri::class.java, UriAdapter())
        .registerTypeAdapter(BigDecimal::class.java, BigDecimalAdapter())
        .registerTypeAdapter(Date::class.java, DateAdapter())
        .registerTypeAdapter(Duration::class.java, DurationAdapter())
        .registerTypeAdapter(UUID::class.java, UUIDAdapter())
        .registerTypeAdapter(Pattern::class.java, PatternAdapter())
        .registerTypeAdapter(TimeZone::class.java, TimeZoneAdapter())
        .registerTypeAdapter(Locale::class.java, LocaleAdapter())
        .registerTypeAdapter(Calendar::class.java, CalendarAdapter())
        .create()
}