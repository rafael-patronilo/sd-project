# base image
FROM nunopreguica/sd2122tpbase

# working directory inside docker image
WORKDIR /home/sd

# copy the jar created by assembly to the docker image
COPY target/*jar-with-dependencies.jar sd2122.jar

# copy the keystore and truststore to the docker image
COPY *.*ks /home/sd/

# copy the file of properties to the docker image
COPY trab.props trab.props

# run Discovery when starting the docker image
CMD ["java", "-cp", "/home/sd/sd2122.jar", \
"tp1.server.rest.RESTUsersServer"]
