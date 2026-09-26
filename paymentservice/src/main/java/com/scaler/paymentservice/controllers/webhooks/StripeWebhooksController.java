package com.scaler.paymentservice.controllers.webhooks;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks/stripe")
public class StripeWebhooksController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhooksController.class);

    // The endpoint's signing secret from the Stripe dashboard (whsec_...).
    private final String webhookSecret;

    public StripeWebhooksController(@Value("${stripe.webhook_secret}") String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    // Stripe signs every event, so anything without a valid, recent signature is rejected.
    // The raw body is needed for that check, so it's read as a String rather than parsed first.
    @PostMapping
    public ResponseEntity<Void> handleWebhookRequest(@RequestBody String payload,
                                                     @RequestHeader(value = "Stripe-Signature", required = false) String signature){
        if (signature == null) {
            return ResponseEntity.badRequest().build();
        }
        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.badRequest().build();
        }

        // A Payment Link is paid through a Checkout Session. There's no order record here yet,
        // so this is where marking the order as paid would go.
        if ("checkout.session.completed".equals(event.getType())) {
            log.info("Payment completed: event {}", event.getId());
        }
        return ResponseEntity.ok().build();
    }
}
