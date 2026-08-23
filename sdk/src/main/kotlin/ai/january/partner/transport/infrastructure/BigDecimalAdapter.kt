package ai.january.partner.transport.infrastructure

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import java.math.BigDecimal

internal class BigDecimalAdapter {
    @ToJson
    fun toJson(writer: JsonWriter, value: BigDecimal?) {
        if (value == null) writer.nullValue() else writer.value(value)
    }

    @FromJson
    fun fromJson(reader: JsonReader): BigDecimal? {
        if (reader.peek() == JsonReader.Token.NULL) return reader.nextNull()
        return BigDecimal(reader.nextString())
    }
}
