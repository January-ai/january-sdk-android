package ai.january.partner.demo

import ai.january.partner.glucose.WeightUnit
import ai.january.partner.waterlogs.VolumeUnit
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackingDraftsTest {
    @Test fun waterKeepsItsAmountAcrossUnits() {
        assertEquals("8.5", convertWaterDraft("250", VolumeUnit.ML, VolumeUnit.FL_OZ))
        assertEquals("237", convertWaterDraft("8", VolumeUnit.FL_OZ, VolumeUnit.ML))
        assertEquals("1.1", convertWaterDraft("250", VolumeUnit.ML, VolumeUnit.CUP))
        assertEquals("16", convertWaterDraft("2", VolumeUnit.CUP, VolumeUnit.FL_OZ))
        assertEquals("0.3", convertWaterDraft("2", VolumeUnit.FL_OZ, VolumeUnit.CUP))
        assertEquals("473", convertWaterDraft("2", VolumeUnit.CUP, VolumeUnit.ML))
        assertEquals("240", convertWaterDraft("8.1", VolumeUnit.FL_OZ, VolumeUnit.ML))
    }

    @Test fun theLimitsOfTheOtherUnitsStayWithinTheCupRange() {
        // The API takes 0.1–101.4 cups; one decimal keeps every loggable fl oz or ml amount inside it.
        assertEquals("0.1", convertWaterDraft("1", VolumeUnit.FL_OZ, VolumeUnit.CUP))
        assertEquals("0.1", convertWaterDraft("30", VolumeUnit.ML, VolumeUnit.CUP))
        assertEquals("101.4", convertWaterDraft("811.5", VolumeUnit.FL_OZ, VolumeUnit.CUP))
        assertEquals("101.4", convertWaterDraft("24000", VolumeUnit.ML, VolumeUnit.CUP))
    }

    @Test fun weightKeepsItsAmountAcrossUnits() {
        assertEquals("68", convertWeightDraft("150", WeightUnit.POUNDS, WeightUnit.KILOGRAMS))
        assertEquals("149.9", convertWeightDraft("68", WeightUnit.KILOGRAMS, WeightUnit.POUNDS))
        assertEquals("165.8", convertWeightDraft("75.2", WeightUnit.KILOGRAMS, WeightUnit.POUNDS))
    }

    @Test fun anEmptyUnfinishedOrUnchangedDraftStaysAsTyped() {
        assertEquals("", convertWaterDraft("", VolumeUnit.FL_OZ, VolumeUnit.ML))
        assertEquals(".", convertWaterDraft(".", VolumeUnit.ML, VolumeUnit.CUP))
        assertEquals("", convertWeightDraft("", WeightUnit.POUNDS, WeightUnit.KILOGRAMS))
        assertEquals("8.25", convertWaterDraft("8.25", VolumeUnit.FL_OZ, VolumeUnit.FL_OZ))
        assertEquals("160.5", convertWeightDraft("160.5", WeightUnit.POUNDS, WeightUnit.POUNDS))
    }

    @Test fun theDraftUsesADecimalPointInEveryLocale() {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.GERMANY)
        try {
            assertEquals("8.5", convertWaterDraft("250", VolumeUnit.ML, VolumeUnit.FL_OZ))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test fun anEntryIsTimedNowOnItsOwnDayAndAtNoonOnAnother() {
        val zone = ZoneId.of("America/New_York")
        val day = LocalDate.parse("2026-09-23")
        val lateEvening = ZonedDateTime.parse("2026-09-23T23:59:59.987654321-04:00[America/New_York]")
        // On the day itself: the moment of the tap, to the millisecond, even a breath before midnight.
        assertEquals(OffsetDateTime.parse("2026-09-23T23:59:59.987-04:00"), entryTime(day, zone, lateEvening))
        // Another day: noon there.
        assertEquals(OffsetDateTime.parse("2026-09-22T12:00-04:00"), entryTime(LocalDate.parse("2026-09-22"), zone, lateEvening))
        // Just past midnight, a tap for the day that has just ended still lands on it.
        assertEquals(OffsetDateTime.parse("2026-09-23T12:00-04:00"), entryTime(day, zone, lateEvening.plusSeconds(1)))
        // "Now" is read in the user's timezone: 01:00 UTC on the 24th is still the 23rd in New York.
        assertEquals(OffsetDateTime.parse("2026-09-23T21:00-04:00"), entryTime(day, zone, ZonedDateTime.parse("2026-09-24T01:00Z")))
    }
}
