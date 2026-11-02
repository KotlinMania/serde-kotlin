#[cfg(test)]
mod tests {
    use serde_core::{Deserialize, de::{self, DeserializeSeed, EnumAccess, VariantAccess, Visitor,
        value::{Error, StrDeserializer}}};
    use std::{cell::RefCell, net::{IpAddr, Ipv4Addr, Ipv6Addr, SocketAddr, SocketAddrV4, SocketAddrV6}, rc::Rc};
    enum Identifier { Number(u64), Text(&'static str), Bytes(&'static [u8]) }
    impl<'de> de::Deserializer<'de> for Identifier {
        type Error = Error;
        fn deserialize_any<V:Visitor<'de>>(self,visitor:V)->Result<V::Value,Error> {
            match self {Self::Number(v)=>visitor.visit_u64(v),Self::Text(v)=>visitor.visit_str(v),Self::Bytes(v)=>visitor.visit_bytes(v)}
        }
        serde_core::forward_to_deserialize_any! {bool i8 i16 i32 i64 i128 u8 u16 u32 u64 u128 f32 f64 char str string bytes byte_buf option unit unit_struct newtype_struct seq tuple tuple_struct map struct enum identifier ignored_any}
    }
    struct Input { identifier: Identifier, payload: Binary, trace: Rc<RefCell<Vec<&'static str>>> }
    fn input(identifier:Identifier,payload:Binary)->(Input,Rc<RefCell<Vec<&'static str>>>) {let trace=Rc::new(RefCell::new(vec![]));(Input{identifier,payload,trace:trace.clone()},trace)}
    impl<'de> de::Deserializer<'de> for Input {
        type Error = Error;
        fn is_human_readable(&self)->bool{false}
        fn deserialize_any<V:Visitor<'de>>(self,visitor:V)->Result<V::Value,Error> {visitor.visit_enum(self)}
        serde_core::forward_to_deserialize_any! {bool i8 i16 i32 i64 i128 u8 u16 u32 u64 u128 f32 f64 char str string bytes byte_buf option unit unit_struct newtype_struct seq tuple tuple_struct map struct enum identifier ignored_any}
    }
    struct Payload { value: Binary, trace: Rc<RefCell<Vec<&'static str>>> }
    impl<'de> EnumAccess<'de> for Input {
        type Error = Error;type Variant = Payload;
        fn variant_seed<V:DeserializeSeed<'de>>(self,seed:V)->Result<(V::Value,Payload),Error> {Ok((seed.deserialize(self.identifier)?,Payload{value:self.payload,trace:self.trace}))}
    }
    impl<'de> VariantAccess<'de> for Payload {
        type Error = Error;
        fn unit_variant(self)->Result<(),Error>{self.trace.borrow_mut().push("unit");Ok(())}
        fn newtype_variant_seed<T:DeserializeSeed<'de>>(self,seed:T)->Result<T::Value,Error>{self.trace.borrow_mut().push("newtype");seed.deserialize(self.value)}
        fn tuple_variant<V:Visitor<'de>>(self,_:usize,_:V)->Result<V::Value,Error>{Err(de::Error::custom("unexpected tuple"))}
        fn struct_variant<V:Visitor<'de>>(self,_:&'static[&'static str],_:V)->Result<V::Value,Error>{Err(de::Error::custom("unexpected struct"))}
    }
    enum Binary { Byte(u8), Port(u16), Sequence(Vec<Binary>) }
    struct Access(std::vec::IntoIter<Binary>);
    impl<'de> de::SeqAccess<'de> for Access {
        type Error = Error;
        fn next_element_seed<T:DeserializeSeed<'de>>(&mut self,seed:T)->Result<Option<T::Value>,Error>{self.0.next().map(|v|seed.deserialize(v)).transpose()}
        fn size_hint(&self)->Option<usize>{Some(self.0.len())}
    }
    impl<'de> de::Deserializer<'de> for Binary {
        type Error = Error;
        fn is_human_readable(&self)->bool{false}
        fn deserialize_any<V:Visitor<'de>>(self,visitor:V)->Result<V::Value,Error>{match self{Self::Byte(v)=>visitor.visit_u8(v),Self::Port(v)=>visitor.visit_u16(v),Self::Sequence(v)=>visitor.visit_seq(Access(v.into_iter()))}}
        serde_core::forward_to_deserialize_any! {bool i8 i16 i32 i64 i128 u8 u16 u32 u64 u128 f32 f64 char str string bytes byte_buf option unit unit_struct newtype_struct seq tuple tuple_struct map struct enum identifier ignored_any}
    }
    fn octets(v6:bool)->Vec<u8>{if v6{Ipv6Addr::LOCALHOST.octets().to_vec()}else{Ipv4Addr::LOCALHOST.octets().to_vec()}}
    fn ip_payload(v6:bool)->Binary{Binary::Sequence(octets(v6).into_iter().map(Binary::Byte).collect())}
    fn socket_payload(v6:bool)->Binary{Binary::Sequence(vec![ip_payload(v6),Binary::Port(8080)])}
    #[test]
    fn numeric_ip_variants_decode_both_address_families(){for(index,expected)in[IpAddr::V4(Ipv4Addr::LOCALHOST),IpAddr::V6(Ipv6Addr::LOCALHOST)].into_iter().enumerate(){let(d,t)=input(Identifier::Number(index as u64),ip_payload(index==1));assert_eq!(IpAddr::deserialize(d).unwrap(),expected);assert_eq!(*t.borrow(),vec!["newtype"]);}}
    #[test]
    fn numeric_socket_variants_decode_address_and_port(){for(index,expected)in[SocketAddr::V4(SocketAddrV4::new(Ipv4Addr::LOCALHOST,8080)),SocketAddr::V6(SocketAddrV6::new(Ipv6Addr::LOCALHOST,8080,0,0))].into_iter().enumerate(){let(d,t)=input(Identifier::Number(index as u64),socket_payload(index==1));assert_eq!(SocketAddr::deserialize(d).unwrap(),expected);assert_eq!(*t.borrow(),vec!["newtype"]);}}
    #[test]
    fn known_byte_ip_names_keep_both_families(){for(index,(name,expected))in[("V4",IpAddr::V4(Ipv4Addr::LOCALHOST)),("V6",IpAddr::V6(Ipv6Addr::LOCALHOST))].into_iter().enumerate(){let(d,t)=input(Identifier::Bytes(name.as_bytes()),ip_payload(index==1));assert_eq!(IpAddr::deserialize(d).unwrap(),expected);assert_eq!(*t.borrow(),vec!["newtype"]);}}
    #[test]
    fn unknown_socket_name_is_unknown_variant_before_payload(){let(d,t)=input(Identifier::Text("Other"),socket_payload(false));assert_eq!(SocketAddr::deserialize(d).unwrap_err().to_string(),"unknown variant `Other`, expected `V4` or `V6`");assert!(t.borrow().is_empty());}
    #[test]
    fn malformed_ip_name_bytes_are_invalid_bytes_before_payload(){let(d,t)=input(Identifier::Bytes(&[0xff]),ip_payload(false));assert_eq!(IpAddr::deserialize(d).unwrap_err().to_string(),"invalid value: byte array, expected `V4` or `V6`");assert!(t.borrow().is_empty());}
    #[test]
    fn out_of_range_network_indices_are_invalid_values_before_payload(){let(d,t)=input(Identifier::Number(2),ip_payload(false));assert_eq!(IpAddr::deserialize(d).unwrap_err().to_string(),"invalid value: integer `2`, expected `V4` or `V6`");assert!(t.borrow().is_empty());let(d,t)=input(Identifier::Number(2),socket_payload(false));assert_eq!(SocketAddr::deserialize(d).unwrap_err().to_string(),"invalid value: integer `2`, expected `V4` or `V6`");assert!(t.borrow().is_empty());}
    #[test]
    fn human_readable_ipv4_paths_keep_working(){assert_eq!(IpAddr::deserialize(StrDeserializer::<Error>::new("127.0.0.1")).unwrap(),IpAddr::V4(Ipv4Addr::LOCALHOST));assert_eq!(SocketAddr::deserialize(StrDeserializer::<Error>::new("127.0.0.1:80")).unwrap(),SocketAddr::V4(SocketAddrV4::new(Ipv4Addr::LOCALHOST,80)));}
}
