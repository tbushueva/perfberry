FROM golang:1.19.2-alpine3.16

WORKDIR /app

ADD . .
RUN CGO_ENABLED=0 go build -o /usr/local/bin/perfberry-cli -ldflags '-w'


FROM alpine:3.16

RUN apk add --update ca-certificates && rm /var/cache/apk/*

COPY --from=0 /usr/local/bin/perfberry-cli /usr/local/bin/

CMD perfberry-cli
