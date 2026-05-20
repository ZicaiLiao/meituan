# Netty Protocol

The gateway uses newline-delimited JSON frames.

## Common Fields

- `type`: `AUTH`, `PING`, `PONG`, `CHAT`, `SYSTEM`, `ACK`
- `traceId`: unique request identifier
- `payload`: frame-specific body

## Examples

### Authenticate

```json
{"type":"AUTH","traceId":"t-1","payload":{"token":"demo-customer-1001"}}
```

### Ping

```json
{"type":"PING","traceId":"t-2","payload":{}}
```

### Chat Message

```json
{"type":"CHAT","traceId":"t-3","payload":{"conversationId":7001,"receiverId":2001,"content":"订单到了吗？"}}
```

### Ack

```json
{"type":"ACK","traceId":"t-4","payload":{"messageId":"msg-9001"}}
```
