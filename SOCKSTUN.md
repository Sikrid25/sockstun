# SocksTun ADB Control Guide

## Initial Setup (one-time)

Before controlling SocksTun via adb, you must approve VPN permission once:

```bash
# 1. Allow VPN activation for the app
adb shell appops set hev.sockstun ACTIVATE_VPN allow

# 2. Open the app to trigger the VPN consent dialog
adb shell am start -n hev.sockstun/.MainActivity
```

Tap **OK** on the VPN permission dialog that appears on the device screen.
After this, all operations can be done via `content://` without opening the app again.

---

## ContentProvider URI

All commands use the authority: `content://hev.sockstun.provider`

| URI | Method | Description |
|-----|--------|-------------|
| `/config` | `query` | Read all config values |
| `/config` | `update` | Update config values (VPN must be off) |
| `/status` | `query` | Check VPN status (Enable=1 on, Enable=0 off) |
| `/` | `call connect` | Start VPN |
| `/` | `call disconnect` | Stop VPN |

---

## Query

### Read config

```bash
adb shell content query --uri content://hev.sockstun.provider/config
```

Output example:
```
Row: 0 SocksAddr=127.0.0.1, SocksUdpAddr=, SocksPort=1080, SocksUser=, SocksPass=, DnsIpv4=8.8.8.8, DnsIpv6=2001:4860:4860::8888, Ipv4=1, Ipv6=1, Global=1, UdpInTcp=0, RemoteDNS=1
```

### Check VPN status

```bash
adb shell content query --uri content://hev.sockstun.provider/status
```

Output:
```
Row: 0 Enable=1    # VPN is running
Row: 0 Enable=0    # VPN is stopped
```

---

## Update Config

Config can only be updated when VPN is **stopped**. Updates while VPN is running will be silently ignored.

### SOCKS5 server

```bash
# Address and port
adb shell content update --uri content://hev.sockstun.provider/config \
  --bind SocksAddr:s:10.0.0.1 \
  --bind SocksPort:i:1080

# Separate UDP relay address (optional)
adb shell content update --uri content://hev.sockstun.provider/config \
  --bind SocksUdpAddr:s:10.0.0.2

# Authentication (optional)
adb shell content update --uri content://hev.sockstun.provider/config \
  --bind SocksUser:s:myuser \
  --bind SocksPass:s:mypass
```

### DNS

```bash
adb shell content update --uri content://hev.sockstun.provider/config \
  --bind DnsIpv4:s:1.1.1.1 \
  --bind DnsIpv6:s:2606:4700:4700::1111
```

### Boolean flags

Use `i:1` to enable, `i:0` to disable.

```bash
adb shell content update --uri content://hev.sockstun.provider/config \
  --bind Global:i:1 \
  --bind Ipv4:i:1 \
  --bind Ipv6:i:0 \
  --bind UdpInTcp:i:1 \
  --bind RemoteDNS:i:1
```

### All keys at once

```bash
adb shell content update --uri content://hev.sockstun.provider/config \
  --bind SocksAddr:s:10.0.0.1 \
  --bind SocksPort:i:1080 \
  --bind SocksUdpAddr:s: \
  --bind SocksUser:s:myuser \
  --bind SocksPass:s:mypass \
  --bind DnsIpv4:s:8.8.8.8 \
  --bind DnsIpv6:s:2001:4860:4860::8888 \
  --bind Ipv4:i:1 \
  --bind Ipv6:i:1 \
  --bind Global:i:1 \
  --bind UdpInTcp:i:0 \
  --bind RemoteDNS:i:1
```

---

## Connect / Disconnect

```bash
# Start VPN
adb shell content call --uri content://hev.sockstun.provider --method connect

# Stop VPN
adb shell content call --uri content://hev.sockstun.provider --method disconnect
```

Response examples:
```
Result: Bundle[{success=true}]
Result: Bundle[{success=false, error=already connected}]
Result: Bundle[{success=false, error=already disconnected}]
Result: Bundle[{success=false, error=unknown method}]
```

---

## Config Reference

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `SocksAddr` | `s` (string) | `127.0.0.1` | SOCKS5 server address |
| `SocksPort` | `i` (int) | `1080` | SOCKS5 server port |
| `SocksUdpAddr` | `s` | *(empty)* | Separate UDP relay address |
| `SocksUser` | `s` | *(empty)* | SOCKS5 username |
| `SocksPass` | `s` | *(empty)* | SOCKS5 password |
| `DnsIpv4` | `s` | `8.8.8.8` | DNS resolver IPv4 |
| `DnsIpv6` | `s` | `2001:4860:4860::8888` | DNS resolver IPv6 |
| `Ipv4` | `i` (0/1) | `1` | Route IPv4 traffic |
| `Ipv6` | `i` (0/1) | `1` | Route IPv6 traffic |
| `Global` | `i` (0/1) | `1` | Global mode (all apps) |
| `UdpInTcp` | `i` (0/1) | `0` | Relay UDP over TCP |
| `RemoteDNS` | `i` (0/1) | `1` | Use server-side DNS |

---

## Quick Example

```bash
# Setup proxy and connect in one go
adb shell content update --uri content://hev.sockstun.provider/config \
  --bind SocksAddr:s:192.168.1.100 --bind SocksPort:i:7890
adb shell content call --uri content://hev.sockstun.provider --method connect

# Check it's running
adb shell content query --uri content://hev.sockstun.provider/status

# Done, disconnect
adb shell content call --uri content://hev.sockstun.provider --method disconnect
```
