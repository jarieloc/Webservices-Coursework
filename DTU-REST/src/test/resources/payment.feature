Feature: Payment System
  As a user
  I want to register customers and merchants
  So that I can make payments between them

  Scenario: Registering a customer
    Given a customer with name "John Doe"
    When the customer is registered with Simple DTU Pay
    Then the response status must be 201

  Scenario: Registering a merchant
    Given a merchant with name "SuperMart"
    When the merchant is registered with Simple DTU Pay
    Then the response status must be 201

  Scenario: Making a payment
    Given a customer with name "John Doe", who is registered with Simple DTU Pay
    And a merchant with name "SuperMart", who is registered with Simple DTU Pay
    When the merchant initiates a payment of 50 kr from the customer
    Then the payment is successful
    And the response status must be 201

  Scenario: Listing all payments
    Given a customer with name "John Doe", who is registered with Simple DTU Pay
    And a merchant with name "SuperMart", who is registered with Simple DTU Pay
    And a successful payment of 50 kr from the customer to the merchant
    When the manager asks for a list of payments
    Then the list contains a payment where customer "John Doe" paid "50" kr to merchant "SuperMart"
