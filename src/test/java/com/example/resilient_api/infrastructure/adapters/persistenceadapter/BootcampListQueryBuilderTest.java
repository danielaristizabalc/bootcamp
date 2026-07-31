package com.example.resilient_api.infrastructure.adapters.persistenceadapter;

import com.example.resilient_api.domain.model.BootcampListCriteria;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BootcampListQueryBuilderTest {

    private final BootcampListQueryBuilder bootcampListQueryBuilder = new BootcampListQueryBuilder();

    @Test
    void debeNormalizarLosParametrosDeListadoYCalcularElOffset() {
        // Given
        BootcampListCriteria criteria = new BootcampListCriteria(-1, 0, "capabilities", "desc");

        // When
        BootcampListQueryBuilder.BootcampListQuery query = bootcampListQueryBuilder.resolve(criteria);

        // Then
        assertThat(query.page()).isEqualTo(0);
        assertThat(query.size()).isEqualTo(10);
        assertThat(query.offset()).isEqualTo(0);
        assertThat(query.sortBy()).isEqualTo(BootcampListQueryBuilder.SORT_COLUMN_CAPABILITY_COUNT);
        assertThat(query.sortDirection()).isEqualTo("DESC");
    }

    @Test
    void debeConstruirLaConsultaPaginadaConOrdenPorDefectoCuandoElSortEsInvalido() {
        // Given
        BootcampListQueryBuilder.BootcampListQuery query = new BootcampListQueryBuilder.BootcampListQuery(
                1,
                10,
                10,
                BootcampListQueryBuilder.SORT_COLUMN_NAME,
                "ASC"
        );

        // When
        String sql = bootcampListQueryBuilder.buildQuery(query);

        // Then
        assertThat(sql).contains("FROM bootcamps b");
        assertThat(sql).contains("ORDER BY b.name ASC, b.id ASC");
        assertThat(sql).contains("LIMIT :limit OFFSET :offset");
    }

    @Test
    void debeCalcularCeroPaginasCuandoNoHayElementos() {
        // Given

        // When
        Integer totalPages = bootcampListQueryBuilder.totalPages(0L, 10);

        // Then
        assertThat(totalPages).isEqualTo(0);
    }
}