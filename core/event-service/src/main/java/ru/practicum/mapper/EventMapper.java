package ru.practicum.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.aop.ClientErrorHandler;
import ru.practicum.client.UserClient;
import ru.practicum.dto.event.*;
import ru.practicum.entity.Event;
import ru.practicum.entity.Location;

@Component
@RequiredArgsConstructor
public class EventMapper {

    private final UserClient userClient;
    private final CategoryMapper categoryMapper;

    @ClientErrorHandler
    public EventShortDto toShortDto(Event event) {
        return EventShortDto.builder()
                .annotation(event.getAnnotation())
                .category(categoryMapper.toDto(event.getCategory()))
                .eventDate(event.getEventDate())
                .id(event.getId())
                .initiator(userClient.getUserShortDtoById(event.getInitiator()))
                .paid(event.getPaid())
                .title(event.getTitle())
                .confirmedRequests(0L)
                .rating(0.0)
                .build();
    }

    @ClientErrorHandler
    public EventFullDto toFullDto(Event event) {
        return EventFullDto.builder()
                .annotation(event.getAnnotation())
                .category(categoryMapper.toDto(event.getCategory()))
                .eventDate(event.getEventDate())
                .id(event.getId())
                .initiator(userClient.getUserShortDtoById(event.getInitiator()))
                .paid(event.getPaid())
                .title(event.getTitle())
                .createdOn(event.getCreatedOn())
                .description(event.getDescription())
                .location(toLocationDto(event))
                .participantLimit(event.getParticipantLimit())
                .publishedOn(event.getPublishedOn())
                .state(event.getState())
                .requestModeration(event.getRequestModeration())
                .build();
    }

    public Event toEntity(NewEventDto dto, Long userId) {
        Boolean paid = dto.getPaid() != null ? dto.getPaid() : false;
        Integer participantLimit = dto.getParticipantLimit() != null ? dto.getParticipantLimit() : 0;
        Boolean requestModeration = dto.getRequestModeration() != null ? dto.getRequestModeration() : true;

        return Event.builder()
                .annotation(dto.getAnnotation())
                .description(dto.getDescription())
                .eventDate(dto.getEventDate())
                .location(new Location(
                        dto.getLocation().getLat(),
                        dto.getLocation().getLon()
                ))
                .paid(paid)
                .participantLimit(participantLimit)
                .requestModeration(requestModeration)
                .title(dto.getTitle())
                .initiator(userId)
                .state(EventState.PENDING)
                .build();
    }

    private LocationDto toLocationDto(Event event) {
        return LocationDto.builder()
                .lat(event.getLocation().getLat())
                .lon(event.getLocation().getLon())
                .build();
    }
}
