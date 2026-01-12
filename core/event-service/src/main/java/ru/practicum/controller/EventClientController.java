package ru.practicum.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.service.impl.EventServiceImpl;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("api/v1/event")
public class EventClientController {

    private final EventServiceImpl eventService;

    @GetMapping("/{eventId}")
    public EventFullDto getEventById(@PathVariable @Positive Long eventId) {
        log.info("Getting EventFullDto eventId={}", eventId);
        return eventService.getEventById(eventId);
    }
}
