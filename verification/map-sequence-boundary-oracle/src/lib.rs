#[cfg(test)]
mod tests {
    use serde_core::{
        de::{
            value::{Error, I32Deserializer, MapDeserializer},
            DeserializeSeed, Deserializer, IntoDeserializer, MapAccess, SeqAccess, Visitor,
        },
        Deserialize,
    };
    use std::{cell::Cell, fmt, rc::Rc};
    struct PanicSeed;
    impl<'de> DeserializeSeed<'de> for PanicSeed {
        type Value = i32;
        fn deserialize<D: Deserializer<'de>>(self, _: D) -> Result<i32, D::Error> {
            panic!("seed panic")
        }
    }
    struct ErrorSeed;
    impl<'de> DeserializeSeed<'de> for ErrorSeed {
        type Value = i32;
        fn deserialize<D: Deserializer<'de>>(self, _: D) -> Result<i32, D::Error> {
            Err(serde_core::de::Error::custom("seed error"))
        }
    }
    struct NullSeed;
    impl<'de> DeserializeSeed<'de> for NullSeed {
        type Value = Option<i32>;
        fn deserialize<D: Deserializer<'de>>(self, _: D) -> Result<Self::Value, D::Error> {
            Ok(None)
        }
    }
    struct VisitorSeed<V>(V);
    impl<'de, V: Visitor<'de>> DeserializeSeed<'de> for VisitorSeed<V> {
        type Value = V::Value;
        fn deserialize<D: Deserializer<'de>>(self, d: D) -> Result<Self::Value, D::Error> {
            d.deserialize_seq(self.0)
        }
    }
    enum Mode {
        Panic,
        KeyPanic,
        ValuePanic,
        Partial,
        TypedKeyError,
    }
    struct PairVisit(Mode);
    impl<'de> Visitor<'de> for PairVisit {
        type Value = i32;
        fn expecting(&self, f: &mut fmt::Formatter) -> fmt::Result {
            f.write_str("pair")
        }
        fn visit_seq<A: SeqAccess<'de>>(self, mut a: A) -> Result<i32, A::Error> {
            match self.0 {
                Mode::Panic => panic!("visitor panic"),
                Mode::KeyPanic => {
                    let p = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
                        let _ = a.next_element_seed(PanicSeed);
                    }))
                    .unwrap_err();
                    assert_eq!(panic_text(p), "seed panic");
                    assert_eq!(a.size_hint(), Some(1));
                    let value = a.next_element::<i32>()?.unwrap();
                    assert_eq!(value, 10);
                    assert_eq!(a.size_hint(), Some(0));
                    assert_eq!(a.next_element::<i32>()?, None);
                    Ok(value)
                }
                Mode::ValuePanic => {
                    let key = a.next_element::<i32>()?.unwrap();
                    assert_eq!(key, 1);
                    let p = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
                        let _ = a.next_element_seed(PanicSeed);
                    }))
                    .unwrap_err();
                    assert_eq!(panic_text(p), "seed panic");
                    assert_eq!(a.size_hint(), Some(0));
                    assert_eq!(a.next_element::<i32>()?, None);
                    Ok(key)
                }
                Mode::Partial => Ok(a.next_element::<i32>()?.unwrap()),
                Mode::TypedKeyError => a.next_element_seed(ErrorSeed).map(|v| v.unwrap()),
            }
        }
    }
    struct NullPairVisit;
    impl<'de> Visitor<'de> for NullPairVisit {
        type Value = (Option<i32>, Option<i32>);
        fn expecting(&self, f: &mut fmt::Formatter) -> fmt::Result {
            f.write_str("nullable pair")
        }
        fn visit_seq<A: SeqAccess<'de>>(self, mut a: A) -> Result<Self::Value, A::Error> {
            assert_eq!(a.next_element_seed(NullSeed)?, Some(None));
            assert_eq!(a.next_element_seed(NullSeed)?, Some(None));
            assert_eq!(a.next_element::<i32>()?, None);
            Ok((None, None))
        }
    }
    struct PoisonValue(Rc<Cell<usize>>);
    impl<'de> IntoDeserializer<'de, Error> for PoisonValue {
        type Deserializer = I32Deserializer<Error>;
        fn into_deserializer(self) -> Self::Deserializer {
            self.0.set(self.0.get() + 1);
            panic!("unexpected value conversion")
        }
    }
    fn panic_text(p: Box<dyn std::any::Any + Send>) -> String {
        if let Some(s) = p.downcast_ref::<&str>() {
            s.to_string()
        } else {
            p.downcast_ref::<String>().unwrap().clone()
        }
    }
    #[test]
    fn outer_seed_panic_consumes_entry() {
        let mut d = MapDeserializer::<_, Error>::new(vec![(1, 10), (2, 20)].into_iter());
        let p = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            let _ = SeqAccess::next_element_seed(&mut d, PanicSeed);
        }))
        .unwrap_err();
        assert_eq!(panic_text(p), "seed panic");
        assert_eq!(d.next_entry::<i32, i32>().unwrap(), Some((2, 20)));
    }
    #[test]
    fn outer_seed_error_remains_error() {
        let mut d = MapDeserializer::<_, Error>::new(vec![(1, 10), (2, 20)].into_iter());
        assert_eq!(
            SeqAccess::next_element_seed(&mut d, ErrorSeed)
                .unwrap_err()
                .to_string(),
            "seed error"
        );
        assert_eq!(d.next_entry::<i32, i32>().unwrap(), Some((2, 20)));
    }
    #[test]
    fn pair_visitor_panic_propagates() {
        let mut d = MapDeserializer::<_, Error>::new(vec![(1, 10)].into_iter());
        let p = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            let _ = SeqAccess::next_element_seed(&mut d, VisitorSeed(PairVisit(Mode::Panic)));
        }))
        .unwrap_err();
        assert_eq!(panic_text(p), "visitor panic");
    }
    #[test]
    fn pair_key_panic_consumes_key_slot() {
        let mut d = MapDeserializer::<_, Error>::new(vec![(1, 10)].into_iter());
        assert_eq!(
            SeqAccess::next_element_seed(&mut d, VisitorSeed(PairVisit(Mode::KeyPanic))).unwrap(),
            Some(10)
        );
    }
    #[test]
    fn pair_value_panic_consumes_value_slot() {
        let mut d = MapDeserializer::<_, Error>::new(vec![(1, 10)].into_iter());
        assert_eq!(
            SeqAccess::next_element_seed(&mut d, VisitorSeed(PairVisit(Mode::ValuePanic))).unwrap(),
            Some(1)
        );
    }
    #[test]
    fn partial_pair_retains_length_error() {
        let mut d = MapDeserializer::<_, Error>::new(vec![(1, 10)].into_iter());
        assert_eq!(
            SeqAccess::next_element_seed(&mut d, VisitorSeed(PairVisit(Mode::Partial)))
                .unwrap_err()
                .to_string(),
            "invalid length 2, expected 1 element in sequence"
        );
    }
    #[test]
    fn typed_pair_key_error_skips_value() {
        let conversions = Rc::new(Cell::new(0));
        let mut d = MapDeserializer::<_, Error>::new(
            vec![(1, PoisonValue(conversions.clone()))].into_iter(),
        );
        assert_eq!(
            SeqAccess::next_element_seed(&mut d, VisitorSeed(PairVisit(Mode::TypedKeyError)))
                .unwrap_err()
                .to_string(),
            "seed error"
        );
        assert_eq!(conversions.get(), 0);
    }
    #[test]
    fn nullable_pair_is_present() {
        let mut d = MapDeserializer::<_, Error>::new(vec![(1, 10)].into_iter());
        assert_eq!(
            SeqAccess::next_element_seed(&mut d, VisitorSeed(NullPairVisit)).unwrap(),
            Some((None, None))
        );
        assert_eq!(
            SeqAccess::next_element_seed(&mut d, VisitorSeed(NullPairVisit)).unwrap(),
            None
        );
    }
}
