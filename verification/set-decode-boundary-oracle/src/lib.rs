#[cfg(test)]
mod tests {
    use serde_core::{
        de::{value::Error, DeserializeSeed, Deserializer, IntoDeserializer, SeqAccess, Visitor},
        Deserialize,
    };
    use std::{
        cell::{Cell, RefCell},
        collections::HashSet,
        panic::{catch_unwind, AssertUnwindSafe},
        rc::Rc,
    };
    thread_local! {static TRACE:RefCell<Vec<i32>>=const{RefCell::new(Vec::new())};}
    #[derive(Debug, PartialEq, Eq, Hash)]
    struct Item(i32);
    impl<'de> Deserialize<'de> for Item {
        fn deserialize<D: Deserializer<'de>>(d: D) -> Result<Self, D::Error> {
            let n = i32::deserialize(d)?;
            TRACE.with(|t| t.borrow_mut().push(n));
            match n {
                -2 => panic!("codec panic"),
                -1 => Err(serde_core::de::Error::custom("codec error")),
                _ => Ok(Item(n)),
            }
        }
    }
    struct Access {
        values: std::vec::IntoIter<i32>,
        mode: u8,
        polls: Rc<Cell<usize>>,
        hints: Rc<Cell<usize>>,
    }
    impl<'de> SeqAccess<'de> for Access {
        type Error = Error;
        fn next_element_seed<T: DeserializeSeed<'de>>(
            &mut self,
            seed: T,
        ) -> Result<Option<T::Value>, Error> {
            self.polls.set(self.polls.get() + 1);
            if self.mode == 2 {
                panic!("access panic")
            }
            match self.values.next() {
                Some(n) => seed.deserialize(n.into_deserializer()).map(Some),
                None => Ok(None),
            }
        }
        fn size_hint(&self) -> Option<usize> {
            self.hints.set(self.hints.get() + 1);
            if self.mode == 1 {
                panic!("hint panic")
            }
            Some(self.values.len())
        }
    }
    struct Driver(Access);
    impl<'de> Deserializer<'de> for Driver {
        type Error = Error;
        fn deserialize_any<V: Visitor<'de>>(self, visitor: V) -> Result<V::Value, Error> {
            visitor.visit_seq(self.0)
        }
        serde_core::forward_to_deserialize_any! {bool i8 i16 i32 i64 i128 u8 u16 u32 u64 u128 f32 f64 char str string bytes byte_buf option unit unit_struct newtype_struct seq tuple tuple_struct map struct enum identifier ignored_any}
    }
    type Outcome = Result<Result<HashSet<Item>, Error>, Box<dyn std::any::Any + Send>>;
    fn observe(input: Vec<i32>, mode: u8) -> (Outcome, usize, usize, Vec<i32>) {
        TRACE.with(|t| t.borrow_mut().clear());
        let p = Rc::new(Cell::new(0));
        let h = Rc::new(Cell::new(0));
        let a = Access {
            values: input.into_iter(),
            mode,
            polls: p.clone(),
            hints: h.clone(),
        };
        let outcome = catch_unwind(AssertUnwindSafe(|| HashSet::<Item>::deserialize(Driver(a))));
        (
            outcome,
            p.get(),
            h.get(),
            TRACE.with(|t| t.borrow().clone()),
        )
    }
    fn panic_text(p: Box<dyn std::any::Any + Send>) -> String {
        if let Some(s) = p.downcast_ref::<&str>() {
            s.to_string()
        } else {
            p.downcast_ref::<String>().unwrap().clone()
        }
    }
    #[test]
    fn success_preserves_members() {
        let (o, p, h, t) = observe(vec![1, 2], 0);
        let values = o.unwrap().unwrap();
        assert_eq!(values.len(), 2);
        assert!(values.contains(&Item(1)));
        assert!(values.contains(&Item(2)));
        assert_eq!((p, h, t), (3, 1, vec![1, 2]))
    }
    #[test]
    fn empty_succeeds() {
        let (o, p, h, t) = observe(vec![], 0);
        assert!(o.unwrap().unwrap().is_empty());
        assert_eq!((p, h, t), (1, 1, vec![]))
    }
    #[test]
    fn hint_panic_precedes_access() {
        let (o, p, h, t) = observe(vec![1, 2], 1);
        assert_eq!(panic_text(o.unwrap_err()), "hint panic");
        assert_eq!((p, h, t), (0, 1, vec![]))
    }
    #[test]
    fn access_panic_precedes_codec() {
        let (o, p, h, t) = observe(vec![1, 2], 2);
        assert_eq!(panic_text(o.unwrap_err()), "access panic");
        assert_eq!((p, h, t), (1, 1, vec![]))
    }
    #[test]
    fn first_codec_panic_stops_input() {
        let (o, p, h, t) = observe(vec![-2, 3], 0);
        assert_eq!(panic_text(o.unwrap_err()), "codec panic");
        assert_eq!((p, h, t), (1, 1, vec![-2]))
    }
    #[test]
    fn second_codec_panic_stops_input() {
        let (o, p, h, t) = observe(vec![1, -2, 3], 0);
        assert_eq!(panic_text(o.unwrap_err()), "codec panic");
        assert_eq!((p, h, t), (2, 1, vec![1, -2]))
    }
    #[test]
    fn first_typed_error_stops_input() {
        let (o, p, h, t) = observe(vec![-1, 3], 0);
        assert_eq!(o.unwrap().unwrap_err().to_string(), "codec error");
        assert_eq!((p, h, t), (1, 1, vec![-1]))
    }
    #[test]
    fn second_typed_error_stops_input() {
        let (o, p, h, t) = observe(vec![1, -1, 3], 0);
        assert_eq!(o.unwrap().unwrap_err().to_string(), "codec error");
        assert_eq!((p, h, t), (2, 1, vec![1, -1]))
    }
    #[test]
    fn duplicate_values_are_decoded_before_deduplication() {
        let (o, p, h, t) = observe(vec![1, 1, 2], 0);
        let values = o.unwrap().unwrap();
        assert_eq!(values.len(), 2);
        assert!(values.contains(&Item(1)));
        assert!(values.contains(&Item(2)));
        assert_eq!((p, h, t), (4, 1, vec![1, 1, 2]));
    }
}
