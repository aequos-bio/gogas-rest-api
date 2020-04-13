package eu.aequos.gogas.dto;

import lombok.Data;

import java.util.List;

@Data
public class PageResultDTO<T> {
    private final List<T> items;
    private final boolean hasNext;
}
