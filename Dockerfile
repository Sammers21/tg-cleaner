FROM fedora:30

RUN dnf install \
    gradle \
    java-11-openjdk \
    tmux \
    -y \
    && dnf clean all \
    && dnf autoremove

RUN ls -la /usr/lib/jvm
ENV JAVA_HOME /usr/lib/jvm/java-11-openjdk-*

RUN mkdir /app
COPY . /app
RUN cd /app \
    && gradle fatJar \
    && cp /app/build/libs/tg-cleaner.jar /tg-cleaner.jar \
    && rm -rf /app

VOLUME /tdlib

ENTRYPOINT tmux new-session -s "tg-cleaner" -d 'java -jar /tg-cleaner.jar' && tmux a