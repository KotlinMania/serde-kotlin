// port-lint: source internals/receiver.rs
package io.github.kotlinmania.serderive.internals

import io.github.kotlinmania.syn.DeriveInput

public fun replaceReceiver(input: DeriveInput) {
    input.generics
    input.data
}
