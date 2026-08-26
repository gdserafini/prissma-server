package br.pucpr.prissma_server.stage;

import br.pucpr.prissma_server.projects.ConstructionProject;
import br.pucpr.prissma_server.projects.ConstructionProjectMemberRepository;
import br.pucpr.prissma_server.projects.ConstructionProjectRepository;
import br.pucpr.prissma_server.users.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

        validateAuthorization(userId, projectId);

        ConstructionProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));


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

        List<Stage> stages = stageRepository.findByConstructionProjectIdOrderByDisplayOrder(projectId);
        Map<Long, Stage> stagesById = stages.stream()
                .collect(Collectors.toMap(Stage::getId, stage -> stage));

        if (new HashSet<>(stageIds).size() != stageIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Reorder request contains duplicate stage ids");
        }

        for (Long stageId : stageIds) {
            if (!stagesById.containsKey(stageId)) {
                if (stageRepository.existsById(stageId)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Stage does not belong to this project");
                }
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Stage not found: " + stageId);
            }
        }

        if (stageIds.size() != stages.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Reorder request must contain all stages of the project");
        }

        // A constraint UNIQUE (construction_project_id, display_order) e validada a cada
        // UPDATE, entao gravar a ordem final direto estoura no meio do caminho, enquanto
        // duas etapas ainda dividem a mesma posicao. Primeiro move-se todas para uma faixa
        // temporaria acima do maior display_order atual (positiva, por causa do
        // CHECK display_order > 0) e so depois grava-se 1..N. Em nenhuma das duas fases um
        // valor de destino colide com um valor ainda nao regravado, entao a ordem em que o
        // Hibernate emite os UPDATEs nao importa.
        int tempOffset = stages.stream().mapToInt(Stage::getDisplayOrder).max().orElse(0) + 1;
        Instant now = Instant.now();

        for (int i = 0; i < stageIds.size(); i++) {
            Stage stage = stagesById.get(stageIds.get(i));
            stage.setDisplayOrder(tempOffset + i);
            stage.setUpdatedAt(now);
            stageRepository.save(stage);
        }
        stageRepository.flush();

        for (int i = 0; i < stageIds.size(); i++) {
            Stage stage = stagesById.get(stageIds.get(i));
            stage.setDisplayOrder(i + 1);
            stageRepository.save(stage);
        }
        stageRepository.flush();
    }
}
