#[cfg(test)]
mod tests {
    use serde_core::de::{
        value::{Error, MapAccessDeserializer, MapDeserializer},
        Deserializer, EnumAccess, IntoDeserializer, VariantAccess, Visitor,
    };
    use std::{cell::Cell, rc::Rc};

    fn unit<V: IntoDeserializer<'static, Error>>(value: V) -> Result<(), Error> {
        let map = MapDeserializer::<_, Error>::new(vec![(1_i32, value)].into_iter());
        let (key, variant) = MapAccessDeserializer::new(map).variant::<i32>()?;
        assert_eq!(key, 1);
        variant.unit_variant()
    }

    struct Spy {
        mode: u8,
        calls: Rc<Cell<usize>>,
    }
    impl IntoDeserializer<'static, Error> for Spy {
        type Deserializer = Self;
        fn into_deserializer(self) -> Self {
            self
        }
    }
    impl<'de> Deserializer<'de> for Spy {
        type Error = Error;
        fn deserialize_any<V: Visitor<'de>>(self, _: V) -> Result<V::Value, Error> {
            panic!("wrong dispatch")
        }
        fn deserialize_unit<V: Visitor<'de>>(self, visitor: V) -> Result<V::Value, Error> {
            self.calls.set(self.calls.get() + 1);
            match self.mode {
                0 => visitor.visit_unit(),
                1 => Err(serde_core::de::Error::custom("unit error")),
                _ => panic!("unit panic"),
            }
        }
        serde_core::forward_to_deserialize_any! { bool i8 i16 i32 i64 i128 u8 u16 u32 u64 u128 f32 f64 char str string bytes byte_buf option unit_struct newtype_struct seq tuple tuple_struct map struct enum identifier ignored_any }
    }
    #[test]
    fn accepts_unit() {
        assert!(unit(()).is_ok());
    }
    #[test]
    fn rejects_integer() {
        assert_eq!(
            unit(7_i32).unwrap_err().to_string(),
            "invalid type: integer `7`, expected unit"
        );
    }
    #[test]
    fn rejects_boolean() {
        assert_eq!(
            unit(true).unwrap_err().to_string(),
            "invalid type: boolean `true`, expected unit"
        );
    }
    #[test]
    fn rejects_string() {
        assert_eq!(
            unit("payload").unwrap_err().to_string(),
            "invalid type: string \"payload\", expected unit"
        );
    }
    #[test]
    fn dispatches_to_unit_once() {
        let calls = Rc::new(Cell::new(0));
        assert!(unit(Spy {
            mode: 0,
            calls: calls.clone()
        })
        .is_ok());
        assert_eq!(calls.get(), 1);
    }
    #[test]
    fn retains_typed_error() {
        let calls = Rc::new(Cell::new(0));
        assert_eq!(
            unit(Spy {
                mode: 1,
                calls: calls.clone()
            })
            .unwrap_err()
            .to_string(),
            "unit error"
        );
        assert_eq!(calls.get(), 1);
    }
    #[test]
    fn propagates_decoder_panic() {
        let calls = Rc::new(Cell::new(0));
        let panic = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            unit(Spy {
                mode: 2,
                calls: calls.clone(),
            })
        }))
        .unwrap_err();
        assert_eq!(panic.downcast_ref::<&str>(), Some(&"unit panic"));
        assert_eq!(calls.get(), 1);
    }
}
