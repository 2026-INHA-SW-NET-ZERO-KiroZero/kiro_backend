package com.kirozero.netzero.domain.dashboard.controller;

import com.kirozero.netzero.domain.dashboard.service.DashboardQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardQueryService queryService;

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("lastUpdatedDate", queryService.findLastSuccessDate());
        return "admin/dashboard";
    }
}
