# base image - an image with openjdk 17
FROM openjdk:17

# working directory inside docker image
WORKDIR /home/sd

# copy the jar created by assembly to the docker image
COPY target/*jar-with-dependencies.jar sd2122.jar

# run Discovery when starting the docker image
CMD ["java", "-cp", "/home/sd/sd2122.jar", "sd2122.aula2.server.UsersServer"]
