package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.client.EventClient;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.EventsFeedRequest;
import ru.practicum.service.EventService;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Validated
public class FeignEventController implements EventClient {

    private final EventService eventService;

    @Override
    @GetMapping("/pr/events/{eventId}")
    public EventFullDto getEventForParticipationService(@PathVariable Long eventId) {
        return eventService.getEventForRequest(eventId);
    }

    @Override
    @PostMapping("/feed/list")
    public List<EventShortDto> getEventsFeedList(@RequestBody EventsFeedRequest request) {
        return eventService.getEventsFeedCogList(
                request.getFollowedUsersIds(),
                request.getParam()
        );
    }

    @Override
    @PostMapping("/feed/map")
    public Map<Long, EventFullDto> getEventsFeedMap(@RequestBody EventsFeedRequest request) {
        return eventService.getEventsFeedCogMap(
                request.getFollowedUsersIds(),
                request.getParam()
        );
    }
}
