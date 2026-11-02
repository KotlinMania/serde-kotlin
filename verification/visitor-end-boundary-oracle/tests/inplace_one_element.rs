use serde_core::de::{
    value::{Error, SeqDeserializer},
    Deserialize, Deserializer,
};
use std::panic::{catch_unwind, AssertUnwindSafe};

struct Item(i32);

impl<'de> Deserialize<'de> for Item {
    fn deserialize<D: Deserializer<'de>>(d: D) -> Result<Self, D::Error> {
        let value = i32::deserialize(d)?;
        if value == -2 {
            panic!("sentinel");
        }
        Ok(Item(value))
    }

    fn deserialize_in_place<D: Deserializer<'de>>(d: D, place: &mut Self) -> Result<(), D::Error> {
        let value = i32::deserialize(d)?;
        place.0 = if value == -2 { 800 } else { value };
        if value == -2 {
            panic!("sentinel");
        }
        Ok(())
    }
}

#[test]
fn one_existing_element_retains_mutation_and_storage_when_panic_escapes() {
    let mut values = vec![Item(1)];
    let original_storage = values.as_ptr();
    let panic = catch_unwind(AssertUnwindSafe(|| {
        Vec::<Item>::deserialize_in_place(
            SeqDeserializer::<_, Error>::new(vec![-2].into_iter()),
            &mut values,
        )
    }))
    .expect_err("the element's panic must escape instead of becoming a typed error");
    assert_eq!(panic.downcast_ref::<&str>(), Some(&"sentinel"));
    assert_eq!(values.len(), 1);
    assert_eq!(values[0].0, 800);
    assert_eq!(values.as_ptr(), original_storage);
}
