### Throughput
```sh
exec tcpkali \
-m xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx \
--connect-rate=200 \
-c 200 \
-T 30s \
localhost:8007 > result.txt
```

Raw:
```
Total data sent:     42865.5 MiB (44947716988 bytes)
Total data received: 42862.5 MiB (44944560360 bytes)
Bandwidth per channel: 119.848⇅ Mbps (14981.0 kBps)
Aggregate bandwidth: 11984.345↓, 11985.187↑ Mbps
Packet rate estimate: 1051083.2↓, 1069718.2↑ (11↓, 23↑ TCP MSS/op)
Test duration: 30.0022 s.
```

Netty4z:
```
Total data sent:     38874.3 MiB (40762621008 bytes)
Total data received: 38871.5 MiB (40759744076 bytes)
Bandwidth per channel: 108.676⇅ Mbps (13584.4 kBps)
Aggregate bandwidth: 10867.169↓, 10867.936↑ Mbps
Packet rate estimate: 938557.1↓, 942150.2↑ (11↓, 22↑ TCP MSS/op)
Test duration: 30.0058 s.
```


