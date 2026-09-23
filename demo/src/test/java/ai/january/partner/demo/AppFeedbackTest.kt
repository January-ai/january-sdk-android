package ai.january.partner.demo

import ai.january.partner.ErrorCategory
import ai.january.partner.JanuaryException
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Test

class AppFeedbackTest {
    @Test fun onlyARefusedCreateIsSafeToSendAgain() {
        val refused = listOf(
            ErrorCategory.VALIDATION,
            ErrorCategory.AUTHENTICATION,
            ErrorCategory.AUTHORIZATION,
            ErrorCategory.NOT_FOUND,
            ErrorCategory.RATE_LIMITED,
        )
        ErrorCategory.entries.forEach { category ->
            assertEquals(category.name, category !in refused, mayHaveBeenSaved(JanuaryException(category, "failed")))
        }
        // A failure from outside the SDK says nothing about whether the server saved the entry.
        assertEquals(true, mayHaveBeenSaved(IOException("connection reset")))
    }
}
