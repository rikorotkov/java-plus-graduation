package ru.practicum.service;

import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.request.RequestStatus;

import java.util.List;
import java.util.Map;

public interface ParticipationRequestService {
    List<ParticipationRequestDto> getRequestForEventByUserId(Long userId, Long eventId);

    List<ParticipationRequestDto> getRequestsByUser(Long userId);

    ParticipationRequestDto createRequest(Long userId, Long eventId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    Map<Long, Long> countRequestsByEventIdsAndStatus(List<Long> ids, RequestStatus status);

    boolean existsByRequesterAndEventAndStatus(Long userId, Long eventId, RequestStatus status);
}
