# 1. Java 17 환경
FROM openjdk:17-jdk-slim

# 2. 작업 폴더
WORKDIR /app

# 3. 파일 복사
COPY . .

# 4. gradlew 실행 권한
RUN chmod +x gradlew

# 5. 빌드
RUN ./gradlew build -x test

# 6. 실행
CMD ["sh", "-c", "java -jar build/libs/*.jar"]