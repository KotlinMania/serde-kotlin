// port-lint: source dummy.rs
package io.github.kotlinmania.serderive.internals

import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.quote
import io.github.kotlinmania.syn.Path

public fun wrapInConst(serdePath: Path?, code: TokenStream): TokenStream {
    val useSerde = if (serdePath != null) {
        quote("""
            use `#`serdePath as _serde;
        """, mapOf("serdePath" to serdePath))
    } else {
        quote("""
            #[allow(unused_extern_crates, clippy::useless_attribute)]
            extern crate serde as _serde;
        """)
    }

    return quote("""
        #[doc(hidden)]
        #[allow(
            non_upper_case_globals,
            unused_attributes,
            unused_qualifications,
            clippy::absolute_paths,
        )]
        const _: () = {
            `#`useSerde

            _serde::__require_serde_not_serde_core!();

            `#`code
        };
    """, mapOf("useSerde" to useSerde, "code" to code))
}
