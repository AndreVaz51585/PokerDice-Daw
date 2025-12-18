1. ./gradlew clean :poker-dice-backend:app:bootJar -x ktlintCheck -x ktlintFormat (Isto para ignorar o ktlint)

2. cd .\poker-dice-frontend\

3. npm run build

4. cd ..

5. cd sql

6. docker-compose up --build -d