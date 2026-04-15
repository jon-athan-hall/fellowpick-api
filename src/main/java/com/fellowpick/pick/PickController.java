package com.fellowpick.pick;

import com.fellowpick.pick.dto.PickCountResponse;
import com.fellowpick.pick.dto.PickRequest;
import com.fellowpick.pick.dto.PickResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// REST endpoints for submitting, retrieving, and removing card picks.
@RestController
@RequestMapping("/api/picks")
public class PickController {

    private final PickService pickService;

    public PickController(PickService pickService) {
        this.pickService = pickService;
    }

    // Returns aggregated CUT/ADD vote counts for every card in a precon.
    @GetMapping("/{preconId}")
    public ResponseEntity<List<PickCountResponse>> getPickCounts(@PathVariable String preconId) {
        return ResponseEntity.ok(pickService.getPickCounts(preconId));
    }

    // Returns the authenticated user's own picks for a given precon.
    @GetMapping("/{preconId}/me")
    public ResponseEntity<List<PickResponse>> getMyPicks(@PathVariable String preconId,
                                                         Authentication authentication) {
        return ResponseEntity.ok(pickService.getUserPicks(authentication.getName(), preconId));
    }

    // Submits a new CUT or ADD pick for a card in a precon.
    @PostMapping
    public ResponseEntity<PickResponse> makePick(@Valid @RequestBody PickRequest request,
                                                 Authentication authentication) {
        return ResponseEntity.ok(pickService.makePick(authentication.getName(), request));
    }

    // Deletes a pick owned by the authenticated user.
    @DeleteMapping("/{pickId}")
    public ResponseEntity<Void> removePick(@PathVariable String pickId,
                                           Authentication authentication) {
        pickService.removePick(pickId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
