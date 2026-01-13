package ru.practicum.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.AnalyzerClient;
import ru.practicum.client.RequestClient;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationRequest;
import ru.practicum.dto.request.RequestStatus;
import ru.practicum.entity.Compilation;
import ru.practicum.entity.Event;
import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.CompilationMapper;
import ru.practicum.repository.CompilationRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.service.CompilationService;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final CompilationMapper compilationMapper;
    private final EventRepository eventRepository;
    private final RequestClient requestClient;
    private final AnalyzerClient analyzerClient;

    @Override
    public List<CompilationDto> getAllCompilations(Pageable pageable) {
        List<CompilationDto> dtos = compilationRepository.findAll(pageable).stream()
                .map(compilationMapper::toDto)
                .toList();
        dtos.forEach(this::enrichCompilationEventsWithStats);
        return dtos;
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        CompilationDto dto = compilationRepository.findById(compId)
                .map(compilationMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Подборка не найдена"));
        enrichCompilationEventsWithStats(dto);
        return dto;
    }

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto dto) {
        Compilation compilation = new Compilation();
        compilation.setTitle(dto.getTitle());
        compilation.setPinned(Boolean.TRUE.equals(dto.getPinned()));

        if (dto.getEvents() != null && !dto.getEvents().isEmpty()) {
            List<Event> events = eventRepository.findAllById(dto.getEvents().stream().map(Long::valueOf).toList());
            compilation.setEvents(new HashSet<>(events));
        }

        CompilationDto result = compilationMapper.toDto(compilationRepository.save(compilation));
        enrichCompilationEventsWithStats(result);
        return result;
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        compilationRepository.deleteById(compId);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest dto) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation not found"));

        if (dto.getTitle() != null) compilation.setTitle(dto.getTitle());
        if (dto.getPinned() != null) compilation.setPinned(dto.getPinned());
        if (dto.getEvents() != null) {
            List<Event> events = eventRepository.findAllById(dto.getEvents());
            compilation.setEvents(new HashSet<>(events));
        }

        CompilationDto result = compilationMapper.toDto(compilationRepository.save(compilation));
        enrichCompilationEventsWithStats(result);
        return result;
    }

    private void enrichCompilationEventsWithStats(CompilationDto dto) {
        if (dto == null || dto.getEvents() == null || dto.getEvents().isEmpty()) return;

        List<Long> ids = dto.getEvents().stream()
                .map(e -> e == null ? null : e.getId())
                .filter(Objects::nonNull)
                .toList();

        Map<Long, Long> confirmed = requestClient.countRequestsByEventIdsAndStatus(ids, RequestStatus.CONFIRMED);
        if (confirmed == null) confirmed = Map.of();

        Map<Long, Double> ratings = analyzerClient.getInteractionsCount(
                        InteractionsCountRequestProto.newBuilder().addAllEventId(ids).build()
                ).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(RecommendedEventProto::getEventId, RecommendedEventProto::getScore, (a, b) -> a));

        Map<Long, Long> finalConfirmed = confirmed;
        dto.getEvents().forEach(e -> {
            if (e == null || e.getId() == null) return;
            e.setConfirmedRequests(finalConfirmed.getOrDefault(e.getId(), 0L));
            e.setRating(ratings.getOrDefault(e.getId(), 0.0));
        });
    }
}
