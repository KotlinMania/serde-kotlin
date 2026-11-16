#[cfg(test)]
mod tests {
    use serde_core::{
        de::{value::Error, DeserializeSeed, Deserializer, IntoDeserializer, SeqAccess, Visitor},
        Deserialize,
    };
    use std::{
        cell::RefCell,
        panic::{catch_unwind, AssertUnwindSafe},
    };
    thread_local! {static TRACE:RefCell<Vec<String>>=const{RefCell::new(Vec::new())};}
    fn trace(s: String) {
        TRACE.with(|t| t.borrow_mut().push(s));
    }
    struct Item(i32);
    impl<'de> Deserialize<'de> for Item {
        fn deserialize<D: Deserializer<'de>>(d: D) -> Result<Self, D::Error> {
            let n = i32::deserialize(d)?;
            trace(format!("value:{n}"));
            match n {
                -1 => Err(serde_core::de::Error::custom("codec error")),
                -2 => panic!("codec panic"),
                _ => Ok(Item(n)),
            }
        }
    }
    struct Access {
        values: std::vec::IntoIter<i32>,
        index: usize,
        mode: u8,
        at: usize,
    }
    impl<'de> SeqAccess<'de> for Access {
        type Error = Error;
        fn next_element_seed<T: DeserializeSeed<'de>>(
            &mut self,
            seed: T,
        ) -> Result<Option<T::Value>, Error> {
            let index = self.index;
            self.index += 1;
            trace(format!("pull:{index}"));
            if index == self.at {
                match self.mode {
                    1 => return Err(serde_core::de::Error::custom("access error")),
                    2 => panic!("access panic"),
                    _ => {}
                }
            }
            match self.values.next() {
                Some(n) => seed.deserialize(n.into_deserializer()).map(Some),
                None => Ok(None),
            }
        }
    }
    struct Driver {
        arity: usize,
        access: Access,
    }
    impl<'de> Deserializer<'de> for Driver {
        type Error = Error;
        fn deserialize_any<V: Visitor<'de>>(self, _: V) -> Result<V::Value, Error> {
            panic!("unexpected driver path")
        }
        fn deserialize_tuple<V: Visitor<'de>>(
            self,
            len: usize,
            visitor: V,
        ) -> Result<V::Value, Error> {
            assert_eq!(len, self.arity);
            trace(format!("tuple:{len}"));
            match self.access.mode {
                3 => Err(serde_core::de::Error::custom("backend error")),
                4 => panic!("backend panic"),
                _ => visitor.visit_seq(self.access),
            }
        }
        serde_core::forward_to_deserialize_any! {bool i8 i16 i32 i64 i128 u8 u16 u32 u64 u128 f32 f64 char str string bytes byte_buf option unit unit_struct newtype_struct seq tuple_struct map struct enum identifier ignored_any}
    }
    fn check(
        arity: usize,
        input: &[i32],
        mode: u8,
        at: usize,
        outcome: &str,
        values: &[i32],
        events: &[&str],
    ) {
        TRACE.with(|t| t.borrow_mut().clear());
        let d = Driver {
            arity,
            access: Access {
                values: input.to_vec().into_iter(),
                index: 0,
                mode,
                at,
            },
        };
        let result = catch_unwind(AssertUnwindSafe(|| {
            if arity == 2 {
                <(Item, Item)>::deserialize(d).map(|(a, b)| vec![a.0, b.0])
            } else {
                <(Item, Item, Item)>::deserialize(d).map(|(a, b, c)| vec![a.0, b.0, c.0])
            }
        }));
        let (actual, decoded) = match result {
            Ok(Ok(v)) => ("ok".into(), v),
            Ok(Err(e)) => (format!("error:{e}"), vec![]),
            Err(e) => {
                let message = if let Some(s) = e.downcast_ref::<&str>() {
                    s.to_string()
                } else {
                    e.downcast_ref::<String>().unwrap().clone()
                };
                (format!("panic:{message}"), vec![])
            }
        };
        assert_eq!(actual, outcome);
        assert_eq!(decoded, values);
        TRACE.with(|t| assert_eq!(*t.borrow(), events));
    }
    #[test]
    #[allow(non_snake_case)]
    fn success() {
        check(
            2,
            &[10, 20],
            0,
            0,
            "ok",
            &[10, 20],
            &["tuple:2", "pull:0", "value:10", "pull:1", "value:20"],
        );
        check(
            3,
            &[10, 20, 30],
            0,
            0,
            "ok",
            &[10, 20, 30],
            &[
                "tuple:3", "pull:0", "value:10", "pull:1", "value:20", "pull:2", "value:30",
            ],
        );
    }
    #[test]
    #[allow(non_snake_case)]
    fn missingElements() {
        check(
            2,
            &[],
            0,
            0,
            "error:invalid length 0, expected a tuple of size 2",
            &[],
            &["tuple:2", "pull:0"],
        );
        check(
            2,
            &[10],
            0,
            0,
            "error:invalid length 1, expected a tuple of size 2",
            &[],
            &["tuple:2", "pull:0", "value:10", "pull:1"],
        );
        check(
            3,
            &[],
            0,
            0,
            "error:invalid length 0, expected a tuple of size 3",
            &[],
            &["tuple:3", "pull:0"],
        );
        check(
            3,
            &[10],
            0,
            0,
            "error:invalid length 1, expected a tuple of size 3",
            &[],
            &["tuple:3", "pull:0", "value:10", "pull:1"],
        );
        check(
            3,
            &[10, 20],
            0,
            0,
            "error:invalid length 2, expected a tuple of size 3",
            &[],
            &[
                "tuple:3", "pull:0", "value:10", "pull:1", "value:20", "pull:2",
            ],
        );
    }
    #[test]
    #[allow(non_snake_case)]
    fn codecErrors() {
        check(
            2,
            &[-1, 20],
            0,
            0,
            "error:codec error",
            &[],
            &["tuple:2", "pull:0", "value:-1"],
        );
        check(
            2,
            &[10, -1],
            0,
            0,
            "error:codec error",
            &[],
            &["tuple:2", "pull:0", "value:10", "pull:1", "value:-1"],
        );
        check(
            3,
            &[-1, 20, 30],
            0,
            0,
            "error:codec error",
            &[],
            &["tuple:3", "pull:0", "value:-1"],
        );
        check(
            3,
            &[10, -1, 30],
            0,
            0,
            "error:codec error",
            &[],
            &["tuple:3", "pull:0", "value:10", "pull:1", "value:-1"],
        );
        check(
            3,
            &[10, 20, -1],
            0,
            0,
            "error:codec error",
            &[],
            &[
                "tuple:3", "pull:0", "value:10", "pull:1", "value:20", "pull:2", "value:-1",
            ],
        );
    }
    #[test]
    #[allow(non_snake_case)]
    fn codecPanics() {
        check(
            2,
            &[-2, 20],
            0,
            0,
            "panic:codec panic",
            &[],
            &["tuple:2", "pull:0", "value:-2"],
        );
        check(
            2,
            &[10, -2],
            0,
            0,
            "panic:codec panic",
            &[],
            &["tuple:2", "pull:0", "value:10", "pull:1", "value:-2"],
        );
        check(
            3,
            &[-2, 20, 30],
            0,
            0,
            "panic:codec panic",
            &[],
            &["tuple:3", "pull:0", "value:-2"],
        );
        check(
            3,
            &[10, -2, 30],
            0,
            0,
            "panic:codec panic",
            &[],
            &["tuple:3", "pull:0", "value:10", "pull:1", "value:-2"],
        );
        check(
            3,
            &[10, 20, -2],
            0,
            0,
            "panic:codec panic",
            &[],
            &[
                "tuple:3", "pull:0", "value:10", "pull:1", "value:20", "pull:2", "value:-2",
            ],
        );
    }
    #[test]
    #[allow(non_snake_case)]
    fn accessErrors() {
        check(
            2,
            &[10, 20],
            1,
            0,
            "error:access error",
            &[],
            &["tuple:2", "pull:0"],
        );
        check(
            2,
            &[10, 20],
            1,
            1,
            "error:access error",
            &[],
            &["tuple:2", "pull:0", "value:10", "pull:1"],
        );
        check(
            3,
            &[10, 20, 30],
            1,
            0,
            "error:access error",
            &[],
            &["tuple:3", "pull:0"],
        );
        check(
            3,
            &[10, 20, 30],
            1,
            1,
            "error:access error",
            &[],
            &["tuple:3", "pull:0", "value:10", "pull:1"],
        );
        check(
            3,
            &[10, 20, 30],
            1,
            2,
            "error:access error",
            &[],
            &[
                "tuple:3", "pull:0", "value:10", "pull:1", "value:20", "pull:2",
            ],
        );
    }
    #[test]
    #[allow(non_snake_case)]
    fn accessPanics() {
        check(
            2,
            &[10, 20],
            2,
            0,
            "panic:access panic",
            &[],
            &["tuple:2", "pull:0"],
        );
        check(
            2,
            &[10, 20],
            2,
            1,
            "panic:access panic",
            &[],
            &["tuple:2", "pull:0", "value:10", "pull:1"],
        );
        check(
            3,
            &[10, 20, 30],
            2,
            0,
            "panic:access panic",
            &[],
            &["tuple:3", "pull:0"],
        );
        check(
            3,
            &[10, 20, 30],
            2,
            1,
            "panic:access panic",
            &[],
            &["tuple:3", "pull:0", "value:10", "pull:1"],
        );
        check(
            3,
            &[10, 20, 30],
            2,
            2,
            "panic:access panic",
            &[],
            &[
                "tuple:3", "pull:0", "value:10", "pull:1", "value:20", "pull:2",
            ],
        );
    }
    #[test]
    #[allow(non_snake_case)]
    fn backendErrors() {
        check(2, &[10, 20], 3, 0, "error:backend error", &[], &["tuple:2"]);
        check(
            3,
            &[10, 20, 30],
            3,
            0,
            "error:backend error",
            &[],
            &["tuple:3"],
        );
    }
    #[test]
    #[allow(non_snake_case)]
    fn backendPanics() {
        check(2, &[10, 20], 4, 0, "panic:backend panic", &[], &["tuple:2"]);
        check(
            3,
            &[10, 20, 30],
            4,
            0,
            "panic:backend panic",
            &[],
            &["tuple:3"],
        );
    }
    #[test]
    fn nullable_presence() {
        use serde_core::de::value::SeqDeserializer;
        let pair = <(Option<i32>, Option<i32>)>::deserialize(SeqDeserializer::<_, Error>::new(
            [(), ()].into_iter(),
        ))
        .unwrap();
        let triple =
            <(Option<i32>, Option<i32>, Option<i32>)>::deserialize(
                SeqDeserializer::<_, Error>::new([(), (), ()].into_iter()),
            )
            .unwrap();
        assert_eq!(pair, (None, None));
        assert_eq!(triple, (None, None, None));
    }
}
