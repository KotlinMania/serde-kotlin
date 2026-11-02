#[cfg(test)]
mod tests {
    use serde_core::{
        de::value::{Error, StrDeserializer, StringDeserializer},
        Deserialize,
    };
    use std::net::{IpAddr, Ipv4Addr, SocketAddrV4};
    fn decode(s: &str) -> Result<Ipv4Addr, Error> {
        Ipv4Addr::deserialize(StrDeserializer::<Error>::new(s))
    }
    fn rejected(s: &str) {
        assert_eq!(
            decode(s).unwrap_err().to_string(),
            "invalid IPv4 address syntax",
            "{s:?}"
        )
    }
    fn pieces_rejected(pieces: &[&str]) {
        for index in 0..4 {
            for piece in pieces {
                let mut fields = ["1", "2", "3", "4"];
                fields[index] = piece;
                rejected(&fields.join("."));
            }
        }
    }
    #[test]
    fn all_octet_values_in_each_position() {
        for index in 0..4 {
            for value in 0..=255u8 {
                let mut octets = [0; 4];
                octets[index] = value;
                let expected = Ipv4Addr::from(octets);
                assert_eq!(decode(&expected.to_string()).unwrap().octets(), octets)
            }
        }
    }
    #[test]
    fn rejects_signed_octets() {
        pieces_rejected(&["+1", "+0", "-0", "-1"])
    }
    #[test]
    fn rejects_multi_digit_zero_prefixes() {
        pieces_rejected(&["00", "01", "001", "0000"])
    }
    #[test]
    fn rejects_non_ascii_digits() {
        pieces_rejected(&["١", "१", "１", "²"])
    }
    #[test]
    fn rejects_overflow_and_width() {
        pieces_rejected(&["256", "999", "1000", "2147483648", "999999999999999999"])
    }
    #[test]
    fn rejects_malformed_shapes() {
        for s in [
            "",
            "1",
            "1.2.3",
            "1.2.3.4.5",
            "1..2.3",
            ".1.2.3",
            "1.2.3.",
            " 1.2.3.4",
            "1.2.3.4 ",
            "1.2.3.4\n",
            "1.2.3.4\0",
        ] {
            rejected(s)
        }
    }
    #[test]
    fn owned_strings_use_same_rules() {
        assert_eq!(
            Ipv4Addr::deserialize(StringDeserializer::<Error>::new("127.0.0.1".into())).unwrap(),
            Ipv4Addr::LOCALHOST
        );
        assert_eq!(
            Ipv4Addr::deserialize(StringDeserializer::<Error>::new("+1.2.3.4".into()))
                .unwrap_err()
                .to_string(),
            "invalid IPv4 address syntax"
        )
    }
    #[test]
    fn generic_ip_caller_rejects_invalid_ipv4() {
        for s in ["+1.2.3.4", "01.2.3.4", "١.2.3.4"] {
            assert!(
                IpAddr::deserialize(StrDeserializer::<Error>::new(s)).is_err(),
                "{s}"
            )
        }
        assert_eq!(
            IpAddr::deserialize(StrDeserializer::<Error>::new("127.0.0.1")).unwrap(),
            IpAddr::V4(Ipv4Addr::LOCALHOST)
        )
    }
    #[test]
    fn socket_v4_caller_rejects_invalid_host() {
        for s in ["+1.2.3.4:80", "01.2.3.4:80", "١.2.3.4:80"] {
            assert!(
                SocketAddrV4::deserialize(StrDeserializer::<Error>::new(s)).is_err(),
                "{s}"
            )
        }
        assert_eq!(
            SocketAddrV4::deserialize(StrDeserializer::<Error>::new("127.0.0.1:80")).unwrap(),
            SocketAddrV4::new(Ipv4Addr::LOCALHOST, 80)
        )
    }
}
