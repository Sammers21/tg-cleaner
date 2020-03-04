FROM adoptopenjdk/openjdk11:jdk-11.0.4_11-ubuntu

RUN apt-get update
RUN apt-get -y install unzip

RUN \
    cd /usr/local && \
    curl -L https://services.gradle.org/distributions/gradle-5.6.2-bin.zip -o gradle-5.6.2-bin.zip && \
    unzip gradle-5.6.2-bin.zip && \
    rm gradle-5.6.2-bin.zip

# Export some environment variables
ENV GRADLE_HOME=/usr/local/gradle-5.6.2
ENV PATH=$PATH:$GRADLE_HOME/bin

RUN apt-get -y install tmux

RUN mkdir /app
COPY . /app
RUN cd /app \
    && gradle fatJar \
    && cp /app/build/libs/tg-cleaner.jar /tg-cleaner.jar \
    && rm -rf /app

VOLUME /tdlib

ENTRYPOINT tmux new-session -s "tg-cleaner" -d 'java -jar /tg-cleaner.jar' && tmux a