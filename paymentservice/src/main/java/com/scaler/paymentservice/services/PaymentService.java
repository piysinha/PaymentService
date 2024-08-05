package com.scaler.paymentservice.services;

import com.scaler.paymentservice.paymentgateways.stripe.StripePaymentGateWay;
import com.stripe.exception.StripeException;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private StripePaymentGateWay stripePaymentGateWay;

    public PaymentService(StripePaymentGateWay stripePaymentGateWay) {
        this.stripePaymentGateWay = stripePaymentGateWay;
    }


    public String createPaymentLink(long orderId) throws StripeException {
        //String emailOfCustomer =
        //String phoneNumber =
        //Long amount =
        return stripePaymentGateWay.generatePaymentLink(15000L);

    }
}
