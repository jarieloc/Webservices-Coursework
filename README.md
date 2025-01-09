# DTU Pay REST
---

Used quarkus and cucumber (will update later)

To run cucumber tests you must first run the server

Change directory:
```
cd DTU-REST
```

Start the server with ```mvn quarkus:de``` or with compile (both work)
```
mvn compile quarkus:dev
```
or just run using:
```
java -jar target/quarkus-app/quarkus-run.jar
```

Once the server is started, ```mvn test``` can be executed:
```
cd /DTU-REST
mvn test
```
This should run the tests on all the paths: ```/customers```, ```/merchants``` and ```/payments```, but if you want to play around with it or do manual testing, you can also just run ```curl <<Type, JSON and address details here>>``` and then go into (or curl from) localhost:8080/<<path of interest>>. For example, run:

```
curl -X POST -H "Content-Type: application/json" -d '{"name": "John Doe"}' http://localhost:8080/customers
```
And then go onto localhost:8080/customers