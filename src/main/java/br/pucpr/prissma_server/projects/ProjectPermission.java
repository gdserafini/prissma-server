package br.pucpr.prissma_server.projects;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum ProjectPermission {
    VIEW_PROJECT,       
    MANAGE_MEMBERS,      
    MANAGE_BUDGET,       
    MANAGE_STAGES,      
    MANAGE_TEAMS,       
    MANAGE_TASKS,        
    MANAGE_ATTACHMENTS; 

    public static ProjectPermission fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Permission is required");
        }
        try {
            return ProjectPermission.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid permission: " + value);
        }
    }
}
