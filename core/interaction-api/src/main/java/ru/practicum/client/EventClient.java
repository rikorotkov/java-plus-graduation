package ru.practicum.client;

import feign.FeignException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.EventsFeedRequest;

import java.util.List;
import java.util.Map;

@FeignClient(name = "event-service")
public interface EventClient {

    @GetMapping("/pr/events/{eventId}")
    EventFullDto getEventForParticipationService(@PathVariable Long eventId) throws FeignException;

    @PostMapping("/feed/list")
    List<EventShortDto> getEventsFeedList(@RequestBody EventsFeedRequest request) throws FeignException;

    @PostMapping("/feed/map")
    Map<Long, EventFullDto> getEventsFeedMap(@RequestBody EventsFeedRequest request) throws FeignException;
}
