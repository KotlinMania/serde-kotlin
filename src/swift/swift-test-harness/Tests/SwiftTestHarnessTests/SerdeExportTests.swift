#if canImport(Testing)
import Testing
import Serde

@Suite("Serde Swift Export Tests")
struct SerdeExportTests {
    @Test("Swift module loads")
    func testSwiftModuleLoads() {
        let outcome = io.github.kotlinmania.serde.SerdeDecoding.shared.unit(
            input: io.github.kotlinmania.serde.SerdeDecodeInput.companion.unit())
        #expect(outcome.completed)
        #expect(outcome.error == nil)
    }
}
#elseif canImport(XCTest)
import XCTest
import Serde

final class SerdeExportTests: XCTestCase {
    func testSwiftModuleLoads() throws {
        let outcome = io.github.kotlinmania.serde.SerdeDecoding.shared.unit(
            input: io.github.kotlinmania.serde.SerdeDecodeInput.companion.unit())
        XCTAssertTrue(outcome.completed)
        XCTAssertNil(outcome.error)
    }
}
#endif
