#[cfg(test)]
mod tests {
    use serde_core::de::{
        value::Error, DeserializeSeed, Deserializer, EnumAccess, IgnoredAny, MapAccess, SeqAccess,
        VariantAccess, Visitor,
    };
    use std::{
        cell::{Cell, RefCell},
        panic::{catch_unwind, AssertUnwindSafe},
        rc::Rc,
    };
    thread_local! {static TRACE:RefCell<Vec<String>>=const{RefCell::new(Vec::new())};}
    fn trace(s: String) {
        TRACE.with(|t| t.borrow_mut().push(s));
    }
    fn probe(mode: u8, stage: &str, target: &str) -> Result<(), Error> {
        if stage == target {
            match mode {
                1 => return Err(serde_core::de::Error::custom("probe error")),
                2 => panic!("probe panic"),
                _ => {}
            }
        }
        Ok(())
    }
    struct Scalar(i32);
    impl<'de> Deserializer<'de> for Scalar {
        type Error = Error;
        fn deserialize_any<V: Visitor<'de>>(self, _: V) -> Result<V::Value, Error> {
            panic!("unexpected scalar path")
        }
        fn deserialize_ignored_any<V: Visitor<'de>>(self, visitor: V) -> Result<V::Value, Error> {
            trace(format!("discard:{}", self.0));
            visitor.visit_i32(self.0)
        }
        serde_core::forward_to_deserialize_any! {bool i8 i16 i32 i64 i128 u8 u16 u32 u64 u128 f32 f64 char str string bytes byte_buf option unit unit_struct newtype_struct seq tuple tuple_struct map struct enum identifier}
    }
    struct Sequence<'a> {
        index: usize,
        mode: u8,
        target: &'a str,
        length: usize,
        calls: Rc<Cell<usize>>,
        delivered: Rc<Cell<usize>>,
    }
    impl<'de> SeqAccess<'de> for Sequence<'_> {
        type Error = Error;
        fn next_element_seed<T: DeserializeSeed<'de>>(
            &mut self,
            seed: T,
        ) -> Result<Option<T::Value>, Error> {
            let i = self.index;
            self.index += 1;
            self.calls.set(self.calls.get() + 1);
            let stage = format!("seq:{i}");
            trace(stage.clone());
            probe(self.mode, &stage, self.target)?;
            if i >= self.length {
                Ok(None)
            } else {
                self.delivered.set(self.delivered.get() + 1);
                seed.deserialize(Scalar(10 * (i as i32 + 1))).map(Some)
            }
        }
    }
    struct Map<'a> {
        index: usize,
        mode: u8,
        target: &'a str,
    }
    impl<'de> MapAccess<'de> for Map<'_> {
        type Error = Error;
        fn next_key_seed<T: DeserializeSeed<'de>>(
            &mut self,
            seed: T,
        ) -> Result<Option<T::Value>, Error> {
            let stage = format!("key:{}", self.index);
            trace(stage.clone());
            probe(self.mode, &stage, self.target)?;
            if self.index == 2 {
                Ok(None)
            } else {
                seed.deserialize(Scalar(self.index as i32 + 1)).map(Some)
            }
        }
        fn next_value_seed<T: DeserializeSeed<'de>>(&mut self, seed: T) -> Result<T::Value, Error> {
            let i = self.index;
            let stage = format!("value:{i}");
            trace(stage.clone());
            probe(self.mode, &stage, self.target)?;
            self.index += 1;
            seed.deserialize(Scalar(10 * (i as i32 + 1)))
        }
    }
    struct Enum<'a> {
        mode: u8,
        target: &'a str,
    }
    struct Variant<'a> {
        mode: u8,
        target: &'a str,
    }
    impl<'de, 'a> EnumAccess<'de> for Enum<'a> {
        type Error = Error;
        type Variant = Variant<'a>;
        fn variant_seed<T: DeserializeSeed<'de>>(
            self,
            seed: T,
        ) -> Result<(T::Value, Self::Variant), Error> {
            trace("variant".into());
            probe(self.mode, "variant", self.target)?;
            seed.deserialize(Scalar(99)).map(|key| {
                (
                    key,
                    Variant {
                        mode: self.mode,
                        target: self.target,
                    },
                )
            })
        }
    }
    impl<'de> VariantAccess<'de> for Variant<'_> {
        type Error = Error;
        fn newtype_variant_seed<T: DeserializeSeed<'de>>(self, seed: T) -> Result<T::Value, Error> {
            trace("payload".into());
            probe(self.mode, "payload", self.target)?;
            seed.deserialize(Scalar(100))
        }
        fn unit_variant(self) -> Result<(), Error> {
            panic!("unexpected unit variant")
        }
        fn tuple_variant<V: Visitor<'de>>(self, _: usize, _: V) -> Result<V::Value, Error> {
            panic!("unexpected tuple variant")
        }
        fn struct_variant<V: Visitor<'de>>(
            self,
            _: &'static [&'static str],
            _: V,
        ) -> Result<V::Value, Error> {
            panic!("unexpected struct variant")
        }
    }
    fn check(kind: &str, mode: u8, target: &str, outcome: &str, events: &[&str]) {
        TRACE.with(|t| t.borrow_mut().clear());
        let result = catch_unwind(AssertUnwindSafe(|| match kind {
            "seq" => IgnoredAny.visit_seq(Sequence {
                index: 0,
                mode,
                target,
                length: 2,
                calls: Rc::new(Cell::new(0)),
                delivered: Rc::new(Cell::new(0)),
            }),
            "map" => IgnoredAny.visit_map(Map {
                index: 0,
                mode,
                target,
            }),
            "enum" => IgnoredAny.visit_enum(Enum { mode, target }),
            _ => unreachable!(),
        }));
        let actual = match result {
            Ok(Ok(_)) => "ok".into(),
            Ok(Err(e)) => format!("error:{e}"),
            Err(e) => {
                let s = if let Some(s) = e.downcast_ref::<&str>() {
                    s.to_string()
                } else {
                    e.downcast_ref::<String>().unwrap().clone()
                };
                format!("panic:{s}")
            }
        };
        assert_eq!(actual, outcome);
        TRACE.with(|t| assert_eq!(*t.borrow(), events));
    }
    #[test]
    #[allow(non_snake_case)]
    fn seqSuccess() {
        check(
            "seq",
            0,
            "",
            "ok",
            &["seq:0", "discard:10", "seq:1", "discard:20", "seq:2"],
        );
    }
    #[test]
    #[allow(non_snake_case)]
    fn seqErrors() {
        check("seq", 1, "seq:0", "error:probe error", &["seq:0"]);
        check(
            "seq",
            1,
            "seq:1",
            "error:probe error",
            &["seq:0", "discard:10", "seq:1"],
        );
        check(
            "seq",
            1,
            "seq:2",
            "error:probe error",
            &["seq:0", "discard:10", "seq:1", "discard:20", "seq:2"],
        );
    }
    #[test]
    #[allow(non_snake_case)]
    fn seqPanics() {
        check("seq", 2, "seq:0", "panic:probe panic", &["seq:0"]);
        check(
            "seq",
            2,
            "seq:1",
            "panic:probe panic",
            &["seq:0", "discard:10", "seq:1"],
        );
        check(
            "seq",
            2,
            "seq:2",
            "panic:probe panic",
            &["seq:0", "discard:10", "seq:1", "discard:20", "seq:2"],
        );
    }
    #[test]
    #[allow(non_snake_case)]
    fn mapSuccess() {
        check(
            "map",
            0,
            "",
            "ok",
            &[
                "key:0",
                "discard:1",
                "value:0",
                "discard:10",
                "key:1",
                "discard:2",
                "value:1",
                "discard:20",
                "key:2",
            ],
        );
    }
    #[test]
    #[allow(non_snake_case)]
    fn mapErrors() {
        check(
            "map",
            1,
            "key:1",
            "error:probe error",
            &["key:0", "discard:1", "value:0", "discard:10", "key:1"],
        );
        check(
            "map",
            1,
            "value:1",
            "error:probe error",
            &[
                "key:0",
                "discard:1",
                "value:0",
                "discard:10",
                "key:1",
                "discard:2",
                "value:1",
            ],
        );
    }
    #[test]
    #[allow(non_snake_case)]
    fn mapPanics() {
        check(
            "map",
            2,
            "key:1",
            "panic:probe panic",
            &["key:0", "discard:1", "value:0", "discard:10", "key:1"],
        );
        check(
            "map",
            2,
            "value:1",
            "panic:probe panic",
            &[
                "key:0",
                "discard:1",
                "value:0",
                "discard:10",
                "key:1",
                "discard:2",
                "value:1",
            ],
        );
    }
    #[test]
    #[allow(non_snake_case)]
    fn enumSuccess() {
        check(
            "enum",
            0,
            "",
            "ok",
            &["variant", "discard:99", "payload", "discard:100"],
        );
    }
    #[test]
    #[allow(non_snake_case)]
    fn enumErrors() {
        check("enum", 1, "variant", "error:probe error", &["variant"]);
        check(
            "enum",
            1,
            "payload",
            "error:probe error",
            &["variant", "discard:99", "payload"],
        );
    }
    #[test]
    #[allow(non_snake_case)]
    fn enumPanics() {
        check("enum", 2, "variant", "panic:probe panic", &["variant"]);
        check(
            "enum",
            2,
            "payload",
            "panic:probe panic",
            &["variant", "discard:99", "payload"],
        );
    }

    fn check_drain(
        length: usize,
        mode: u8,
        at: usize,
        outcome: &str,
        expected_calls: usize,
        expected_delivered: usize,
        events: &[&str],
    ) {
        TRACE.with(|t| t.borrow_mut().clear());
        let calls = Rc::new(Cell::new(0));
        let delivered = Rc::new(Cell::new(0));
        let target = format!("seq:{at}");
        let result = catch_unwind(AssertUnwindSafe(|| {
            IgnoredAny.visit_seq(Sequence {
                index: 0,
                mode,
                target: &target,
                length,
                calls: calls.clone(),
                delivered: delivered.clone(),
            })
        }));
        let actual = match result {
            Ok(Ok(_)) => "ok".into(),
            Ok(Err(e)) => format!("error:{e}"),
            Err(e) => {
                let message = if let Some(s) = e.downcast_ref::<&str>() {
                    s.to_string()
                } else {
                    e.downcast_ref::<String>().unwrap().clone()
                };
                format!("panic:{message}")
            }
        };
        assert_eq!(actual, outcome);
        assert_eq!(calls.get(), expected_calls);
        assert_eq!(delivered.get(), expected_delivered);
        TRACE.with(|t| assert_eq!(*t.borrow(), events));
    }
    #[test]
    #[allow(non_snake_case)]
    fn zeroElementsStopAfterOneRead() {
        check_drain(0, 0, 0, "ok", 1, 0, &["seq:0"]);
    }
    #[test]
    #[allow(non_snake_case)]
    fn oneElementStopsAfterTwoReads() {
        check_drain(1, 0, 0, "ok", 2, 1, &["seq:0", "discard:10", "seq:1"]);
    }
    #[test]
    #[allow(non_snake_case)]
    fn threeElementsStopAfterFourReads() {
        check_drain(
            3,
            0,
            0,
            "ok",
            4,
            3,
            &[
                "seq:0",
                "discard:10",
                "seq:1",
                "discard:20",
                "seq:2",
                "discard:30",
                "seq:3",
            ],
        );
    }
    #[test]
    #[allow(non_snake_case)]
    fn oddLengthFailureTiming() {
        check_drain(1, 1, 0, "error:probe error", 1, 0, &["seq:0"]);
        check_drain(1, 2, 0, "panic:probe panic", 1, 0, &["seq:0"]);
        check_drain(
            1,
            1,
            1,
            "error:probe error",
            2,
            1,
            &["seq:0", "discard:10", "seq:1"],
        );
        check_drain(
            1,
            2,
            1,
            "panic:probe panic",
            2,
            1,
            &["seq:0", "discard:10", "seq:1"],
        );
        check_drain(
            3,
            1,
            2,
            "error:probe error",
            3,
            2,
            &["seq:0", "discard:10", "seq:1", "discard:20", "seq:2"],
        );
        check_drain(
            3,
            2,
            2,
            "panic:probe panic",
            3,
            2,
            &["seq:0", "discard:10", "seq:1", "discard:20", "seq:2"],
        );
        check_drain(
            3,
            1,
            3,
            "error:probe error",
            4,
            3,
            &[
                "seq:0",
                "discard:10",
                "seq:1",
                "discard:20",
                "seq:2",
                "discard:30",
                "seq:3",
            ],
        );
        check_drain(
            3,
            2,
            3,
            "panic:probe panic",
            4,
            3,
            &[
                "seq:0",
                "discard:10",
                "seq:1",
                "discard:20",
                "seq:2",
                "discard:30",
                "seq:3",
            ],
        );
    }
}
