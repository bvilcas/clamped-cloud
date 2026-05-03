package io.clamped.cloud.message;

import io.clamped.cloud.project.Project;
import io.clamped.cloud.project.ProjectRepository;
import io.clamped.cloud.user.User;
import io.clamped.cloud.user.UserPrincipal;
import io.clamped.cloud.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    public MessageService(MessageRepository messageRepository, UserRepository userRepository, ProjectRepository projectRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public MessageDto sendMessage(SendMessageRequest request, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User sender = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));

        Message message = Message.builder()
                .content(request.content().trim())
                .sender(sender)
                .project(project)
                .sentAt(Instant.now())
                .build();

        return toDto(messageRepository.save(message));
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getProjectMessages(Long projectId) {
        return messageRepository.findTop100ByProjectIdOrderBySentAtAsc(projectId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private MessageDto toDto(Message m) {
        return new MessageDto(
                m.getId(),
                m.getContent(),
                m.getSender().getId(),
                m.getSender().getFirstname(),
                m.getSender().getLastname(),
                m.getProject().getId(),
                m.getSentAt()
        );
    }
}
