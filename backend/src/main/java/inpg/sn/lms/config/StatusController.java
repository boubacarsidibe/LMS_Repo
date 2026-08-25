package inpg.sn.lms.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api")
public class StatusController {

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(java.util.Map.of(
            "status", "UP",
            "timestamp", Instant.now().toString()
        ));
    }
}
