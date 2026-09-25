package com.podcastrelease.controller;

import com.podcastrelease.model.*;
import com.podcastrelease.repository.*;
import com.podcastrelease.service.EpisodeService;
import com.podcastrelease.service.OtpService;
import com.podcastrelease.service.TeamSecurityService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class WebController {

    private final EpisodeService episodeService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PodcastShowRepository podcastShowRepository;
    private final OtpService otpService;
    private final TeamRepository teamRepository;
    private final TeamMembershipRepository teamMembershipRepository;
    private final TeamInviteRepository teamInviteRepository;
    private final TeamSecurityService teamSecurityService;

    public WebController(EpisodeService episodeService,
                         UserRepository userRepository,
                         PasswordEncoder passwordEncoder,
                         PodcastShowRepository podcastShowRepository,
                         OtpService otpService,
                         TeamRepository teamRepository,
                         TeamMembershipRepository teamMembershipRepository,
                         TeamInviteRepository teamInviteRepository,
                         TeamSecurityService teamSecurityService) {
        this.episodeService = episodeService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.podcastShowRepository = podcastShowRepository;
        this.otpService = otpService;
        this.teamRepository = teamRepository;
        this.teamMembershipRepository = teamMembershipRepository;
        this.teamInviteRepository = teamInviteRepository;
        this.teamSecurityService = teamSecurityService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/signup")
    public String signupPage(Model model) {
        return "signup";
    }

    @PostMapping("/signup")
    public String registerUser(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            RedirectAttributes redirectAttributes) {

        if (username == null || username.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Username, email, and password are all required.");
            return "redirect:/signup";
        }

        if (userRepository.existsByUsername(username.trim())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Username '" + username.trim() + "' is already taken.");
            return "redirect:/signup";
        }

        if (userRepository.existsByEmail(email.trim())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Email '" + email.trim() + "' is already registered.");
            return "redirect:/signup";
        }

        User user = new User(username.trim(), email.trim(), passwordEncoder.encode(password));
        user.setEnabled(false); // Disabled until OTP is verified
        userRepository.save(user);

        String otpCode = otpService.generateAndSendOtp(email.trim());

        redirectAttributes.addFlashAttribute("email", email.trim());
        redirectAttributes.addFlashAttribute("latestOtp", otpCode);
        redirectAttributes.addFlashAttribute("successMessage", "Account created! A 6-digit OTP verification code has been dispatched to " + email.trim());
        return "redirect:/verify-otp?email=" + email.trim();
    }

    @GetMapping("/verify-otp")
    public String verifyOtpPage(@RequestParam(required = false) String email, Model model) {
        model.addAttribute("email", email);
        return "verify-otp";
    }

    @PostMapping("/verify-otp")
    public String processVerifyOtp(
            @RequestParam String email,
            @RequestParam String otpCode,
            RedirectAttributes redirectAttributes) {

        boolean verified = otpService.verifyOtp(email, otpCode);
        if (verified) {
            redirectAttributes.addFlashAttribute("successMessage", "Email verified successfully! Your account is now active. Please sign in below.");
            return "redirect:/login";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid or expired OTP verification code. Please check your code and try again.");
            redirectAttributes.addFlashAttribute("email", email);
            return "redirect:/verify-otp?email=" + email;
        }
    }

    @GetMapping({"/", "/episodes", "/dashboard"})
    public String dashboard(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) EpisodeStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model,
            Authentication authentication) {

        User currentUser = authentication != null ? userRepository.findByUsername(authentication.getName()).orElse(null) : null;
        List<Episode> episodes = episodeService.search(title, status, from, to, currentUser);
        model.addAttribute("episodes", episodes);
        model.addAttribute("summary", episodeService.getDashboardSummary(currentUser));
        model.addAttribute("titleFilter", title);
        model.addAttribute("statusFilter", status);
        model.addAttribute("fromDateFilter", from);
        model.addAttribute("toDateFilter", to);
        model.addAttribute("statuses", EpisodeStatus.values());
        model.addAttribute("shows", podcastShowRepository.findAll());
        model.addAttribute("currentUser", currentUser);

        return "episodes";
    }

    @GetMapping("/teams")
    public String teamsPage(
            @RequestParam(required = false) Long teamId,
            jakarta.servlet.http.HttpServletRequest request,
            Model model,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        User currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (currentUser == null) {
            return "redirect:/login";
        }

        jakarta.servlet.http.HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("inviteSuccessMsg") != null) {
            model.addAttribute("successMessage", session.getAttribute("inviteSuccessMsg"));
            session.removeAttribute("inviteSuccessMsg");
        }

        List<TeamMembership> memberships = teamMembershipRepository.findByUserId(currentUser.getId());
        List<Team> myTeams = memberships.stream().map(TeamMembership::getTeam).collect(Collectors.toList());

        Map<Long, TeamRole> myRoles = new HashMap<>();
        for (TeamMembership m : memberships) {
            myRoles.put(m.getTeam().getId(), m.getRole());
        }

        Team activeTeam = null;
        if (teamId != null) {
            activeTeam = myTeams.stream().filter(t -> t.getId().equals(teamId)).findFirst().orElse(null);
        }
        if (activeTeam == null && !myTeams.isEmpty()) {
            activeTeam = myTeams.get(0);
        }

        List<TeamMembership> members = List.of();
        List<TeamInvite> pendingInvites = List.of();

        if (activeTeam != null) {
            members = teamMembershipRepository.findByTeamId(activeTeam.getId());
            pendingInvites = teamInviteRepository.findByTeamId(activeTeam.getId())
                    .stream()
                    .filter(i -> !i.isAccepted() && i.getExpiresAt().isAfter(LocalDateTime.now()))
                    .collect(Collectors.toList());
        }

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("myTeams", myTeams);
        model.addAttribute("myRoles", myRoles);
        model.addAttribute("activeTeam", activeTeam);
        model.addAttribute("members", members);
        model.addAttribute("pendingInvites", pendingInvites);

        return "teams";
    }

    @PostMapping("/teams/create")
    public String createTeamWeb(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        User currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (currentUser == null) return "redirect:/login";

        if (name == null || name.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Team name is required.");
            return "redirect:/teams";
        }

        if (teamRepository.findByName(name.trim()).isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Team name '" + name.trim() + "' is already taken.");
            return "redirect:/teams";
        }

        Team team = teamRepository.save(new Team(name.trim(), description));
        teamMembershipRepository.save(new TeamMembership(team, currentUser, TeamRole.OWNER, false));

        redirectAttributes.addFlashAttribute("successMessage", "Team '" + team.getName() + "' created successfully!");
        return "redirect:/teams?teamId=" + team.getId();
    }

    @PostMapping("/teams/{id}/invite")
    public String inviteMemberWeb(
            @PathVariable Long id,
            @RequestParam String email,
            @RequestParam String role,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        User currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);
        Team team = teamSecurityService.verifyTeamMember(id, currentUser);
        teamSecurityService.verifyTeamOwner(id, currentUser);

        if (email == null || email.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Email is required.");
            return "redirect:/teams?teamId=" + id;
        }

        TeamRole assignedRole = TeamRole.CREATOR;
        try {
            TeamRole parsed = TeamRole.valueOf(role.trim().toUpperCase());
            if (parsed != TeamRole.OWNER) { // Strictly forbid OWNER role via invitation
                assignedRole = parsed;
            }
        } catch (Exception ignored) {}

        String token = UUID.randomUUID().toString();
        TeamInvite invite = new TeamInvite(team, email.trim(), assignedRole, token, currentUser);
        teamInviteRepository.save(invite);

        String acceptUrl = "/invites/" + token + "/accept";
        redirectAttributes.addFlashAttribute("createdInviteUrl", acceptUrl);
        redirectAttributes.addFlashAttribute("successMessage", "Invitation created for " + email.trim() + " as " + assignedRole + "!");
        return "redirect:/teams?teamId=" + id;
    }

    @GetMapping("/invites/{token}/accept")
    public String acceptInviteWeb(
            @PathVariable String token,
            jakarta.servlet.http.HttpServletRequest request,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        TeamInvite invite = teamInviteRepository.findByToken(token).orElse(null);
        if (invite == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid invitation token.");
            return "redirect:/login";
        }

        if (invite.isAccepted()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invitation has already been accepted.");
            return "redirect:/login";
        }

        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invitation token has expired.");
            return "redirect:/login";
        }

        jakarta.servlet.http.HttpSession session = request.getSession(true);
        session.setAttribute("pendingInviteToken", token);

        if (authentication != null && authentication.isAuthenticated()) {
            User currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);
            if (currentUser != null) {
                session.removeAttribute("pendingInviteToken");
                TeamMembership membership = teamMembershipRepository.findByTeamIdAndUserId(invite.getTeam().getId(), currentUser.getId())
                        .orElse(new TeamMembership(invite.getTeam(), currentUser, invite.getRole()));

                membership.setRole(invite.getRole());
                teamMembershipRepository.save(membership);

                invite.setAccepted(true);
                teamInviteRepository.save(invite);

                redirectAttributes.addFlashAttribute("successMessage", "Successfully joined team '" + invite.getTeam().getName() + "' as " + invite.getRole() + "!");
                return "redirect:/teams?teamId=" + invite.getTeam().getId();
            }
        }

        redirectAttributes.addFlashAttribute("successMessage", "Invitation saved! Please log in or create an account below to join team '" + invite.getTeam().getName() + "'.");
        return "redirect:/login";
    }

    @GetMapping("/episodes/new")
    public String newEpisodeForm(Model model) {
        model.addAttribute("episode", new Episode());
        model.addAttribute("shows", podcastShowRepository.findAll());
        return "create";
    }

    @PostMapping("/episodes/create")
    public String createEpisode(
            @ModelAttribute Episode episode,
            @RequestParam(value = "podcastShowId", required = false) Long podcastShowId,
            @RequestParam(value = "audioFile", required = false) org.springframework.web.multipart.MultipartFile audioFile,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            if (podcastShowId != null) {
                podcastShowRepository.findById(podcastShowId).ifPresent(episode::setPodcastShow);
            }
            if (audioFile != null && !audioFile.isEmpty()) {
                String uploadedUrl = episodeService.saveAudioFile(audioFile);
                episode.setAudioFileUrl(uploadedUrl);
            }
            String username = authentication != null ? authentication.getName() : null;
            Episode saved = episodeService.create(episode, username);
            redirectAttributes.addFlashAttribute("successMessage", "Episode '" + saved.getTitle() + "' created successfully!");
            return "redirect:/episodes/" + saved.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/episodes/new";
        }
    }

    @GetMapping("/episodes/{id}")
    public String episodeDetail(@PathVariable Long id, Model model, Authentication authentication) {
        User currentUser = authentication != null ? userRepository.findByUsername(authentication.getName()).orElse(null) : null;
        Episode episode = episodeService.findById(id, currentUser);
        model.addAttribute("episode", episode);
        model.addAttribute("auditLogs", episodeService.getAuditLogs(id));
        model.addAttribute("statuses", EpisodeStatus.values());
        model.addAttribute("currentUser", currentUser);

        return "detail";
    }

    @GetMapping("/episodes/{id}/edit")
    public String editEpisodeForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Episode episode = episodeService.findById(id);
        if (episode.getStatus() == EpisodeStatus.PUBLISHED || episode.getStatus() == EpisodeStatus.FAILED) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot edit episode in " + episode.getStatus() + " status.");
            return "redirect:/episodes/" + id;
        }
        model.addAttribute("episode", episode);
        model.addAttribute("shows", podcastShowRepository.findAll());
        return "edit";
    }

    @PostMapping("/episodes/{id}/edit")
    public String updateEpisode(
            @PathVariable Long id,
            @ModelAttribute Episode updatedEpisode,
            @RequestParam(value = "podcastShowId", required = false) Long podcastShowId,
            @RequestParam(value = "audioFile", required = false) org.springframework.web.multipart.MultipartFile audioFile,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            if (podcastShowId != null) {
                updatedEpisode.setPodcastShow(podcastShowRepository.findById(podcastShowId).orElse(null));
            }
            if (audioFile != null && !audioFile.isEmpty()) {
                String uploadedUrl = episodeService.saveAudioFile(audioFile);
                updatedEpisode.setAudioFileUrl(uploadedUrl);
            }
            String username = authentication != null ? authentication.getName() : null;
            episodeService.update(id, updatedEpisode, username);
            redirectAttributes.addFlashAttribute("successMessage", "Episode metadata updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/episodes/" + id;
    }

    @PostMapping("/episodes/{id}/status")
    public String transitionStatus(
            @PathVariable Long id,
            @RequestParam EpisodeStatus status,
            @RequestParam(value = "reviewNotes", required = false) String reviewNotes,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            String username = authentication != null ? authentication.getName() : null;
            episodeService.updateStatus(id, status, username, reviewNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Episode status updated to " + status);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/episodes/" + id;
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public String handleMaxSizeException(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "Uploaded audio file exceeds maximum allowed limit (100MB). Please choose a smaller file.");
        return "redirect:/episodes";
    }
}
