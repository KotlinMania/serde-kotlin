import Testing
import Serde
import SerdeCore

@Suite("Concrete Serde outcomes")
struct ConcreteOutcomeTests {
    @Test func textHasConcreteSwiftType() {
        let input = io.github.kotlinmania.serde.SerdeDecodeInput.companion.text(value: "hello")
        let result = io.github.kotlinmania.serde.SerdeDecoding.shared.text(input: input)
        let value: String? = result.value
        #expect(value == "hello")
        #expect(result.error == nil)
    }

    @Test func primitivesHaveConcreteSwiftTypes() {
        let number = io.github.kotlinmania.serde.SerdeDecoding.shared.signed(
            input: io.github.kotlinmania.serde.SerdeDecodeInput.companion.signed(value: 0))
        let value: Int64? = number.value
        #expect(value == 0)
        let boolean = io.github.kotlinmania.serde.SerdeDecoding.shared.boolean(
            input: io.github.kotlinmania.serde.SerdeDecodeInput.companion.boolean(value: false))
        let flag: Bool? = boolean.value
        #expect(flag == false)
        #expect(boolean.error == nil)
    }

    @Test func failureRetainsOriginalDiagnostic() {
        let diagnostic = io.github.kotlinmania.serde.SerdeError(message: "original", source: nil)
        let result = io.github.kotlinmania.serde.SerdeStringOutcome.companion.failure(error: diagnostic)
        let value: String? = result.value
        let message: String? = result.error?.message
        #expect(value == nil)
        #expect(message == "original")
        #expect(result.error === diagnostic)
    }

    @Test func actualDecodeFailureIsTyped() {
        let result = io.github.kotlinmania.serde.SerdeDecoding.shared.signed(
            input: io.github.kotlinmania.serde.SerdeDecodeInput.companion.text(value: "invalid"))
        let value: Int64? = result.value
        let diagnostic: io.github.kotlinmania.serde.SerdeError? = result.error
        #expect(value == nil)
        #expect(diagnostic != nil)
        #expect(result.isFailure)
    }
}
