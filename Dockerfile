FROM clojure
MAINTAINER Andrew Rosa

RUN useradd -r -s /bin/false -m app
USER app
WORKDIR /home/app
ADD project.clj /home/app/project.clj
RUN lein deps

ADD src /home/app/src
RUN lein uberjar

FROM openjdk:8u131-jre-alpine

RUN addgroup -g 1000 app && \
    adduser -D -u 1000 -G app  app && \
    mkdir -p /code && \
    chown app:app /code

USER app

WORKDIR /code

COPY --from=0 /home/app/target/codeclimate-kibit.jar .

CMD \
  [ "java" \
  , "-XX:+UseParNewGC" \
  , "-XX:MinHeapFreeRatio=5" \
  , "-XX:MaxHeapFreeRatio=10" \
  , "-jar", "./codeclimate-kibit.jar", "." \
  , "-C", "/config.json" \
  ]
