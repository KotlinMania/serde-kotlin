#[cfg(test)]
mod tests {
    use serde_core::{
        de::value::{Error, StrDeserializer, StringDeserializer},
        Deserialize,
    };
    use std::net::{IpAddr, Ipv4Addr, Ipv6Addr, SocketAddr, SocketAddrV4, SocketAddrV6};
    fn ip(s: &str) -> Result<IpAddr, Error> {
        IpAddr::deserialize(StrDeserializer::<Error>::new(s))
    }
    fn socket(s: &str) -> Result<SocketAddr, Error> {
        SocketAddr::deserialize(StrDeserializer::<Error>::new(s))
    }
    fn v4(s: &str) -> Result<SocketAddrV4, Error> {
        SocketAddrV4::deserialize(StrDeserializer::<Error>::new(s))
    }
    fn v6(s: &str) -> Result<SocketAddrV6, Error> {
        SocketAddrV6::deserialize(StrDeserializer::<Error>::new(s))
    }
    fn mapped() -> Ipv6Addr {
        Ipv6Addr::new(0, 0, 0, 0, 0, 0xffff, 0xc000, 0x0201)
    }
    #[test]
    fn generic_ip_errors_use_ip_kind() {
        for s in [
            "",
            "foo",
            "999.1.1.1",
            "+1.2.3.4",
            "1:2:3",
            "::ffff:001.2.3.4",
            "::1%2",
            "127.0.0.1:80",
            "[::1]",
        ] {
            assert_eq!(
                ip(s).unwrap_err().to_string(),
                "invalid IP address syntax",
                "{s:?}"
            )
        }
    }
    #[test]
    fn generic_socket_errors_use_socket_kind() {
        for s in [
            "",
            "foo",
            "127.0.0.1",
            "::1",
            "127.0.0.1:+80",
            "[::1]:+80",
            "999.1.1.1:80",
            "[z]:80",
            "[::1]:65536",
            "[::1]:",
            "127..0.1:80",
            "[::ffff:001.2.3.4]:80",
        ] {
            assert_eq!(
                socket(s).unwrap_err().to_string(),
                "invalid socket address syntax",
                "{s:?}"
            )
        }
    }
    #[test]
    fn typed_v4_invalid_hosts_use_socket_kind() {
        for s in [
            "999.1.1.1:80",
            "01.2.3.4:80",
            "+1.2.3.4:80",
            "1.2.3:80",
            "::1:80",
            "[::1]:80",
        ] {
            assert_eq!(
                v4(s).unwrap_err().to_string(),
                "invalid IPv4 socket address syntax",
                "{s:?}"
            )
        }
    }
    #[test]
    fn typed_v6_invalid_hosts_use_socket_kind() {
        for s in [
            "[z]:80",
            "[+1:0:0:0:0:0:0:1]:80",
            "[::ffff:001.2.3.4]:80",
            "[1::2::3]:80",
            "[::ffff:256.0.0.1]:80",
            "[::1.2.3.4.5]:80",
        ] {
            assert_eq!(
                v6(s).unwrap_err().to_string(),
                "invalid IPv6 socket address syntax",
                "{s:?}"
            )
        }
    }
    #[test]
    fn generic_ip_values_remain_distinct() {
        assert_eq!(ip("127.0.0.1").unwrap(), IpAddr::V4(Ipv4Addr::LOCALHOST));
        assert_eq!(ip("::1").unwrap(), IpAddr::V6(Ipv6Addr::LOCALHOST));
        assert_eq!(ip("::ffff:192.0.2.1").unwrap(), IpAddr::V6(mapped()))
    }
    #[test]
    fn generic_socket_values_remain_distinct() {
        assert_eq!(
            socket("127.0.0.1:00080").unwrap(),
            SocketAddr::V4(SocketAddrV4::new(Ipv4Addr::LOCALHOST, 80))
        );
        assert_eq!(
            socket("[::1]:00080").unwrap(),
            SocketAddr::V6(SocketAddrV6::new(Ipv6Addr::LOCALHOST, 80, 0, 0))
        );
        assert_eq!(
            socket("[::ffff:192.0.2.1]:443").unwrap(),
            SocketAddr::V6(SocketAddrV6::new(mapped(), 443, 0, 0))
        )
    }
    #[test]
    fn both_invalid_host_and_port_keep_typed_kind() {
        for s in ["999.1.1.1:+80", "01.2.3.4:65536"] {
            assert_eq!(
                v4(s).unwrap_err().to_string(),
                "invalid IPv4 socket address syntax"
            )
        }
        for s in ["[z]:+80", "[1::2::3]:65536"] {
            assert_eq!(
                v6(s).unwrap_err().to_string(),
                "invalid IPv6 socket address syntax"
            )
        }
    }
    #[test]
    fn owned_strings_use_wrapper_error_kind() {
        assert_eq!(
            IpAddr::deserialize(StringDeserializer::<Error>::new("foo".into()))
                .unwrap_err()
                .to_string(),
            "invalid IP address syntax"
        );
        assert_eq!(
            SocketAddr::deserialize(StringDeserializer::<Error>::new("[z]:80".into()))
                .unwrap_err()
                .to_string(),
            "invalid socket address syntax"
        );
        assert_eq!(
            SocketAddrV4::deserialize(StringDeserializer::<Error>::new("999.1.1.1:80".into()))
                .unwrap_err()
                .to_string(),
            "invalid IPv4 socket address syntax"
        );
        assert_eq!(
            SocketAddrV6::deserialize(StringDeserializer::<Error>::new("[z]:80".into()))
                .unwrap_err()
                .to_string(),
            "invalid IPv6 socket address syntax"
        )
    }
    #[test]
    fn owned_strings_preserve_values() {
        assert_eq!(
            IpAddr::deserialize(StringDeserializer::<Error>::new("::1".into())).unwrap(),
            IpAddr::V6(Ipv6Addr::LOCALHOST)
        );
        assert_eq!(
            SocketAddr::deserialize(StringDeserializer::<Error>::new("127.0.0.1:00080".into()))
                .unwrap(),
            SocketAddr::V4(SocketAddrV4::new(Ipv4Addr::LOCALHOST, 80))
        );
        assert_eq!(
            SocketAddrV6::deserialize(StringDeserializer::<Error>::new(
                "[::ffff:192.0.2.1]:443".into()
            ))
            .unwrap(),
            SocketAddrV6::new(mapped(), 443, 0, 0)
        )
    }
}
