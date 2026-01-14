package ru.practicum.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.entity.Compilation;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class CompilationMapper {

    private final EventMapper eventMapper;

    public CompilationDto toDto(Compilation compilation) {
        return CompilationDto.builder()
                .id(compilation.getId())
                .events(compilation.getEvents().stream()
                        .map(eventMapper::toShortDto)
                        .toList())
                .pinned(compilation.getPinned())
                .title(compilation.getTitle())
                .build();
    }

    public CompilationDto toDto(Compilation compilation, Map<Long, UserShortDto> initiators) {
        return CompilationDto.builder()
                .id(compilation.getId())
                .events(compilation.getEvents().stream()
                        .map(e -> eventMapper.toShortDto(e, initiators.get(e.getInitiator())))
                        .toList())
                .pinned(compilation.getPinned())
                .title(compilation.getTitle())
                .build();
    }

}
