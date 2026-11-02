#[cfg(test)]
mod tests {
    use serde_core::{Deserialize, de::{self, DeserializeSeed, EnumAccess, VariantAccess, Visitor,
        value::{Error, I32Deserializer, MapDeserializer}}};
    use std::{cell::RefCell, ops::{Bound, Range}, rc::Rc};
    enum Identifier { Number(u64), Text(&'static str), Bytes(&'static [u8]) }
    impl<'de> de::Deserializer<'de> for Identifier {
        type Error = Error;
        fn deserialize_any<V:Visitor<'de>>(self,visitor:V)->Result<V::Value,Error> {
            match self {Self::Number(v)=>visitor.visit_u64(v),Self::Text(v)=>visitor.visit_str(v),Self::Bytes(v)=>visitor.visit_bytes(v)}
        }
        serde_core::forward_to_deserialize_any! {bool i8 i16 i32 i64 i128 u8 u16 u32 u64 u128 f32 f64 char str string bytes byte_buf option unit unit_struct newtype_struct seq tuple tuple_struct map struct enum identifier ignored_any}
    }
    struct Input { identifier: Identifier, trace: Rc<RefCell<Vec<&'static str>>> }
    fn input(identifier:Identifier)->(Input,Rc<RefCell<Vec<&'static str>>>) {let trace=Rc::new(RefCell::new(vec![]));(Input{identifier,trace:trace.clone()},trace)}
    impl<'de> de::Deserializer<'de> for Input {
        type Error = Error;
        fn deserialize_any<V:Visitor<'de>>(self,visitor:V)->Result<V::Value,Error> {visitor.visit_enum(self)}
        serde_core::forward_to_deserialize_any! {bool i8 i16 i32 i64 i128 u8 u16 u32 u64 u128 f32 f64 char str string bytes byte_buf option unit unit_struct newtype_struct seq tuple tuple_struct map struct enum identifier ignored_any}
    }
    struct Payload { trace: Rc<RefCell<Vec<&'static str>>> }
    impl<'de> EnumAccess<'de> for Input {
        type Error = Error;type Variant = Payload;
        fn variant_seed<V:DeserializeSeed<'de>>(self,seed:V)->Result<(V::Value,Payload),Error> {Ok((seed.deserialize(self.identifier)?,Payload{trace:self.trace}))}
    }
    impl<'de> VariantAccess<'de> for Payload {
        type Error = Error;
        fn unit_variant(self)->Result<(),Error>{self.trace.borrow_mut().push("unit");Ok(())}
        fn newtype_variant_seed<T:DeserializeSeed<'de>>(self,seed:T)->Result<T::Value,Error>{self.trace.borrow_mut().push("newtype");seed.deserialize(I32Deserializer::new(7))}
        fn tuple_variant<V:Visitor<'de>>(self,_:usize,_:V)->Result<V::Value,Error>{Err(de::Error::custom("unexpected tuple"))}
        fn struct_variant<V:Visitor<'de>>(self,_:&'static[&'static str],_:V)->Result<V::Value,Error>{Err(de::Error::custom("unexpected struct"))}
    }
    #[test]
    fn bound_accepts_all_numeric_variant_indices() {
        for(index,expected)in[Bound::Unbounded,Bound::Included(7),Bound::Excluded(7)].into_iter().enumerate(){let(d,trace)=input(Identifier::Number(index as u64));assert_eq!(Bound::<i32>::deserialize(d).unwrap(),expected);assert_eq!(*trace.borrow(),vec![if index==0{"unit"}else{"newtype"}]);}
    }
    #[test]
    fn result_accepts_both_numeric_variant_indices() {
        for(index,expected)in[Ok(7),Err(7)].into_iter().enumerate(){let(d,trace)=input(Identifier::Number(index as u64));assert_eq!(Result::<i32,i32>::deserialize(d).unwrap(),expected);assert_eq!(*trace.borrow(),vec!["newtype"]);}
    }
    #[test]
    fn out_of_range_bound_index_is_invalid_value_before_payload() {
        let(d,trace)=input(Identifier::Number(3));assert_eq!(Bound::<i32>::deserialize(d).unwrap_err().to_string(),"invalid value: integer `3`, expected `Unbounded`, `Included` or `Excluded`");assert!(trace.borrow().is_empty());
    }
    #[test]
    fn out_of_range_result_index_is_invalid_value_before_payload() {
        let(d,trace)=input(Identifier::Number(2));assert_eq!(Result::<i32,i32>::deserialize(d).unwrap_err().to_string(),"invalid value: integer `2`, expected `Ok` or `Err`");assert!(trace.borrow().is_empty());
    }
    #[test]
    fn unknown_bound_string_reports_unknown_variant() {
        let(d,trace)=input(Identifier::Text("Other"));assert_eq!(Bound::<i32>::deserialize(d).unwrap_err().to_string(),"unknown variant `Other`, expected one of `Unbounded`, `Included`, `Excluded`");assert!(trace.borrow().is_empty());
    }
    #[test]
    fn malformed_result_variant_bytes_report_invalid_bytes() {
        let(d,trace)=input(Identifier::Bytes(&[0xff]));assert_eq!(Result::<i32,i32>::deserialize(d).unwrap_err().to_string(),"invalid value: byte array, expected `Ok` or `Err`");assert!(trace.borrow().is_empty());
    }
    #[test]
    fn numeric_struct_fields_remain_rejected() {
        let d=MapDeserializer::<_,Error>::new(vec![(0u64,1i32),(1,2)].into_iter());assert_eq!(Range::<i32>::deserialize(d).unwrap_err().to_string(),"invalid type: integer `0`, expected `start` or `end`");
    }
    #[test]
    fn known_byte_names_select_all_bound_and_result_variants() {
        for(name,expected)in[("Unbounded",Bound::Unbounded),("Included",Bound::Included(7)),("Excluded",Bound::Excluded(7))] {
            let(d,trace)=input(Identifier::Bytes(name.as_bytes()));assert_eq!(Bound::<i32>::deserialize(d).unwrap(),expected);assert_eq!(*trace.borrow(),vec![if name=="Unbounded"{"unit"}else{"newtype"}]);
        }
        for(name,expected)in[("Ok",Ok(7)),("Err",Err(7))] {
            let(d,trace)=input(Identifier::Bytes(name.as_bytes()));assert_eq!(Result::<i32,i32>::deserialize(d).unwrap(),expected);assert_eq!(*trace.borrow(),vec!["newtype"]);
        }
    }

}
