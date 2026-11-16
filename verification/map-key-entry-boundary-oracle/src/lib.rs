#[cfg(test)]
mod tests {
    use serde_core::{
        de::{
            value::{Error, I32Deserializer, MapDeserializer},
            DeserializeSeed, Deserializer, IntoDeserializer, MapAccess,
        },
        Deserialize,
    };
    use std::{cell::Cell, rc::Rc};
    #[derive(Clone, Copy)]
    enum Mode {
        Read,
        Panic,
        Error,
    }
    struct Seed {
        calls: Rc<Cell<usize>>,
        mode: Mode,
    }
    impl<'de> DeserializeSeed<'de> for Seed {
        type Value = i32;
        fn deserialize<D: Deserializer<'de>>(self, d: D) -> Result<i32, D::Error> {
            self.calls.set(self.calls.get() + 1);
            match self.mode {
                Mode::Read => i32::deserialize(d),
                Mode::Panic => panic!("seed panic"),
                Mode::Error => Err(serde_core::de::Error::custom("seed error")),
            }
        }
    }
    struct NullSeed;
    impl<'de> DeserializeSeed<'de> for NullSeed {
        type Value = Option<i32>;
        fn deserialize<D: Deserializer<'de>>(self, _: D) -> Result<Self::Value, D::Error> {
            Ok(None)
        }
    }
    struct Value {
        number: i32,
        panic: bool,
        conversions: Rc<Cell<usize>>,
    }
    impl<'de> IntoDeserializer<'de, Error> for Value {
        type Deserializer = I32Deserializer<Error>;
        fn into_deserializer(self) -> Self::Deserializer {
            self.conversions.set(self.conversions.get() + 1);
            if self.panic {
                panic!("conversion panic");
            }
            I32Deserializer::new(self.number)
        }
    }
    fn seed(c: &Rc<Cell<usize>>, mode: Mode) -> Seed {
        Seed {
            calls: c.clone(),
            mode,
        }
    }
    fn counter() -> Rc<Cell<usize>> {
        Rc::new(Cell::new(0))
    }
    fn panic_text(p: Box<dyn std::any::Any + Send>) -> String {
        if let Some(s) = p.downcast_ref::<&str>() {
            s.to_string()
        } else {
            p.downcast_ref::<String>().unwrap().clone()
        }
    }
    #[test]
    fn key_seed_panic_preserves_pending_value() {
        let c = counter();
        let mut d = MapDeserializer::<_, Error>::new(vec![(1, 10)].into_iter());
        let p = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            let _ = d.next_key_seed(seed(&c, Mode::Panic));
        }))
        .unwrap_err();
        assert_eq!(panic_text(p), "seed panic");
        assert_eq!(d.next_value::<i32>().unwrap(), 10);
        assert_eq!(c.get(), 1);
    }
    #[test]
    fn key_conversion_panic_preserves_pending_value() {
        let c = counter();
        let conversions = counter();
        let mut d = MapDeserializer::<_, Error>::new(
            vec![(
                Value {
                    number: 1,
                    panic: true,
                    conversions: conversions.clone(),
                },
                10,
            )]
            .into_iter(),
        );
        let p = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            let _ = d.next_key_seed(seed(&c, Mode::Read));
        }))
        .unwrap_err();
        assert_eq!(panic_text(p), "conversion panic");
        assert_eq!(d.next_value::<i32>().unwrap(), 10);
        assert_eq!(c.get(), 0);
        assert_eq!(conversions.get(), 1);
    }
    #[test]
    fn key_typed_error_preserves_pending_value() {
        let c = counter();
        let mut d = MapDeserializer::<_, Error>::new(vec![(1, 10)].into_iter());
        assert_eq!(
            d.next_key_seed(seed(&c, Mode::Error))
                .unwrap_err()
                .to_string(),
            "seed error"
        );
        assert_eq!(d.next_value::<i32>().unwrap(), 10);
        assert_eq!(c.get(), 1);
    }
    #[test]
    fn key_eof_does_not_invoke_seed() {
        let c = counter();
        let mut d = MapDeserializer::<_, Error>::new(std::iter::empty::<(i32, i32)>());
        for _ in 0..2 {
            assert_eq!(d.next_key_seed(seed(&c, Mode::Panic)).unwrap(), None);
        }
        assert_eq!(c.get(), 0);
    }
    #[test]
    fn entry_key_panic_skips_value_seed() {
        let keys = counter();
        let values = counter();
        let mut d = MapDeserializer::<_, Error>::new(vec![(1, 10), (2, 20)].into_iter());
        let p = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            let _ = d.next_entry_seed(seed(&keys, Mode::Panic), seed(&values, Mode::Read));
        }))
        .unwrap_err();
        assert_eq!(panic_text(p), "seed panic");
        assert_eq!(keys.get(), 1);
        assert_eq!(values.get(), 0);
        assert_eq!(
            d.next_entry_seed(seed(&keys, Mode::Read), seed(&values, Mode::Read))
                .unwrap(),
            Some((2, 20))
        );
        assert_eq!(keys.get(), 2);
        assert_eq!(values.get(), 1);
    }
    #[test]
    fn entry_value_panic_consumes_entry() {
        let keys = counter();
        let values = counter();
        let mut d = MapDeserializer::<_, Error>::new(vec![(1, 10), (2, 20)].into_iter());
        let p = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            let _ = d.next_entry_seed(seed(&keys, Mode::Read), seed(&values, Mode::Panic));
        }))
        .unwrap_err();
        assert_eq!(panic_text(p), "seed panic");
        assert_eq!(keys.get(), 1);
        assert_eq!(values.get(), 1);
        assert_eq!(
            d.next_entry_seed(seed(&keys, Mode::Read), seed(&values, Mode::Read))
                .unwrap(),
            Some((2, 20))
        );
        assert_eq!(keys.get(), 2);
        assert_eq!(values.get(), 2);
    }
    #[test]
    fn entry_key_error_skips_value_conversion() {
        let keys = counter();
        let values = counter();
        let conversions = counter();
        let mut d = MapDeserializer::<_, Error>::new(
            vec![
                (
                    1,
                    Value {
                        number: 10,
                        panic: true,
                        conversions: conversions.clone(),
                    },
                ),
                (
                    2,
                    Value {
                        number: 20,
                        panic: false,
                        conversions: conversions.clone(),
                    },
                ),
            ]
            .into_iter(),
        );
        assert_eq!(
            d.next_entry_seed(seed(&keys, Mode::Error), seed(&values, Mode::Read))
                .unwrap_err()
                .to_string(),
            "seed error"
        );
        assert_eq!(values.get(), 0);
        assert_eq!(conversions.get(), 0);
        assert_eq!(
            d.next_entry_seed(seed(&keys, Mode::Read), seed(&values, Mode::Read))
                .unwrap(),
            Some((2, 20))
        );
        assert_eq!(keys.get(), 2);
        assert_eq!(values.get(), 1);
        assert_eq!(conversions.get(), 1);
    }
    #[test]
    fn entry_nullable_pair_is_present() {
        let mut d = MapDeserializer::<_, Error>::new(vec![(1, 10)].into_iter());
        assert_eq!(
            d.next_entry_seed(NullSeed, NullSeed).unwrap(),
            Some((None, None))
        );
        assert_eq!(d.next_entry_seed(NullSeed, NullSeed).unwrap(), None);
    }
    #[test]
    fn entry_value_conversion_panic_skips_value_seed() {
        let keys = counter();
        let values = counter();
        let conversions = counter();
        let mut d = MapDeserializer::<_, Error>::new(
            vec![
                (
                    1,
                    Value {
                        number: 10,
                        panic: true,
                        conversions: conversions.clone(),
                    },
                ),
                (
                    2,
                    Value {
                        number: 20,
                        panic: false,
                        conversions: conversions.clone(),
                    },
                ),
            ]
            .into_iter(),
        );
        let p = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            let _ = d.next_entry_seed(seed(&keys, Mode::Read), seed(&values, Mode::Read));
        }))
        .unwrap_err();
        assert_eq!(panic_text(p), "conversion panic");
        assert_eq!(keys.get(), 1);
        assert_eq!(values.get(), 0);
        assert_eq!(conversions.get(), 1);
        assert_eq!(
            d.next_entry_seed(seed(&keys, Mode::Read), seed(&values, Mode::Read))
                .unwrap(),
            Some((2, 20))
        );
        assert_eq!(keys.get(), 2);
        assert_eq!(values.get(), 1);
        assert_eq!(conversions.get(), 2);
    }
}
