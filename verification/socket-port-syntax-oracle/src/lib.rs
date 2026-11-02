#[cfg(test)]
mod tests {
    use serde_core::{
        de::value::{Error, StrDeserializer, StringDeserializer},
        Deserialize,
    };
    use std::net::{Ipv4Addr, Ipv6Addr, SocketAddr, SocketAddrV4, SocketAddrV6};
    fn v4(port: &str) -> Result<SocketAddrV4, Error> {
        SocketAddrV4::deserialize(StrDeserializer::<Error>::new(&format!("127.0.0.1:{port}")))
    }
    fn v6(port: &str) -> Result<SocketAddrV6, Error> {
        SocketAddrV6::deserialize(StrDeserializer::<Error>::new(&format!("[::1]:{port}")))
    }
    fn rejected(port: &str) {
        assert_eq!(
            v4(port).unwrap_err().to_string(),
            "invalid IPv4 socket address syntax",
            "{port:?}"
        );
        assert_eq!(
            v6(port).unwrap_err().to_string(),
            "invalid IPv6 socket address syntax",
            "{port:?}"
        )
    }
    #[test]
    fn all_v4_port_values() {
        for port in 0..=65535u16 {
            assert_eq!(
                v4(&port.to_string()).unwrap(),
                SocketAddrV4::new(Ipv4Addr::LOCALHOST, port)
            )
        }
    }
    #[test]
    fn v6_port_boundaries() {
        for port in [0, 1, 9, 10, 80, 443, 65534, 65535] {
            assert_eq!(
                v6(&port.to_string()).unwrap(),
                SocketAddrV6::new(Ipv6Addr::LOCALHOST, port, 0, 0)
            )
        }
    }
    #[test]
    fn leading_zeros_are_allowed() {
        for (port, value) in [
            ("00".to_string(), 0),
            ("00080".to_string(), 80),
            (format!("{}80", "0".repeat(128)), 80),
            ("00065535".to_string(), 65535),
        ] {
            assert_eq!(v4(&port).unwrap().port(), value);
            assert_eq!(v6(&port).unwrap().port(), value)
        }
    }
    #[test]
    fn rejects_signs() {
        for port in ["+0", "+80", "-0", "-1"] {
            rejected(port)
        }
    }
    #[test]
    fn rejects_non_ascii_digits() {
        for port in ["٨٠", "८०", "８０", "٠", "²"] {
            rejected(port)
        }
    }
    #[test]
    fn rejects_overflow() {
        for port in [
            "65536".to_string(),
            "99999".to_string(),
            "18446744073709551615".to_string(),
            "9".repeat(128),
            format!("{}65536", "0".repeat(128)),
        ] {
            rejected(&port)
        }
    }
    #[test]
    fn rejects_malformed_ports() {
        for port in ["", " 80", "80 ", "80\n", "8.0", "0x50", "80:1", "80\0"] {
            rejected(port)
        }
    }
    #[test]
    fn generic_socket_caller_uses_port_rules() {
        for s in ["127.0.0.1:+80", "[::1]:+80", "127.0.0.1:٨٠", "[::1]:65536"] {
            assert!(
                SocketAddr::deserialize(StrDeserializer::<Error>::new(s)).is_err(),
                "{s}"
            )
        }
        assert_eq!(
            SocketAddr::deserialize(StrDeserializer::<Error>::new("127.0.0.1:00080")).unwrap(),
            SocketAddr::V4(SocketAddrV4::new(Ipv4Addr::LOCALHOST, 80))
        );
        assert_eq!(
            SocketAddr::deserialize(StrDeserializer::<Error>::new("[::1]:00080")).unwrap(),
            SocketAddr::V6(SocketAddrV6::new(Ipv6Addr::LOCALHOST, 80, 0, 0))
        )
    }
    #[test]
    fn owned_string_uses_port_rules() {
        assert_eq!(
            SocketAddrV4::deserialize(StringDeserializer::<Error>::new(
                "127.0.0.1:00080".to_string()
            ))
            .unwrap()
            .port(),
            80
        );
        assert_eq!(
            SocketAddrV4::deserialize(StringDeserializer::<Error>::new(
                "127.0.0.1:+80".to_string()
            ))
            .unwrap_err()
            .to_string(),
            "invalid IPv4 socket address syntax"
        )
    }
}
