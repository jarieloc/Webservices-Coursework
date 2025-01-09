package org.acme;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.restassured.response.Response;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

public class PaymentStepDefinitions {

    private Map<String, String> customers = new HashMap<>();
    private Map<String, String> merchants = new HashMap<>();
    private Response response;

    @Given("a customer with name {string}")
    public void aCustomerWithName(String name) {
        customers.put(name, null); // Keep track of customer names for tests
    }

    @When("the customer is registered with Simple DTU Pay")
    public void theCustomerIsRegisteredWithSimpleDTUPay() {
        for (String name : customers.keySet()) {
            response = given()
                .contentType("application/json")
                .body("{\"name\":\"" + name + "\"}")
                .post("/customers");
            customers.put(name, response.jsonPath().getString("id")); // Extract customer ID from response
        }
    }

    @Given("a merchant with name {string}")
    public void aMerchantWithName(String name) {
        merchants.put(name, null); // Keep track of merchant names for tests
    }

    @When("the merchant is registered with Simple DTU Pay")
    public void theMerchantIsRegisteredWithSimpleDTUPay() {
        for (String name : merchants.keySet()) {
            response = given()
                .contentType("application/json")
                .body("{\"name\":\"" + name + "\"}")
                .post("/merchants");
            merchants.put(name, response.jsonPath().getString("id")); // Extract merchant ID from response
        }
    }

    @When("the merchant initiates a payment of {int} kr from the customer")
    public void theMerchantInitiatesAPaymentOfKrFromTheCustomer(Integer amount) {
        String customerId = customers.get("John Doe");
        String merchantId = merchants.get("SuperMart");
        response = given()
            .contentType("application/json")
            .body("{\"amount\":" + amount + ", \"customerId\":\"" + customerId + "\", \"merchantId\":\"" + merchantId + "\"}")
            .post("/payments");
    }

    @Then("the payment is successful")
    public void thePaymentIsSuccessful() {
        response.then()
            .statusCode(201)
            .body("amount", notNullValue())
            .body("customerId", notNullValue())
            .body("merchantId", notNullValue());
    }

    @When("the manager asks for a list of payments")
    public void theManagerAsksForAListOfPayments() {
        response = when().get("/payments");
    }

    @Then("the list contains a payment where customer {string} paid {string} kr to merchant {string}")
    public void theListContainsAPaymentWhereCustomerPaidKrToMerchant(String customerName, String amount, String merchantName) {
        response.then()
            .statusCode(200)
            .body("find { it.customerId == '" + customers.get(customerName) + "' && it.merchantId == '" + merchants.get(merchantName) + "' && it.amount == " + amount + " }", notNullValue());
    }

    @Then("the response status must be {int}")
    public void theResponseStatusMustBe(Integer statusCode) {
        response.then().statusCode(statusCode);
    }


    @Given("a customer with name {string}, who is registered with Simple DTU Pay")
    public void a_customer_with_name_who_is_registered_with_simple_dtu_pay(String name) {
        // Register the customer using the same logic as the existing step
        given()
            .contentType("application/json")
            .body("{\"name\":\"" + name + "\"}")
            .post("/customers")
            .then()
            .statusCode(201);
    }

    @Given("a merchant with name {string}, who is registered with Simple DTU Pay")
    public void a_merchant_with_name_who_is_registered_with_simple_dtu_pay(String name) {
        // Register the merchant using the same logic as the existing step
        given()
            .contentType("application/json")
            .body("{\"name\":\"" + name + "\"}")
            .post("/merchants")
            .then()
            .statusCode(201);
    }

    @Given("a successful payment of {int} kr from the customer to the merchant")
    public void a_successful_payment_of_kr_from_the_customer_to_the_merchant(Integer amount) {
        // Assuming "John Doe" is the customer and "SuperMart" is the merchant
        Response customerResponse = given()
            .contentType("application/json")
            .body("{\"name\":\"John Doe\"}")
            .post("/customers");
        String customerId = customerResponse.jsonPath().getString("id");

        Response merchantResponse = given()
            .contentType("application/json")
            .body("{\"name\":\"SuperMart\"}")
            .post("/merchants");
        String merchantId = merchantResponse.jsonPath().getString("id");

        given()
            .contentType("application/json")
            .body("{\"amount\":" + amount + ", \"customerId\":\"" + customerId + "\", \"merchantId\":\"" + merchantId + "\"}")
            .post("/payments")
            .then()
            .statusCode(201);
    }

}
