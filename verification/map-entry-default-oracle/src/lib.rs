#[cfg(test)]
mod tests {
    use serde_core::{
        de::{
            value::{Error, MapDeserializer},
            DeserializeSeed, Deserializer, MapAccess,
        },
        Deserialize,
    };
    use std::panic::{catch_unwind, AssertUnwindSafe};
    struct Seed(u8);
    impl<'de> DeserializeSeed<'de> for Seed {
        type Value = i32;
        fn deserialize<D: Deserializer<'de>>(self, d: D) -> Result<i32, D::Error> {
            match self.0 {
                0 => i32::deserialize(d),
                1 => panic!("seed panic"),
                _ => Err(serde_core::de::Error::custom("seed error")),
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
    struct Access {
        inner: MapDeserializer<'static, std::vec::IntoIter<(i32, i32)>, Error>,
        panic_at: u8,
        key_calls: usize,
        value_calls: usize,
    }
    impl MapAccess<'static> for Access {
        type Error = Error;
        fn next_key_seed<K: DeserializeSeed<'static>>(
            &mut self,
            seed: K,
        ) -> Result<Option<K::Value>, Error> {
            self.key_calls += 1;
            if self.panic_at == 1 {
                panic!("key access panic")
            }
            self.inner.next_key_seed(seed)
        }
        fn next_value_seed<V: DeserializeSeed<'static>>(
            &mut self,
            seed: V,
        ) -> Result<V::Value, Error> {
            self.value_calls += 1;
            if self.panic_at == 2 {
                panic!("value access panic")
            }
            self.inner.next_value_seed(seed)
        }
    }
    fn access(empty: bool, panic_at: u8) -> Access {
        Access {
            inner: MapDeserializer::new(if empty { vec![] } else { vec![(1, 10)] }.into_iter()),
            panic_at,
            key_calls: 0,
            value_calls: 0,
        }
    }
    fn counts(a: &Access, k: usize, v: usize) {
        assert_eq!(a.key_calls, k);
        assert_eq!(a.value_calls, v)
    }
    fn panic_text(p: Box<dyn std::any::Any + Send>) -> String {
        if let Some(s) = p.downcast_ref::<&str>() {
            s.to_string()
        } else {
            p.downcast_ref::<String>().unwrap().clone()
        }
    }
    #[test]
    fn key_seed_panic_skips_value() {
        let mut a = access(false, 0);
        let p = catch_unwind(AssertUnwindSafe(|| {
            let _ = a.next_entry_seed(Seed(1), Seed(0));
        }))
        .unwrap_err();
        assert_eq!(panic_text(p), "seed panic");
        counts(&a, 1, 0);
        assert_eq!(a.inner.next_value::<i32>().unwrap(), 10)
    }
    #[test]
    fn value_seed_panic_consumes_value() {
        let mut a = access(false, 0);
        let p = catch_unwind(AssertUnwindSafe(|| {
            let _ = a.next_entry_seed(Seed(0), Seed(1));
        }))
        .unwrap_err();
        assert_eq!(panic_text(p), "seed panic");
        counts(&a, 1, 1);
        assert!(a.inner.next_entry::<i32, i32>().unwrap().is_none())
    }
    #[test]
    fn key_access_panic_skips_value() {
        let mut a = access(false, 1);
        let p = catch_unwind(AssertUnwindSafe(|| {
            let _ = a.next_entry_seed(Seed(0), Seed(0));
        }))
        .unwrap_err();
        assert_eq!(panic_text(p), "key access panic");
        counts(&a, 1, 0)
    }
    #[test]
    fn value_access_panic_retains_pending_value() {
        let mut a = access(false, 2);
        let p = catch_unwind(AssertUnwindSafe(|| {
            let _ = a.next_entry_seed(Seed(0), Seed(0));
        }))
        .unwrap_err();
        assert_eq!(panic_text(p), "value access panic");
        counts(&a, 1, 1);
        assert_eq!(a.inner.next_value::<i32>().unwrap(), 10)
    }
    #[test]
    fn typed_key_error_skips_value() {
        let mut a = access(false, 0);
        assert_eq!(
            a.next_entry_seed(Seed(2), Seed(0)).unwrap_err().to_string(),
            "seed error"
        );
        counts(&a, 1, 0);
        assert_eq!(a.inner.next_value::<i32>().unwrap(), 10)
    }
    #[test]
    fn typed_value_error_consumes_value() {
        let mut a = access(false, 0);
        assert_eq!(
            a.next_entry_seed(Seed(0), Seed(2)).unwrap_err().to_string(),
            "seed error"
        );
        counts(&a, 1, 1);
        assert!(a.inner.next_entry::<i32, i32>().unwrap().is_none())
    }
    #[test]
    fn empty_skips_value() {
        let mut a = access(true, 0);
        assert_eq!(a.next_entry_seed(Seed(0), Seed(0)).unwrap(), None);
        counts(&a, 1, 0)
    }
    #[test]
    fn success_preserves_pair() {
        let mut a = access(false, 0);
        assert_eq!(a.next_entry_seed(Seed(0), Seed(0)).unwrap(), Some((1, 10)));
        counts(&a, 1, 1)
    }
    #[test]
    fn nullable_value_preserves_present_pair() {
        let mut a = access(false, 0);
        assert_eq!(
            a.next_entry_seed(Seed(0), NullSeed).unwrap(),
            Some((1, None))
        );
        counts(&a, 1, 1)
    }
}
