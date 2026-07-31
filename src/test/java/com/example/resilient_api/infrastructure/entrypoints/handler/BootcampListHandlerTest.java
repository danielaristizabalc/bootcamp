package com.example.resilient_api.infrastructure.entrypoints.handler;

import com.example.resilient_api.domain.api.BootcampListServicePort;
import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.BusinessException;
import com.example.resilient_api.domain.model.BootcampListCriteria;
import com.example.resilient_api.domain.model.BootcampListItem;
import com.example.resilient_api.domain.model.BootcampListResult;
import com.example.resilient_api.infrastructure.entrypoints.dto.BootcampListItemDTO;
import com.example.resilient_api.infrastructure.entrypoints.dto.BootcampPageDTO;
import com.example.resilient_api.infrastructure.entrypoints.mapper.BootcampMapper;
import com.example.resilient_api.infrastructure.entrypoints.util.APIResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BootcampListHandlerTest {

    private static final String MESSAGE_ID = "message-id-123";

    @Mock
    private BootcampListServicePort bootcampListServicePort;

    @Mock
    private BootcampMapper bootcampMapper;

    @Mock
    private BootcampHandlerSupport support;

    @Mock
    private ServerRequest request;

    private BootcampListHandler bootcampListHandler;

    @BeforeEach
    void setUp() {
        bootcampListHandler = new BootcampListHandler(bootcampListServicePort, bootcampMapper, support);
    }

    @Test
    void debeListarBootcampsCuandoLaPeticionEsValida() {
        // Given
        BootcampListCriteria criteria = new BootcampListCriteria(0, 10, "name", "asc");
        BootcampListResult listResult = new BootcampListResult(
                List.of(new BootcampListItem(
                        1L,
                        "Bootcamp Java",
                        "Descripcion",
                        LocalDate.of(2026, Month.JULY, 31),
                        120,
                        List.of()
                )),
                0,
                10,
                1L,
                1
        );
        BootcampPageDTO pageDTO = BootcampPageDTO.builder()
                .content(List.of(BootcampListItemDTO.builder()
                        .id(1L)
                        .name("Bootcamp Java")
                        .description("Descripcion")
                        .releaseDate(LocalDate.of(2026, Month.JULY, 31))
                        .duration(120)
                        .build()))
                .page(0)
                .size(10)
                .totalElements(1L)
                .totalPages(1)
                .build();
        APIResponse apiResponse = mock(APIResponse.class);

        when(support.getMessageId(request)).thenReturn(MESSAGE_ID);
        when(support.buildCriteria(request)).thenReturn(criteria);
        when(bootcampListServicePort.listBootcamps(criteria, MESSAGE_ID)).thenReturn(Mono.just(listResult));
        when(bootcampMapper.bootcampListResultToBootcampPageDTO(listResult)).thenReturn(pageDTO);
        when(support.buildApiResponse(TechnicalMessage.BOOTCAMP_LISTED, MESSAGE_ID, pageDTO)).thenReturn(apiResponse);

        // When
        Mono<ServerResponse> responseMono = bootcampListHandler.handle(request);

        // Then
        StepVerifier.create(responseMono)
                .assertNext(response -> assertThat(response.statusCode()).isEqualTo(HttpStatus.OK))
                .verifyComplete();
        verify(support).getMessageId(request);
        verify(support).buildCriteria(request);
        verify(bootcampListServicePort).listBootcamps(criteria, MESSAGE_ID);
        verify(bootcampMapper).bootcampListResultToBootcampPageDTO(listResult);
        verify(support).buildApiResponse(TechnicalMessage.BOOTCAMP_LISTED, MESSAGE_ID, pageDTO);
    }

    @Test
    void debeRetornarBadRequestCuandoLosParametrosSonInvalidos() {
        // Given
        BusinessException businessException = new BusinessException(TechnicalMessage.INVALID_PARAMETERS);
        ServerResponse expectedResponse = mock(ServerResponse.class);

        when(support.getMessageId(request)).thenReturn(MESSAGE_ID);
        when(support.buildCriteria(request)).thenThrow(businessException);
        when(support.buildErrorResponse(HttpStatus.BAD_REQUEST, MESSAGE_ID, TechnicalMessage.INVALID_PARAMETERS))
                .thenReturn(Mono.just(expectedResponse));

        // When
        Mono<ServerResponse> responseMono = bootcampListHandler.handle(request);

        // Then
        StepVerifier.create(responseMono)
                .expectNext(expectedResponse)
                .verifyComplete();
        verify(support).buildErrorResponse(HttpStatus.BAD_REQUEST, MESSAGE_ID, TechnicalMessage.INVALID_PARAMETERS);
    }

    @Test
    void debeRetornarInternalServerErrorCuandoOcurreUnErrorInesperado() {
        // Given
        BootcampListCriteria criteria = new BootcampListCriteria(0, 10, "name", "asc");
        RuntimeException unexpectedException = new RuntimeException("unexpected");
        ServerResponse expectedResponse = mock(ServerResponse.class);

        when(support.getMessageId(request)).thenReturn(MESSAGE_ID);
        when(support.buildCriteria(request)).thenReturn(criteria);
        when(bootcampListServicePort.listBootcamps(criteria, MESSAGE_ID)).thenReturn(Mono.error(unexpectedException));
        when(support.buildErrorResponse(any(), any(), any())).thenReturn(Mono.just(expectedResponse));

        // When
        Mono<ServerResponse> responseMono = bootcampListHandler.handle(request);

        // Then
        StepVerifier.create(responseMono)
                .expectNext(expectedResponse)
                .verifyComplete();
        verify(support).buildErrorResponse(any(), any(), any());
    }
}