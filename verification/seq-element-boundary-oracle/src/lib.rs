#[cfg(test)]
mod tests {
    use serde_core::{
        de::{
            value::{Error, I32Deserializer, SeqDeserializer},
            DeserializeSeed, Deserializer, IntoDeserializer, SeqAccess,
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
        value: i32,
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
            I32Deserializer::new(self.value)
        }
    }
    struct PanickingInput {
        attempt: usize,
    }
    impl Iterator for PanickingInput {
        type Item = i32;
        fn next(&mut self) -> Option<i32> {
            self.attempt += 1;
            match self.attempt {
                1 => panic!("iterator panic"),
                2 => Some(7),
                _ => None,
            }
        }
    }
    struct EmptyInput {
        probes: Rc<Cell<usize>>,
    }
    impl Iterator for EmptyInput {
        type Item = i32;
        fn next(&mut self) -> Option<i32> {
            self.probes.set(self.probes.get() + 1);
            None
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
    fn seed_panic_propagates_after_consumption() {
        let c = Rc::new(Cell::new(0));
        let mut d = SeqDeserializer::<_, Error>::new(vec![10, 20].into_iter());
        let p = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            let _ = d.next_element_seed(seed(&c, Mode::Panic));
        }))
        .unwrap_err();
        assert_eq!(panic_text(p), "seed panic");
        assert_eq!(d.next_element_seed(seed(&c, Mode::Read)).unwrap(), Some(20));
        assert_eq!(c.get(), 2);
    }
    #[test]
    fn conversion_panic_propagates_after_consumption() {
        let conversions = Rc::new(Cell::new(0));
        let c = Rc::new(Cell::new(0));
        let mut d = SeqDeserializer::<_, Error>::new(
            vec![
                Value {
                    value: 10,
                    panic: true,
                    conversions: conversions.clone(),
                },
                Value {
                    value: 20,
                    panic: false,
                    conversions: conversions.clone(),
                },
            ]
            .into_iter(),
        );
        let p = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            let _ = d.next_element_seed(seed(&c, Mode::Read));
        }))
        .unwrap_err();
        assert_eq!(panic_text(p), "conversion panic");
        assert_eq!(c.get(), 0);
        assert_eq!(conversions.get(), 1);
        assert_eq!(d.next_element_seed(seed(&c, Mode::Read)).unwrap(), Some(20));
        assert_eq!(c.get(), 1);
        assert_eq!(conversions.get(), 2);
    }
    #[test]
    fn iterator_panic_does_not_fuse() {
        let c = Rc::new(Cell::new(0));
        let mut d = SeqDeserializer::<_, Error>::new(PanickingInput { attempt: 0 });
        let p = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            let _ = d.next_element_seed(seed(&c, Mode::Read));
        }))
        .unwrap_err();
        assert_eq!(panic_text(p), "iterator panic");
        assert_eq!(c.get(), 0);
        assert_eq!(d.next_element_seed(seed(&c, Mode::Read)).unwrap(), Some(7));
        assert_eq!(d.next_element_seed(seed(&c, Mode::Read)).unwrap(), None);
        assert_eq!(c.get(), 1);
    }
    #[test]
    fn typed_error_stays_error_and_consumes_item() {
        let c = Rc::new(Cell::new(0));
        let mut d = SeqDeserializer::<_, Error>::new(vec![10, 20].into_iter());
        assert_eq!(
            d.next_element_seed(seed(&c, Mode::Error))
                .unwrap_err()
                .to_string(),
            "seed error"
        );
        assert_eq!(d.next_element_seed(seed(&c, Mode::Read)).unwrap(), Some(20));
        assert_eq!(c.get(), 2);
    }
    #[test]
    fn nullable_decoded_item_is_some_null() {
        let mut d = SeqDeserializer::<_, Error>::new(vec![10].into_iter());
        assert_eq!(d.next_element_seed(NullSeed).unwrap(), Some(None));
        assert_eq!(d.next_element_seed(NullSeed).unwrap(), None);
    }
    #[test]
    fn empty_iterator_is_fused_before_seed() {
        let probes = Rc::new(Cell::new(0));
        let c = Rc::new(Cell::new(0));
        let mut d = SeqDeserializer::<_, Error>::new(EmptyInput {
            probes: probes.clone(),
        });
        for _ in 0..2 {
            assert_eq!(d.next_element_seed(seed(&c, Mode::Panic)).unwrap(), None);
        }
        assert_eq!(probes.get(), 1);
        assert_eq!(c.get(), 0);
    }
    #[test]
    fn count_precedes_typed_error() {
        let c = Rc::new(Cell::new(0));
        let mut d = SeqDeserializer::<_, Error>::new(vec![10, 20].into_iter());
        assert_eq!(
            d.next_element_seed(seed(&c, Mode::Error))
                .unwrap_err()
                .to_string(),
            "seed error"
        );
        assert_eq!(
            d.end().unwrap_err().to_string(),
            "invalid length 2, expected 1 element in sequence"
        );
        assert_eq!(c.get(), 1);
    }
    #[test]
    fn count_precedes_seed_panic() {
        let c = Rc::new(Cell::new(0));
        let mut d = SeqDeserializer::<_, Error>::new(vec![10, 20].into_iter());
        let p = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            let _ = d.next_element_seed(seed(&c, Mode::Panic));
        }))
        .unwrap_err();
        assert_eq!(panic_text(p), "seed panic");
        assert_eq!(
            d.end().unwrap_err().to_string(),
            "invalid length 2, expected 1 element in sequence"
        );
        assert_eq!(c.get(), 1);
    }
}
