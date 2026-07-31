package com.example.resilient_api.infrastructure.adapters.persistenceadapter;

import com.example.resilient_api.domain.model.BootcampListCriteria;
import com.example.resilient_api.domain.model.BootcampPageResult;
import com.example.resilient_api.domain.model.BootcampSummary;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.entity.BootcampCapabilityEntity;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.repository.BootcampCapabilityRepository;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.RowsFetchSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.Month;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BootcampListQueryAdapterTest {

    private static final BootcampListCriteria DEFAULT_CRITERIA = new BootcampListCriteria(0, 10, "name", "asc");
    private static final String BOOTCAMP_NAME = "Bootcamp Java";
    private static final String BOOTCAMP_DESCRIPTION = "Backend con Spring";
    private static final String TOTAL_ELEMENTS_SQL = "SELECT COUNT(*) AS total_elements FROM bootcamps";

    @Mock
    private DatabaseClient databaseClient;

    @Mock
    private BootcampCapabilityRepository bootcampCapabilityRepository;

    private final BootcampListQueryBuilder bootcampListQueryBuilder = new BootcampListQueryBuilder();

    private BootcampListQueryAdapter bootcampListQueryAdapter;

    @BeforeEach
    void setUp() {
        bootcampListQueryAdapter = new BootcampListQueryAdapter(databaseClient, bootcampCapabilityRepository, bootcampListQueryBuilder);
    }

    @Test
    void debeListarBootcampsPaginadosYEnriquecerConCapacidades() {
        // Given
        BootcampListQueryBuilder.BootcampListQuery query = bootcampListQueryBuilder.resolve(DEFAULT_CRITERIA);
        String sql = bootcampListQueryBuilder.buildQuery(query);
        DatabaseClient.GenericExecuteSpec listSpec = mock(DatabaseClient.GenericExecuteSpec.class, RETURNS_SELF);
        DatabaseClient.GenericExecuteSpec countSpec = mock(DatabaseClient.GenericExecuteSpec.class, RETURNS_SELF);
        RowsFetchSpec<Object> listFetchSpec = mock(RowsFetchSpec.class);
        RowsFetchSpec<Long> countFetchSpec = mock(RowsFetchSpec.class);
        RowMetadata metadata = mock(RowMetadata.class);
        Row firstRow = bootcampRow(1L, BOOTCAMP_NAME, BOOTCAMP_DESCRIPTION, LocalDate.of(2026, Month.JULY, 31), 120, 2L);
        Row secondRow = bootcampRow(2L, "Bootcamp Cloud", "Arquitectura y despliegue", LocalDate.of(2026, Month.AUGUST, 1), 90, 1L);

        when(databaseClient.sql(sql)).thenReturn(listSpec);
        when(databaseClient.sql(TOTAL_ELEMENTS_SQL)).thenReturn(countSpec);
        when(listSpec.map(org.mockito.ArgumentMatchers.<BiFunction<Row, RowMetadata, Object>>any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            BiFunction<Row, RowMetadata, Object> mapper = invocation.getArgument(0);
            Object mappedFirstRow = mapper.apply(firstRow, metadata);
            Object mappedSecondRow = mapper.apply(secondRow, metadata);
            when(listFetchSpec.all()).thenReturn(Flux.just(mappedFirstRow, mappedSecondRow));
            return listFetchSpec;
        });
        when(countSpec.map(org.mockito.ArgumentMatchers.<BiFunction<Row, RowMetadata, Long>>any())).thenAnswer(invocation -> {
            when(countFetchSpec.one()).thenReturn(Mono.just(2L));
            return countFetchSpec;
        });
        when(bootcampCapabilityRepository.findByBootcampId(1L)).thenReturn(Flux.just(capability(1L), capability(2L)));
        when(bootcampCapabilityRepository.findByBootcampId(2L)).thenReturn(Flux.just(capability(2L)));

        // When
        Mono<BootcampPageResult> responseMono = bootcampListQueryAdapter.listBootcamps(DEFAULT_CRITERIA);

        // Then
        StepVerifier.create(responseMono)
                .assertNext(result -> {
                    assertThat(result.page()).isEqualTo(0);
                    assertThat(result.size()).isEqualTo(10);
                    assertThat(result.totalElements()).isEqualTo(2L);
                    assertThat(result.totalPages()).isEqualTo(1);
                    assertThat(result.content()).hasSize(2);

                    BootcampSummary firstBootcamp = result.content().get(0);
                    assertThat(firstBootcamp.id()).isEqualTo(1L);
                    assertThat(firstBootcamp.name()).isEqualTo(BOOTCAMP_NAME);
                    assertThat(firstBootcamp.description()).isEqualTo(BOOTCAMP_DESCRIPTION);
                    assertThat(firstBootcamp.releaseDate()).isEqualTo(LocalDate.of(2026, Month.JULY, 31));
                    assertThat(firstBootcamp.duration()).isEqualTo(120);
                    assertThat(firstBootcamp.capabilityCount()).isEqualTo(2L);
                    assertThat(firstBootcamp.capabilityIds()).containsExactly(1L, 2L);

                    BootcampSummary secondBootcamp = result.content().get(1);
                    assertThat(secondBootcamp.id()).isEqualTo(2L);
                    assertThat(secondBootcamp.name()).isEqualTo("Bootcamp Cloud");
                    assertThat(secondBootcamp.description()).isEqualTo("Arquitectura y despliegue");
                    assertThat(secondBootcamp.releaseDate()).isEqualTo(LocalDate.of(2026, Month.AUGUST, 1));
                    assertThat(secondBootcamp.duration()).isEqualTo(90);
                    assertThat(secondBootcamp.capabilityCount()).isEqualTo(1L);
                    assertThat(secondBootcamp.capabilityIds()).containsExactly(2L);
                })
                .verifyComplete();

        verify(bootcampCapabilityRepository).findByBootcampId(1L);
        verify(bootcampCapabilityRepository).findByBootcampId(2L);
    }

    @Test
    void debeRetornarListaVaciaCuandoNoHayBootcampsEnLaPagina() {
        // Given
        BootcampListQueryBuilder.BootcampListQuery query = bootcampListQueryBuilder.resolve(DEFAULT_CRITERIA);
        String sql = bootcampListQueryBuilder.buildQuery(query);
        DatabaseClient.GenericExecuteSpec listSpec = mock(DatabaseClient.GenericExecuteSpec.class, RETURNS_SELF);
        DatabaseClient.GenericExecuteSpec countSpec = mock(DatabaseClient.GenericExecuteSpec.class, RETURNS_SELF);
        RowsFetchSpec<Object> listFetchSpec = mock(RowsFetchSpec.class);
        RowsFetchSpec<Long> countFetchSpec = mock(RowsFetchSpec.class);

        when(databaseClient.sql(sql)).thenReturn(listSpec);
        when(databaseClient.sql(TOTAL_ELEMENTS_SQL)).thenReturn(countSpec);
        when(listSpec.map(org.mockito.ArgumentMatchers.<BiFunction<Row, RowMetadata, Object>>any())).thenAnswer(invocation -> {
            when(listFetchSpec.all()).thenReturn(Flux.empty());
            return listFetchSpec;
        });
        when(countSpec.map(org.mockito.ArgumentMatchers.<BiFunction<Row, RowMetadata, Long>>any())).thenAnswer(invocation -> {
            when(countFetchSpec.one()).thenReturn(Mono.just(0L));
            return countFetchSpec;
        });

        // When
        Mono<BootcampPageResult> responseMono = bootcampListQueryAdapter.listBootcamps(DEFAULT_CRITERIA);

        // Then
        StepVerifier.create(responseMono)
                .assertNext(result -> {
                    assertThat(result.page()).isEqualTo(0);
                    assertThat(result.size()).isEqualTo(10);
                    assertThat(result.totalElements()).isEqualTo(0L);
                    assertThat(result.totalPages()).isEqualTo(0);
                    assertThat(result.content()).isEmpty();
                })
                .verifyComplete();

        verifyNoInteractions(bootcampCapabilityRepository);
    }

    @Test
    void debePropagarElErrorCuandoFallaLaConsultaDeCapacidades() {
        // Given
        BootcampListQueryBuilder.BootcampListQuery query = bootcampListQueryBuilder.resolve(DEFAULT_CRITERIA);
        String sql = bootcampListQueryBuilder.buildQuery(query);
        DatabaseClient.GenericExecuteSpec listSpec = mock(DatabaseClient.GenericExecuteSpec.class, RETURNS_SELF);
        DatabaseClient.GenericExecuteSpec countSpec = mock(DatabaseClient.GenericExecuteSpec.class, RETURNS_SELF);
        RowsFetchSpec<Object> listFetchSpec = mock(RowsFetchSpec.class);
        RowsFetchSpec<Long> countFetchSpec = mock(RowsFetchSpec.class);
        RowMetadata metadata = mock(RowMetadata.class);
        Row firstRow = bootcampRow(1L, BOOTCAMP_NAME, BOOTCAMP_DESCRIPTION, LocalDate.of(2026, Month.JULY, 31), 120, 1L);
        RuntimeException unexpectedException = new RuntimeException("unexpected");

        when(databaseClient.sql(sql)).thenReturn(listSpec);
        when(databaseClient.sql(TOTAL_ELEMENTS_SQL)).thenReturn(countSpec);
        when(listSpec.map(org.mockito.ArgumentMatchers.<BiFunction<Row, RowMetadata, Object>>any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            BiFunction<Row, RowMetadata, Object> mapper = invocation.getArgument(0);
            Object mappedFirstRow = mapper.apply(firstRow, metadata);
            when(listFetchSpec.all()).thenReturn(Flux.just(mappedFirstRow));
            return listFetchSpec;
        });
        when(countSpec.map(org.mockito.ArgumentMatchers.<BiFunction<Row, RowMetadata, Long>>any())).thenAnswer(invocation -> {
            when(countFetchSpec.one()).thenReturn(Mono.just(1L));
            return countFetchSpec;
        });
        when(bootcampCapabilityRepository.findByBootcampId(1L)).thenReturn(Flux.error(unexpectedException));

        // When
        Mono<BootcampPageResult> responseMono = bootcampListQueryAdapter.listBootcamps(DEFAULT_CRITERIA);

        // Then
        StepVerifier.create(responseMono)
                .expectErrorSatisfies(error -> assertThat(error).isSameAs(unexpectedException))
                .verify();

        verify(bootcampCapabilityRepository).findByBootcampId(1L);
    }

    private BootcampCapabilityEntity capability(Long capabilityId) {
        BootcampCapabilityEntity entity = new BootcampCapabilityEntity();
        entity.setCapabilityId(capabilityId);
        return entity;
    }

    private Row bootcampRow(Long id, String name, String description, LocalDate releaseDate, Integer duration, Long capabilityCount) {
        Row row = mock(Row.class);
        when(row.get("id", Long.class)).thenReturn(id);
        when(row.get("name", String.class)).thenReturn(name);
        when(row.get("description", String.class)).thenReturn(description);
        when(row.get("release_date", LocalDate.class)).thenReturn(releaseDate);
        when(row.get("duration", Integer.class)).thenReturn(duration);
        when(row.get("capability_count", Long.class)).thenReturn(capabilityCount);
        return row;
    }
}
