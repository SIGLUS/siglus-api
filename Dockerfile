FROM alpine:3.18

COPY servicebase/package.json /package.json
COPY servicebase/run.sh /run.sh
COPY servicebase/wait-for-postgres.sh /wait-for-postgres.sh

RUN chmod +x run.sh \
  && apk update \
  && apk add bash \
  && apk add nodejs \
  && apk add npm \
  && apk add openjdk8 \
  && apk add --no-cache freetype \
  && apk add postgresql-client \
  && apk add postgresql \
  && apk add dmidecode \
  && npm install

COPY build/libs/*.jar /service.jar
COPY build/consul /consul

EXPOSE 8080
CMD ./run.sh
