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

@Service
public class PickService {

    private final PickRepository pickRepository;
    private final UserRepository userRepository;

    public PickService(PickRepository pickRepository, UserRepository userRepository) {
        this.pickRepository = pickRepository;
        this.userRepository = userRepository;
    }

    public List<PickCountResponse> getPickCounts(String preconId) {
        return pickRepository.countByPrecon(preconId).stream()
                .map(p -> new PickCountResponse(p.getCardId(), p.getPickType(), p.getCount()))
                .toList();
    }

    public List<PickResponse> getUserPicks(String userId, String preconId) {
        return pickRepository.findByUserIdAndPreconId(userId, preconId).stream()
                .map(this::toResponse)
                .toList();
    }

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

    @Transactional
    public void removePick(String pickId, String userId) {
        Pick pick = pickRepository.findByIdAndUserId(pickId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Pick not found"));
        pickRepository.delete(pick);
    }

    private PickResponse toResponse(Pick pick) {
        return new PickResponse(pick.getId(), pick.getPreconId(), pick.getCardId(), pick.getPickType());
    }
}
