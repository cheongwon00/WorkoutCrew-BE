package com.example.workoutcrew.crew.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

final class PageRequestFactory {
    private PageRequestFactory() {
    }

    static Pageable create(int page, int size, String sort) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("페이지 범위가 올바르지 않습니다.");
        Sort.Direction direction;
        if ("id,desc".equals(sort)) direction = Sort.Direction.DESC;
        else if ("id,asc".equals(sort)) direction = Sort.Direction.ASC;
        else throw new IllegalArgumentException("허용하지 않는 정렬입니다.");
        return PageRequest.of(page, size, Sort.by(direction, "id"));
    }
}
