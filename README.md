# DTUPay SOAP
---

In order to run the code, the server needs to be started.

Change directory to the location of the pom file
```
cd DTU-REST
```
and then start the server with ```mvn quarkus:de``` or with compile (both work)
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

The tests executed are from the following feature file declaration:
```
Feature: Payment
  Scenario: Successful Payment
    Given a customer with name "Susan", last name "Baldwin", and CPR "030154-4421"
    And the customer is registered with the bank with an initial balance of 1000 kr
    And the customer is registered with Simple DTU Pay using their bank account
    And a merchant with name "Daniel", last name "Oliver", and CPR "131161-3045"
    And the merchant is registered with the bank with an initial balance of 1000 kr
    And the merchant is registered with Simple DTU Pay using their bank account
    When the merchant initiates a payment for 10 kr by the customer
    Then the payment is successful
    And the balance of the customer at the bank is 990 kr
    And the balance of the merchant at the bank is 1010 kr
```

### DTU REST Paths

The previous paths should work from the earlier iteration of this repo: ```/customers```, ```/merchants``` and ```/payments```, where if you want to play around with it or do manual testing, you can also just run ```curl <<Type, JSON and address details here>>``` and then go into (or curl from) localhost:8080/<<path of interest>>. For example, run:

```
curl -X POST -H "Content-Type: application/json" -d '{"name": "John Doe"}' http://localhost:8080/customers
```
And then go onto localhost:8080/customers



### Additional paths are added