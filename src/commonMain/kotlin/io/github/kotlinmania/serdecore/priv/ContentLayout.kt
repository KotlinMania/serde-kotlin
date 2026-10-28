package io.github.kotlinmania.serdecore.priv

/** Allocation bound for the source's concrete Content specialization. */
fun cautiousContentCapacity(hint: Int?): Int =
    contentCapacity(hint, rustContentSizes(rustContentLayoutTarget()).first)

/** Allocation bound for the source's complete (Content, Content) tuple specialization. */
fun cautiousContentMapCapacity(hint: Int?): Int =
    contentCapacity(hint, rustContentSizes(rustContentLayoutTarget()).second)

private fun contentCapacity(hint: Int?, rustElementSizeBytes: ULong): Int {
    require(hint == null || hint >= 0) { "A Rust size hint cannot be negative" }
    val capacity = cautious(hint?.toULong(), rustElementSizeBytes)
    check(capacity <= Int.MAX_VALUE.toULong()) { "Rust allocation capacity does not fit a Kotlin collection" }
    return capacity.toInt()
}

/**
 * Kotlin allocation adapter for Content capture. On the measured Rust profile,
 * preallocate using the exact generated bound. Else use ordinary Kotlin list
 * growth: no Rust element size or equivalent capacity is implied for that target.
 */
fun newContentBuffer(hint: Int?): MutableList<Content> =
    contentBuffer(hint, verifiedRustContentSizes(rustContentLayoutTarget())?.first)

/** Kotlin allocation adapter for the source's (Content, Content) sequence. */
fun newContentMapBuffer(hint: Int?): MutableList<ContentMapEntry> =
    contentBuffer(hint, verifiedRustContentSizes(rustContentLayoutTarget())?.second)

internal fun <T> contentBuffer(hint: Int?, rustElementSizeBytes: ULong?): ArrayList<T> {
    require(hint == null || hint >= 0) { "A Rust size hint cannot be negative" }
    return if (rustElementSizeBytes == null) {
        // Missing layout disables optional reservation, not deserialization.
        // This is a portability adapter, not a size or capacity estimate.
        ArrayList()
    } else {
        ArrayList(contentCapacity(hint, rustElementSizeBytes))
    }
}

internal expect fun rustContentLayoutTarget(): String

// Measured from serde_core 1.0.228's canonical __private228::Content<'static> and
// the complete (Content<'static>, Content<'static>) tuple, with std + alloc.
// Rust target: aarch64-apple-darwin; rustc: 7189d39d9ce936881b8132c6addeaf3e295cd455.
// Source: tmp/serde/serde_core/src/private/content.rs
// SHA-256: 5fdfb2bb95ecc80375507acb813a4c640496385e56fc99ab448f6b19e01fcc01.
// Evidence and reproduction: verification/direct-adoption-01/README.md.
// These are Rust sizes, not Kotlin object sizes. The tuple was measured separately.
internal fun rustContentSizes(target: String): Pair<ULong, ULong> =
    verifiedRustContentSizes(target)
        ?: throw IllegalStateException("Missing verified Rust Content layout for target $target")

private fun verifiedRustContentSizes(target: String): Pair<ULong, ULong>? = when (target) {
    "aarch64-apple-darwin" -> Pair(32uL, 64uL)
    else -> null
}

internal fun <T> generatedContentCapacity(hint: Int?, pairs: Boolean): ArrayList<T> {
    val sizes = verifiedRustContentSizes(rustContentLayoutTarget())
    return contentBuffer(hint, if (pairs) sizes?.second else sizes?.first)
}
