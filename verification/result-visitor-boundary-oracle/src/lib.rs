#[cfg(test)]
mod tests {
    use serde_core::{
        de::{
            value::{Error, MapAccessDeserializer, MapDeserializer, U32Deserializer},
            DeserializeSeed, Deserializer, EnumAccess, Visitor,
        },
        Deserialize,
    };
    use std::{
        cell::RefCell,
        panic::{catch_unwind, AssertUnwindSafe},
    };
    thread_local! {static TRACE:RefCell<Vec<String>>=const{RefCell::new(Vec::new())};}
    #[derive(Debug, PartialEq)]
    struct Item<const OK: bool>(i32);
    impl<'de, const OK: bool> Deserialize<'de> for Item<OK> {
        fn deserialize<D: Deserializer<'de>>(d: D) -> Result<Self, D::Error> {
            let n = i32::deserialize(d)?;
            let label = if OK { "ok" } else { "err" };
            TRACE.with(|t| t.borrow_mut().push(format!("{label}:{n}")));
            match n {
                -2 => panic!("{label} panic"),
                -1 => Err(serde_core::de::Error::custom(format!("{label} error"))),
                _ => Ok(Item(n)),
            }
        }
    }
    type Data = Result<Item<true>, Item<false>>;
    fn decode(key: &'static str, n: i32) -> Result<Data, Error> {
        TRACE.with(|t| t.borrow_mut().clear());
        Data::deserialize(MapAccessDeserializer::new(
            MapDeserializer::<_, Error>::new(vec![(key, n)].into_iter()),
        ))
    }
    fn trace(expected: &[&str]) {
        TRACE.with(|t| {
            assert_eq!(
                t.borrow().iter().map(String::as_str).collect::<Vec<_>>(),
                expected
            )
        })
    }
    fn panic_text(p: Box<dyn std::any::Any + Send>) -> String {
        if let Some(s) = p.downcast_ref::<&str>() {
            s.to_string()
        } else {
            p.downcast_ref::<String>().unwrap().clone()
        }
    }
    struct Access(bool);
    impl<'de> EnumAccess<'de> for Access {
        type Error = Error;
        type Variant = <U32Deserializer<Error> as EnumAccess<'de>>::Variant;
        fn variant_seed<T: DeserializeSeed<'de>>(
            self,
            _: T,
        ) -> Result<(T::Value, Self::Variant), Error> {
            if self.0 {
                panic!("access panic")
            }
            Err(serde_core::de::Error::custom("access error"))
        }
    }
    struct Driver(bool);
    impl<'de> Deserializer<'de> for Driver {
        type Error = Error;
        fn deserialize_any<V: Visitor<'de>>(self, v: V) -> Result<V::Value, Error> {
            v.visit_enum(Access(self.0))
        }
        serde_core::forward_to_deserialize_any! {bool i8 i16 i32 i64 i128 u8 u16 u32 u64 u128 f32 f64 char str string bytes byte_buf option unit unit_struct newtype_struct seq tuple tuple_struct map struct enum identifier ignored_any}
    }
    #[test]
    fn ok_success_selects_ok_codec() {
        assert_eq!(decode("Ok", 7).unwrap(), Ok(Item(7)));
        trace(&["ok:7"])
    }
    #[test]
    fn err_success_is_data() {
        assert_eq!(decode("Err", 8).unwrap(), Err(Item(8)));
        trace(&["err:8"])
    }
    #[test]
    fn ok_codec_panic_propagates() {
        let p = catch_unwind(AssertUnwindSafe(|| decode("Ok", -2))).unwrap_err();
        assert_eq!(panic_text(p), "ok panic");
        trace(&["ok:-2"])
    }
    #[test]
    fn err_codec_panic_propagates() {
        let p = catch_unwind(AssertUnwindSafe(|| decode("Err", -2))).unwrap_err();
        assert_eq!(panic_text(p), "err panic");
        trace(&["err:-2"])
    }
    #[test]
    fn ok_typed_error_remains_failure() {
        assert_eq!(decode("Ok", -1).unwrap_err().to_string(), "ok error");
        trace(&["ok:-1"])
    }
    #[test]
    fn err_typed_error_remains_failure() {
        assert_eq!(decode("Err", -1).unwrap_err().to_string(), "err error");
        trace(&["err:-1"])
    }
    #[test]
    fn access_panic_propagates() {
        TRACE.with(|t| t.borrow_mut().clear());
        let p = catch_unwind(AssertUnwindSafe(|| Data::deserialize(Driver(true)))).unwrap_err();
        assert_eq!(panic_text(p), "access panic");
        trace(&[])
    }
    #[test]
    fn typed_access_error_remains_failure() {
        TRACE.with(|t| t.borrow_mut().clear());
        assert_eq!(
            Data::deserialize(Driver(false)).unwrap_err().to_string(),
            "access error"
        );
        trace(&[])
    }
    #[test]
    fn unknown_variant_skips_payload() {
        assert_eq!(
            decode("Other", 7).unwrap_err().to_string(),
            "unknown variant `Other`, expected `Ok` or `Err`"
        );
        trace(&[])
    }
    #[test]
    fn nullable_ok_is_present_data() {
        let map = MapDeserializer::<_, Error>::new(vec![("Ok", ())].into_iter());
        assert_eq!(
            Result::<Option<i32>, i32>::deserialize(MapAccessDeserializer::new(map)).unwrap(),
            Ok(None)
        )
    }
}
