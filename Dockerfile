FROM fedora:30

RUN dnf install \
    java-11-openjdk.x86_64 \
    tmux \
    -y \
    && dnf clean all \
    && dnf autoremove

ENV JAVA_HOME /usr/lib/jvm/java-11-openjdk-11.0.4.11-0.fc30.x86_64

RUN mkdir /app
COPY . /app
RUN cd /app \
    && ./gradlew fatJar \
    && cp /app/build/libs/tg-cleaner.jar /tg-cleaner.jar \
    && rm -rf /app

VOLUME /tdlib

ENTRYPOINT tmux new-session -s "tg-cleaner" -d 'java -jar /tg-cleaner.jar' && tmux a