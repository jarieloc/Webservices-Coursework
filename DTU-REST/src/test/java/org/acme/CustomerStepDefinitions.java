package org.acme;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.restassured.response.Response;
import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

public class CustomerStepDefinitions {

    private Response response;

    @Given("the server is running")
    public void the_server_is_running() {
        // Ensure the server is running
        baseURI = "http://localhost:8080";
    }

    @When("I create a customer with name {string}")
    public void i_create_a_customer_with_name(String name) {
        response = given()
            .contentType("application/json")
            .body("{\"name\": \"" + name + "\"}")
            .when()
            .post("/customers");
    }

    @Then("the response status should be {int}")
    public void the_response_status_should_be(Integer statusCode) {
        response.then().statusCode(statusCode);
    }

    @Then("the customer should be added successfully")
    public void the_customer_should_be_added_successfully() {
        response.then().body("id", notNullValue());
    }

    @When("I retrieve all customers")
    public void i_retrieve_all_customers() {
        response = when().get("/customers");
    }

    @Then("the customer list should not be empty")
    public void the_customer_list_should_not_be_empty() {
        response.then().body("$.size()", greaterThan(0));
    }
}