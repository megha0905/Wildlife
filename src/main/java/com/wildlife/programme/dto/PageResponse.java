package com.wildlife.programme.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Pagination wrapper (CONTRACTS.md 3.2). Never return Spring's raw Page JSON. */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
