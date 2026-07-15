package com.fail.app.common.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "상태 확인", description = "애플리케이션과 데이터베이스 연결 상태 확인")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/health")
public class HealthController {

    private final DataSource dataSource;

    @Operation(summary = "서버 상태 확인")
    @GetMapping
    public ResponseEntity<HealthResponse> health() {
        boolean databaseUp = isDatabaseUp();
        HealthResponse response = new HealthResponse(
                databaseUp ? "UP" : "DOWN",
                "UP",
                databaseUp ? "UP" : "DOWN",
                LocalDateTime.now()
        );
        return ResponseEntity.status(databaseUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(response);
    }

    private boolean isDatabaseUp() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (SQLException exception) {
            return false;
        }
    }
}
