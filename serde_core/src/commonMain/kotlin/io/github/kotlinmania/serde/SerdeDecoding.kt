package io.github.kotlinmania.serde

import io.github.kotlinmania.serdecore.de.BooleanDeserialize
import io.github.kotlinmania.serdecore.de.I64Deserialize
import io.github.kotlinmania.serdecore.de.StringDeserialize
import io.github.kotlinmania.serdecore.de.UnitDeserialize

/** Concrete decoding operations over the serialization data model. */
object SerdeDecoding {
    fun text(input: SerdeDecodeInput): SerdeStringOutcome =
        when (val result = input.decode(StringDeserialize)) {
            is SerdeResult.Success -> SerdeStringOutcome.success(result.value)
            is SerdeResult.Failure -> SerdeStringOutcome.failure(result.error)
        }

    fun signed(input: SerdeDecodeInput): SerdeLongOutcome =
        when (val result = input.decode(I64Deserialize)) {
            is SerdeResult.Success -> SerdeLongOutcome.success(result.value)
            is SerdeResult.Failure -> SerdeLongOutcome.failure(result.error)
        }

    fun boolean(input: SerdeDecodeInput): SerdeBooleanOutcome =
        when (val result = input.decode(BooleanDeserialize)) {
            is SerdeResult.Success -> SerdeBooleanOutcome.success(result.value)
            is SerdeResult.Failure -> SerdeBooleanOutcome.failure(result.error)
        }

    fun unit(input: SerdeDecodeInput): SerdeUnitOutcome =
        when (val result = input.decode(UnitDeserialize)) {
            is SerdeResult.Success -> SerdeUnitOutcome.success()
            is SerdeResult.Failure -> SerdeUnitOutcome.failure(result.error)
        }
}
