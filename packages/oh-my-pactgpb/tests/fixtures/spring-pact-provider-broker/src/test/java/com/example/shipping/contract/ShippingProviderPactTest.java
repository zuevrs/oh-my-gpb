package com.example.shipping.contract;

import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;

@Provider("shipping-provider")
@PactBroker
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class ShippingProviderPactTest {

  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider.class)
  void verifyPacts(PactVerificationContext context) {
    context.verifyInteraction();
  }

  @State("shipment exists")
  void shipmentExists() {
  }
}
