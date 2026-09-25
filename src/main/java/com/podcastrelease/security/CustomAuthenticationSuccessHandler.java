package com.podcastrelease.security;

import com.podcastrelease.model.TeamInvite;
import com.podcastrelease.model.TeamMembership;
import com.podcastrelease.model.User;
import com.podcastrelease.repository.TeamInviteRepository;
import com.podcastrelease.repository.TeamMembershipRepository;
import com.podcastrelease.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final TeamInviteRepository teamInviteRepository;
    private final TeamMembershipRepository teamMembershipRepository;
    private final UserRepository userRepository;

    public CustomAuthenticationSuccessHandler(TeamInviteRepository teamInviteRepository,
                                               TeamMembershipRepository teamMembershipRepository,
                                               UserRepository userRepository) {
        this.teamInviteRepository = teamInviteRepository;
        this.teamMembershipRepository = teamMembershipRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String pendingToken = (String) session.getAttribute("pendingInviteToken");
            if (pendingToken != null) {
                session.removeAttribute("pendingInviteToken");
                User currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);
                if (currentUser != null) {
                    TeamInvite invite = teamInviteRepository.findByToken(pendingToken).orElse(null);
                    if (invite != null && !invite.isAccepted() && invite.getExpiresAt().isAfter(LocalDateTime.now())) {
                        TeamMembership membership = teamMembershipRepository.findByTeamIdAndUserId(invite.getTeam().getId(), currentUser.getId())
                                .orElse(new TeamMembership(invite.getTeam(), currentUser, invite.getRole()));
                        membership.setRole(invite.getRole());
                        teamMembershipRepository.save(membership);

                        invite.setAccepted(true);
                        teamInviteRepository.save(invite);

                        session.setAttribute("inviteSuccessMsg", "Welcome! You have automatically joined team '" + invite.getTeam().getName() + "' as " + invite.getRole() + "!");
                        getRedirectStrategy().sendRedirect(request, response, "/teams?teamId=" + invite.getTeam().getId());
                        return;
                    }
                }
            }
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
