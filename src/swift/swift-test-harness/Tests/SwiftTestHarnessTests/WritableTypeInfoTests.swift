import Foundation
import KotlinStdlib
import Serde
import Testing

private typealias CapturedContent = io.github.kotlinmania.serdecore.priv.generated.content.Content
private typealias ContentAccess = io.github.kotlinmania.serdecore.priv.contentaccess
private typealias CoreDe = io.github.kotlinmania.serdecore.de
private typealias ExportedMutableList = ExportedKotlinPackages.kotlin.collections.MutableList

@Suite("Swift writable type-info interoperability")
struct WritableTypeInfoTests {
    @Test func mutableBufferPreservesElementIdentityAndSubviewMutation() {
        let first: CapturedContent = ContentAccess.newI32Content(value: 1)
        let second: CapturedContent = ContentAccess.newI32Content(value: 2)
        let replacement: CapturedContent = ContentAccess.newI32Content(value: 3)
        let buffer = ContentAccess.newContentBuffer(hint: nil)
        #expect(buffer.size == 0)
        #expect(buffer.add(element: first))
        #expect(buffer.add(element: second))
        #expect((buffer._get(index: 0) as? CapturedContent) === first)
        #expect((buffer._get(index: 1) as? CapturedContent) === second)
        let view: any ExportedMutableList = buffer.subList(fromIndex: 0, toIndex: 2)
        let previous = view._set(index: 1, element: replacement)
        #expect((previous as? CapturedContent) === second)
        #expect((buffer._get(index: 1) as? CapturedContent) === replacement)
        let removed = view.removeAt(index: 0)
        #expect((removed as? CapturedContent) === first)
        #expect(buffer.size == 1)
        #expect(view.size == 1)
        #expect((buffer._get(index: 0) as? CapturedContent) === replacement)
        #expect(view.add(element: second))
        #expect(buffer.size == 2)
        #expect((buffer._get(index: 1) as? CapturedContent) === second)
        view.clear()
        #expect(buffer.size == 0)
        #expect(view.size == 0)
    }

    @Test func sequenceFoundationRoundTripPreservesOrderAndElementIdentity() {
        let first: CapturedContent = ContentAccess.newI32Content(value: 10)
        let second: CapturedContent = ContentAccess.newI32Content(value: 20)
        var input: [CapturedContent] = [first, second]
        let sequence = ContentAccess.newSeqContent(value: input)
        input.removeAll()
        let output: [CapturedContent] = ContentAccess.getValue(sequence)
        #expect(output.count == 2)
        #expect(output.first === first)
        #expect(output.last === second)
        let foundation = output as NSArray
        #expect(foundation.count == 2)
        #expect((foundation.firstObject as? CapturedContent) === first)
        #expect((foundation.lastObject as? CapturedContent) === second)
        let empty = ContentAccess.newSeqContent(value: [])
        let emptyOutput: [CapturedContent] = ContentAccess.getValue(empty)
        #expect(emptyOutput.isEmpty)
    }

    @Test func mutablePlaceCallbacksRetainTheCallersSlotAndIdentity() {
        let first: CapturedContent = ContentAccess.newI32Content(value: 100)
        let second: CapturedContent = ContentAccess.newI32Content(value: 200)
        var current: CapturedContent? = first
        var reads = 0
        var writes = 0
        let place = CoreDe.mutablePlace(
            getValue: { reads += 1; return current },
            setValue: { value in writes += 1; current = value as? CapturedContent }
        )
        #expect(reads == 0)
        #expect(writes == 0)
        let initial = place.value as? CapturedContent
        #expect(initial === first)
        #expect(reads == 1)
        place.value = second
        #expect(current === second)
        #expect(writes == 1)
        let replaced = place.value as? CapturedContent
        #expect(replaced === second)
        current = first
        let reread = place.value as? CapturedContent
        #expect(reread === first)
        #expect(reads == 3)
        place.value = nil
        #expect(current == nil)
        #expect(writes == 2)
        let cleared = place.value
        #expect(cleared == nil)
        #expect(reads == 4)
    }
}
