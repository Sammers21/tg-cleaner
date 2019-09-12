FROM fedora:30
RUN dnf install java-11-openjdk.x86_64 -y
ENV JAVA_HOME /usr/lib/jvm/java-11-openjdk-11.0.4.11-0.fc30.x86_64
RUN mkdir /app
COPY . /app
RUN cd /app && ./gradlew fatJar
RUN cp /app/build/libs/tg-cleaner.jar /tg-cleaner.jar
RUN rm -rf /app
RUN dnf clean all
ENTRYPOINT java -jar /tg-cleaner.jar