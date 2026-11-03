#[cfg(test)]
mod tests {
    use serde_core::{
        de::value::{Error, StrDeserializer, StringDeserializer},
        Deserialize,
    };
    use std::net::{IpAddr, Ipv4Addr, Ipv6Addr, SocketAddr, SocketAddrV6};
    fn decode(s: &str) -> Result<Ipv6Addr, Error> {
        Ipv6Addr::deserialize(StrDeserializer::<Error>::new(s))
    }
    fn rejected(s: &str) {
        assert_eq!(
            decode(s).unwrap_err().to_string(),
            "invalid IPv6 address syntax",
            "{s}"
        )
    }
    fn mapped() -> Ipv6Addr {
        Ipv6Addr::new(0, 0, 0, 0, 0, 0xffff, 0xc000, 0x0201)
    }
    #[test]
    fn embedded_ipv4_vectors() {
        for (s, g) in [
            ("::ffff:192.0.2.1", [0, 0, 0, 0, 0, 0xffff, 0xc000, 0x0201]),
            ("::192.0.2.1", [0, 0, 0, 0, 0, 0, 0xc000, 0x0201]),
            ("1:2:3:4:5:6:192.0.2.1", [1, 2, 3, 4, 5, 6, 0xc000, 0x0201]),
            ("1:2:3:4:5::192.0.2.1", [1, 2, 3, 4, 5, 0, 0xc000, 0x0201]),
            ("1::192.0.2.1", [1, 0, 0, 0, 0, 0, 0xc000, 0x0201]),
            (
                "0:0:0:0:0:ffff:192.0.2.1",
                [0, 0, 0, 0, 0, 0xffff, 0xc000, 0x0201],
            ),
            ("::ffff:0.0.0.0", [0, 0, 0, 0, 0, 0xffff, 0, 0]),
            (
                "::ffff:255.255.255.255",
                [0, 0, 0, 0, 0, 0xffff, 0xffff, 0xffff],
            ),
        ] {
            assert_eq!(decode(s).unwrap(), Ipv6Addr::from(g), "{s}")
        }
    }
    #[test]
    fn all_mapped_octet_positions() {
        for index in 0..4 {
            for value in 0..=255u8 {
                let mut v4 = [0; 4];
                v4[index] = value;
                let text = format!("::ffff:{}", Ipv4Addr::from(v4));
                let mut expected = [0; 16];
                expected[10] = 255;
                expected[11] = 255;
                expected[12..].copy_from_slice(&v4);
                assert_eq!(decode(&text).unwrap().octets(), expected)
            }
        }
    }
    #[test]
    fn hex_case_and_zero_prefixes_remain_valid() {
        for (s, g) in [
            ("::", [0; 8]),
            ("::1", [0, 0, 0, 0, 0, 0, 0, 1]),
            ("1:2:3:4:5:6:7:8", [1, 2, 3, 4, 5, 6, 7, 8]),
            (
                "ABCD:0001:0002:0003:0004:0005:0006:ffff",
                [0xabcd, 1, 2, 3, 4, 5, 6, 0xffff],
            ),
            (
                "0000:0000:0000:0000:0000:0000:0000:0001",
                [0, 0, 0, 0, 0, 0, 0, 1],
            ),
        ] {
            assert_eq!(decode(s).unwrap(), Ipv6Addr::from(g), "{s}")
        }
    }
    #[test]
    fn rejects_invalid_hex_groups() {
        for s in [
            "+1:0:0:0:0:0:0:1",
            "-0:0:0:0:0:0:0:1",
            "１:0:0:0:0:0:0:1",
            "١:0:0:0:0:0:0:1",
            "Ｆ:0:0:0:0:0:0:1",
            "10000:0:0:0:0:0:0:1",
            "0x1:0:0:0:0:0:0:1",
        ] {
            rejected(s)
        }
    }
    #[test]
    fn rejects_invalid_ipv4_tail_or_placement() {
        for s in [
            "::ffff:192.168.001.1",
            "::ffff:256.1.2.3",
            "::ffff:+1.2.3.4",
            "::ffff:١.2.3.4",
            "::ffff:1.2.3",
            "::ffff:1.2.3.4.5",
            "::ffff:192.0.2.1:7",
            "192.0.2.1::",
            "1:2:3:4:5:6::192.0.2.1",
            "1:2:3:4:5:192.0.2.1",
        ] {
            rejected(s)
        }
    }
    #[test]
    fn rejects_malformed_compression_or_group_count() {
        for s in [
            "",
            "1:2:3:4:5:6:7",
            "1:2:3:4:5:6:7:8:9",
            "1::2::3",
            ":::1",
            "1:::",
            "1:2:3:4:5:6:7:8::",
            "::1:2:3:4:5:6:7:8",
            ":1:2:3:4:5:6:7:8",
            "1:2:3:4:5:6:7:8:",
            "1::z",
            "::1%2",
        ] {
            rejected(s)
        }
    }
    #[test]
    fn owned_string_supports_embedded_tail() {
        assert_eq!(
            Ipv6Addr::deserialize(StringDeserializer::<Error>::new("::ffff:192.0.2.1".into()))
                .unwrap(),
            mapped()
        );
        assert_eq!(
            Ipv6Addr::deserialize(StringDeserializer::<Error>::new("+1:0:0:0:0:0:0:1".into()))
                .unwrap_err()
                .to_string(),
            "invalid IPv6 address syntax"
        )
    }
    #[test]
    fn generic_ip_caller_supports_embedded_tail() {
        assert_eq!(
            IpAddr::deserialize(StrDeserializer::<Error>::new("::ffff:192.0.2.1")).unwrap(),
            IpAddr::V6(mapped())
        );
        assert_eq!(
            IpAddr::deserialize(StrDeserializer::<Error>::new("127.0.0.1")).unwrap(),
            IpAddr::V4(Ipv4Addr::LOCALHOST)
        );
        for s in ["::ffff:192.168.001.1", "+1:0:0:0:0:0:0:1"] {
            assert!(
                IpAddr::deserialize(StrDeserializer::<Error>::new(s)).is_err(),
                "{s}"
            )
        }
    }
    #[test]
    fn socket_caller_supports_embedded_tail() {
        assert_eq!(
            SocketAddrV6::deserialize(StrDeserializer::<Error>::new("[::ffff:192.0.2.1]:00080"))
                .unwrap(),
            SocketAddrV6::new(mapped(), 80, 0, 0)
        );
        assert_eq!(
            SocketAddr::deserialize(StrDeserializer::<Error>::new("[::ffff:192.0.2.1]:80"))
                .unwrap(),
            SocketAddr::V6(SocketAddrV6::new(mapped(), 80, 0, 0))
        );
        assert!(SocketAddrV6::deserialize(StrDeserializer::<Error>::new(
            "[::ffff:192.168.001.1]:80"
        ))
        .is_err())
    }
    #[test]
    fn compressed_head_limits_reserve_a_zero_group() {
        for n in 0..=5 {
            let prefix = (1..=n)
                .map(|x| format!("{x:x}"))
                .collect::<Vec<_>>()
                .join(":");
            let text = format!("{prefix}::192.0.2.1");
            let mut groups = [0; 8];
            for (i, g) in groups[..n].iter_mut().enumerate() {
                *g = (i + 1) as u16
            }
            groups[6] = 0xc000;
            groups[7] = 0x0201;
            assert_eq!(decode(&text).unwrap(), Ipv6Addr::from(groups), "{text}")
        }
    }
}
