// port-lint: source serde_derive/src/dummy.rs
package io.github.kotlinmania.serde.serdederive.src

import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.syn.Path

public fun wrapInConst(serdePath: Path?, code: TokenStream): TokenStream {
    val useSerde =
        if (serdePath != null) {
            "use $serdePath as _serde;"
        } else {
            """
            #[allow(unused_extern_crates, clippy::useless_attribute)]
            extern crate serde as _serde;
            """.trimIndent()
        }

    return TokenStream.fromString(
        """
        #[doc(hidden)]
        #[allow(
            non_upper_case_globals,
            unused_attributes,
            unused_qualifications,
            clippy::absolute_paths,
        )]
        const _: () = {
            $useSerde

            _serde::__require_serde_not_serde_core!();

            $code
        };
        """.trimIndent(),
    ).getOrThrow()
}
