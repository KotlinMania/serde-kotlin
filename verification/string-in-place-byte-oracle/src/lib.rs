#[cfg(test)]
mod tests {
    use serde_core::{Deserialize, de::{self, Visitor, value::Error}};
    enum Input<'a> { Bytes(&'a [u8]), Buffer(Vec<u8>) }
    impl<'de> de::Deserializer<'de> for Input<'de> {
        type Error = Error;
        fn deserialize_any<V:Visitor<'de>>(self, visitor:V)->Result<V::Value,Error> {
            match self { Self::Bytes(bytes)=>visitor.visit_bytes(bytes), Self::Buffer(bytes)=>visitor.visit_byte_buf(bytes) }
        }
        serde_core::forward_to_deserialize_any! {
            bool i8 i16 i32 i64 i128 u8 u16 u32 u64 u128 f32 f64 char str string
            bytes byte_buf option unit unit_struct newtype_struct seq tuple tuple_struct
            map struct enum identifier ignored_any
        }
    }
    #[test]
    fn valid_borrowed_bytes_replace_supplementary_and_nul() {
        let mut value=String::from("before");let text="a🙂\0";
        assert!(String::deserialize_in_place(Input::Bytes(text.as_bytes()),&mut value).is_ok());assert_eq!(value,text);
    }
    #[test]
    fn valid_owned_empty_buffer_clears_string() {
        let mut value=String::from("before");assert!(String::deserialize_in_place(Input::Buffer(vec![]),&mut value).is_ok());assert_eq!(value,"");
    }
    #[test]
    fn invalid_borrowed_overlong_bytes_leave_string_untouched() {
        let mut value=String::from("before");let error=String::deserialize_in_place(Input::Bytes(&[0xc0,0x80]),&mut value).unwrap_err();
        assert_eq!(error.to_string(),"invalid value: byte array, expected a string");assert_eq!(value,"before");
    }
    #[test]
    fn invalid_owned_truncated_bytes_leave_string_untouched() {
        let mut value=String::from("before");let error=String::deserialize_in_place(Input::Buffer(vec![0xf0,0x9f,0x99]),&mut value).unwrap_err();
        assert_eq!(error.to_string(),"invalid value: byte array, expected a string");assert_eq!(value,"before");
    }
}
