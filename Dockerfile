FROM openjdk:8-jdk-alpine
VOLUME /tmp
ADD target/facebook-page-events-to-google-calendar-1.0-SNAPSHOT.jar app.jar
ENV JAVA_OPTS=""
EXPOSE 8080
ENV facebookAccessToken ""
CMD java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar --facebook.accessToken=${facebookAccessToken}