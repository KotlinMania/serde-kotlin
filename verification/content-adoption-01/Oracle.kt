import io.github.kotlinmania.serdecore.priv.generated.content.*
import org.rustlang.core.slice.SharedByteSlice
private fun bytes(v: ByteArray): String = v.joinToString("") { it.toUByte().toString(16).padStart(2,'0') }
private fun show(c: Content): String = when(c) {
    is Content.Bool -> "Bool:${c.__rtkField0}"
    is Content.U8 -> "U8:${c.__rtkField0}"
    is Content.U16 -> "U16:${c.__rtkField0}"
    is Content.U32 -> "U32:${c.__rtkField0}"
    is Content.U64 -> "U64:${c.__rtkField0}"
    is Content.I8 -> "I8:${c.__rtkField0}"
    is Content.I16 -> "I16:${c.__rtkField0}"
    is Content.I32 -> "I32:${c.__rtkField0}"
    is Content.I64 -> "I64:${c.__rtkField0}"
    is Content.F32 -> "F32:${c.__rtkField0.toRawBits().toUInt()}"
    is Content.F64 -> "F64:${c.__rtkField0.toRawBits().toULong()}"
    is Content.Char -> "Char:${c.__rtkField0.codePoint}"
    is Content.String -> "String:${bytes(c.__rtkField0.toString().encodeToByteArray(throwOnInvalidSequence=true))}"
    is Content.Str -> "Str:${bytes(c.__rtkField0.encodeToByteArray(throwOnInvalidSequence=true))}"
    is Content.ByteBuf -> "ByteBuf:${bytes(c.__rtkField0.map { it.toByte() }.toByteArray())}"
    is Content.Bytes -> "Bytes:${bytes(c.__rtkField0.copyBytes())}"
    Content.None -> "None"
    Content.Unit -> "Unit"
    is Content.Some -> "Some(${show(c.__rtkField0)})"
    is Content.Newtype -> "Newtype(${show(c.__rtkField0)})"
    is Content.Seq -> "Seq[${c.__rtkField0.joinToString(",",transform=::show)}]"
    is Content.Map -> "Map[${c.__rtkField0.joinToString(",") { (k,v)->"${show(k)}=>${show(v)}" }}]"
}
private fun emit(c: Content) = println(show(c))
fun main() {
    emit(Content.Bool(false));emit(Content.Bool(true))
    for(x in 0..255) {emit(Content.U8(x.toUByte()));emit(Content.I8(x.toByte()))}
    for(x in listOf(0uL,1uL,127uL,128uL,255uL,256uL,32767uL,32768uL,65535uL,UInt.MAX_VALUE.toULong(),ULong.MAX_VALUE)) {
        emit(Content.U16(x.toUShort()));emit(Content.U32(x.toUInt()));emit(Content.U64(x))
        emit(Content.I16(x.toShort()));emit(Content.I32(x.toInt()));emit(Content.I64(x.toLong()))
    }
    for(bits in listOf(0u,1u,0x7fffffu,0x800000u,0x3f800000u,0x7f7fffffu,0x7f800000u,0x7fc00001u,0x7f800001u,0x80000000u,0xff800000u,0xffc12345u,UInt.MAX_VALUE)) emit(Content.F32(Float.fromBits(bits.toInt())))
    for(bits in listOf(0uL,1uL,0xfffffffffffffuL,0x10000000000000uL,0x3ff0000000000000uL,0x7fefffffffffffffuL,0x7ff0000000000000uL,0x7ff8000000000001uL,0x7ff0000000000001uL,0x8000000000000000uL,0xfff0000000000000uL,0xfff8123456789abcuL,ULong.MAX_VALUE)) emit(Content.F64(Double.fromBits(bits.toLong())))
    for(cp in listOf(0,1,65,127,128,255,256,0x7ff,0x800,0xd7ff,0xe000,0xffff,0x10000,0x1f680,0x10ffff)) emit(Content.Char(__rtkChar(cp)))
    for(text in listOf("","a","é","a\u0000b","line\nnext","水🚀\uDBFF\uDFFF")) {
        emit(Content.String(StringBuilder(text)));emit(Content.Str(text))
        val owned=StringBuilder(text);val moved=Content.String(owned)
        println("owned-text-retained:${moved.__rtkField0 === owned}")
        val borrowed=Content.Str(text);println("borrowed-text-retained:${borrowed.__rtkField0 === text}")
    }
    val storage=ByteArray(256) { it.toByte() }
    for((start,end) in listOf(0 to 0,0 to 256,1 to 255,17 to 18,255 to 256)) {
        val view=SharedByteSlice.borrow(storage,start,end);emit(Content.Bytes(view));emit(Content.ByteBuf(view.copyBytes().map { it.toUByte() }.toMutableList()))
        val borrowed=Content.Bytes(view);println("borrowed-region:${borrowed.__rtkField0.isSameRegion(view)}:$start:${borrowed.__rtkField0.size}")
        val owned=view.copyBytes().map { it.toUByte() }.toMutableList();val moved=Content.ByteBuf(owned)
        println("owned-bytes-retained:${moved.__rtkField0 === owned}")
    }
    emit(Content.None);emit(Content.Unit)
    for(n in 0..32) {
        var tree: Content=Content.U32(n.toUInt())
        for(k in 0 until n) tree=if(k%2==0) Content.Some(tree) else Content.Newtype(tree)
        emit(tree)
        emit(Content.Seq((0 until n).map<Int, Content> { Content.I32(it-16) }.toMutableList()))
        emit(Content.Map((0 until n).map<Int, Pair<Content, Content>> { Content.U32(it.toUInt()) to Content.Some(Content.Bool(it%2==0)) }.toMutableList()))
    }
    var state=0x123456789abcdef0uL
    repeat(64) {
        state=state*6364136223846793005uL+1442695040888963407uL
        emit(Content.F64(Double.fromBits(state.toLong())));emit(Content.F32(Float.fromBits(state.toInt())))
    }
}
