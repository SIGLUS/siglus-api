FROM openlmis/service-base:4

RUN apk add dmidecode

COPY build/libs/*.jar /service.jar
COPY build/consul /consul
