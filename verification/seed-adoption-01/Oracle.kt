import io.github.kotlinmania.serdecore.priv.InPlaceSeed
import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.I32Deserialize
import io.github.kotlinmania.serdecore.de.MutablePlace
import io.github.kotlinmania.serdecore.de.mutablePlace
import io.github.kotlinmania.serdecore.de.value.I32Deserializer

private class Item(var value: Int)
private val trace = mutableListOf<String>()
private val rejected = SerdeResult.failure(SerdeError.custom("rejected"))
private val panic = IllegalStateException("sentinel")
private var expectedInput: Deserializer? = null
private var expectedPlace: MutablePlace<Item>? = null
private fun decode(d: Deserializer): SerdeResult<Int> {
    check(d === expectedInput)
    return I32Deserialize.deserialize(d).map { n -> trace.add("decode:$n"); n }
}
private fun outcome(n: Int): SerdeResult<Unit> = when(n) {
    -1 -> rejected
    -2 -> throw panic
    else -> SerdeResult.success(Unit)
}
private val defaultCodec = object : Deserialize<Item> {
    override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<Item> {
        trace.add("new")
        return decode(deserializer).flatMap { n -> outcome(n).map { Item(n) } }
    }
}
private val updateCodec = object : Deserialize<Item> {
    override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<Item> = error("wrong dispatch")
    override fun <D : Deserializer> deserializeInPlace(deserializer: D, place: MutablePlace<Item>): SerdeResult<Unit> {
        check(place === expectedPlace)
        trace.add("entry:${place.value.value}")
        return decode(deserializer).flatMap { n ->
            place.value.value = when(n) { -1 -> 700; -2 -> 800; else -> n }
            trace.add("write:${place.value.value}")
            outcome(n)
        }
    }
}
private val setterCodec = object : Deserialize<Item> {
    override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<Item> = error("wrong dispatch")
    override fun <D : Deserializer> deserializeInPlace(deserializer: D, place: (Item) -> Unit): SerdeResult<Unit> {
        trace.add("setter")
        return decode(deserializer).flatMap { n ->
            val updated = when(n) { -1 -> 700; -2 -> 800; else -> n }
            place(Item(updated))
            trace.add("write:$updated")
            outcome(n)
        }
    }
}
private fun result(call: () -> SerdeResult<Unit>): String = try {
    when(val r = call()) {
        is SerdeResult.Success -> "ok"
        is SerdeResult.Failure -> { check(r === rejected); "error:${r.error.message}" }
    }
} catch(t: IllegalStateException) { check(t === panic); "panic" }
private fun run(kind: String, old: Int, n: Int, codec: Deserialize<Item>) {
    trace.clear()
    val original = Item(old)
    var value = original
    val place = mutablePlace({ value }, { value = it })
    val input = I32Deserializer.new(n)
    expectedInput = input
    expectedPlace = place
    val seed = InPlaceSeed(place, codec)
    check(seed.__rtkField0 === place)
    val outcome = result { seed.deserialize(input) }
    if(kind == "update") check(value === original)
    println("$kind|old=$old|input=$n|state=${value.value}|trace=${trace.joinToString(";")}|result=$outcome")
}
fun main() {
    for((old,n) in listOf(4 to 9, 4 to -1, 4 to -2, 0 to 0, 19 to Int.MIN_VALUE, Int.MIN_VALUE to Int.MAX_VALUE, Int.MAX_VALUE to Int.MIN_VALUE)) {
        run("default",old,n,defaultCodec)
        run("update",old,n,updateCodec)
        run("setter",old,n,setterCodec)
        var value = old
        InPlaceSeed(mutablePlace({ value }, { value = it }), I32Deserialize).deserialize(I32Deserializer.new(n)).getOrThrow()
        println("primitive|old=$old|input=$n|state=$value|trace=|result=ok")
    }
}
