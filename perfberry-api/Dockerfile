FROM sbtscala/scala-sbt:eclipse-temurin-11.0.16_1.7.2_2.13.10

WORKDIR /app

RUN apt update && apt install unzip

COPY . /app

RUN sbt dist
RUN unzip target/universal/perfberryapi-1.0.0.zip


FROM eclipse-temurin:11.0.16.1_1-jre

COPY --from=0 /app/perfberryapi-1.0.0/ /app/

CMD /app/bin/perfberryapi -J-Xms768M -J-Xmx1920M

EXPOSE 9000
