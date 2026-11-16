#[cfg(test)]
mod tests {
    use serde_core::{
        de::{
            value::{Error, MapDeserializer},
            DeserializeSeed, Deserializer, MapAccess, Visitor,
        },
        Deserialize,
    };
    use std::{
        cell::{Cell, RefCell},
        collections::HashMap,
        panic::{catch_unwind, AssertUnwindSafe},
        rc::Rc,
    };
    thread_local! {static TRACE:RefCell<Vec<String>>=const{RefCell::new(Vec::new())};}
    #[derive(Debug, PartialEq, Eq, Hash)]
    struct Item<const KEY: bool>(i32);
    impl<'de, const KEY: bool> Deserialize<'de> for Item<KEY> {
        fn deserialize<D: Deserializer<'de>>(d: D) -> Result<Self, D::Error> {
            let n = i32::deserialize(d)?;
            let label = if KEY { "key" } else { "value" };
            TRACE.with(|t| t.borrow_mut().push(format!("{label}:{n}")));
            match n {
                -2 => panic!("{label} panic"),
                -1 => Err(serde_core::de::Error::custom(format!("{label} error"))),
                _ => Ok(Item(n)),
            }
        }
    }
    struct Access {
        inner: MapDeserializer<'static, std::vec::IntoIter<(i32, i32)>, Error>,
        mode: u8,
        keys: Rc<Cell<usize>>,
        values: Rc<Cell<usize>>,
        hints: Rc<Cell<usize>>,
    }
    impl MapAccess<'static> for Access {
        type Error = Error;
        fn next_key_seed<K: DeserializeSeed<'static>>(
            &mut self,
            seed: K,
        ) -> Result<Option<K::Value>, Error> {
            self.keys.set(self.keys.get() + 1);
            if self.mode == 2 {
                panic!("key access panic")
            }
            self.inner.next_key_seed(seed)
        }
        fn next_value_seed<V: DeserializeSeed<'static>>(
            &mut self,
            seed: V,
        ) -> Result<V::Value, Error> {
            self.values.set(self.values.get() + 1);
            if self.mode == 3 {
                panic!("value access panic")
            }
            self.inner.next_value_seed(seed)
        }
        fn size_hint(&self) -> Option<usize> {
            self.hints.set(self.hints.get() + 1);
            if self.mode == 1 {
                panic!("hint panic")
            }
            self.inner.size_hint()
        }
    }
    struct Driver(Access);
    impl Deserializer<'static> for Driver {
        type Error = Error;
        fn deserialize_any<V: Visitor<'static>>(self, v: V) -> Result<V::Value, Error> {
            v.visit_map(self.0)
        }
        serde_core::forward_to_deserialize_any! {<V: Visitor<'static>> bool i8 i16 i32 i64 i128 u8 u16 u32 u64 u128 f32 f64 char str string bytes byte_buf option unit unit_struct newtype_struct seq tuple tuple_struct map struct enum identifier ignored_any}
    }
    type Data = HashMap<Item<true>, Item<false>>;
    type Outcome = Result<Result<Data, Error>, Box<dyn std::any::Any + Send>>;
    fn observe(input: Vec<(i32, i32)>, mode: u8) -> (Outcome, usize, usize, usize, Vec<String>) {
        TRACE.with(|t| t.borrow_mut().clear());
        let k = Rc::new(Cell::new(0));
        let v = Rc::new(Cell::new(0));
        let h = Rc::new(Cell::new(0));
        let a = Access {
            inner: MapDeserializer::new(input.into_iter()),
            mode,
            keys: k.clone(),
            values: v.clone(),
            hints: h.clone(),
        };
        let o = catch_unwind(AssertUnwindSafe(|| Data::deserialize(Driver(a))));
        (
            o,
            k.get(),
            v.get(),
            h.get(),
            TRACE.with(|t| t.borrow().clone()),
        )
    }
    fn observations(
        k: usize,
        v: usize,
        h: usize,
        t: Vec<String>,
        expected: (usize, usize, usize, &[&str]),
    ) {
        assert_eq!((k, v, h), (expected.0, expected.1, expected.2));
        assert_eq!(t.iter().map(String::as_str).collect::<Vec<_>>(), expected.3)
    }
    fn panic_text(p: Box<dyn std::any::Any + Send>) -> String {
        if let Some(s) = p.downcast_ref::<&str>() {
            s.to_string()
        } else {
            p.downcast_ref::<String>().unwrap().clone()
        }
    }
    #[test]
    fn success_preserves_entries() {
        let (o, k, v, h, t) = observe(vec![(1, 10), (2, 20)], 0);
        let data = o.unwrap().unwrap();
        assert_eq!(data.len(), 2);
        assert_eq!(data.get(&Item(1)), Some(&Item(10)));
        assert_eq!(data.get(&Item(2)), Some(&Item(20)));
        observations(
            k,
            v,
            h,
            t,
            (3, 2, 1, &["key:1", "value:10", "key:2", "value:20"]),
        )
    }
    #[test]
    fn duplicate_key_keeps_last_value() {
        let (o, k, v, h, t) = observe(vec![(1, 10), (1, 20)], 0);
        let data = o.unwrap().unwrap();
        assert_eq!(data.len(), 1);
        assert_eq!(data.get(&Item(1)), Some(&Item(20)));
        observations(
            k,
            v,
            h,
            t,
            (3, 2, 1, &["key:1", "value:10", "key:1", "value:20"]),
        )
    }
    #[test]
    fn empty_succeeds() {
        let (o, k, v, h, t) = observe(vec![], 0);
        assert!(o.unwrap().unwrap().is_empty());
        observations(k, v, h, t, (1, 0, 1, &[]))
    }
    #[test]
    fn hint_panic_precedes_access() {
        let (o, k, v, h, t) = observe(vec![(1, 10)], 1);
        assert_eq!(panic_text(o.unwrap_err()), "hint panic");
        observations(k, v, h, t, (0, 0, 1, &[]))
    }
    #[test]
    fn key_access_panic_skips_value() {
        let (o, k, v, h, t) = observe(vec![(1, 10)], 2);
        assert_eq!(panic_text(o.unwrap_err()), "key access panic");
        observations(k, v, h, t, (1, 0, 1, &[]))
    }
    #[test]
    fn value_access_panic_follows_key() {
        let (o, k, v, h, t) = observe(vec![(1, 10)], 3);
        assert_eq!(panic_text(o.unwrap_err()), "value access panic");
        observations(k, v, h, t, (1, 1, 1, &["key:1"]))
    }
    #[test]
    fn key_codec_panic_skips_value() {
        let (o, k, v, h, t) = observe(vec![(-2, 10), (2, 20)], 0);
        assert_eq!(panic_text(o.unwrap_err()), "key panic");
        observations(k, v, h, t, (1, 0, 1, &["key:-2"]))
    }
    #[test]
    fn value_codec_panic_stops_suffix() {
        let (o, k, v, h, t) = observe(vec![(1, -2), (2, 20)], 0);
        assert_eq!(panic_text(o.unwrap_err()), "value panic");
        observations(k, v, h, t, (1, 1, 1, &["key:1", "value:-2"]))
    }
    #[test]
    fn typed_key_error_skips_value() {
        let (o, k, v, h, t) = observe(vec![(-1, 10), (2, 20)], 0);
        assert_eq!(o.unwrap().unwrap_err().to_string(), "key error");
        observations(k, v, h, t, (1, 0, 1, &["key:-1"]))
    }
    #[test]
    fn typed_value_error_stops_suffix() {
        let (o, k, v, h, t) = observe(vec![(1, -1), (2, 20)], 0);
        assert_eq!(o.unwrap().unwrap_err().to_string(), "value error");
        observations(k, v, h, t, (1, 1, 1, &["key:1", "value:-1"]))
    }
}
