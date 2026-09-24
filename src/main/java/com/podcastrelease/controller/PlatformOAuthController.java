package com.podcastrelease.controller;

import com.podcastrelease.model.PlatformAccount;
import com.podcastrelease.service.PlatformOAuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/platforms/oauth2")
public class PlatformOAuthController {

    private final PlatformOAuthService oAuthService;

    public PlatformOAuthController(PlatformOAuthService oAuthService) {
        this.oAuthService = oAuthService;
    }

    @GetMapping("/connect/{platform}")
    public String connectPlatform(@PathVariable String platform,
                                  HttpServletRequest request,
                                  RedirectAttributes redirectAttributes) {
        try {
            PlatformAccount.PlatformType platformType = PlatformAccount.PlatformType.valueOf(platform.toUpperCase());
            String baseUrl = getBaseUrl(request);

            // In local/demo mode, route directly to callback simulation if live OAuth credentials are not set
            String authUrl = oAuthService.buildAuthorizationUrl(platformType, baseUrl);
            if (authUrl.contains("demo_")) {
                // Direct demo redirect to callback
                return "redirect:/platforms/oauth2/callback/" + platformType.name().toLowerCase() + "?code=demo_auth_code_12345";
            }
            return "redirect:" + authUrl;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid platform target: " + platform);
            return "redirect:/platforms";
        }
    }

    @GetMapping("/callback/{platform}")
    public String oauthCallback(@PathVariable String platform,
                                @RequestParam(value = "code", required = false) String code,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        try {
            PlatformAccount.PlatformType platformType = PlatformAccount.PlatformType.valueOf(platform.toUpperCase());
            String baseUrl = getBaseUrl(request);
            PlatformAccount account = oAuthService.handleOAuthCallback(platformType, code != null ? code : "demo_code", baseUrl);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Successfully connected " + platformType.name() + " account (" + account.getConnectedAccountName() + ") via 1-Click OAuth 2.0!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "OAuth 2.0 connection failed: " + e.getMessage());
        }
        return "redirect:/platforms";
    }

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();

        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);
        if ((scheme.equals("http") && serverPort != 80) || (scheme.equals("https") && serverPort != 443)) {
            url.append(":").append(serverPort);
        }
        url.append(contextPath);
        return url.toString();
    }
}
