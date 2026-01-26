package ru.practicum.service;

import ru.practicum.dto.event.*;
import ru.practicum.parameters.EventAdminSearchParam;
import ru.practicum.parameters.EventUserSearchParam;
import ru.practicum.parameters.PublicSearchParam;

import java.util.List;

public interface EventService {
    List<EventShortDto> getUsersEvents(EventUserSearchParam params);

    EventFullDto saveEvent(NewEventDto dto, Long userId);

    List<EventShortDto> searchEvents(PublicSearchParam param);

    EventFullDto getPublishedEventById(Long id);

    EventFullDto getEventById(Long id);

    EventFullDto getEventByIdAndUserId(Long eventId, Long userId);

    EventFullDto updateEventByUser(Long eventId, Long userId, UpdateEventUserRequest event);

    List<EventFullDto> getEventsByParams(EventAdminSearchParam params);

    EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateRequest);

    List<EventShortDto> getRecommendationsForUser(Long userId);
}
