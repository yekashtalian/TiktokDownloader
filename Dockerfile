FROM openjdk:17
COPY . /src/main
WORKDIR /src/main
RUN javac Bot.java
CMD ["java", "Bain"]
