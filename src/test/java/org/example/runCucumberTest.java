package org.example;

import org.junit.runner.RunWith;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;

@RunWith(Cucumber.class)
@CucumberOptions(
    features = "src/test/resources/org/example",
    plugin = {"pretty"},
    glue = "org.example"
)


public class runCucumberTest {
}
