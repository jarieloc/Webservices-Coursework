package org.acme;

import dtu.ws.fastmoney.BankService;
import dtu.ws.fastmoney.BankServiceService;
import dtu.ws.fastmoney.User;
import dtu.ws.fastmoney.BankServiceException_Exception;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;

import org.acme.models.Customer;
import org.acme.models.Merchant;
import org.acme.models.Payment;

public class PaymentStepDefinitions {

    private BankService bankService = new BankServiceService().getBankServicePort();
    private List<String> createdAccounts = new ArrayList<>();
    private BigDecimal customerInitialBalance;
    private BigDecimal merchantInitialBalance;

    private Customer currentCustomer;
    private Merchant currentMerchant;

    //private final Client client = ClientBuilder.newClient();
    private final Client client = org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder.newBuilder().build();
    private final String baseUrl = "http://localhost:8080"; // Replace with actual base URL of your REST service

    @Given("a customer with name {string}, last name {string}, and CPR {string}")
    public void createCustomer(String firstName, String lastName, String cpr) throws Exception {
        currentCustomer = new Customer();
        currentCustomer.setFirstName(firstName);
        currentCustomer.setLastName(lastName);
        
        // Generate a unique CPR number by appending a UUID
        String uniqueCpr = cpr + "-" + UUID.randomUUID().toString();
        currentCustomer.setCprNumber(uniqueCpr);

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setCprNumber(uniqueCpr);

        try {
            String account = bankService.createAccountWithBalance(user, BigDecimal.valueOf(1000));
            currentCustomer.setBankAccount(account);
            createdAccounts.add(account);
            customerInitialBalance = BigDecimal.valueOf(1000);
            System.out.println("Created customer account: " + account + " with balance 1000 kr");
        } catch (BankServiceException_Exception e) {
            if (e.getMessage().contains("Account already exists")) {
                String account = bankService.getAccountByCprNumber(uniqueCpr).getId();
                currentCustomer.setBankAccount(account);
                System.out.println("Existing customer account found: " + account);
            } else {
                throw e;
            }
        }
    }

    @Given("the customer is registered with Simple DTU Pay using their bank account")
    public void the_customer_is_registered_with_simple_dtu_pay_using_their_bank_account() {
        if (currentCustomer == null || currentCustomer.getBankAccount() == null) {
            throw new RuntimeException("No current customer or bank account defined");
        }

        // Check if the customer is already registered (you already have logic for that).
        Response getResponse = client.target(baseUrl + "/customers/" + currentCustomer.getCprNumber())
                .request()
                .get();

        if (getResponse.getStatus() == 200) {
            System.out.println("Customer already registered with Simple DTU Pay.");
        
            // read the existing CUSTOMER from the response
            Customer existingCustomer = getResponse.readEntity(Customer.class);
        
            // Update your local 'currentCustomer'
            currentCustomer.setId(existingCustomer.getId());
            currentCustomer.setBankAccount(existingCustomer.getBankAccount());
    
            // b) Double-check the SOAP bank also knows about this bank account
            try {
                bankService.getAccount(currentCustomer.getBankAccount());
                // If this call does not throw => SOAP bank already recognizes this account
            } catch (BankServiceException_Exception e) {
                // If the SOAP bank says "Account does not exist," let's re-create it
                if (e.getMessage().contains("Account does not exist")) {
                    System.out.println("SOAP bank is missing this merchant account. Re-creating.");
    
                    // Re-create the bank account for this merchant
                    User user = new User();
                    user.setCprNumber(currentCustomer.getCprNumber());
                    user.setFirstName(currentCustomer.getFirstName());
                    user.setLastName(currentCustomer.getLastName());
    
                    try {
                        String newAccount = bankService.createAccountWithBalance(user, BigDecimal.valueOf(1000));
                        currentCustomer.setBankAccount(newAccount);
    
                    } catch (BankServiceException_Exception ee) {
                        // If creating the account says "Account already exists", let's just look it up
                        if (ee.getMessage().contains("Account already exists")) {
                            System.out.println("The bank actually has an account for this CPR. We'll look it up instead.");
                            try {
                                String existingAccountId = bankService.getAccountByCprNumber(user.getCprNumber()).getId();
                                currentCustomer.setBankAccount(existingAccountId);
                            } catch (BankServiceException_Exception x) {
                                // Another fallback if getAccountByCprNumber also fails
                                throw new RuntimeException("Could not retrieve existing merchant account by CPR: " 
                                                            + x.getMessage(), x);
                            }
                        } else {
                            // Some other creation error
                            throw new RuntimeException("Could not create merchant account: " + ee.getMessage(), ee);
                        }
                    }
                } else {
                    // Some other SOAP bank error, not "Account does not exist"
                    throw new RuntimeException("Unexpected merchant bank error: " + e.getMessage(), e);
                }
            }
    
            // If we reach here, the local resource and SOAP bank are both in sync.
            return;
        } else if (getResponse.getStatus() == 404 || getResponse.getStatus() == 204) {
            // Means "not found," so proceed to register them
        } else {
            throw new RuntimeException("Unexpected response code: " + getResponse.getStatus() 
                + ", body: " + getResponse.readEntity(String.class));
        }

        // If not found, proceed with registration:
        Response postResponse = client.target(baseUrl + "/customers")
                .request()
                .post(Entity.json(currentCustomer));

        if (postResponse.getStatus() != 201) {
            throw new RuntimeException("Failed to register customer with Simple DTU Pay: " 
                + postResponse.readEntity(String.class));
        }

        // IMPORTANT: read the newly created Customer from the response.
        Customer createdCustomer = postResponse.readEntity(Customer.class);

        // Now update the local object to have the same ID and bankAccount.
        currentCustomer.setId(createdCustomer.getId());
        currentCustomer.setBankAccount(createdCustomer.getBankAccount());
    }

    @Given("the customer is registered with the bank with an initial balance of {int} kr")
    public void the_customer_is_registered_with_the_bank_with_an_initial_balance_of_kr(Integer initialBalance) throws Exception {
        if (currentCustomer == null) {
            throw new RuntimeException("Customer is not defined");
        }

        User user = new User();
        user.setFirstName(currentCustomer.getFirstName());
        user.setLastName(currentCustomer.getLastName());
        user.setCprNumber(currentCustomer.getCprNumber());

        try {
            String account = bankService.createAccountWithBalance(user, BigDecimal.valueOf(initialBalance));
            currentCustomer.setBankAccount(account);
            createdAccounts.add(account);
        } catch (BankServiceException_Exception e) {
            if (e.getMessage().contains("Account already exists")) {
                String account = bankService.getAccountByCprNumber(currentCustomer.getCprNumber()).getId();
                currentCustomer.setBankAccount(account);
            } else {
                throw new RuntimeException("Failed to create bank account for customer: " + e.getMessage());
            }
        }
    }

    @Given("a merchant with name {string}, last name {string}, and CPR {string}")
    public void createMerchant(String firstName, String lastName, String cpr) throws Exception {
        currentMerchant = new Merchant();
        currentMerchant.setFirstName(firstName);
        currentMerchant.setLastName(lastName);
        
        // Generate a unique CPR number by appending a UUID
        String uniqueCpr = cpr + "-" + UUID.randomUUID().toString();
        currentMerchant.setCprNumber(uniqueCpr);

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setCprNumber(uniqueCpr);

        try {
            String account = bankService.createAccountWithBalance(user, BigDecimal.valueOf(1000));
            currentMerchant.setBankAccount(account);
            createdAccounts.add(account);
            merchantInitialBalance = BigDecimal.valueOf(1000);
            System.out.println("Created merchant account: " + account + " with balance 1000 kr");
        } catch (BankServiceException_Exception e) {
            if (e.getMessage().contains("Account already exists")) {
                String account = bankService.getAccountByCprNumber(uniqueCpr).getId();
                currentMerchant.setBankAccount(account);
                System.out.println("Existing merchant account found: " + account);
            } else {
                throw e;
            }
        }
    }

    @Given("the merchant is registered with Simple DTU Pay using their bank account")
    public void the_merchant_is_registered_with_simple_dtu_pay_using_their_bank_account() {
        if (currentMerchant == null || currentMerchant.getBankAccount() == null) {
            throw new RuntimeException("No current merchant or bank account defined");
        }

        // Possibly check if the merchant is already registered as you do now
        // Check if the merchant is already registered
        Response getResponse = client.target(baseUrl + "/merchants/" + currentMerchant.getCprNumber())
                .request()
                .get();

            if (getResponse.getStatus() == 200) {
                System.out.println("Merchant already registered with Simple DTU Pay.");
            
                Merchant existingMerchant = getResponse.readEntity(Merchant.class);
                currentMerchant.setId(existingMerchant.getId());
                currentMerchant.setBankAccount(existingMerchant.getBankAccount());
            
                // Double-check with the SOAP bank
                try {
                    bankService.getAccount(currentMerchant.getBankAccount());
                } catch (BankServiceException_Exception e) {
                    if (e.getMessage().contains("Account does not exist")) {
                        System.out.println("SOAP bank is missing this merchant account. Re-creating.");
            
                        // Recreate it
                        User user = new User();
                        user.setFirstName(currentMerchant.getFirstName());
                        user.setLastName(currentMerchant.getLastName());
                        user.setCprNumber(currentMerchant.getCprNumber());
            
                        try {
                            String newAccount = bankService.createAccountWithBalance(user, BigDecimal.valueOf(1000));
                            currentMerchant.setBankAccount(newAccount); // Correct: Sets merchant's account
                            createdAccounts.add(newAccount);
                            merchantInitialBalance = BigDecimal.valueOf(1000);
                            System.out.println("Re-created merchant account with balance 1000 kr: " + newAccount);
                        } catch (BankServiceException_Exception ee) {
                            if (ee.getMessage().contains("Account already exists")) {
                                // handle that scenario
                            } else {
                                // handle or re-throw
                                throw new RuntimeException("Could not create account: " + ee.getMessage(), ee);
                            }
                        }
                    } else {
                        throw new RuntimeException("Unexpected bank error: " + e.getMessage());
                    }
                }
            
                return;
        } else if (getResponse.getStatus() != 404) {
            throw new RuntimeException("Unexpected response when checking merchant registration: " + getResponse.readEntity(String.class));
        }

        Response postResponse = client.target(baseUrl + "/merchants")
                .request()
                .post(Entity.json(currentMerchant));

        if (postResponse.getStatus() != 201) {
            throw new RuntimeException("Failed to register merchant with Simple DTU Pay: " 
                + postResponse.readEntity(String.class));
        }

        // Now read the created Merchant from the response
        Merchant createdMerchant = postResponse.readEntity(Merchant.class);

        // Update the local object with that ID
        currentMerchant.setId(createdMerchant.getId());
        currentMerchant.setBankAccount(createdMerchant.getBankAccount());
    }


    @Given("the merchant is registered with the bank with an initial balance of {int} kr")
    public void the_merchant_is_registered_with_the_bank_with_an_initial_balance_of_kr(Integer initialBalance) throws Exception {
        if (currentMerchant == null) {
            throw new RuntimeException("Merchant is not defined");
        }

        User user = new User();
        user.setFirstName(currentMerchant.getFirstName());
        user.setLastName(currentMerchant.getLastName());
        user.setCprNumber(currentMerchant.getCprNumber());

        try {
            String account = bankService.createAccountWithBalance(user, BigDecimal.valueOf(initialBalance));
            currentMerchant.setBankAccount(account);
            createdAccounts.add(account);
        } catch (BankServiceException_Exception e) {
            if (e.getMessage().contains("Account already exists")) {
                String account = bankService.getAccountByCprNumber(currentMerchant.getCprNumber()).getId();
                currentMerchant.setBankAccount(account);
            } else {
                throw new RuntimeException("Failed to create bank account for merchant: " + e.getMessage());
            }
        }
    }

    @When("the merchant initiates a payment for {int} kr by the customer")
    public void initiatePayment(int amount) throws Exception {
        if (currentCustomer == null || currentMerchant == null) {
            throw new RuntimeException("Customer or Merchant not defined");
        }

        Payment payment = new Payment();
        payment.setCustomerId(currentCustomer.getId());
        payment.setMerchantId(currentMerchant.getId());
        payment.setAmount(amount);

        Response response = client.target(baseUrl + "/payments")
                .request()
                .post(Entity.json(payment));

        if (response.getStatus() != 201) {
            throw new RuntimeException("Payment failed: " + response.readEntity(String.class));
        }
    }

    @Then("the payment is successful")
    public void the_payment_is_successful() {
        System.out.println("Payment was successfully processed between customer and merchant.");
    }

    @Then("the balance of the customer at the bank is {int} kr")
    public void the_balance_of_the_customer_at_the_bank_is_kr(Integer expectedBalance) throws Exception {
        BigDecimal actualBalance = bankService.getAccount(currentCustomer.getBankAccount()).getBalance();
        
        // Convert expected balance to BigDecimal with scale 1
        BigDecimal expectedBalanceBD = BigDecimal.valueOf(expectedBalance).setScale(1, RoundingMode.HALF_UP);
        
        // Round actual balance to scale 1
        BigDecimal actualBalanceRounded = actualBalance.setScale(1, RoundingMode.HALF_UP);
        
        System.out.println("Customer expected balance: " + expectedBalanceBD + " kr");
        System.out.println("Customer actual balance: " + actualBalanceRounded + " kr");

        assertEquals("Customer balance mismatch", expectedBalanceBD, actualBalanceRounded);
    }

    @Then("the balance of the merchant at the bank is {int} kr")
    public void the_balance_of_the_merchant_at_the_bank_is_kr(Integer expectedBalance) throws Exception {
        BigDecimal actualBalance = bankService.getAccount(currentMerchant.getBankAccount()).getBalance();
        
        // Convert expected balance to BigDecimal with scale 1
        BigDecimal expectedBalanceBD = BigDecimal.valueOf(expectedBalance).setScale(1, RoundingMode.HALF_UP);
        
        // Round actual balance to scale 1
        BigDecimal actualBalanceRounded = actualBalance.setScale(1, RoundingMode.HALF_UP);
        
        System.out.println("Merchant expected balance: " + expectedBalanceBD + " kr");
        System.out.println("Merchant actual balance: " + actualBalanceRounded + " kr");

        assertEquals("Merchant balance mismatch", expectedBalanceBD, actualBalanceRounded);
    }

    @After
    public void cleanupBankAccounts() throws Exception {
        for (String account : createdAccounts) {
            try {
                bankService.retireAccount(account);
                System.out.println("Retired account: " + account);
            } catch (BankServiceException_Exception e) {
                // Log the exception for debugging purposes
                System.err.println("Failed to retire account " + account + ": " + e.getMessage());
            }
        }
        createdAccounts.clear();
    }
}




















/* package org.acme;

import dtu.ws.fastmoney.BankService;
import dtu.ws.fastmoney.BankServiceService;
import dtu.ws.fastmoney.User;
import dtu.ws.fastmoney.BankServiceException_Exception;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;

import org.acme.models.Customer;
import org.acme.models.Merchant;
import org.acme.models.Payment;

public class PaymentStepDefinitions {

    private final BankService bankService = new BankServiceService().getBankServicePort();
    private final List<String> createdAccounts = new ArrayList<>();
    private final Client client = ClientBuilder.newClient(); // Initialize REST client
    private final String baseUrl = "http://localhost:8080"; // Replace with actual base URL

    private Customer currentCustomer;
    private Merchant currentMerchant;

    @Given("a customer with name {string}, last name {string}, and CPR {string}")
    public void createCustomer(String firstName, String lastName, String cpr) throws Exception {
        currentCustomer = new Customer();
        currentCustomer.setFirstName(firstName);
        currentCustomer.setLastName(lastName);
        currentCustomer.setCprNumber(cpr);

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setCprNumber(cpr);

        try {
            String account = bankService.createAccountWithBalance(user, BigDecimal.valueOf(1000));
            currentCustomer.setBankAccount(account);
            createdAccounts.add(account);
        } catch (BankServiceException_Exception e) {
            if (e.getMessage().contains("Account already exists")) {
                String account = bankService.getAccountByCprNumber(cpr).getId();
                currentCustomer.setBankAccount(account);
            } else {
                throw e;
            }
        }
    }

    @Given("the customer is registered with Simple DTU Pay using their bank account")
    public void the_customer_is_registered_with_simple_dtu_pay_using_their_bank_account() {
        if (currentCustomer == null || currentCustomer.getBankAccount() == null) {
            throw new RuntimeException("No current customer or bank account defined");
        }

        Response response = client.target(baseUrl + "/customers")
                .request()
                .post(Entity.json(currentCustomer));

        if (response.getStatus() != 201) {
            System.err.println("Failed to register customer with Simple DTU Pay: " + response.readEntity(String.class));
            throw new RuntimeException("Customer registration failed");
        }
    }

    @Given("a merchant with name {string}, last name {string}, and CPR {string}")
    public void createMerchant(String firstName, String lastName, String cpr) throws Exception {
        currentMerchant = new Merchant();
        currentMerchant.setFirstName(firstName);
        currentMerchant.setLastName(lastName);
        currentMerchant.setCprNumber(cpr);

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setCprNumber(cpr);

        try {
            String account = bankService.createAccountWithBalance(user, BigDecimal.valueOf(1000));
            currentMerchant.setBankAccount(account);
            createdAccounts.add(account);
        } catch (BankServiceException_Exception e) {
            if (e.getMessage().contains("Account already exists")) {
                String account = bankService.getAccountByCprNumber(cpr).getId();
                currentMerchant.setBankAccount(account);
            } else {
                throw e;
            }
        }
    }

    @Given("the merchant is registered with Simple DTU Pay using their bank account")
    public void the_merchant_is_registered_with_simple_dtu_pay_using_their_bank_account() {
        if (currentMerchant == null || currentMerchant.getBankAccount() == null) {
            throw new RuntimeException("No current merchant or bank account defined");
        }

        Response response = client.target(baseUrl + "/merchants")
                .request()
                .post(Entity.json(currentMerchant));

        if (response.getStatus() != 201) {
            System.err.println("Failed to register merchant with Simple DTU Pay: " + response.readEntity(String.class));
            throw new RuntimeException("Merchant registration failed");
        }
    }

    @When("the merchant initiates a payment for {int} kr by the customer")
    public void initiatePayment(int amount) {
        if (currentCustomer == null || currentMerchant == null) {
            throw new RuntimeException("Customer or Merchant not defined");
        }

        Payment payment = new Payment();
        payment.setCustomerId(currentCustomer.getId());
        payment.setMerchantId(currentMerchant.getId());
        payment.setAmount(amount);

        Response response = client.target(baseUrl + "/payments")
                .request()
                .post(Entity.json(payment));

        if (response.getStatus() != 201) {
            System.err.println("Payment failed: " + response.readEntity(String.class));
            throw new RuntimeException("Payment failed");
        }
    }

    @After
    public void cleanup() {
        createdAccounts.forEach(account -> {
            try {
                bankService.retireAccount(account);
            } catch (BankServiceException_Exception e) {
                System.err.println("Failed to retire account: " + account);
            }
        });
        createdAccounts.clear();
        client.close(); // Close REST client
    }
} */


/* package org.acme;

import dtu.ws.fastmoney.BankService;
import dtu.ws.fastmoney.BankServiceService;
import dtu.ws.fastmoney.User;
import dtu.ws.fastmoney.BankServiceException_Exception;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;


import org.acme.models.Customer;
import org.acme.models.Merchant;
import org.acme.models.Payment;

public class PaymentStepDefinitions {

    private BankService bankService = new BankServiceService().getBankServicePort();
    private List<String> createdAccounts = new ArrayList<>();
    private BigDecimal customerInitialBalance;
    private BigDecimal merchantInitialBalance;

    private Customer currentCustomer;
    private Merchant currentMerchant;

    private final Client client = ClientBuilder.newBuilder().build();
    private final String baseUrl = "http://localhost:8080"; // Replace with actual base URL of your REST service

    @Given("a customer with name {string}, last name {string}, and CPR {string}")
    public void createCustomer(String firstName, String lastName, String cpr) throws Exception {
        currentCustomer = new Customer();
        currentCustomer.setFirstName(firstName);
        currentCustomer.setLastName(lastName);
        currentCustomer.setCprNumber(cpr);

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setCprNumber(cpr);

        try {
            String account = bankService.createAccountWithBalance(user, BigDecimal.valueOf(1000));
            currentCustomer.setBankAccount(account);
            createdAccounts.add(account);
            customerInitialBalance = BigDecimal.valueOf(1000);
        } catch (BankServiceException_Exception e) {
            if (e.getMessage().contains("Account already exists")) {
                String account = bankService.getAccountByCprNumber(cpr).getId();
                currentCustomer.setBankAccount(account);
            } else {
                throw e;
            }
        }
    }

    @Given("the customer is registered with Simple DTU Pay using their bank account")
    public void the_customer_is_registered_with_simple_dtu_pay_using_their_bank_account() {
        if (currentCustomer == null || currentCustomer.getBankAccount() == null) {
            throw new RuntimeException("No current customer or bank account defined");
        }

        Response response = client.target(baseUrl + "/customers")
                .request()
                .post(Entity.json(currentCustomer));

        if (response.getStatus() != 201) {
            throw new RuntimeException("Failed to register customer with Simple DTU Pay: " + response.readEntity(String.class));
        }
        System.out.println("Customer registered with Simple DTU Pay: " + currentCustomer.getFirstName() + " " + currentCustomer.getLastName());
    }

    @Given("a merchant with name {string}, last name {string}, and CPR {string}")
    public void createMerchant(String firstName, String lastName, String cpr) throws Exception {
        currentMerchant = new Merchant();
        currentMerchant.setFirstName(firstName);
        currentMerchant.setLastName(lastName);
        currentMerchant.setCprNumber(cpr);

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setCprNumber(cpr);

        try {
            String account = bankService.createAccountWithBalance(user, BigDecimal.valueOf(1000));
            currentMerchant.setBankAccount(account);
            createdAccounts.add(account);
            merchantInitialBalance = BigDecimal.valueOf(1000);
        } catch (BankServiceException_Exception e) {
            if (e.getMessage().contains("Account already exists")) {
                String account = bankService.getAccountByCprNumber(cpr).getId();
                currentMerchant.setBankAccount(account);
            } else {
                throw e;
            }
        }
    }

    @Given("the merchant is registered with Simple DTU Pay using their bank account")
    public void the_merchant_is_registered_with_simple_dtu_pay_using_their_bank_account() {
        if (currentMerchant == null || currentMerchant.getBankAccount() == null) {
            throw new RuntimeException("No current merchant or bank account defined");
        }

        Response response = client.target(baseUrl + "/merchants")
                .request()
                .post(Entity.json(currentMerchant));

        if (response.getStatus() != 201) {
            throw new RuntimeException("Failed to register merchant with Simple DTU Pay: " + response.readEntity(String.class));
        }
        System.out.println("Merchant registered with Simple DTU Pay: " + currentMerchant.getFirstName() + " " + currentMerchant.getLastName());
    }

    @When("the merchant initiates a payment for {int} kr by the customer")
    public void initiatePayment(int amount) throws Exception {
        if (currentCustomer == null || currentMerchant == null) {
            throw new RuntimeException("Customer or Merchant not defined");
        }

        Payment payment = new Payment();
        payment.setCustomerId(currentCustomer.getId());
        payment.setMerchantId(currentMerchant.getId());
        payment.setAmount(amount);

        Response response = client.target(baseUrl + "/payments")
                .request()
                .post(Entity.json(payment));

        if (response.getStatus() != 201) {
            throw new RuntimeException("Payment failed: " + response.readEntity(String.class));
        }

        System.out.println("Payment initiated successfully for " + amount + " kr.");
    }

    @Then("the payment is successful")
    public void the_payment_is_successful() {
        System.out.println("Payment was successfully processed between customer and merchant.");
    }

    @Then("the balance of the customer at the bank is {int} kr")
    public void verifyCustomerBalance(int expectedBalance) throws Exception {
        BigDecimal actualBalance = bankService.getAccount(currentCustomer.getBankAccount()).getBalance();
        assertEquals(BigDecimal.valueOf(expectedBalance), actualBalance);
    }

    @Then("the balance of the merchant at the bank is {int} kr")
    public void verifyMerchantBalance(int expectedBalance) throws Exception {
        BigDecimal actualBalance = bankService.getAccount(currentMerchant.getBankAccount()).getBalance();
        assertEquals(BigDecimal.valueOf(expectedBalance), actualBalance);
    }

    // @After
    // public void cleanupBankAccounts() throws Exception {
    //     for (String account : createdAccounts) {
    //         try {
    //             bankService.retireAccount(account);
    //         } catch (BankServiceException_Exception e) {
    //             // Handle exceptions during cleanup
    //         }
    //     }
    //     createdAccounts.clear();
    // }

    @Given("the customer is registered with the bank with an initial balance of {int} kr")
    public void the_customer_is_registered_with_the_bank_with_an_initial_balance_of_kr(Integer initialBalance) throws Exception {
        if (currentCustomer == null) {
            throw new RuntimeException("Customer is not defined");
        }

        User user = new User();
        user.setFirstName(currentCustomer.getFirstName());
        user.setLastName(currentCustomer.getLastName());
        user.setCprNumber(currentCustomer.getCprNumber());

        try {
            String account = bankService.createAccountWithBalance(user, BigDecimal.valueOf(initialBalance));
            currentCustomer.setBankAccount(account);
            createdAccounts.add(account);
        } catch (BankServiceException_Exception e) {
            if (e.getMessage().contains("Account already exists")) {
                String account = bankService.getAccountByCprNumber(currentCustomer.getCprNumber()).getId();
                currentCustomer.setBankAccount(account);
            } else {
                throw new RuntimeException("Failed to create bank account for customer: " + e.getMessage());
            }
        }
    }

    @Given("the merchant is registered with the bank with an initial balance of {int} kr")
    public void the_merchant_is_registered_with_the_bank_with_an_initial_balance_of_kr(Integer initialBalance) throws Exception {
        if (currentMerchant == null) {
            throw new RuntimeException("Merchant is not defined");
        }

        User user = new User();
        user.setFirstName(currentMerchant.getFirstName());
        user.setLastName(currentMerchant.getLastName());
        user.setCprNumber(currentMerchant.getCprNumber());

        try {
            String account = bankService.createAccountWithBalance(user, BigDecimal.valueOf(initialBalance));
            currentMerchant.setBankAccount(account);
            createdAccounts.add(account);
        } catch (BankServiceException_Exception e) {
            if (e.getMessage().contains("Account already exists")) {
                String account = bankService.getAccountByCprNumber(currentMerchant.getCprNumber()).getId();
                currentMerchant.setBankAccount(account);
            } else {
                throw new RuntimeException("Failed to create bank account for merchant: " + e.getMessage());
            }
        }
    }
} */


/* package org.acme;

import dtu.ws.fastmoney.BankService;
import dtu.ws.fastmoney.BankServiceService;
import dtu.ws.fastmoney.User;
import dtu.ws.fastmoney.BankServiceException_Exception;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.acme.models.Customer;
import org.acme.models.Merchant;

public class PaymentStepDefinitions {

    private BankService bankService = new BankServiceService().getBankServicePort();
    private List<String> createdAccounts = new ArrayList<>();
    private BigDecimal customerInitialBalance;
    private BigDecimal merchantInitialBalance;

    private Customer currentCustomer;
    private Merchant currentMerchant;

    @Given("a customer with name {string}, last name {string}, and CPR {string}")
    public void createCustomer(String firstName, String lastName, String cpr) throws Exception {
        currentCustomer = new Customer();
        currentCustomer.setFirstName(firstName);
        currentCustomer.setLastName(lastName);
        currentCustomer.setCprNumber(cpr);

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setCprNumber(cpr);

        try {
            String account = bankService.createAccountWithBalance(user, BigDecimal.valueOf(1000));
            currentCustomer.setBankAccount(account);
            createdAccounts.add(account);
            customerInitialBalance = BigDecimal.valueOf(1000);
        } catch (BankServiceException_Exception e) {
            if (e.getMessage().contains("Account already exists")) {
                String account = bankService.getAccountByCprNumber(cpr).getId();
                currentCustomer.setBankAccount(account);
            } else {
                throw e;
            }
        }
    }


    @Given("the customer is registered with the bank with an initial balance of {int} kr")
    public void the_customer_is_registered_with_the_bank_with_an_initial_balance_of_kr(Integer initialBalance) throws Exception {
        if (currentCustomer == null) {
            throw new RuntimeException("No current customer defined");
        }

        User user = new User();
        user.setFirstName(currentCustomer.getFirstName());
        user.setLastName(currentCustomer.getLastName());
        user.setCprNumber(currentCustomer.getCprNumber());

        try {
            String account = bankService.createAccountWithBalance(user, BigDecimal.valueOf(initialBalance));
            currentCustomer.setBankAccount(account);
            createdAccounts.add(account);
        } catch (BankServiceException_Exception e) {
            if (e.getMessage().contains("Account already exists")) {
                String account = bankService.getAccountByCprNumber(currentCustomer.getCprNumber()).getId();
                currentCustomer.setBankAccount(account);
            } else {
                throw new RuntimeException("Failed to create bank account for customer: " + e.getMessage());
            }
        }
    }

    @Given("a merchant with name {string}, last name {string}, and CPR {string}")
    public void createMerchant(String firstName, String lastName, String cpr) throws Exception {
        currentMerchant = new Merchant();
        currentMerchant.setFirstName(firstName);
        currentMerchant.setLastName(lastName);
        currentMerchant.setCprNumber(cpr);

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setCprNumber(cpr);

        try {
            String account = bankService.createAccountWithBalance(user, BigDecimal.valueOf(1000));
            currentMerchant.setBankAccount(account);
            createdAccounts.add(account);
            merchantInitialBalance = BigDecimal.valueOf(1000);
        } catch (BankServiceException_Exception e) {
            if (e.getMessage().contains("Account already exists")) {
                String account = bankService.getAccountByCprNumber(cpr).getId();
                currentMerchant.setBankAccount(account);
            } else {
                throw e;
            }
        }
    }

    @Given("the merchant is registered with the bank with an initial balance of {int} kr")
    public void the_merchant_is_registered_with_the_bank_with_an_initial_balance_of_kr(Integer initialBalance) throws Exception {
        if (currentMerchant == null) {
            throw new RuntimeException("No current merchant defined");
        }

        User user = new User();
        user.setFirstName(currentMerchant.getFirstName());
        user.setLastName(currentMerchant.getLastName());
        user.setCprNumber(currentMerchant.getCprNumber());

        try {
            String account = bankService.createAccountWithBalance(user, BigDecimal.valueOf(initialBalance));
            currentMerchant.setBankAccount(account);
            createdAccounts.add(account);
        } catch (BankServiceException_Exception e) {
            if (e.getMessage().contains("Account already exists")) {
                String account = bankService.getAccountByCprNumber(currentMerchant.getCprNumber()).getId();
                currentMerchant.setBankAccount(account);
            } else {
                throw new RuntimeException("Failed to create bank account for merchant: " + e.getMessage());
            }
        }
    }

    @When("the merchant initiates a payment for {int} kr by the customer")
    public void initiatePayment(int amount) throws Exception {
        if (currentCustomer == null || currentMerchant == null) {
            throw new RuntimeException("Customer or Merchant not defined");
        }

        bankService.transferMoneyFromTo(
                currentCustomer.getBankAccount(),
                currentMerchant.getBankAccount(),
                BigDecimal.valueOf(amount),
                "Payment from DTU Pay"
        );
    }

    @Then("the balance of the customer at the bank is {int} kr")
    public void verifyCustomerBalance(int expectedBalance) throws Exception {
        BigDecimal actualBalance = bankService.getAccount(currentCustomer.getBankAccount()).getBalance();
        assertEquals(BigDecimal.valueOf(expectedBalance), actualBalance);
    }

    @Then("the balance of the merchant at the bank is {int} kr")
    public void verifyMerchantBalance(int expectedBalance) throws Exception {
        BigDecimal actualBalance = bankService.getAccount(currentMerchant.getBankAccount()).getBalance();
        assertEquals(BigDecimal.valueOf(expectedBalance), actualBalance);
    }

    // @After
    // public void cleanupBankAccounts() throws Exception {
    //     for (String account : createdAccounts) {
    //         try {
    //             bankService.retireAccount(account);
    //         } catch (BankServiceException_Exception e) {
    //             // Handle exceptions during cleanup
    //         }
    //     }
    //     createdAccounts.clear();
    // }
} */


/* package org.acme;

import dtu.ws.fastmoney.BankService;
import dtu.ws.fastmoney.BankServiceService;
import dtu.ws.fastmoney.User;
import dtu.ws.fastmoney.BankServiceException_Exception;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.acme.models.Customer;
import org.acme.models.Merchant;

public class PaymentStepDefinitions {

    private BankService bankService = new BankServiceService().getBankServicePort();
    private List<String> createdAccounts = new ArrayList<>();
    private String customerAccount;
    private String merchantAccount;
    private BigDecimal customerInitialBalance;
    private BigDecimal merchantInitialBalance;

    private Customer currentCustomer;
    private Merchant currentMerchant;
    private String customerBankAccount;
    private String merchantBankAccount;


    @Given("a customer with name {string}, last name {string}, and CPR {string}")
    public void createCustomer(String firstName, String lastName, String cpr) throws Exception {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setCprNumber(cpr);

        try {
            customerAccount = bankService.createAccountWithBalance(user, BigDecimal.valueOf(1000));
            createdAccounts.add(customerAccount);
            customerInitialBalance = BigDecimal.valueOf(1000);
        } catch (BankServiceException_Exception e) {
            if (e.getMessage().contains("Account already exists")) {
                customerAccount = bankService.getAccountByCprNumber(cpr).getId();
            } else {
                throw e;
            }
        }
    } */

/*     @Given("a customer with name {string}, last name {string}, and CPR {string}")
    public void createCustomer(String firstName, String lastName, String cpr) throws Exception {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setCprNumber(cpr);

        customerAccount = bankService.createAccountWithBalance(user, BigDecimal.valueOf(1000));
        createdAccounts.add(customerAccount);
        customerInitialBalance = BigDecimal.valueOf(1000);
    } */

/*     @Given("the customer is registered with Simple DTU Pay using their bank account")
    public void registerCustomerWithDTUPay() {
        // Call your Customer Resource service to register the customer
    }

    @Given("a merchant with name {string}, last name {string}, and CPR {string}")
    public void createMerchant(String firstName, String lastName, String cpr) throws Exception {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setCprNumber(cpr);
    
        try {
            merchantAccount = bankService.createAccountWithBalance(user, BigDecimal.valueOf(1000));
            createdAccounts.add(merchantAccount);
            merchantInitialBalance = BigDecimal.valueOf(1000);
        } catch (BankServiceException_Exception e) {
            if (e.getMessage().contains("Account already exists")) {
                merchantAccount = bankService.getAccountByCprNumber(cpr).getId();
            } else {
                throw e;
            }
        }
    } */


/*     @Given("a merchant with name {string}, last name {string}, and CPR {string}")
    public void createMerchant(String firstName, String lastName, String cpr) throws Exception {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setCprNumber(cpr);

        merchantAccount = bankService.createAccountWithBalance(user, BigDecimal.valueOf(1000));
        createdAccounts.add(merchantAccount);
        merchantInitialBalance = BigDecimal.valueOf(1000);
    }
 */
 /*    @Given("the merchant is registered with Simple DTU Pay using their bank account")
    public void registerMerchantWithDTUPay() {
        // Call your Merchant Resource service to register the merchant
    }

    @When("the merchant initiates a payment for {int} kr by the customer")
    public void initiatePayment(int amount) throws Exception {
        bankService.transferMoneyFromTo(
                customerAccount, 
                merchantAccount, 
                BigDecimal.valueOf(amount), 
                "Payment from DTU Pay"
        );
    }

    @Then("the payment is successful")
    public void verifyPaymentSuccess() {
        // Can include assertions here if needed for HTTP response or status
    }

    @Then("the balance of the customer at the bank is {int} kr")
    public void verifyCustomerBalance(int expectedBalance) throws Exception {
        BigDecimal actualBalance = bankService.getAccount(customerAccount).getBalance();
        assertEquals(BigDecimal.valueOf(expectedBalance), actualBalance);
    }

    @Then("the balance of the merchant at the bank is {int} kr")
    public void verifyMerchantBalance(int expectedBalance) throws Exception {
        BigDecimal actualBalance = bankService.getAccount(merchantAccount).getBalance();
        assertEquals(BigDecimal.valueOf(expectedBalance), actualBalance);
    }

    @After
    public void cleanupBankAccounts() throws Exception {
        for (String account : createdAccounts) {
            try {
                bankService.retireAccount(account);
            } catch (BankServiceException_Exception e) {
                // Handle cleanup errors
            }
        }
        createdAccounts.clear();
    } */

    /* @After
    public void cleanupBankAccounts() throws Exception {
        for (String account : createdAccounts) {
            try {
                bankService.retireAccount(account);
            } catch (BankServiceException_Exception e) {
                // Handle exceptions
            }
        }
        createdAccounts.clear();
    } */

 /*    @Given("the customer is registered with the bank with an initial balance of {int} kr")
    public void the_customer_is_registered_with_the_bank_with_an_initial_balance_of_kr(Integer initialBalance) throws Exception {
        // Use the existing customer details and create a bank account
        User user = new User();
        user.setFirstName(currentCustomer.getFirstName());
        user.setLastName(currentCustomer.getLastName());
        user.setCprNumber(currentCustomer.getCprNumber());
        try {
            customerBankAccount = bankService.createAccountWithBalance(user, BigDecimal.valueOf(initialBalance));
            currentCustomer.setBankAccount(customerBankAccount);
        } catch (BankServiceException_Exception e) {
            throw new RuntimeException("Failed to create bank account for customer: " + e.getMessage());
        }
    }

    @Given("the merchant is registered with the bank with an initial balance of {int} kr")
    public void the_merchant_is_registered_with_the_bank_with_an_initial_balance_of_kr(Integer initialBalance) throws Exception {
        // Use the existing merchant details and create a bank account
        User user = new User();
        user.setFirstName(currentMerchant.getFirstName());
        user.setLastName(currentMerchant.getLastName());
        user.setCprNumber(currentMerchant.getCprNumber());
        try {
            merchantBankAccount = bankService.createAccountWithBalance(user, BigDecimal.valueOf(initialBalance));
            currentMerchant.setBankAccount(merchantBankAccount);
        } catch (BankServiceException_Exception e) {
            throw new RuntimeException("Failed to create bank account for merchant: " + e.getMessage());
        }
    }

    
} */
