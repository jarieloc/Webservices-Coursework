Feature: Customer Management
  As a user
  I want to manage customers
  So that I can add and view customer information

  Scenario: Adding a new customer
    Given the server is running
    When I create a customer with name "John Doe"
    Then the response status should be 201
    And the customer should be added successfully

  Scenario: Listing all customers
    Given the server is running
    When I retrieve all customers
    Then the response status should be 200
    And the customer list should not be empty