#[cfg(test)]
mod tests {
    use serde_core::de::{
        value::{Error, I32Deserializer, MapAccessDeserializer, MapDeserializer},
        DeserializeSeed, Deserializer, EnumAccess, IntoDeserializer, MapAccess, VariantAccess,
    };
    use std::{
        cell::Cell,
        panic::{catch_unwind, AssertUnwindSafe},
        rc::Rc,
    };
    struct Seed(bool);
    impl<'de> DeserializeSeed<'de> for Seed {
        type Value = i32;
        fn deserialize<D: Deserializer<'de>>(self, _: D) -> Result<i32, D::Error> {
            if self.0 {
                panic!("seed panic")
            }
            Err(serde_core::de::Error::custom("seed error"))
        }
    }
    struct Access(bool);
    impl<'de> MapAccess<'de> for Access {
        type Error = Error;
        fn next_key_seed<K: DeserializeSeed<'de>>(
            &mut self,
            _: K,
        ) -> Result<Option<K::Value>, Error> {
            if self.0 {
                panic!("access panic")
            }
            Err(serde_core::de::Error::custom("access error"))
        }
        fn next_value_seed<V: DeserializeSeed<'de>>(&mut self, _: V) -> Result<V::Value, Error> {
            panic!("unexpected value")
        }
    }
    struct Value {
        calls: Rc<Cell<usize>>,
        number: i32,
        panic: bool,
    }
    impl<'de> IntoDeserializer<'de, Error> for Value {
        type Deserializer = I32Deserializer<Error>;
        fn into_deserializer(self) -> Self::Deserializer {
            self.calls.set(self.calls.get() + 1);
            if self.panic {
                panic!("conversion panic")
            }
            I32Deserializer::new(self.number)
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
    fn seed_panic_retains_pending_value() {
        let mut map = MapDeserializer::<_, Error>::new(vec![(1_i32, 10_i32)].into_iter());
        let p = catch_unwind(AssertUnwindSafe(|| {
            let _ = MapAccessDeserializer::new(&mut map).variant_seed(Seed(true));
        }))
        .unwrap_err();
        assert_eq!(panic_text(p), "seed panic");
        assert_eq!(map.next_value::<i32>().unwrap(), 10);
    }
    #[test]
    fn access_panic_propagates() {
        let p = catch_unwind(AssertUnwindSafe(|| {
            let _ = MapAccessDeserializer::new(Access(true)).variant::<i32>();
        }))
        .unwrap_err();
        assert_eq!(panic_text(p), "access panic");
    }
    #[test]
    fn key_conversion_panic_retains_pending_value() {
        let calls = Rc::new(Cell::new(0));
        let mut map = MapDeserializer::<_, Error>::new(
            vec![(
                Value {
                    calls: calls.clone(),
                    number: 1,
                    panic: true,
                },
                10_i32,
            )]
            .into_iter(),
        );
        let p = catch_unwind(AssertUnwindSafe(|| {
            let _ = MapAccessDeserializer::new(&mut map).variant::<i32>();
        }))
        .unwrap_err();
        assert_eq!(panic_text(p), "conversion panic");
        assert_eq!(calls.get(), 1);
        assert_eq!(map.next_value::<i32>().unwrap(), 10);
    }
    #[test]
    fn typed_seed_error_retains_pending_value() {
        let mut map = MapDeserializer::<_, Error>::new(vec![(1_i32, 10_i32)].into_iter());
        match MapAccessDeserializer::new(&mut map).variant_seed(Seed(false)) {
            Err(e) => assert_eq!(e.to_string(), "seed error"),
            Ok(_) => panic!("expected error"),
        };
        assert_eq!(map.next_value::<i32>().unwrap(), 10);
    }
    #[test]
    fn empty_map_retains_type_error() {
        let map = MapDeserializer::<_, Error>::new(Vec::<(i32, i32)>::new().into_iter());
        match MapAccessDeserializer::new(map).variant::<i32>() {
            Err(e) => assert_eq!(e.to_string(), "invalid type: map, expected enum"),
            Ok(_) => panic!("expected error"),
        };
    }
    #[test]
    fn success_routes_payload() {
        let map = MapDeserializer::<_, Error>::new(vec![(1_i32, 10_i32)].into_iter());
        let (key, variant) = MapAccessDeserializer::new(map).variant::<i32>().unwrap();
        assert_eq!(key, 1);
        assert_eq!(variant.newtype_variant::<i32>().unwrap(), 10);
    }
    #[test]
    fn payload_conversion_is_deferred() {
        let calls = Rc::new(Cell::new(0));
        let map = MapDeserializer::<_, Error>::new(
            vec![(
                1_i32,
                Value {
                    calls: calls.clone(),
                    number: 10,
                    panic: true,
                },
            )]
            .into_iter(),
        );
        let (key, variant) = MapAccessDeserializer::new(map).variant::<i32>().unwrap();
        assert_eq!(key, 1);
        assert_eq!(calls.get(), 0);
        let p = catch_unwind(AssertUnwindSafe(|| {
            let _ = variant.newtype_variant::<i32>();
        }))
        .unwrap_err();
        assert_eq!(panic_text(p), "conversion panic");
        assert_eq!(calls.get(), 1);
    }
    #[test]
    fn typed_access_error_propagates() {
        match MapAccessDeserializer::new(Access(false)).variant::<i32>() {
            Err(e) => assert_eq!(e.to_string(), "access error"),
            Ok(_) => panic!("expected error"),
        };
    }
}
