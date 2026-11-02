#[cfg(test)]
mod tests {
    use serde_core::de::{
        value::{Error, MapDeserializer, SeqDeserializer},
        Deserializer, MapAccess, SeqAccess, Visitor,
    };
    use std::{cell::Cell, fmt, marker::PhantomData, rc::Rc};
    #[derive(Clone, Copy)]
    enum Mode {
        Panic,
        Error,
        Empty,
        Partial,
        All,
    }
    struct Visit {
        mode: Mode,
        pairs: bool,
    }
    impl<'de> Visitor<'de> for Visit {
        type Value = i32;
        fn expecting(&self, f: &mut fmt::Formatter) -> fmt::Result {
            f.write_str("container")
        }
        fn visit_seq<A: SeqAccess<'de>>(self, mut a: A) -> Result<i32, A::Error> {
            match self.mode {
                Mode::Panic => panic!("visitor panic"),
                Mode::Error => return Err(serde_core::de::Error::custom("visitor error")),
                Mode::Empty => return Ok(0),
                _ => {}
            }
            let mut sum = 0;
            loop {
                let next = if self.pairs {
                    a.next_element::<(i32, i32)>()?.map(|(k, v)| k + v)
                } else {
                    a.next_element::<i32>()?
                };
                match next {
                    Some(v) => sum += v,
                    None => break,
                }
                if matches!(self.mode, Mode::Partial) {
                    break;
                }
            }
            Ok(sum)
        }
        fn visit_map<A: MapAccess<'de>>(self, mut a: A) -> Result<i32, A::Error> {
            match self.mode {
                Mode::Panic => panic!("visitor panic"),
                Mode::Error => return Err(serde_core::de::Error::custom("visitor error")),
                Mode::Empty => return Ok(0),
                _ => {}
            }
            let mut sum = 0;
            while let Some((k, v)) = a.next_entry::<i32, i32>()? {
                sum += k + v;
                if matches!(self.mode, Mode::Partial) {
                    break;
                }
            }
            Ok(sum)
        }
    }
    struct Input<T> {
        values: std::vec::IntoIter<T>,
        reads: Rc<Cell<usize>>,
    }
    impl<T> Iterator for Input<T> {
        type Item = T;
        fn next(&mut self) -> Option<T> {
            self.values.next().map(|v| {
                self.reads.set(self.reads.get() + 1);
                v
            })
        }
    }
    fn input<T>(v: Vec<T>) -> (Input<T>, Rc<Cell<usize>>) {
        let r = Rc::new(Cell::new(0));
        (
            Input {
                values: v.into_iter(),
                reads: r.clone(),
            },
            r,
        )
    }
    struct PanicInput<T> {
        fired: bool,
        marker: PhantomData<T>,
    }
    impl<T> Iterator for PanicInput<T> {
        type Item = T;
        fn next(&mut self) -> Option<T> {
            if !self.fired {
                self.fired = true;
                panic!("iterator panic");
            }
            None
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
    fn sequence_visitor_panic_skips_end() {
        let (i, r) = input(vec![10, 20]);
        let d = SeqDeserializer::<_, Error>::new(i);
        let p = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            let _ = d.deserialize_any(Visit {
                mode: Mode::Panic,
                pairs: false,
            });
        }))
        .unwrap_err();
        assert_eq!(panic_text(p), "visitor panic");
        assert_eq!(r.get(), 0);
    }
    #[test]
    fn map_visitor_panic_skips_end() {
        let (i, r) = input(vec![(1, 10), (2, 20)]);
        let d = MapDeserializer::<_, Error>::new(i);
        let p = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            let _ = d.deserialize_any(Visit {
                mode: Mode::Panic,
                pairs: true,
            });
        }))
        .unwrap_err();
        assert_eq!(panic_text(p), "visitor panic");
        assert_eq!(r.get(), 0);
    }
    #[test]
    fn map_sequence_visitor_panic_skips_end() {
        let (i, r) = input(vec![(1, 10), (2, 20)]);
        let d = MapDeserializer::<_, Error>::new(i);
        let p = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            let _ = d.deserialize_seq(Visit {
                mode: Mode::Panic,
                pairs: true,
            });
        }))
        .unwrap_err();
        assert_eq!(panic_text(p), "visitor panic");
        assert_eq!(r.get(), 0);
    }
    #[test]
    fn sequence_end_iterator_panic_propagates() {
        let d = SeqDeserializer::<_, Error>::new(PanicInput::<i32> {
            fired: false,
            marker: PhantomData,
        });
        let p = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            let _ = d.deserialize_any(Visit {
                mode: Mode::Empty,
                pairs: false,
            });
        }))
        .unwrap_err();
        assert_eq!(panic_text(p), "iterator panic");
    }
    #[test]
    fn map_end_iterator_panic_propagates() {
        let d = MapDeserializer::<_, Error>::new(PanicInput::<(i32, i32)> {
            fired: false,
            marker: PhantomData,
        });
        let p = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            let _ = d.deserialize_any(Visit {
                mode: Mode::Empty,
                pairs: true,
            });
        }))
        .unwrap_err();
        assert_eq!(panic_text(p), "iterator panic");
    }
    #[test]
    fn typed_visitor_errors_skip_end() {
        let (i, r) = input(vec![10, 20]);
        let d = SeqDeserializer::<_, Error>::new(i);
        assert_eq!(
            d.deserialize_any(Visit {
                mode: Mode::Error,
                pairs: false
            })
            .unwrap_err()
            .to_string(),
            "visitor error"
        );
        assert_eq!(r.get(), 0);
        for sequence in [false, true] {
            let (i, r) = input(vec![(1, 10), (2, 20)]);
            let d = MapDeserializer::<_, Error>::new(i);
            let result = if sequence {
                d.deserialize_seq(Visit {
                    mode: Mode::Error,
                    pairs: true,
                })
            } else {
                d.deserialize_any(Visit {
                    mode: Mode::Error,
                    pairs: true,
                })
            };
            assert_eq!(result.unwrap_err().to_string(), "visitor error");
            assert_eq!(r.get(), 0);
        }
    }
    #[test]
    fn partial_visitors_retain_length_errors() {
        let (i, r) = input(vec![10, 20]);
        let d = SeqDeserializer::<_, Error>::new(i);
        assert_eq!(
            d.deserialize_any(Visit {
                mode: Mode::Partial,
                pairs: false
            })
            .unwrap_err()
            .to_string(),
            "invalid length 2, expected 1 element in sequence"
        );
        assert_eq!(r.get(), 2);
        for sequence in [false, true] {
            let (i, r) = input(vec![(1, 10), (2, 20)]);
            let d = MapDeserializer::<_, Error>::new(i);
            let result = if sequence {
                d.deserialize_seq(Visit {
                    mode: Mode::Partial,
                    pairs: true,
                })
            } else {
                d.deserialize_any(Visit {
                    mode: Mode::Partial,
                    pairs: true,
                })
            };
            assert_eq!(
                result.unwrap_err().to_string(),
                "invalid length 2, expected 1 element in map"
            );
            assert_eq!(r.get(), 2);
        }
    }
    #[test]
    fn complete_visitors_succeed() {
        let (i, r) = input(vec![10, 20]);
        let d = SeqDeserializer::<_, Error>::new(i);
        assert_eq!(
            d.deserialize_any(Visit {
                mode: Mode::All,
                pairs: false
            })
            .unwrap(),
            30
        );
        assert_eq!(r.get(), 2);
        for sequence in [false, true] {
            let (i, r) = input(vec![(1, 10), (2, 20)]);
            let d = MapDeserializer::<_, Error>::new(i);
            let result = if sequence {
                d.deserialize_seq(Visit {
                    mode: Mode::All,
                    pairs: true,
                })
            } else {
                d.deserialize_any(Visit {
                    mode: Mode::All,
                    pairs: true,
                })
            };
            assert_eq!(result.unwrap(), 33);
            assert_eq!(r.get(), 2);
        }
    }
}
