package ai.january.partner

@JvmInline
public value class PartnerUserId(public val value: String) {
    init {
        require(value.isNotBlank()) { "Partner user ID must not be blank." }
    }
}

@JvmInline
public value class FoodId(public val value: Long)

@JvmInline
public value class ServingId(public val value: Long)
