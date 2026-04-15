package com.fellowpick.pick;

import com.fellowpick.pick.dto.PickCountResponse;
import com.fellowpick.pick.dto.PickRequest;
import com.fellowpick.pick.dto.PickResponse;
import com.fellowpick.user.User;
import com.fellowpick.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Business logic for creating, querying, and removing card picks.
@Service
public class PickService {

    private final PickRepository pickRepository;
    private final UserRepository userRepository;

    public PickService(PickRepository pickRepository, UserRepository userRepository) {
        this.pickRepository = pickRepository;
        this.userRepository = userRepository;
    }

    // Returns aggregated vote counts for all cards in a precon.
    public List<PickCountResponse> getPickCounts(String preconId) {
        return pickRepository.countByPrecon(preconId).stream()
                .map(p -> new PickCountResponse(p.getCardId(), p.getPickType(), p.getCount()))
                .toList();
    }

    // Returns all picks a user has made in a specific precon.
    public List<PickResponse> getUserPicks(String userId, String preconId) {
        return pickRepository.findByUserIdAndPreconId(userId, preconId).stream()
                .map(this::toResponse)
                .toList();
    }

    // Submits a CUT or ADD vote for a card, rejecting duplicates.
    @Transactional
    public PickResponse makePick(String userId, PickRequest request) {
        if (pickRepository.existsByUserIdAndPreconIdAndCardIdAndPickType(
                userId, request.preconId(), request.cardId(), request.pickType())) {
            throw new IllegalStateException("You have already made this pick");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Pick pick = new Pick();
        pick.setUser(user);
        pick.setPreconId(request.preconId());
        pick.setCardId(request.cardId());
        pick.setPickType(request.pickType());

        return toResponse(pickRepository.save(pick));
    }

    // Deletes a pick, ensuring it belongs to the requesting user.
    @Transactional
    public void removePick(String pickId, String userId) {
        Pick pick = pickRepository.findByIdAndUserId(pickId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Pick not found"));
        pickRepository.delete(pick);
    }

    // Maps a Pick entity to its API response DTO.
    private PickResponse toResponse(Pick pick) {
        return new PickResponse(pick.getId(), pick.getPreconId(), pick.getCardId(), pick.getPickType());
    }
}
