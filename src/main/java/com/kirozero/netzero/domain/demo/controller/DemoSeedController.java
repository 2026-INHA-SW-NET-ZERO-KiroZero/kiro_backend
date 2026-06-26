package com.kirozero.netzero.domain.demo.controller;

import com.kirozero.netzero.domain.demo.dto.DemoSeedResponse;
import com.kirozero.netzero.domain.demo.service.DemoSeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/demo")
@RequiredArgsConstructor
public class DemoSeedController {

    private final DemoSeedService demoSeedService;

    @PostMapping("/seed")
    public DemoSeedResponse seed() {
        return demoSeedService.seed();
    }
}
