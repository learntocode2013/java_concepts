package com.github.learntocode2013.eventsourcing.order.controller;

public record OrderRequest(String customerId, Double amount) {
}
