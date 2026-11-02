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
    struct PanicValue {
        calls: Rc<Cell<usize>>,
    }
    impl<'de> IntoDeserializer<'de, Error> for PanicValue {
        type Deserializer = I32Deserializer<Error>;
        fn into_deserializer(self) -> Self::Deserializer {
            self.calls.set(self.calls.get() + 1);
            panic!("value conversion panic")
        }
    }
    fn seed(c: &Rc<Cell<usize>>, mode: Mode) -> Seed {
        Seed {
            calls: c.clone(),
            mode,
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
    fn value_before_key_is_programming_panic() {
        let mut d = MapDeserializer::<_, Error>::new(vec![(1, 10)].into_iter());
        let c = Rc::new(Cell::new(0));
        let p = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            let _ = d.next_value_seed(seed(&c, Mode::Read));
        }))
        .unwrap_err();
        assert_eq!(
            panic_text(p),
            "MapAccess::next_value called before next_key"
        );
        assert_eq!(c.get(), 0);
    }
    #[test]
    fn second_value_is_programming_panic() {
        let mut d = MapDeserializer::<_, Error>::new(vec![(1, 10)].into_iter());
        let c = Rc::new(Cell::new(0));
        assert_eq!(d.next_key::<i32>().unwrap(), Some(1));
        assert_eq!(d.next_value_seed(seed(&c, Mode::Read)).unwrap(), 10);
        let p = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            let _ = d.next_value_seed(seed(&c, Mode::Read));
        }))
        .unwrap_err();
        assert_eq!(
            panic_text(p),
            "MapAccess::next_value called before next_key"
        );
        assert_eq!(c.get(), 1);
    }
    #[test]
    fn seed_panic_propagates_and_consumes_value() {
        let mut d = MapDeserializer::<_, Error>::new(vec![(1, 10)].into_iter());
        let c = Rc::new(Cell::new(0));
        assert_eq!(d.next_key::<i32>().unwrap(), Some(1));
        let p = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            let _ = d.next_value_seed(seed(&c, Mode::Panic));
        }))
        .unwrap_err();
        assert_eq!(panic_text(p), "seed panic");
        let p = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            let _ = d.next_value_seed(seed(&c, Mode::Read));
        }))
        .unwrap_err();
        assert_eq!(
            panic_text(p),
            "MapAccess::next_value called before next_key"
        );
        assert_eq!(c.get(), 1);
    }
    #[test]
    fn conversion_panic_propagates_and_consumes_value() {
        let conversions = Rc::new(Cell::new(0));
        let c = Rc::new(Cell::new(0));
        let mut d = MapDeserializer::<_, Error>::new(
            vec![(
                1,
                PanicValue {
                    calls: conversions.clone(),
                },
            )]
            .into_iter(),
        );
        assert_eq!(d.next_key::<i32>().unwrap(), Some(1));
        let p = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            let _ = d.next_value_seed(seed(&c, Mode::Read));
        }))
        .unwrap_err();
        assert_eq!(panic_text(p), "value conversion panic");
        let p = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            let _ = d.next_value_seed(seed(&c, Mode::Read));
        }))
        .unwrap_err();
        assert_eq!(
            panic_text(p),
            "MapAccess::next_value called before next_key"
        );
        assert_eq!(conversions.get(), 1);
        assert_eq!(c.get(), 0);
    }
    #[test]
    fn typed_seed_error_remains_error() {
        let mut d = MapDeserializer::<_, Error>::new(vec![(1, 10)].into_iter());
        let c = Rc::new(Cell::new(0));
        assert_eq!(d.next_key::<i32>().unwrap(), Some(1));
        assert_eq!(
            d.next_value_seed(seed(&c, Mode::Error))
                .unwrap_err()
                .to_string(),
            "seed error"
        );
        assert_eq!(c.get(), 1);
    }
    #[test]
    fn nullable_decoded_value_is_success() {
        let mut d = MapDeserializer::<_, Error>::new(vec![(1, 10)].into_iter());
        assert_eq!(d.next_key::<i32>().unwrap(), Some(1));
        assert_eq!(d.next_value_seed(NullSeed).unwrap(), None);
    }
    #[test]
    fn entry_access_does_not_populate_value_slot() {
        let mut d = MapDeserializer::<_, Error>::new(vec![(1, 10)].into_iter());
        let c = Rc::new(Cell::new(0));
        assert_eq!(d.next_entry::<i32, i32>().unwrap(), Some((1, 10)));
        let p = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            let _ = d.next_value_seed(seed(&c, Mode::Read));
        }))
        .unwrap_err();
        assert_eq!(
            panic_text(p),
            "MapAccess::next_value called before next_key"
        );
        assert_eq!(c.get(), 0);
    }
    #[test]
    fn next_entry_available_after_typed_error() {
        let mut d = MapDeserializer::<_, Error>::new(vec![(1, 10), (2, 20)].into_iter());
        let c = Rc::new(Cell::new(0));
        assert_eq!(d.next_key::<i32>().unwrap(), Some(1));
        assert_eq!(
            d.next_value_seed(seed(&c, Mode::Error))
                .unwrap_err()
                .to_string(),
            "seed error"
        );
        assert_eq!(d.next_key::<i32>().unwrap(), Some(2));
        assert_eq!(d.next_value_seed(seed(&c, Mode::Read)).unwrap(), 20);
        assert_eq!(c.get(), 2);
    }
}
