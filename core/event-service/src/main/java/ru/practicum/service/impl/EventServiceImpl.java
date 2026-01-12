package ru.practicum.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.AnalyzerClient;
import ru.practicum.aop.ClientErrorHandler;
import ru.practicum.client.RequestClient;
import ru.practicum.client.UserClient;
import ru.practicum.dto.event.*;
import ru.practicum.dto.request.RequestStatus;
import ru.practicum.entity.Category;
import ru.practicum.entity.Event;
import ru.practicum.entity.Location;
import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.UserPredictionsRequestProto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.CategoryMapper;
import ru.practicum.mapper.EventMapper;
import ru.practicum.parameters.EventAdminSearchParam;
import ru.practicum.parameters.EventUserSearchParam;
import ru.practicum.parameters.PublicSearchParam;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.service.EventService;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;
import static ru.practicum.specification.EventSpecifications.eventAdminSearchParamSpec;
import static ru.practicum.specification.EventSpecifications.eventPublicSearchParamSpec;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final RequestClient requestClient;
    private final EventMapper eventMapper;
    private final AnalyzerClient analyzerClient;
    private final UserClient userClient;
    private final CategoryRepository categoryRepository;

    @Override
    public List<EventShortDto> getUsersEvents(EventUserSearchParam params) {
        Page<Event> events = eventRepository.findByInitiator(params.getUserId(), params.getPageable());

        List<EventShortDto> result = events.stream()
                .map(eventMapper::toShortDto)
                .toList();
        enrichWithStatsEventShortDto(result);
        return result;

    }

    @Override
    @Transactional
    public EventFullDto saveEvent(NewEventDto dto, Long userId) {
        userClient.getUserShortDtoById(userId);

        Category category = categoryRepository.findById(dto.getCategory())
                .orElseThrow(() -> new NotFoundException("Category not found"));

        Event event = eventMapper.toEntity(dto, userId);
        event.setCategory(category);

        Event saved = eventRepository.saveAndFlush(event);

        EventFullDto fullDto = eventMapper.toFullDto(saved);
        fullDto.setRating(0.0);
        fullDto.setConfirmedRequests(0L);
        return fullDto;
    }

    @Override
    public List<EventShortDto> searchEvents(PublicSearchParam param) {

        Page<Event> events = eventRepository.findAll(eventPublicSearchParamSpec(param), param.getPageable());
        Map<Long, Event> eventsMap = events.stream().collect(toMap(Event::getId, Function.identity()));

        List<EventShortDto> eventShortDtos = events.stream()
                .map(eventMapper::toShortDto)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        enrichWithStatsEventShortDto(eventShortDtos);

        if (param.getOnlyAvailable()) {
            eventShortDtos = eventShortDtos.stream()
                    .filter(dto -> {
                        Event e = eventsMap.get(dto.getId());
                        if (e == null) return true;
                        int limit = e.getParticipantLimit() == null ? 0 : e.getParticipantLimit();
                        long confirmed = dto.getConfirmedRequests() == null ? 0L : dto.getConfirmedRequests();
                        return limit == 0 || confirmed < limit;
                    })
                    .collect(Collectors.toList());
        }
        if (param.getSort() == SortSearchParam.RATING) {
            eventShortDtos.sort(Comparator.comparingDouble(EventShortDto::getRating).reversed());
        }
        return eventShortDtos;
    }

    @Override
    public EventFullDto getPublishedEventById(Long id) {
        Event event = eventRepository.findByIdAndState(id, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Событие не найдено или не опубликовано"));
        EventFullDto dto = eventMapper.toFullDto(event);
        enrichWithStats(dto);
        return dto;
    }

    @Override
    public EventFullDto getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Событие id" + id + "не найдено"));
        EventFullDto dto = eventMapper.toFullDto(event);
        enrichWithStats(dto);
        return dto;
    }

    @Override
    public EventFullDto getEventByIdAndUserId(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));
        if (!Objects.equals(event.getInitiator(), userId)) {
            throw new ConflictException("Событие добавленно не теущем пользователем");
        }

        EventFullDto dto = eventMapper.toFullDto(event);
        enrichWithStats(dto);
        return dto;
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public EventFullDto updateEventByUser(Long eventId, Long userId, UpdateEventUserRequest event) {
        Event eventToUpdate = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено id=" + eventId));
        if (!Objects.equals(eventToUpdate.getInitiator(), userId) ||
                eventToUpdate.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Событие добавленно не теущем пользователем или уже было опубликовано");
        }
        updateNouNullFields(eventToUpdate, event);
        if (event.getStateAction() == UserEventAction.CANCEL_REVIEW) {
            eventToUpdate.setState(EventState.CANCELED);
        } else if (event.getStateAction() == UserEventAction.SEND_TO_REVIEW) {
            eventToUpdate.setState(EventState.PENDING);
        }

        Event updated = eventRepository.save(eventToUpdate);

        EventFullDto result = eventMapper.toFullDto(updated);
        enrichWithStats(result);
        return result;
    }

    @Override
    public List<EventFullDto> getEventsByParams(EventAdminSearchParam params) {
        Page<Event> searched = eventRepository.findAll(eventAdminSearchParamSpec(params), params.getPageable());

        List<EventFullDto> result = searched.stream()
                .limit(params.getSize())
                .map(eventMapper::toFullDto)
                .toList();
        enrichWithStatsEventFullDto(result);
        return result;
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateRequest) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event id=" + eventId + "not found"));
        if (event.getState() != EventState.PENDING && updateRequest.getStateAction() == AdminEventAction.PUBLISH_EVENT) {
            throw new ConflictException("Cannot publish the event because it's not in the right state: " + event.getState());
        }
        if (event.getState() == EventState.PUBLISHED && updateRequest.getStateAction() == AdminEventAction.REJECT_EVENT) {
            throw new ConflictException("Cannot reject the event because it's not in the right state: PUBLISHED");
        }
        if (updateRequest.getStateAction() == AdminEventAction.PUBLISH_EVENT
                && event.getEventDate().minusHours(1).isBefore(LocalDateTime.now())) {
            throw new ConflictException("To late to publish event");
        }
        updateNouNullFields(event, updateRequest);
        event.setState(updateRequest.getStateAction() == AdminEventAction.PUBLISH_EVENT ? EventState.PUBLISHED : EventState.CANCELED);
        if (event.getState() == EventState.PUBLISHED &&
                updateRequest.getStateAction() == AdminEventAction.PUBLISH_EVENT) {
            event.setPublishedOn(LocalDateTime.now());
        }
        Event updated = eventRepository.save(event);

        EventFullDto dto = eventMapper.toFullDto(updated);
        enrichWithStats(dto);

        return dto;
    }

    @Override
    public List<EventShortDto> getRecommendationsForUser(Long userId) {
        List<RecommendedEventProto> recommendationsForUser = analyzerClient.getRecommendationsForUser(UserPredictionsRequestProto.newBuilder()
                .setUserId(userId)
                .setMaxResult(10)
                .build());
        List<Long> ids = recommendationsForUser.stream().map(RecommendedEventProto::getEventId).toList();

        List<Event> events = eventRepository.findAllById(ids);
        List<EventShortDto> eventShortDtos = events.stream().map(eventMapper::toShortDto).collect(Collectors.toList());
        enrichWithStatsEventShortDto(eventShortDtos);
        eventShortDtos.sort(Comparator.comparingDouble(EventShortDto::getRating).reversed());
        return eventShortDtos;
    }

    @ClientErrorHandler
    private void enrichWithStats(EventFullDto dto) {
        Long eventId = dto.getId();

        Map<Long, Long> confirmedRequests = requestClient.countRequestsByEventIdsAndStatus(
                List.of(eventId),
                RequestStatus.CONFIRMED
        );
        if (confirmedRequests == null) {
            confirmedRequests = Map.of();
        }

        List<RecommendedEventProto> ratings = analyzerClient.getInteractionsCount(
                InteractionsCountRequestProto.newBuilder()
                        .addAllEventId(List.of(eventId))
                        .build()
        );

        double rating = 0.0;
        if (ratings != null && !ratings.isEmpty() && ratings.getFirst() != null) {
            rating = ratings.getFirst().getScore();
        }

        dto.setRating(rating);
        dto.setConfirmedRequests(confirmedRequests.getOrDefault(eventId, 0L));
    }

    @ClientErrorHandler
    private void enrichWithStatsEventFullDto(List<EventFullDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }
        List<Long> ids = dtos.stream().map(EventFullDto::getId).toList();

        Map<Long, Double> ratings = getRatings(ids);
        Map<Long, Long> confirmedRequests = requestClient.countRequestsByEventIdsAndStatus(ids, RequestStatus.CONFIRMED);
        if (confirmedRequests == null) {
            confirmedRequests = Map.of();
        }

        Map<Long, Long> finalConfirmedRequests = confirmedRequests;
        dtos.forEach(dto -> {
            dto.setConfirmedRequests(finalConfirmedRequests.getOrDefault(dto.getId(), 0L));
            dto.setRating(ratings.getOrDefault(dto.getId(), 0.0));
        });
    }

    @ClientErrorHandler
    private void enrichWithStatsEventShortDto(List<EventShortDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }
        List<Long> ids = dtos.stream().map(EventShortDto::getId).toList();

        Map<Long, Double> ratings = getRatings(ids);
        Map<Long, Long> confirmedRequests = requestClient.countRequestsByEventIdsAndStatus(ids, RequestStatus.CONFIRMED);
        if (confirmedRequests == null) {
            confirmedRequests = Map.of();
        }

        Map<Long, Long> finalConfirmedRequests = confirmedRequests;
        dtos.forEach(dto -> {
            dto.setConfirmedRequests(finalConfirmedRequests.getOrDefault(dto.getId(), 0L));
            dto.setRating(ratings.getOrDefault(dto.getId(), 0.0));
        });
    }

    private void updateNouNullFields(Event eventToUpdate, UpdateEventRequest event) {
        if (event.getAnnotation() != null) eventToUpdate.setAnnotation(event.getAnnotation());
        if (event.getCategory() != null) eventToUpdate.setCategory(Category.builder().id(event.getCategory()).build());
        if (event.getDescription() != null) eventToUpdate.setDescription(event.getDescription());
        if (event.getEventDate() != null) eventToUpdate.setEventDate(event.getEventDate());
        if (event.getLocation() != null) {
            LocationDto locDto = event.getLocation();
            Location loc = new Location();
            loc.setLat(locDto.getLat());
            loc.setLon(locDto.getLon());
            eventToUpdate.setLocation(loc);
        }
        if (event.getPaid() != null) eventToUpdate.setPaid(event.getPaid());
        if (event.getParticipantLimit() != null) eventToUpdate.setParticipantLimit(event.getParticipantLimit());
        if (event.getRequestModeration() != null) eventToUpdate.setRequestModeration(event.getRequestModeration());
        if (event.getTitle() != null) eventToUpdate.setTitle(event.getTitle());
    }

    private Map<Long, Double> getRatings(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }

        List<RecommendedEventProto> response = analyzerClient.getInteractionsCount(
                InteractionsCountRequestProto.newBuilder()
                        .addAllEventId(eventIds)
                        .build()
        );

        if (response == null || response.isEmpty()) {
            return Map.of();
        }

        return response.stream()
                .filter(Objects::nonNull)
                .collect(toMap(RecommendedEventProto::getEventId, RecommendedEventProto::getScore, (a, b) -> a));
    }
}
