package br.pucpr.prissma_server.stage;

import br.pucpr.prissma_server.projects.ConstructionProject;
import br.pucpr.prissma_server.projects.ConstructionProjectMemberRepository;
import br.pucpr.prissma_server.projects.ConstructionProjectRepository;
import br.pucpr.prissma_server.users.User;
import br.pucpr.prissma_server.users.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StageService {

    private final StageRepository stageRepository;
    private final ConstructionProjectRepository projectRepository;
    private final ConstructionProjectMemberRepository memberRepository;
    private final UserRepository userRepository;

    public StageService(StageRepository stageRepository,
                       ConstructionProjectRepository projectRepository,
                       ConstructionProjectMemberRepository memberRepository,
                       UserRepository userRepository) {
        this.stageRepository = stageRepository;
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    private void validateDates(StageRequest request) {
        if (request.getPlannedStartDate() != null && request.getPlannedEndDate() != null) {
            if (request.getPlannedStartDate().isAfter(request.getPlannedEndDate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Planned start date cannot be after planned end date");
            }
        }
        if (request.getActualStartDate() != null && request.getActualEndDate() != null) {
            if (request.getActualStartDate().isAfter(request.getActualEndDate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Actual start date cannot be after actual end date");
            }
        }
    }

    private void validateAuthorization(Long userId, Long projectId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        var memberOpt = memberRepository.findAll().stream()
                .filter(m -> m.getConstructionProject().getId().equals(projectId) && m.getUser().getId().equals(userId))
                .findFirst();

        if (memberOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "User is not a member of this project");
        }

        String roleInProject = memberOpt.get().getRoleInProject();
        if (!roleInProject.equals("OWNER") && !roleInProject.equals("ENGINEER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only OWNER or ENGINEER can manage stages");
        }
    }

    @Transactional
    public StageResponse create(Long projectId, StageRequest request, Long userId) {
        validateDates(request);

        ConstructionProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        validateAuthorization(userId, projectId);

        if (stageRepository.findByConstructionProjectIdAndDisplayOrder(projectId, request.getDisplayOrder()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Stage with this display order already exists in this project");
        }

        Stage stage = StageMapper.toEntity(request, project);
        Instant now = Instant.now();
        stage.setCreatedAt(now);
        stage.setUpdatedAt(now);

        try {
            Stage saved = stageRepository.save(stage);
            return StageMapper.toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Stage with this display order already exists in this project");
        }
    }

    public List<StageResponse> listByProject(Long projectId) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        return stageRepository.findByConstructionProjectIdOrderByDisplayOrder(projectId)
                .stream()
                .map(StageMapper::toResponse)
                .collect(Collectors.toList());
    }

    public StageResponse get(Long id) {
        Stage stage = stageRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stage not found"));
        return StageMapper.toResponse(stage);
    }

    @Transactional
    public StageResponse update(Long id, StageRequest request, Long userId) {
        Stage stage = stageRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stage not found"));

        validateAuthorization(userId, stage.getConstructionProject().getId());
        validateDates(request);

        if (request.getDisplayOrder() != null && !request.getDisplayOrder().equals(stage.getDisplayOrder())) {
            if (stageRepository.findByConstructionProjectIdAndDisplayOrder(
                    stage.getConstructionProject().getId(), request.getDisplayOrder()).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Stage with this display order already exists in this project");
            }
        }

        StageMapper.updateEntity(request, stage);
        stage.setUpdatedAt(Instant.now());

        try {
            Stage updated = stageRepository.save(stage);
            return StageMapper.toResponse(updated);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Stage with this display order already exists in this project");
        }
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Stage stage = stageRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stage not found"));

        validateAuthorization(userId, stage.getConstructionProject().getId());

        stageRepository.deleteById(id);
    }

    @Transactional
    public void reorder(Long projectId, List<Long> stageIds, Long userId) {
        validateAuthorization(userId, projectId);

        projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        for (int i = 0; i < stageIds.size(); i++) {
            Long stageId = stageIds.get(i);
            Stage stage = stageRepository.findById(stageId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Stage not found: " + stageId));

            if (!stage.getConstructionProject().getId().equals(projectId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Stage does not belong to this project");
            }

            stage.setDisplayOrder(i + 1);
            stage.setUpdatedAt(Instant.now());
            stageRepository.save(stage);
        }
    }
}





