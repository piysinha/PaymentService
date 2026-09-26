package com.scaler.paymentservice.controllers.webhooks;

import com.scaler.paymentservice.security.SpringSecurityConfig;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StripeWebhooksController.class)
@Import(SpringSecurityConfig.class)
@TestPropertySource(properties = "STRIPE_WEBHOOK_SECRET=" + StripeWebhooksControllerTest.SECRET)
class StripeWebhooksControllerTest {

    static final String SECRET = "whsec_test_secret";

    private static final String PAYMENT_COMPLETED = """
            {"id": "evt_1", "object": "event", "type": "checkout.session.completed",
             "data": {"object": {"id": "cs_test_1", "object": "checkout.session", "payment_status": "paid"}}}""";
    private static final String OTHER_EVENT = """
            {"id": "evt_2", "object": "event", "type": "payment_link.created",
             "data": {"object": {"id": "plink_1", "object": "payment_link"}}}""";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void a_signed_payment_completed_event_is_accepted() throws Exception {
        mockMvc.perform(webhook(PAYMENT_COMPLETED).header("Stripe-Signature", signature(PAYMENT_COMPLETED, SECRET, now())))
                .andExpect(status().isOk());
    }

    @Test
    void other_signed_events_are_acknowledged() throws Exception {
        mockMvc.perform(webhook(OTHER_EVENT).header("Stripe-Signature", signature(OTHER_EVENT, SECRET, now())))
                .andExpect(status().isOk());
    }

    @Test
    void an_event_signed_with_another_secret_is_rejected() throws Exception {
        mockMvc.perform(webhook(PAYMENT_COMPLETED).header("Stripe-Signature", signature(PAYMENT_COMPLETED, "whsec_attacker", now())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void an_unsigned_event_is_rejected() throws Exception {
        mockMvc.perform(webhook(PAYMENT_COMPLETED)).andExpect(status().isBadRequest());
    }

    @Test
    void a_replayed_old_event_is_rejected() throws Exception {
        long tenMinutesAgo = now() - 600;
        mockMvc.perform(webhook(PAYMENT_COMPLETED).header("Stripe-Signature", signature(PAYMENT_COMPLETED, SECRET, tenMinutesAgo)))
                .andExpect(status().isBadRequest());
    }

    private static MockHttpServletRequestBuilder webhook(String payload) {
        return post("/webhooks/stripe").contentType(MediaType.APPLICATION_JSON).content(payload);
    }

    /** A Stripe-Signature header, computed the way Stripe signs events. */
    private static String signature(String payload, String secret, long timestamp) throws Exception {
        return "t=" + timestamp + ",v1=" + Webhook.Util.computeHmacSha256(secret, timestamp + "." + payload);
    }

    private static long now() {
        return System.currentTimeMillis() / 1000;
    }
}
