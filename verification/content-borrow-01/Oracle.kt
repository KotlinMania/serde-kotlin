import io.github.kotlinmania.serdecore.priv.Content
import io.github.kotlinmania.serdecore.priv.ContentVisitor
import io.github.kotlinmania.serdecore.priv.contentClone
fun main() {
    for(size in listOf(0,1,4,257,512)) {
        val storage=ByteArray(size) {it.toByte()}
        val captured=ContentVisitor.new().visitBorrowedBytes(storage).getOrThrow() as Content.Bytes
        val cloned=contentClone(captured) as Content.Bytes
        println("borrow:$size:${captured.value === storage}:${cloned.value === storage}:${captured.value.contentEquals(cloned.value)}")
        val owned=ContentVisitor.new().visitBytes(storage).getOrThrow() as Content.ByteBuf
        val copy=contentClone(owned) as Content.ByteBuf
        println("owned:$size:${size==0 || owned.value !== storage}:${size==0 || copy.value !== owned.value}:${owned.value.contentEquals(copy.value)}")
    }
}
