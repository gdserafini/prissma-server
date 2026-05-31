package br.pucpr.prissma_server.projects;

import br.pucpr.prissma_server.users.User;
import br.pucpr.prissma_server.users.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

@Service
public class ConstructionProjectService {

    private final ConstructionProjectRepository repository;
    private final ConstructionProjectMemberRepository memberRepository;
    private final UserRepository userRepository;

    public ConstructionProjectService(ConstructionProjectRepository repository,
                                      ConstructionProjectMemberRepository memberRepository,
                                      UserRepository userRepository) {
        this.repository = repository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private void requireProjectManager(Long projectId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        ConstructionProjectMember member = memberRepository.findByConstructionProjectIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "User is not a member of this project"));

        if (!"ACTIVE".equals(member.getMembershipStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "User is not an active member of this project");
        }

        String roleInProject = member.getRoleInProject();
        if (!"OWNER".equals(roleInProject) && !"ENGINEER".equals(roleInProject)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only OWNER or ENGINEER can manage project members");
        }
    }

    private void requireProjectAccess(Long projectId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() == br.pucpr.prissma_server.users.Role.ADMIN) {
            return;
        }

        ConstructionProjectMember member = memberRepository.findByConstructionProjectIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "User is not a member of this project"));

        if (!"ACTIVE".equals(member.getMembershipStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "User is not an active member of this project");
        }
    }

    @Transactional
    public ConstructionProject createProject(ConstructionProject project, Long ownerUserId) {
        requireText(project.getTitle(), "Title is required");
        requireText(project.getStreet(), "Street is required");
        requireText(project.getProjectType(), "Project type is required");
        requireText(project.getCategory(), "Category is required");
        if (project.getLandArea() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Land area is required");
        }
        if (project.getBuiltArea() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Built area is required");
        }
        if (repository.existsByTitle(project.getTitle())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project with this title already exists");
        }
        Instant now = Instant.now();
        project.setCreatedAt(now);
        project.setUpdatedAt(now);
        ConstructionProject saved = repository.save(project);

        User owner = userRepository.findById(ownerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        ConstructionProjectMember member = new ConstructionProjectMember();
        member.setConstructionProject(saved);
        member.setUser(owner);
        member.setRoleInProject("OWNER");
        member.setMembershipStatus("ACTIVE");
        member.setJoinedAt(now);
        memberRepository.save(member);

        return saved;
    }

    @Transactional
    public ConstructionProjectMember addMember(Long projectId, Long actorUserId, AddProjectMemberRequest request) {
        if (request == null || request.userId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User id is required");
        }

        ConstructionProject project = repository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        requireProjectManager(projectId, actorUserId);

        User targetUser = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (request.roleInProject() == null || request.roleInProject().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role in project is required");
        }

        String roleInProject = request.roleInProject().trim().toUpperCase();
        if (!List.of("ENGINEER", "ARCHITECT", "FOREMAN").contains(roleInProject)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Role in project must be ENGINEER, ARCHITECT, or FOREMAN");
        }

        if (memberRepository.findByConstructionProjectIdAndUserId(projectId, targetUser.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "User is already a member of this project");
        }

        Instant now = Instant.now();
        ConstructionProjectMember member = new ConstructionProjectMember();
        member.setConstructionProject(project);
        member.setUser(targetUser);
        member.setRoleInProject(roleInProject);
        member.setMembershipStatus("ACTIVE");
        member.setJoinedAt(now);

        return memberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public List<ConstructionProjectMemberResponse> getMembers(Long projectId, Long userId) {
        repository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        requireProjectAccess(projectId, userId);

        return memberRepository.findAllByConstructionProjectIdOrderByJoinedAtAscIdAsc(projectId).stream()
                .map(ConstructionProjectMemberResponse::from)
                .toList();
    }

    @Transactional
    public void removeMember(Long projectId, Long actorUserId, Long targetUserId) {
        // ensure project exists
        repository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        // only OWNER or ENGINEER can remove members
        requireProjectManager(projectId, actorUserId);

        ConstructionProjectMember member = memberRepository.findByConstructionProjectIdAndUserId(projectId, targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project member not found"));

        if ("OWNER".equals(member.getRoleInProject())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot remove project owner");
        }

        memberRepository.delete(member);
    }

    public List<ConstructionProject> getAll() {
        return repository.findAll();
    }

    public List<ConstructionProject> getAllForUser(Long userId) {
        return memberRepository.findAllProjectsByUserId(userId);
    }

    public ConstructionProject getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    public ConstructionProject updateProject(Long id, ConstructionProjectRequest request) {
        ConstructionProject project = getById(id);

        if (request.title() != null && !request.title().isBlank() && !request.title().equals(project.getTitle())) {
            if (repository.existsByTitle(request.title())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project with this title already exists");
            }
            project.setTitle(request.title());
        }
        if (request.cep() != null) project.setCep(request.cep());
        if (request.street() != null) project.setStreet(request.street());
        if (request.city() != null) project.setCity(request.city());
        if (request.state() != null) project.setState(request.state());
        if (request.number() != null) project.setNumber(request.number());
        if (request.complement() != null) project.setComplement(request.complement());
        if (request.projectType() != null) project.setProjectType(request.projectType());
        if (request.category() != null) project.setCategory(request.category());
        if (request.landArea() != null) project.setLandArea(request.landArea());
        if (request.builtArea() != null) project.setBuiltArea(request.builtArea());
        if (request.status() != null) project.setStatus(request.status());
        if (request.plannedStartDate() != null) project.setPlannedStartDate(request.plannedStartDate());
        if (request.plannedEndDate() != null) project.setPlannedEndDate(request.plannedEndDate());

        project.setUpdatedAt(Instant.now());
        return repository.save(project);
    }

    public void deleteProject(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
        repository.deleteById(id);
    }
}

