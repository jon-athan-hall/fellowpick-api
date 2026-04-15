package com.fellowpick.pick;

import com.fellowpick.pick.dto.PickCountResponse;
import com.fellowpick.pick.dto.PickRequest;
import com.fellowpick.pick.dto.PickResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/picks")
public class PickController {

    private final PickService pickService;

    public PickController(PickService pickService) {
        this.pickService = pickService;
    }

    @GetMapping("/{preconId}")
    public ResponseEntity<List<PickCountResponse>> getPickCounts(@PathVariable String preconId) {
        return ResponseEntity.ok(pickService.getPickCounts(preconId));
    }

    @GetMapping("/{preconId}/me")
    public ResponseEntity<List<PickResponse>> getMyPicks(@PathVariable String preconId,
                                                         Authentication authentication) {
        return ResponseEntity.ok(pickService.getUserPicks(authentication.getName(), preconId));
    }

    @PostMapping
    public ResponseEntity<PickResponse> makePick(@Valid @RequestBody PickRequest request,
                                                 Authentication authentication) {
        return ResponseEntity.ok(pickService.makePick(authentication.getName(), request));
    }

    @DeleteMapping("/{pickId}")
    public ResponseEntity<Void> removePick(@PathVariable String pickId,
                                           Authentication authentication) {
        pickService.removePick(pickId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
