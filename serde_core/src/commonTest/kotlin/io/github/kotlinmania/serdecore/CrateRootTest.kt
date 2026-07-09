// port-lint: tests crate_root.rs
package io.github.kotlinmania.serdecore

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

public class CrateRootTest {
    @Test
    public fun triMapsSuccessfulValue() {
        val result = CrateRoot.tri(SerdeResult.success(2)) { value ->
            SerdeResult.success(value + 3)
        }

        assertEquals(5, result.getOrThrow())
    }

    @Test
    public fun triPropagatesFailureWithoutConvertingError() {
        val error = SerdeError.custom("crate root helper failure")
        val result = CrateRoot.tri(SerdeResult.failure(error)) { value: Int ->
            SerdeResult.success(value + 3)
        }

        assertSame(error, result.exceptionOrNull())
    }
}
