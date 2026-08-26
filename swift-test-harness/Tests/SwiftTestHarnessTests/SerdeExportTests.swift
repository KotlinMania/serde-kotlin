#if canImport(Testing)
import Testing
import Serde

@Suite("Serde Swift Export Tests")
struct SerdeExportTests {
    @Test("Swift module loads")
    func testSwiftModuleLoads() {
        #expect(Bool(true), "Serde swift module imported cleanly")
    }
}
#elseif canImport(XCTest)
import XCTest
import Serde

final class SerdeExportTests: XCTestCase {
    func testSwiftModuleLoads() throws {
        XCTAssertTrue(true, "Serde swift module imported cleanly")
    }
}
#endif
