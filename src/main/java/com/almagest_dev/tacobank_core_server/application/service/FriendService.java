package com.almagest_dev.tacobank_core_server.application.service;


import com.almagest_dev.tacobank_core_server.domain.friend.model.Friend;
import com.almagest_dev.tacobank_core_server.domain.friend.repository.FriendRepository;
import com.almagest_dev.tacobank_core_server.presentation.dto.FriendRequestDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.FriendResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FriendService {

    private final FriendRepository friendRepository;

    public FriendService(FriendRepository friendRepository) {
        this.friendRepository = friendRepository;
    }

    private void checkActionPermission(Friend friend, String action, String currentUserId) {
        // 현재 사용자가 요청자(Requester) 또는 수신자(Receiver)인지 확인
        boolean isRequester = currentUserId.equals(friend.getRequesterId());
        boolean isReceiver = currentUserId.equals(friend.getReceiverId());

        // 디버깅을 위한 출력 코드
        System.out.println("=== Debugging checkActionPermission ===");
        System.out.println("currentUserId: " + currentUserId);
        System.out.println("friend.getRequesterId(): " + friend.getRequesterId());
        System.out.println("friend.getReceiverId(): " + friend.getReceiverId());
        System.out.println("isRequester: " + isRequester);
        System.out.println("isReceiver: " + isReceiver);
        System.out.println("friend.getStatus(): " + friend.getStatus());
        System.out.println("action: " + action);
        System.out.println("======================================");


        switch (friend.getStatus()) {
            case "REQ":
                // REQ 상태에서는 추가 동작을 수행하지 않도록 제한
                throw new IllegalStateException("요청 중인 상태에서는 추가 동작을 수행할 수 없습니다.");

            case "REQ_RECEIVED":
                // REQ_RECEIVED 상태에서는 요청을 수락하거나 거절할 수 있도록 설정
                if (!action.equals("accept") && !action.equals("reject")) {
                    throw new IllegalStateException("요청 수신 상태에서는 수락과 거절만 가능합니다.");
                }
                break;

            case "ACC":
                // 수락 상태에서는 요청과 거절이 불가능하고, 차단, 차단 해제, 좋아요, 좋아요 취소, 삭제만 가능
                if (action.equals("request") || action.equals("reject") || action.equals("accept")) {
                    throw new IllegalStateException("수락 상태에서는 요청과 거절이 불가능합니다.");
                }
                break;

            case "REJ":
                // 거절 상태에서는 재요청만 가능
                if (!action.equals("request")) {
                    throw new IllegalStateException("거절 상태에서는 재요청만 가능합니다.");
                }
                break;

            case "BLOCKED":
                // 요청자가 unblock 작업을 수행할 수 있도록 설정
                if (!isRequester || !action.equals("unblock")) {
                    throw new IllegalStateException("차단 상태에서는 차단 해제만 가능합니다.");
                }
                break;

            case "BLOCKED_BY":
                // 차단 당한 사용자는 차단 해제를 포함한 모든 작업을 할 수 없음
                if (isReceiver && (action.equals("unblock") || action.equals("request") || action.equals("accept") ||
                        action.equals("reject") || action.equals("delete") || action.equals("like") || action.equals("unlike"))) {
                    throw new IllegalStateException("해당 사용자에게 차단 당했습니다. 추가 동작이 불가 합니다.");
                }
                break;

            case "NONE":
                // 차단 해제 상태에서는 재요청만 가능
                if (!action.equals("request")) {
                    throw new IllegalStateException("친구 해제 상태에서는 재요청만 가능합니다.");
                }
                break;

            case "DEL":
                // 삭제 상태에서는 재요청만 가능
                if (!action.equals("request")) {
                    throw new IllegalStateException("삭제 상태에서는 재요청만 가능합니다.");
                }
                break;

            default:
                throw new IllegalArgumentException("알 수 없는 상태입니다."+  friend.getStatus());
        }
    }

    @Transactional
    public void requestFriend(FriendRequestDto requestDto) {

        // 요청자와 수신자가 동일한 경우 예외 처리
        if (requestDto.getRequesterId().equals(requestDto.getReceiverId())) {
            throw new IllegalStateException("자신에게 친구 요청을 보낼 수 없습니다.");
        }

        Friend friend = friendRepository.findByRequesterIdAndReceiverId(
                requestDto.getRequesterId(), requestDto.getReceiverId()
        ).orElseGet(() -> new Friend(requestDto.getRequesterId(), requestDto.getReceiverId()));

        // 요청 중복 방지 예외 처리
        // String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        String currentUserId = "1"; // 임시로 현재 사용자 ID 설정
        checkActionPermission(friend, "request", currentUserId);

        // 기존 요청 상태 확인
        Friend reverseFriend = friendRepository.findByRequesterIdAndReceiverId(
                requestDto.getReceiverId(), requestDto.getRequesterId()
        ).orElse(null);

        friend.setStatus("REQ");
        friendRepository.save(friend);

        // 반대 상태 저장 - 상대방이 요청받은 상태로 설정
        if (reverseFriend == null) {
            reverseFriend = new Friend(requestDto.getReceiverId(), requestDto.getRequesterId());
        }
        reverseFriend.setStatus("REQ_RECEIVED");
        friendRepository.save(reverseFriend);
    }

    @Transactional
    public void acceptFriend(FriendRequestDto requestDto) {
        Friend friend = friendRepository.findByRequesterIdAndReceiverId(
                requestDto.getRequesterId(), requestDto.getReceiverId()
        ).orElseThrow(() -> new IllegalArgumentException("친구 요청이 없습니다."));

        String currentUserId = "1";
        checkActionPermission(friend, "accept", currentUserId);

        friend.setStatus("ACC");
        friendRepository.save(friend);

        // 반대 관계 상태를 수락 상태로 설정
        Friend reverseFriend = friendRepository.findByRequesterIdAndReceiverId(
                requestDto.getReceiverId(), requestDto.getRequesterId()
        ).orElseGet(() -> new Friend(requestDto.getReceiverId(), requestDto.getRequesterId()));
        reverseFriend.setStatus("ACC");
        friendRepository.save(reverseFriend);
    }

    @Transactional
    public void rejectFriend(FriendRequestDto requestDto) {
        Friend friend = friendRepository.findByRequesterIdAndReceiverId(
                requestDto.getRequesterId(), requestDto.getReceiverId()
        ).orElseThrow(() -> new IllegalArgumentException("친구 요청이 없습니다."));

        String currentUserId = "1";
        checkActionPermission(friend, "reject", currentUserId);

        friend.setStatus("REJ");
        friendRepository.save(friend);

        // 반대 관계 상태를 NONE으로 설정하여 요청을 보낼 수 있게 초기화
        Friend reverseFriend = friendRepository.findByRequesterIdAndReceiverId(
                requestDto.getReceiverId(), requestDto.getRequesterId()
        ).orElseGet(() -> new Friend(requestDto.getReceiverId(), requestDto.getRequesterId()));
        reverseFriend.setStatus("NONE");
        friendRepository.save(reverseFriend);
    }

    @Transactional
    public void deleteFriend(FriendRequestDto requestDto) {
        Friend friend = friendRepository.findByRequesterIdAndReceiverId(
                requestDto.getRequesterId(), requestDto.getReceiverId()
        ).orElseThrow(() -> new IllegalArgumentException("친구 관계가 존재하지 않습니다."));

        String currentUserId = "1";
        checkActionPermission(friend, "delete", currentUserId);

        friend.setStatus("DEL");
        friend.setLiked("N");
        friendRepository.save(friend);

        // 반대 관계 상태를 NONE으로 설정
        Friend reverseFriend = friendRepository.findByRequesterIdAndReceiverId(
                requestDto.getReceiverId(), requestDto.getRequesterId()
        ).orElseGet(() -> new Friend(requestDto.getReceiverId(), requestDto.getRequesterId()));
        reverseFriend.setStatus("NONE");
        friendRepository.save(reverseFriend);
    }

    @Transactional
    public void blockFriend(FriendRequestDto requestDto) {
        String currentUserId = "1";

        Friend friend = friendRepository.findByRequesterIdAndReceiverId(
                requestDto.getRequesterId(), requestDto.getReceiverId()
        ).orElseGet(() -> new Friend(requestDto.getRequesterId(), requestDto.getReceiverId()));
        checkActionPermission(friend, "block", currentUserId);
        friend.setStatus("BLOCKED");
        friend.setLiked("N");
        friendRepository.save(friend);

        Friend reverseFriend = friendRepository.findByRequesterIdAndReceiverId(
                requestDto.getReceiverId(), requestDto.getRequesterId()
        ).orElseGet(() -> new Friend(requestDto.getReceiverId(), requestDto.getRequesterId()));
        reverseFriend.setStatus("BLOCKED_BY");
        reverseFriend.setLiked("N");
        friendRepository.save(reverseFriend);
    }

    @Transactional
    public void unblockFriend(FriendRequestDto requestDto) {
        Friend friend = friendRepository.findByRequesterIdAndReceiverId(
                requestDto.getRequesterId(), requestDto.getReceiverId()
        ).orElseThrow(() -> new IllegalArgumentException("차단된 관계가 없습니다."));

        String currentUserId = "1";
        checkActionPermission(friend, "unblock", currentUserId);

        if ("BAN".equals(friend.getStatus()) && currentUserId.equals(friend.getRequesterId())) {
            friend.setStatus("NONE");
            friendRepository.save(friend);

            Friend reverseFriend = friendRepository.findByRequesterIdAndReceiverId(
                    requestDto.getReceiverId(), requestDto.getRequesterId()
            ).orElseThrow(() -> new IllegalArgumentException("차단된 관계가 없습니다."));
            reverseFriend.setStatus("NONE");
            friendRepository.save(reverseFriend);
        } else {
            throw new IllegalStateException("차단한 사용자만 차단 해제를 할 수 있습니다.");
        }
    }

    @Transactional
    public void likeFriend(FriendRequestDto requestDto) {
        Friend friend = friendRepository.findByRequesterIdAndReceiverId(
                requestDto.getRequesterId(), requestDto.getReceiverId()
        ).orElseThrow(() -> new IllegalArgumentException("친구 관계가 존재하지 않습니다."));

        if ("Y".equals(friend.getLiked())) {
            throw new IllegalStateException("이미 좋아요를 누른 상태입니다.");
        }

        // 좋아요 가능한 상태인지 확인
        // String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        String currentUserId = "1";
        checkActionPermission(friend, "like",currentUserId);

        friend.setLiked("Y"); // 좋아요 상태 설정
        friendRepository.save(friend);
    }

    @Transactional
    public void unlikeFriend(FriendRequestDto requestDto) {
        Friend friend = friendRepository.findByRequesterIdAndReceiverId(
                requestDto.getRequesterId(), requestDto.getReceiverId()
        ).orElseThrow(() -> new IllegalArgumentException("친구 관계가 존재하지 않습니다."));

        if ("N".equals(friend.getLiked())) {
            throw new IllegalStateException("이미 좋아요 취소 상태입니다.");
        }

        // 좋아요 취소 가능한 상태인지 확인
        // String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        String currentUserId = "1";
        checkActionPermission(friend, "unlike",currentUserId);

        friend.setLiked("N"); // 좋아요 취소 상태 설정
        friendRepository.save(friend);
    }

    public List<FriendResponseDto> getFriendList(String userId) {
        // 친구 목록 조회 - 상태가 ACC인 친구만 가져옴
        List<Friend> friends = friendRepository.findByRequesterIdAndStatusOrReceiverIdAndStatus(userId, "ACC", userId, "ACC");

        // Friend 엔티티를 FriendResponseDto로 변환하여 반환
        return friends.stream()
                .map(friend -> new FriendResponseDto(friend))
                .collect(Collectors.toList());
    }

}