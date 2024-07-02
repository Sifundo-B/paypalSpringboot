package com.Test.paypal_integration.controller;

import com.Test.paypal_integration.paypal.PaypalService;
import com.paypal.api.payments.Links;
import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PaypalController {
    private final PaypalService paypalService;

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @PostMapping("/payment/create")
    public RedirectView createPayment(
            @RequestParam("method") String method,
            @RequestParam("amount") String amount,
            @RequestParam("currency") String currency
    ) {
        try {
            log.info("Create Payment - Method: {}, Amount: {}, Currency: {}", method, amount, currency);

            String cancelUrl = "http://localhost:8080/payment/cancel";
            String successUrl = "http://localhost:8080/payment/success";

            Payment payment = paypalService.createPayment(
                    Double.valueOf(amount),
                    currency,
                    method,
                    "sale",
                    "Payment description",
                    cancelUrl,
                    successUrl
            );

            for (Links links : payment.getLinks()) {
                if (links.getRel().equals("approval_url")) {
                    return new RedirectView(links.getHref());
                }
            }
        } catch (PayPalRESTException e) {
            log.error("Error occurred while creating payment: ", e);
        } catch (Exception e) {
            log.error("Unexpected error occurred: ", e);
        }
        return new RedirectView("/payment/error");
    }

    @GetMapping("/payment/success")
    public ModelAndView paymentSuccess(
            @RequestParam(value = "paymentId", required = false) String paymentId,
            @RequestParam(value = "PayerID", required = false) String payerId
    ) {
        ModelAndView modelAndView = new ModelAndView();
        try {
            log.info("Payment ID: {}", paymentId);
            log.info("Payer ID: {}", payerId);

            if (paymentId == null || payerId == null) {
                log.error("Missing paymentId or payerId");
                modelAndView.setViewName("paymentError");
                return modelAndView;
            }

            Payment payment = paypalService.executePayment(paymentId, payerId);
            if (payment.getState().equals("approved")) {
                modelAndView.setViewName("paymentSuccess");
                modelAndView.addObject("payment", payment);
                return modelAndView;
            }
        } catch (PayPalRESTException e) {
            log.error("Error occurred while executing payment: ", e);
        } catch (Exception e) {
            log.error("Unexpected error occurred: ", e);
        }
        modelAndView.setViewName("paymentError");
        return modelAndView;
    }

    @GetMapping("/payment/cancel")
    public String paymentCancel() {
        return "paymentCancel";
    }

    @GetMapping("/payment/error")
    public String paymentError() {
        return "paymentError";
    }
}
