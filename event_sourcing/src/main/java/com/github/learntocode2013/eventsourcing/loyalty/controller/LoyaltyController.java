package com.github.learntocode2013.eventsourcing.loyalty.controller;

import com.github.learntocode2013.eventsourcing.loyalty.service.LoyaltyReplayer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/loyalty")
public class LoyaltyController {

    @Autowired
    private LoyaltyReplayer loyaltyReplayer;

    @PostMapping("/rebuild")
    public ResponseEntity<String> rebuildLoyaltyState() {
        loyaltyReplayer.replayHistory();
        return ResponseEntity.ok("Loyalty state rebuild triggered from historical events.");
    }
}
