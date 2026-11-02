#[cfg(test)]
mod tests {
    use serde_core::de::{value::{Error, SeqDeserializer}, Deserializer, SeqAccess, Visitor};
    use std::{cell::Cell, fmt, rc::Rc};
    struct ResumingInput { calls: Rc<Cell<usize>>, reads: Rc<Cell<usize>> }
    fn input() -> (ResumingInput, Rc<Cell<usize>>, Rc<Cell<usize>>) {
        let calls=Rc::new(Cell::new(0));let reads=Rc::new(Cell::new(0));
        (ResumingInput{calls:calls.clone(),reads:reads.clone()},calls,reads)
    }
    impl Iterator for ResumingInput {
        type Item = i32;
        fn next(&mut self) -> Option<i32> {
            self.calls.set(self.calls.get()+1);
            if self.calls.get()==2 {self.reads.set(self.reads.get()+1);Some(7)} else {None}
        }
    }
    #[test]
    fn exhaustion_prevents_later_iterator_values_reappearing() {
        let(i,calls,reads)=input();let mut d=SeqDeserializer::<_,Error>::new(i);
        assert_eq!(d.next_element::<i32>().unwrap(),None);assert_eq!(d.next_element::<i32>().unwrap(),None);assert_eq!(calls.get(),1);assert_eq!(reads.get(),0);
    }
    #[test]
    fn end_does_not_resume_after_observed_exhaustion() {
        let(i,calls,reads)=input();let mut d=SeqDeserializer::<_,Error>::new(i);
        assert_eq!(d.next_element::<i32>().unwrap(),None);assert!(d.end().is_ok());assert_eq!(calls.get(),1);assert_eq!(reads.get(),0);
    }
    struct EmptyVisitor;
    impl<'de> Visitor<'de> for EmptyVisitor {
        type Value = ();
        fn expecting(&self,f:&mut fmt::Formatter)->fmt::Result {f.write_str("empty sequence")}
        fn visit_seq<A:SeqAccess<'de>>(self,mut access:A)->Result<(),A::Error> {assert_eq!(access.next_element::<i32>()?,None);Ok(())}
    }
    #[test]
    fn visitor_reading_end_does_not_get_fabricated_length_error() {
        let(i,calls,reads)=input();let d=SeqDeserializer::<_,Error>::new(i);
        assert!(d.deserialize_any(EmptyVisitor).is_ok());assert_eq!(calls.get(),1);assert_eq!(reads.get(),0);
    }
    #[test]
    fn exhausted_sequence_reports_exact_zero_hint() {
        let(i,calls,_)=input();let mut d=SeqDeserializer::<_,Error>::new(i);
        assert_eq!(SeqAccess::size_hint(&d),None);assert_eq!(d.next_element::<i32>().unwrap(),None);assert_eq!(SeqAccess::size_hint(&d),Some(0));assert_eq!(calls.get(),1);
    }
    #[test]
    fn end_still_counts_unconsumed_elements_for_length_error() {
        let mut d=SeqDeserializer::<_,Error>::new(vec![1i32,2].into_iter());assert_eq!(d.next_element::<i32>().unwrap(),Some(1));
        assert_eq!(d.end().unwrap_err().to_string(),"invalid length 2, expected 1 element in sequence");
    }
}
