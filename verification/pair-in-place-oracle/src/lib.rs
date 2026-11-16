use serde_core::de::value::{Error as ValueError, SeqDeserializer};
use serde_core::de::{Deserialize, Deserializer, Error};
use std::cell::RefCell;
use std::panic::{catch_unwind, AssertUnwindSafe};

thread_local! { static TRACE: RefCell<Vec<String>> = const { RefCell::new(Vec::new()) }; }
fn trace(s: String) {
    TRACE.with(|t| t.borrow_mut().push(s));
}
struct Item(i32);
impl<'de> Deserialize<'de> for Item {
    fn deserialize<D: Deserializer<'de>>(d: D) -> Result<Self, D::Error> {
        let n = i32::deserialize(d)?;
        trace(format!("new:{n}"));
        match n {
            -1 => Err(D::Error::custom("rejected")),
            -2 => panic!("sentinel"),
            _ => Ok(Item(n)),
        }
    }
    fn deserialize_in_place<D: Deserializer<'de>>(d: D, place: &mut Self) -> Result<(), D::Error> {
        let n = i32::deserialize(d)?;
        trace(format!("in:{}:{n}", place.0));
        place.0 = match n {
            -1 => 700,
            -2 => 800,
            _ => n,
        };
        match n {
            -1 => Err(D::Error::custom("rejected")),
            -2 => panic!("sentinel"),
            _ => Ok(()),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    fn check(input: &[i32], state: (i32, i32), events: &[&str], outcome: &str) {
        TRACE.with(|t| t.borrow_mut().clear());
        let mut pair = (Item(1), Item(2));
        let actual = match catch_unwind(AssertUnwindSafe(|| {
            <(Item, Item)>::deserialize_in_place(
                SeqDeserializer::<_, ValueError>::new(input.iter().copied()),
                &mut pair,
            )
        })) {
            Ok(Ok(())) => "ok".into(),
            Ok(Err(e)) => format!("error:{e}"),
            Err(_) => "panic".into(),
        };
        assert_eq!((pair.0 .0, pair.1 .0), state);
        TRACE.with(|t| assert_eq!(*t.borrow(), events));
        assert_eq!(actual, outcome);
    }
    #[test]
    fn success() {
        check(&[10, 20], (10, 20), &["in:1:10", "in:2:20"], "ok");
    }
    #[test]
    fn empty() {
        check(
            &[],
            (1, 2),
            &[],
            "error:invalid length 0, expected a tuple of size 2",
        );
    }
    #[test]
    fn missing_second() {
        check(
            &[10],
            (10, 2),
            &["in:1:10"],
            "error:invalid length 1, expected a tuple of size 2",
        );
    }
    #[test]
    fn first_error() {
        check(&[-1, 20], (700, 2), &["in:1:-1"], "error:rejected");
    }
    #[test]
    fn second_error() {
        check(
            &[10, -1],
            (10, 700),
            &["in:1:10", "in:2:-1"],
            "error:rejected",
        );
    }
    #[test]
    fn first_panic() {
        check(&[-2, 20], (800, 2), &["in:1:-2"], "panic");
    }
    #[test]
    fn second_panic() {
        check(&[10, -2], (10, 800), &["in:1:10", "in:2:-2"], "panic");
    }
    #[test]
    fn primitive_fields() {
        let mut pair = (1i32, 2i32);
        <(i32, i32)>::deserialize_in_place(
            SeqDeserializer::<_, ValueError>::new([10i32, 20].into_iter()),
            &mut pair,
        )
        .unwrap();
        assert_eq!(pair, (10, 20));
    }
    #[test]
    fn nullable_fields() {
        let mut pair = (Some(1i32), Some(2i32));
        <(Option<i32>, Option<i32>)>::deserialize_in_place(
            SeqDeserializer::<_, ValueError>::new([(), ()].into_iter()),
            &mut pair,
        )
        .unwrap();
        assert_eq!(pair, (None, None));
    }
}
