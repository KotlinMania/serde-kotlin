use serde::de::{DeserializeSeed, Visitor};
use serde::de::value::Error;
use serde::__private228::de::{Content,ContentRefDeserializer,ContentVisitor};
fn main() {
    for size in [0,1,4,257,512] {
        let storage:Vec<u8>=(0..size).map(|n|n as u8).collect();
        let captured=ContentVisitor::new().visit_borrowed_bytes::<Error>(&storage).unwrap();
        let cloned=ContentVisitor::new().deserialize(ContentRefDeserializer::<Error>::new(&captured)).unwrap();
        if let (Content::Bytes(a),Content::Bytes(b))=(&captured,&cloned) {
            println!("borrow:{size}:{}:{}:{}",a.as_ptr()==storage.as_ptr(),b.as_ptr()==storage.as_ptr(),a==b);
        } else {panic!("borrowed variant changed")}
        let captured=ContentVisitor::new().visit_bytes::<Error>(&storage).unwrap();
        let cloned=ContentVisitor::new().deserialize(ContentRefDeserializer::<Error>::new(&captured)).unwrap();
        if let (Content::ByteBuf(a),Content::ByteBuf(b))=(&captured,&cloned) {
            println!("owned:{size}:{}:{}:{}",size==0 || a.as_ptr()!=storage.as_ptr(),size==0 || b.as_ptr()!=a.as_ptr(),a==b);
        } else {panic!("owned variant changed")}
    }
}
