import Serde
import SerdeCore

func rejectsWrongPayload(input: io.github.kotlinmania.serde.SerdeDecodeInput) {
    let result = io.github.kotlinmania.serde.SerdeDecoding.shared.text(input: input)
    let value: Int64? = result.value
    _ = value
}
