package br.pucpr.prissma_server.schedule;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/projects/{projectId}/schedule")
public class ScheduleController {

    private final ScheduleService service;

    public ScheduleController(ScheduleService service) {
        this.service = service;
    }

    private Long resolveUserId(Authentication auth) {
        Object principal = auth.getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }
        if (principal instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(auth.getName());
    }

    // Datas, view e corpos são validados no service (sem @Valid): erros de
    // conversão/validação do Spring cairiam no handler genérico e virariam 500.
    @GetMapping
    public ResponseEntity<TeamScheduleResponse> get(@PathVariable Long projectId,
                                                    @RequestParam(required = false) String view,
                                                    @RequestParam(required = false) String date,
                                                    Authentication auth) {
        return ResponseEntity.ok(service.getSchedule(projectId, view, date, resolveUserId(auth)));
    }

    @PutMapping("/members/{memberUserId}/responsibility")
    public ResponseEntity<ScheduleMemberResponse> updateResponsibility(@PathVariable Long projectId,
                                                                       @PathVariable Long memberUserId,
                                                                       @RequestBody ScheduleResponsibilityRequest request,
                                                                       Authentication auth) {
        return ResponseEntity.ok(service.updateResponsibility(projectId, memberUserId, request, resolveUserId(auth)));
    }

    @PutMapping("/members/{memberUserId}/allocations/{date}")
    public ResponseEntity<ScheduleAllocationResponse> upsertAllocation(@PathVariable Long projectId,
                                                                       @PathVariable Long memberUserId,
                                                                       @PathVariable String date,
                                                                       @RequestBody ScheduleAllocationRequest request,
                                                                       Authentication auth) {
        return ResponseEntity.ok(service.upsertAllocation(projectId, memberUserId, date, request, resolveUserId(auth)));
    }

    @DeleteMapping("/members/{memberUserId}/allocations/{date}")
    public ResponseEntity<Void> deleteAllocation(@PathVariable Long projectId,
                                                 @PathVariable Long memberUserId,
                                                 @PathVariable String date,
                                                 Authentication auth) {
        service.deleteAllocation(projectId, memberUserId, date, resolveUserId(auth));
        return ResponseEntity.noContent().build();
    }
}
