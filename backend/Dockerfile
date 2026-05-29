FROM golang:1.21-alpine AS builder

WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download

COPY . .

# Build with CGO_ENABLED=1 for sqlite3
RUN CGO_ENABLED=1 GOOS=linux go build -a -ldflags '-linkmode external -extldflags "-static"' -o shadowcell-server ./cmd/server

FROM alpine:latest

WORKDIR /app
RUN apk add --no-cache ca-certificates tzdata sqlite

COPY --from=builder /app/shadowcell-server .

EXPOSE 8443

CMD ["./shadowcell-server"]
