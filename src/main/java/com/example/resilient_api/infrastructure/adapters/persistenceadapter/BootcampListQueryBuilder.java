package com.example.resilient_api.infrastructure.adapters.persistenceadapter;

import com.example.resilient_api.domain.model.BootcampListCriteria;

public final class BootcampListQueryBuilder {

    static final String SORT_COLUMN_NAME = "b.name";
    static final String SORT_COLUMN_CAPABILITY_COUNT = "capability_count";

    BootcampListQuery resolve(BootcampListCriteria criteria) {
        int page = normalizePage(criteria.page());
        int size = normalizeSize(criteria.size());
        String sortBy = normalizeSortBy(criteria.sortBy());
        String sortDirection = normalizeSortDirection(criteria.sortDirection());

        return new BootcampListQuery(page, size, page * size, sortBy, sortDirection);
    }

    String buildQuery(BootcampListQuery query) {
        return """
                SELECT b.id,
                       b.name,
                       b.description,
                       b.release_date,
                       b.duration,
                       COUNT(bc.capability_id) AS capability_count
                FROM bootcamps b
                LEFT JOIN bootcamp_capabilities bc ON bc.bootcamp_id = b.id
                GROUP BY b.id, b.name, b.description, b.release_date, b.duration
                ORDER BY %s %s, b.id ASC
                LIMIT :limit OFFSET :offset
                """.formatted(query.sortBy(), query.sortDirection());
    }

    Integer totalPages(Long totalElements, Integer size) {
        if (totalElements == null || totalElements == 0L) {
            return 0;
        }

        return (int) Math.ceil((double) totalElements / size);
    }

    private int normalizePage(Integer page) {
        return page == null || page < 0 ? 0 : page;
    }

    private int normalizeSize(Integer size) {
        return size == null || size <= 0 ? 10 : size;
    }

    private String normalizeSortBy(String sortBy) {
        if (sortBy == null) {
            return SORT_COLUMN_NAME;
        }

        return switch (sortBy.toLowerCase()) {
            case "name" -> SORT_COLUMN_NAME;
            case "capabilities", "capabilitycount", "capabilitiescount" -> SORT_COLUMN_CAPABILITY_COUNT;
            default -> SORT_COLUMN_NAME;
        };
    }

    private String normalizeSortDirection(String sortDirection) {
        return "desc".equalsIgnoreCase(sortDirection) ? "DESC" : "ASC";
    }

    record BootcampListQuery(
            int page,
            int size,
            int offset,
            String sortBy,
            String sortDirection
    ) {
    }
}