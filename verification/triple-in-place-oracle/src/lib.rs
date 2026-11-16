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
    fn check(input: &[i32], state: (i32, i32, i32), events: &[&str], outcome: &str) {
        TRACE.with(|t| t.borrow_mut().clear());
        let mut triple = (Item(1), Item(2), Item(3));
        let actual = match catch_unwind(AssertUnwindSafe(|| {
            <(Item, Item, Item)>::deserialize_in_place(
                SeqDeserializer::<_, ValueError>::new(input.iter().copied()),
                &mut triple,
            )
        })) {
            Ok(Ok(())) => "ok".into(),
            Ok(Err(e)) => format!("error:{e}"),
            Err(_) => "panic".into(),
        };
        assert_eq!((triple.0 .0, triple.1 .0, triple.2 .0), state);
        TRACE.with(|t| assert_eq!(*t.borrow(), events));
        assert_eq!(actual, outcome);
    }
    #[test]
    #[allow(non_snake_case)]
    fn success() {
        check(
            &[10, 20, 30],
            (10, 20, 30),
            &["in:1:10", "in:2:20", "in:3:30"],
            "ok",
        );
    }
    #[test]
    #[allow(non_snake_case)]
    fn empty() {
        check(
            &[],
            (1, 2, 3),
            &[],
            "error:invalid length 0, expected a tuple of size 3",
        );
    }
    #[test]
    #[allow(non_snake_case)]
    fn missingSecond() {
        check(
            &[10],
            (10, 2, 3),
            &["in:1:10"],
            "error:invalid length 1, expected a tuple of size 3",
        );
    }
    #[test]
    #[allow(non_snake_case)]
    fn missingThird() {
        check(
            &[10, 20],
            (10, 20, 3),
            &["in:1:10", "in:2:20"],
            "error:invalid length 2, expected a tuple of size 3",
        );
    }
    #[test]
    #[allow(non_snake_case)]
    fn firstError() {
        check(&[-1, 20, 30], (700, 2, 3), &["in:1:-1"], "error:rejected");
    }
    #[test]
    #[allow(non_snake_case)]
    fn secondError() {
        check(
            &[10, -1, 30],
            (10, 700, 3),
            &["in:1:10", "in:2:-1"],
            "error:rejected",
        );
    }
    #[test]
    #[allow(non_snake_case)]
    fn thirdError() {
        check(
            &[10, 20, -1],
            (10, 20, 700),
            &["in:1:10", "in:2:20", "in:3:-1"],
            "error:rejected",
        );
    }
    #[test]
    #[allow(non_snake_case)]
    fn firstPanic() {
        check(&[-2, 20, 30], (800, 2, 3), &["in:1:-2"], "panic");
    }
    #[test]
    #[allow(non_snake_case)]
    fn secondPanic() {
        check(
            &[10, -2, 30],
            (10, 800, 3),
            &["in:1:10", "in:2:-2"],
            "panic",
        );
    }
    #[test]
    #[allow(non_snake_case)]
    fn thirdPanic() {
        check(
            &[10, 20, -2],
            (10, 20, 800),
            &["in:1:10", "in:2:20", "in:3:-2"],
            "panic",
        );
    }
    #[test]
    fn primitive_fields() {
        let mut triple = (1i32, 2i32, 3i32);
        <(i32, i32, i32)>::deserialize_in_place(
            SeqDeserializer::<_, ValueError>::new([10i32, 20, 30].into_iter()),
            &mut triple,
        )
        .unwrap();
        assert_eq!(triple, (10, 20, 30));
    }
    #[test]
    fn primitive_partial() {
        let mut triple = (1i32, 2i32, 3i32);
        let original = triple;
        let error = <(i32, i32, i32)>::deserialize_in_place(
            SeqDeserializer::<_, ValueError>::new([10i32, 20].into_iter()),
            &mut triple,
        )
        .unwrap_err();
        assert_eq!(
            error.to_string(),
            "invalid length 2, expected a tuple of size 3"
        );
        assert_eq!(triple, (10, 20, 3));
        assert_eq!(original, (1, 2, 3));
    }
    #[test]
    fn nullable_fields() {
        let mut triple = (Some(1i32), Some(2i32), Some(3i32));
        <(Option<i32>, Option<i32>, Option<i32>)>::deserialize_in_place(
            SeqDeserializer::<_, ValueError>::new([(), (), ()].into_iter()),
            &mut triple,
        )
        .unwrap();
        assert_eq!(triple, (None, None, None));
    }
}
