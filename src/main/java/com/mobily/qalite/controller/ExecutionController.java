package com.mobily.qalite.controller;

import java.util.Map;

import com.mobily.qalite.execution.ExecutionService;
import com.mobily.qalite.execution.ExecutionService.ExecutionResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExecutionController {

    private final ExecutionService executionService;

    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostMapping("/execute")
    ResponseEntity<?> execute(@RequestBody ExecuteRequest request, Authentication authentication, HttpServletRequest servletRequest) {
        try {
            if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse("Please sign in before executing a command."));
}
            ExecutionResult result = executionService.execute(
                    authentication.getName(),
                    isAdmin(authentication),
                    request.environmentId(),
                    request.sqlId(),
                    servletRequest.getRemoteAddr(),
                    request.parameters()
            );
            return ResponseEntity.ok(result);
        } catch (AccessDeniedException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(exception.getMessage()));
        } catch (IllegalArgumentException exception) {
    return ResponseEntity.badRequest()
            .body(new ErrorResponse(exception.getMessage()));
} catch (IllegalStateException exception) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(
                    "The execution outcome could not be reliably reported. "
                            + "Do not retry before an administrator verifies "
                            + "the target database and execution history."
            ));
}
    }

    private static boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    record ExecuteRequest(long environmentId, long sqlId, Map<String, String> parameters) {
    }

    record ErrorResponse(String message) {
    }
}
