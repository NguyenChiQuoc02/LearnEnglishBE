package com.personal.base.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageResponse<T> {
  private List<T> content;
  private int page;
  private int size;
  private long totalElements;
  private int totalPages;

  public static <S, T> PageResponse<T> of(Page<S> page, Function<S, T> mapper) {
    return new PageResponse<>(
            page.getContent().stream().map(mapper).collect(Collectors.toList()),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages());
  }
}
