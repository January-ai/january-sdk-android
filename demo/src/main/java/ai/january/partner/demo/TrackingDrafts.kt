package ai.january.partner.demo

import ai.january.partner.glucose.Weight
import ai.january.partner.glucose.WeightUnit
import ai.january.partner.waterlogs.VolumeUnit
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

const val MILLILITERS_PER_FLUID_OUNCE = 29.5735
const val FLUID_OUNCES_PER_CUP = 8.0

// The water and weight cards keep the amount the user typed when they switch its unit, re-expressed
// in the new unit, so 250 typed in ml becomes 8.5 fl oz rather than 250 fl oz. Milliliters are
// whole; fluid ounces, cups, pounds and kilograms have one decimal. An empty or unfinished draft
// stays as it is.

/** The typed water amount [text], in [from], re-expressed in [to]. */
fun convertWaterDraft(text: String, from: VolumeUnit, to: VolumeUnit): String {
    val value = text.toDoubleOrNull() ?: return text
    if (from == to) return text
    return draftNumber(value * millilitersPer(from) / millilitersPer(to), if (to == VolumeUnit.ML) 0 else 1)
}

/** The typed weight [text], in [from], re-expressed in [to]. */
fun convertWeightDraft(text: String, from: WeightUnit, to: WeightUnit): String {
    val value = text.toDoubleOrNull() ?: return text
    if (from == to) return text
    return draftNumber(convertWeight(Weight(value, from), to), 1)
}

private fun millilitersPer(unit: VolumeUnit): Double = when (unit) {
    VolumeUnit.ML -> 1.0
    VolumeUnit.FL_OZ -> MILLILITERS_PER_FLUID_OUNCE
    VolumeUnit.CUP -> MILLILITERS_PER_FLUID_OUNCE * FLUID_OUNCES_PER_CUP
}

/** [value] rounded half up to [decimals] places, without trailing zeros, and always with a "." for the field. */
private fun draftNumber(value: Double, decimals: Int): String =
    BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

/**
 * When a water or weight entry logged for [day] happened: [now] while it is still that day in [zone],
 * otherwise noon on it, so it lands on that day. The demo always sends it, fixed when the user taps,
 * so a save that reaches the API after midnight still lands on the day it was logged for.
 */
fun entryTime(day: LocalDate, zone: ZoneId, now: ZonedDateTime = ZonedDateTime.now(zone)): OffsetDateTime {
    val local = now.withZoneSameInstant(zone).truncatedTo(ChronoUnit.MILLIS)
    return (if (local.toLocalDate() == day) local else day.atTime(12, 0).atZone(zone)).toOffsetDateTime()
}
