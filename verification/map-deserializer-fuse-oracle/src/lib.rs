#[cfg(test)]
mod tests {
    use serde_core::de::{value::{Error, MapDeserializer}, Deserializer, MapAccess, SeqAccess, Visitor};
    use std::{cell::Cell, fmt, rc::Rc};
    struct ResumingInput { calls: Rc<Cell<usize>>, reads: Rc<Cell<usize>> }
    fn input() -> (ResumingInput, Rc<Cell<usize>>, Rc<Cell<usize>>) {
        let calls=Rc::new(Cell::new(0));let reads=Rc::new(Cell::new(0));
        (ResumingInput{calls:calls.clone(),reads:reads.clone()},calls,reads)
    }
    impl Iterator for ResumingInput {
        type Item = (i32,i32);
        fn next(&mut self) -> Option<Self::Item> {
            self.calls.set(self.calls.get()+1);
            if self.calls.get()==2 {self.reads.set(self.reads.get()+1);Some((7,9))} else {None}
        }
    }
    #[test]
    fn key_access_cannot_resume_after_end() {
        let(i,calls,reads)=input();let mut d=MapDeserializer::<_,Error>::new(i);
        assert_eq!(d.next_key::<i32>().unwrap(),None);assert_eq!(d.next_key::<i32>().unwrap(),None);assert_eq!(calls.get(),1);assert_eq!(reads.get(),0);
    }
    #[test]
    fn entry_access_cannot_resume_after_end() {
        let(i,calls,reads)=input();let mut d=MapDeserializer::<_,Error>::new(i);
        assert_eq!(d.next_entry::<i32,i32>().unwrap(),None);assert_eq!(d.next_entry::<i32,i32>().unwrap(),None);assert_eq!(calls.get(),1);assert_eq!(reads.get(),0);
    }
    #[test]
    fn sequence_view_cannot_resume_after_end() {
        let(i,calls,reads)=input();let mut d=MapDeserializer::<_,Error>::new(i);
        assert_eq!(SeqAccess::next_element::<()>(&mut d).unwrap(),None);assert_eq!(SeqAccess::next_element::<()>(&mut d).unwrap(),None);assert_eq!(calls.get(),1);assert_eq!(reads.get(),0);
    }
    #[test]
    fn end_does_not_resume_after_key_access_reported_end() {
        let(i,calls,reads)=input();let mut d=MapDeserializer::<_,Error>::new(i);
        assert_eq!(d.next_key::<i32>().unwrap(),None);assert!(d.end().is_ok());assert_eq!(calls.get(),1);assert_eq!(reads.get(),0);
    }
    struct EmptyVisitor;
    impl<'de> Visitor<'de> for EmptyVisitor {
        type Value = ();
        fn expecting(&self,f:&mut fmt::Formatter)->fmt::Result {f.write_str("empty map")}
        fn visit_map<A:MapAccess<'de>>(self,mut access:A)->Result<(),A::Error> {assert_eq!(access.next_key::<i32>()?,None);Ok(())}
    }
    #[test]
    fn map_visitor_reading_end_does_not_get_fabricated_length_error() {
        let(i,calls,reads)=input();let d=MapDeserializer::<_,Error>::new(i);
        assert!(d.deserialize_any(EmptyVisitor).is_ok());assert_eq!(calls.get(),1);assert_eq!(reads.get(),0);
    }
    #[test]
    fn exhausted_map_reports_exact_zero_hint() {
        let(i,calls,_)=input();let mut d=MapDeserializer::<_,Error>::new(i);
        assert_eq!(MapAccess::size_hint(&d),None);assert_eq!(d.next_key::<i32>().unwrap(),None);assert_eq!(MapAccess::size_hint(&d),Some(0));assert_eq!(calls.get(),1);
    }
    #[test]
    fn end_still_counts_unconsumed_entries_for_length_error() {
        let mut d=MapDeserializer::<_,Error>::new(vec![(1i32,10i32),(2,20)].into_iter());assert_eq!(d.next_entry::<i32,i32>().unwrap(),Some((1,10)));
        assert_eq!(d.end().unwrap_err().to_string(),"invalid length 2, expected 1 element in map");
    }
}
