#[cfg(test)]
mod tests {
    use serde_core::{
        de::value::Error,
        ser::{Impossible, SerializeMap},
        Serialize, Serializer,
    };
    use std::{
        cell::{Cell, RefCell},
        panic::{catch_unwind, AssertUnwindSafe},
        rc::Rc,
    };
    #[derive(Clone, Copy, PartialEq)]
    enum Mode {
        Normal,
        FactoryPanic,
        HintPanic,
        BeginError,
        BeginPanic,
        NextPanic,
        EndError,
        EndPanic,
    }
    struct State {
        mode: Mode,
        events: RefCell<Vec<String>>,
        hints: Cell<usize>,
    }
    impl State {
        fn event(&self, s: impl Into<String>) {
            self.events.borrow_mut().push(s.into())
        }
    }
    #[derive(Clone)]
    struct Probe(Rc<State>);
    macro_rules! unsupported_scalar {($($name:ident:$ty:ty),*)=>{$(fn $name(self,_v:$ty)->Result<(),Error>{panic!("unexpected serializer path")})*};}
    impl Serializer for Probe {
        type Ok = ();
        type Error = Error;
        type SerializeSeq = Impossible<(), Error>;
        type SerializeTuple = Impossible<(), Error>;
        type SerializeTupleStruct = Impossible<(), Error>;
        type SerializeTupleVariant = Impossible<(), Error>;
        type SerializeMap = Self;
        type SerializeStruct = Impossible<(), Error>;
        type SerializeStructVariant = Impossible<(), Error>;
        unsupported_scalar! {serialize_bool:bool,serialize_i8:i8,serialize_i16:i16,serialize_i64:i64,serialize_u8:u8,serialize_u16:u16,serialize_u32:u32,serialize_u64:u64,serialize_f32:f32,serialize_f64:f64,serialize_char:char,serialize_str:&str,serialize_bytes:&[u8]}
        fn serialize_i32(self, v: i32) -> Result<(), Error> {
            self.0.event(format!("i32:{v}"));
            match v {
                -1 => Err(serde_core::ser::Error::custom("item error")),
                -2 => panic!("item panic"),
                _ => Ok(()),
            }
        }
        fn serialize_map(self, len: Option<usize>) -> Result<Self, Error> {
            self.0.event(format!(
                "begin:{}",
                len.map(|x| x.to_string()).unwrap_or("?".into())
            ));
            match self.0.mode {
                Mode::BeginError => Err(serde_core::ser::Error::custom("begin error")),
                Mode::BeginPanic => panic!("begin panic"),
                _ => Ok(self),
            }
        }
        fn serialize_none(self) -> Result<(), Error> {
            panic!("unexpected serializer path")
        }
        fn serialize_some<T: ?Sized + Serialize>(self, _: &T) -> Result<(), Error> {
            panic!("unexpected serializer path")
        }
        fn serialize_unit(self) -> Result<(), Error> {
            panic!("unexpected serializer path")
        }
        fn serialize_unit_struct(self, _: &'static str) -> Result<(), Error> {
            panic!("unexpected serializer path")
        }
        fn serialize_unit_variant(
            self,
            _: &'static str,
            _: u32,
            _: &'static str,
        ) -> Result<(), Error> {
            panic!("unexpected serializer path")
        }
        fn serialize_newtype_struct<T: ?Sized + Serialize>(
            self,
            _: &'static str,
            _: &T,
        ) -> Result<(), Error> {
            panic!("unexpected serializer path")
        }
        fn serialize_newtype_variant<T: ?Sized + Serialize>(
            self,
            _: &'static str,
            _: u32,
            _: &'static str,
            _: &T,
        ) -> Result<(), Error> {
            panic!("unexpected serializer path")
        }
        fn serialize_tuple(self, _: usize) -> Result<Self::SerializeTuple, Error> {
            panic!("unexpected serializer path")
        }
        fn serialize_tuple_struct(
            self,
            _: &'static str,
            _: usize,
        ) -> Result<Self::SerializeTupleStruct, Error> {
            panic!("unexpected serializer path")
        }
        fn serialize_tuple_variant(
            self,
            _: &'static str,
            _: u32,
            _: &'static str,
            _: usize,
        ) -> Result<Self::SerializeTupleVariant, Error> {
            panic!("unexpected serializer path")
        }
        fn serialize_seq(self, _: Option<usize>) -> Result<Self::SerializeSeq, Error> {
            panic!("unexpected serializer path")
        }
        fn serialize_struct(
            self,
            _: &'static str,
            _: usize,
        ) -> Result<Self::SerializeStruct, Error> {
            panic!("unexpected serializer path")
        }
        fn serialize_struct_variant(
            self,
            _: &'static str,
            _: u32,
            _: &'static str,
            _: usize,
        ) -> Result<Self::SerializeStructVariant, Error> {
            panic!("unexpected serializer path")
        }
    }
    impl SerializeMap for Probe {
        type Ok = ();
        type Error = Error;
        fn serialize_key<T: ?Sized + Serialize>(&mut self, key: &T) -> Result<(), Error> {
            self.0.event("key");
            key.serialize(self.clone())
        }
        fn serialize_value<T: ?Sized + Serialize>(&mut self, value: &T) -> Result<(), Error> {
            self.0.event("value");
            value.serialize(self.clone())
        }
        fn end(self) -> Result<(), Error> {
            self.0.event("end");
            match self.0.mode {
                Mode::EndError => Err(serde_core::ser::Error::custom("end error")),
                Mode::EndPanic => panic!("end panic"),
                _ => Ok(()),
            }
        }
    }
    struct Input {
        state: Rc<State>,
        known: bool,
        values: Vec<(i32, i32)>,
    }
    struct Iter {
        state: Rc<State>,
        known: bool,
        values: std::vec::IntoIter<(i32, i32)>,
    }
    impl IntoIterator for Input {
        type Item = (i32, i32);
        type IntoIter = Iter;
        fn into_iter(self) -> Iter {
            self.state.event("factory");
            if self.state.mode == Mode::FactoryPanic {
                panic!("factory panic")
            }
            Iter {
                state: self.state,
                known: self.known,
                values: self.values.into_iter(),
            }
        }
    }
    impl Iterator for Iter {
        type Item = (i32, i32);
        fn next(&mut self) -> Option<(i32, i32)> {
            if self.state.mode == Mode::NextPanic {
                self.state.event("next panic");
                panic!("next panic")
            }
            let item = self.values.next();
            match item {
                Some((k, v)) => self.state.event(format!("yield:{k}:{v}")),
                None => self.state.event("eof"),
            };
            item
        }
        fn size_hint(&self) -> (usize, Option<usize>) {
            self.state.hints.set(self.state.hints.get() + 1);
            if self.state.mode == Mode::HintPanic {
                panic!("hint panic")
            }
            if self.known {
                let n = self.values.len();
                (n, Some(n))
            } else {
                (0, None)
            }
        }
    }
    type Outcome = Result<Result<(), Error>, Box<dyn std::any::Any + Send>>;
    fn run(values: &[(i32, i32)], mode: Mode, known: bool) -> (Outcome, Vec<String>, usize) {
        let s = Rc::new(State {
            mode,
            events: RefCell::new(Vec::new()),
            hints: Cell::new(0),
        });
        let result = catch_unwind(AssertUnwindSafe(|| {
            Probe(s.clone()).collect_map(Input {
                state: s.clone(),
                known,
                values: values.to_vec(),
            })
        }));
        let events = s.events.borrow().clone();
        (result, events, s.hints.get())
    }
    fn expected(known: bool, len: usize, tail: &[&str]) -> Vec<String> {
        let mut e = vec![
            "factory".into(),
            format!("begin:{}", if known { len.to_string() } else { "?".into() }),
        ];
        e.extend(tail.iter().map(|x| x.to_string()));
        e
    }
    fn panic_text(p: Box<dyn std::any::Any + Send>) -> String {
        if let Some(s) = p.downcast_ref::<&str>() {
            s.to_string()
        } else {
            p.downcast_ref::<String>().unwrap().clone()
        }
    }
    #[test]
    fn success_order() {
        for known in [true, false] {
            let (o, e, h) = run(&[(1, 10), (2, 20)], Mode::Normal, known);
            o.unwrap().unwrap();
            assert_eq!(
                e,
                expected(
                    known,
                    2,
                    &[
                        "yield:1:10",
                        "key",
                        "i32:1",
                        "value",
                        "i32:10",
                        "yield:2:20",
                        "key",
                        "i32:2",
                        "value",
                        "i32:20",
                        "eof",
                        "end"
                    ]
                )
            );
            assert_eq!(h, 1)
        }
    }
    #[test]
    fn empty_order() {
        for known in [true, false] {
            let (o, e, h) = run(&[], Mode::Normal, known);
            o.unwrap().unwrap();
            assert_eq!(e, expected(known, 0, &["eof", "end"]));
            assert_eq!(h, 1)
        }
    }
    #[test]
    fn factory_and_hint_panics_precede_begin() {
        for known in [true, false] {
            let (o, e, h) = run(&[(1, 10), (2, 20)], Mode::FactoryPanic, known);
            assert_eq!(panic_text(o.unwrap_err()), "factory panic");
            assert_eq!(e, vec!["factory"]);
            assert_eq!(h, 0)
        }
        let (o, e, h) = run(&[(1, 10), (2, 20)], Mode::HintPanic, true);
        assert_eq!(panic_text(o.unwrap_err()), "hint panic");
        assert_eq!(e, vec!["factory"]);
        assert_eq!(h, 1)
    }
    #[test]
    fn begin_error_creates_iterator_without_reading() {
        for known in [true, false] {
            let (o, e, h) = run(&[(1, 10), (2, 20)], Mode::BeginError, known);
            assert_eq!(o.unwrap().unwrap_err().to_string(), "begin error");
            assert_eq!(e, expected(known, 2, &[]));
            assert_eq!(h, 1)
        }
    }
    #[test]
    fn begin_panic_creates_iterator_without_reading() {
        for known in [true, false] {
            let (o, e, h) = run(&[(1, 10), (2, 20)], Mode::BeginPanic, known);
            assert_eq!(panic_text(o.unwrap_err()), "begin panic");
            assert_eq!(e, expected(known, 2, &[]));
            assert_eq!(h, 1)
        }
    }
    #[test]
    fn next_panic_skips_entries_and_end() {
        for known in [true, false] {
            let (o, e, h) = run(&[(1, 10), (2, 20)], Mode::NextPanic, known);
            assert_eq!(panic_text(o.unwrap_err()), "next panic");
            assert_eq!(e, expected(known, 2, &["next panic"]));
            assert_eq!(h, 1)
        }
    }
    #[test]
    fn key_or_value_error_stops_suffix_and_end() {
        for known in [true, false] {
            for key in [true, false] {
                let pair = if key { (-1, 10) } else { (1, -1) };
                let tail: &[&str] = if key {
                    &["yield:-1:10", "key", "i32:-1"]
                } else {
                    &["yield:1:-1", "key", "i32:1", "value", "i32:-1"]
                };
                let (o, e, h) = run(&[pair, (2, 20)], Mode::Normal, known);
                assert_eq!(o.unwrap().unwrap_err().to_string(), "item error");
                assert_eq!(e, expected(known, 2, tail));
                assert_eq!(h, 1)
            }
        }
    }
    #[test]
    fn key_or_value_panic_stops_suffix_and_end() {
        for known in [true, false] {
            for key in [true, false] {
                let pair = if key { (-2, 10) } else { (1, -2) };
                let tail: &[&str] = if key {
                    &["yield:-2:10", "key", "i32:-2"]
                } else {
                    &["yield:1:-2", "key", "i32:1", "value", "i32:-2"]
                };
                let (o, e, h) = run(&[pair, (2, 20)], Mode::Normal, known);
                assert_eq!(panic_text(o.unwrap_err()), "item panic");
                assert_eq!(e, expected(known, 2, tail));
                assert_eq!(h, 1)
            }
        }
    }
    #[test]
    fn end_error_is_returned() {
        for known in [true, false] {
            let (o, e, h) = run(&[(1, 10)], Mode::EndError, known);
            assert_eq!(o.unwrap().unwrap_err().to_string(), "end error");
            assert_eq!(
                e,
                expected(
                    known,
                    1,
                    &[
                        "yield:1:10",
                        "key",
                        "i32:1",
                        "value",
                        "i32:10",
                        "eof",
                        "end"
                    ]
                )
            );
            assert_eq!(h, 1)
        }
    }
    #[test]
    fn end_panic_propagates() {
        for known in [true, false] {
            let (o, e, h) = run(&[(1, 10)], Mode::EndPanic, known);
            assert_eq!(panic_text(o.unwrap_err()), "end panic");
            assert_eq!(
                e,
                expected(
                    known,
                    1,
                    &[
                        "yield:1:10",
                        "key",
                        "i32:1",
                        "value",
                        "i32:10",
                        "eof",
                        "end"
                    ]
                )
            );
            assert_eq!(h, 1)
        }
    }
}
