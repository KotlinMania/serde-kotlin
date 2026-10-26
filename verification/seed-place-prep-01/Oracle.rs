use serde_core::de::{Deserialize, Deserializer, Error};
use serde_core::de::value::{Error as ValueError, SeqDeserializer};
use std::cell::RefCell;
use std::panic::{catch_unwind, AssertUnwindSafe};

thread_local! { static TRACE: RefCell<Vec<String>> = const { RefCell::new(Vec::new()) }; }
fn trace(s: String) { TRACE.with(|t| t.borrow_mut().push(s)); }
struct Item(i32);
impl<'de> Deserialize<'de> for Item {
    fn deserialize<D: Deserializer<'de>>(d: D) -> Result<Self, D::Error> {
        let n = i32::deserialize(d)?;
        trace(format!("new:{n}"));
        match n { -1 => Err(D::Error::custom("rejected")), -2 => panic!("sentinel"), _ => Ok(Item(n)) }
    }
    fn deserialize_in_place<D: Deserializer<'de>>(d: D, place: &mut Self) -> Result<(), D::Error> {
        let n = i32::deserialize(d)?;
        trace(format!("in:{}:{n}", place.0));
        place.0 = match n { -1 => 700, -2 => 800, _ => n };
        match n { -1 => Err(D::Error::custom("rejected")), -2 => panic!("sentinel"), _ => Ok(()) }
    }
}
fn main() {
    std::panic::set_hook(Box::new(|_| {}));
    let cases: &[(&str, &[i32], &[i32])] = &[
        ("empty", &[], &[]), ("truncate", &[1, 2, 3], &[10]),
        ("equal", &[1, 2], &[10, 20]), ("append", &[1], &[10, 20, 30]),
        ("empty-append", &[], &[10, 20]),
        ("error-existing", &[1, 2, 3], &[10, -1, 30]), ("error-append", &[1], &[10, -1]),
        ("panic-existing", &[1, 2, 3], &[10, -2, 30]), ("panic-append", &[1], &[10, -2]),
    ];
    for (label, before, input) in cases {
        TRACE.with(|t| t.borrow_mut().clear());
        let mut values: Vec<Item> = before.iter().map(|v| Item(*v)).collect();
        let result = match catch_unwind(AssertUnwindSafe(|| {
            Vec::<Item>::deserialize_in_place(SeqDeserializer::<_, ValueError>::new(input.iter().copied()), &mut values)
        })) {
            Ok(Ok(())) => "ok".into(), Ok(Err(e)) => format!("error:{e}"), Err(_) => "panic".into(),
        };
        let state = values.iter().map(|v| v.0.to_string()).collect::<Vec<_>>().join(",");
        let events = TRACE.with(|t| t.borrow().join(";"));
        println!("{label}|state={state}|trace={events}|result={result}");
    }
}
