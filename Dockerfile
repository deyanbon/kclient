FROM openjdk:17 AS kclient
LABEL authors="dbontch"

#RUN mkdir data

ADD target/kclient-1.0.0-SNAPSHOT.jar kclient.jar

ENTRYPOINT ["java", "-jar", "kclient.jar"]
EXPOSE 8080
