package br.pucpr.prissma_server.projects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class ConstructionProjectService {

    private final ConstructionProjectRepository repository;

    public ConstructionProjectService(ConstructionProjectRepository repository) {
        this.repository = repository;
    }

    public ConstructionProject createProject(ConstructionProject project) {
        if (project.getTitle() == null || project.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
        }
        if (project.getAddress() == null || project.getAddress().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Address is required");
        }
        if (project.getProjectType() == null || project.getProjectType().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project type is required");
        }
        if (project.getCategory() == null || project.getCategory().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category is required");
        }
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
        return repository.save(project);
    }

    public List<ConstructionProject> getAll() {
        return repository.findAll();
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
        if (request.address() != null) project.setAddress(request.address());
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

