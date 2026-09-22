package com.podcastrelease.controller;

import com.podcastrelease.model.PlatformAccount;
import com.podcastrelease.model.WebhookConfig;
import com.podcastrelease.repository.PlatformAccountRepository;
import com.podcastrelease.repository.WebhookConfigRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/platforms")
public class PlatformController {

    private final PlatformAccountRepository platformAccountRepository;
    private final WebhookConfigRepository webhookConfigRepository;

    public PlatformController(PlatformAccountRepository platformAccountRepository,
                              WebhookConfigRepository webhookConfigRepository) {
        this.platformAccountRepository = platformAccountRepository;
        this.webhookConfigRepository = webhookConfigRepository;
    }

    @GetMapping
    public String viewPlatforms(Model model) {
        model.addAttribute("platforms", platformAccountRepository.findAll());
        model.addAttribute("webhooks", webhookConfigRepository.findAll());
        model.addAttribute("newPlatform", new PlatformAccount());
        model.addAttribute("newWebhook", new WebhookConfig());
        return "platforms";
    }

    @PostMapping("/accounts/create")
    public String createPlatformAccount(@ModelAttribute PlatformAccount account, RedirectAttributes redirectAttributes) {
        if (account.getPlatformName() == null || account.getPlatformName().trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Platform name is required.");
            return "redirect:/platforms";
        }
        platformAccountRepository.save(account);
        redirectAttributes.addFlashAttribute("successMessage", "Platform account '" + account.getPlatformName() + "' connected successfully!");
        return "redirect:/platforms";
    }

    @PostMapping("/webhooks/create")
    public String createWebhook(@ModelAttribute WebhookConfig webhook, RedirectAttributes redirectAttributes) {
        if (webhook.getName() == null || webhook.getName().trim().isEmpty() || webhook.getWebhookUrl() == null || webhook.getWebhookUrl().trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Webhook name and URL are required.");
            return "redirect:/platforms";
        }
        webhookConfigRepository.save(webhook);
        redirectAttributes.addFlashAttribute("successMessage", "Webhook '" + webhook.getName() + "' created successfully!");
        return "redirect:/platforms";
    }
}
