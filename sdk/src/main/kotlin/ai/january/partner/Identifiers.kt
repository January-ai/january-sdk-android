package ai.january.partner

@JvmInline
public value class PartnerUserId(public val value: String) {
    init {
        require(value.isNotBlank()) { "Partner user ID must not be blank." }
    }
}

@JvmInline
public value class FoodId(public val value: String) {
    public companion object {
        public operator fun invoke(value: Long): FoodId = FoodId(value.toString())
    }
}

@JvmInline
public value class ServingId(public val value: String) {
    public companion object {
        public operator fun invoke(value: Long): ServingId = ServingId(value.toString())
    }
}
