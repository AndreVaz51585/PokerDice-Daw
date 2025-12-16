1. ./gradlew clean :lesson10-web-app-modules-and-tests:app:bootJar -x ktlintCheck -x ktlintFormat (Isto para ignorar o ktlint)

2. cd .\poker-dice-frontend\

3. npm run build

4. cd sql

5. docker-compose up --build -d