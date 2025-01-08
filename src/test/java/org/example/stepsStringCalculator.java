package org.example;

import static org.junit.Assert.assertEquals;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class stepsStringCalculator{

    private StringCalculator stringCalculator;
    private int result;
/*     private Exception exception; */


    @Given("I hava a string a string calculator")
    public void I_hava_a_string_a_string_calculator() {
        stringCalculator = new StringCalculator();
    }

   @When("with values {string}")
   public void string_calculation(String numbers){
        result = stringCalculator.add(numbers);
   }

    @Then("the calculator returns {int}")
    public void result_string_calculation(int expected){
        assertEquals(expected, result);
    }




}