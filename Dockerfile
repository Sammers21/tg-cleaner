FROM fedora:30
RUN sudo dnf install java-11-openjdk.x86_64 -y
ENV JAVA_HOME /usr/lib/jvm/java-11-openjdk-11.0.4.11-0.fc30.x86_64
RUN mkdir /app
COPY . /app
RUN cd /app && ./gradlew fatJar
ENTRYPOINT java -jar /app/build/libs/tg-cleaner.jar