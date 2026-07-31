package com.example.resilient_api.infrastructure.entrypoints.handler;

import com.example.resilient_api.domain.api.BootcampServicePort;
import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.BusinessException;
import com.example.resilient_api.domain.exceptions.TechnicalException;
import com.example.resilient_api.domain.model.Bootcamp;
import com.example.resilient_api.infrastructure.entrypoints.dto.BootcampDTO;
import com.example.resilient_api.infrastructure.entrypoints.mapper.BootcampMapper;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BootcampCreateHandlerTest {

        private static final String MESSAGE_ID = "message-id-123";
        private static final String BOOTCAMP_NAME = "Backend Java";
        private static final String BOOTCAMP_DESCRIPTION = "Bootcamp de backend";
        private static final LocalDate RELEASE_DATE = LocalDate.of(2026, Month.JULY, 31);

    @Mock
    private BootcampServicePort bootcampServicePort;

    @Mock
    private BootcampMapper bootcampMapper;

    @Mock
    private BootcampHandlerSupport support;

    @Mock
    private ServerRequest request;

    private BootcampCreateHandler bootcampCreateHandler;

    @BeforeEach
    void setUp() {
        bootcampCreateHandler = new BootcampCreateHandler(bootcampServicePort, bootcampMapper, support);
    }

    @Test
    void debeCrearBootcampCuandoLaPeticionEsValida() {
        // Given
        BootcampDTO bootcampDTO = BootcampDTO.builder()
                .name(BOOTCAMP_NAME)
                .description(BOOTCAMP_DESCRIPTION)
                .releaseDate(RELEASE_DATE)
                .duration(120)
                .capabilityIds(List.of(1L, 2L))
                .build();
        Bootcamp bootcamp = new Bootcamp(
                10L,
                BOOTCAMP_NAME,
                BOOTCAMP_DESCRIPTION,
                RELEASE_DATE,
                120,
                List.of(1L, 2L)
        );
        Bootcamp savedBootcamp = new Bootcamp(
                11L,
                BOOTCAMP_NAME,
                BOOTCAMP_DESCRIPTION,
                RELEASE_DATE,
                120,
                List.of(1L, 2L)
        );
        BootcampDTO responseDto = bootcampDTO.toBuilder().id(11L).build();
        when(support.getMessageId(request)).thenReturn(MESSAGE_ID);
        when(request.bodyToMono(BootcampDTO.class)).thenReturn(Mono.just(bootcampDTO));
        when(bootcampMapper.bootcampDTOToBootcamp(bootcampDTO)).thenReturn(bootcamp);
        when(bootcampServicePort.registerBootcamp(bootcamp, MESSAGE_ID)).thenReturn(Mono.just(savedBootcamp));
        when(bootcampMapper.bootcampToBootcampDTO(savedBootcamp)).thenReturn(responseDto);
        when(support.buildApiResponse(eq(TechnicalMessage.BOOTCAMP_CREATED), eq(MESSAGE_ID), eq(responseDto)))
                .thenReturn(org.mockito.Mockito.mock(com.example.resilient_api.infrastructure.entrypoints.util.APIResponse.class));

        // When
        Mono<ServerResponse> responseMono = bootcampCreateHandler.handle(request);

        // Then
        StepVerifier.create(responseMono)
                .assertNext(response -> assertNotNull(response))
                .verifyComplete();
        verify(support).getMessageId(request);
        verify(request).bodyToMono(BootcampDTO.class);
        verify(bootcampMapper).bootcampDTOToBootcamp(bootcampDTO);
                verify(bootcampServicePort).registerBootcamp(bootcamp, MESSAGE_ID);
        verify(bootcampMapper).bootcampToBootcampDTO(savedBootcamp);
                verify(support).buildApiResponse(TechnicalMessage.BOOTCAMP_CREATED, MESSAGE_ID, responseDto);
    }

    @Test
        void debeRetornarBadRequestCuandoElServicioLanzaBusinessException() {
        // Given
                BootcampDTO bootcampDTO = BootcampDTO.builder().name(BOOTCAMP_NAME).build();
                Bootcamp bootcamp = new Bootcamp(10L, BOOTCAMP_NAME, null, null, null, null);
        BusinessException businessException = new BusinessException(TechnicalMessage.BOOTCAMP_ALREADY_EXISTS);
        ServerResponse expectedResponse = org.mockito.Mockito.mock(ServerResponse.class);

                when(support.getMessageId(request)).thenReturn(MESSAGE_ID);
        when(request.bodyToMono(BootcampDTO.class)).thenReturn(Mono.just(bootcampDTO));
        when(bootcampMapper.bootcampDTOToBootcamp(bootcampDTO)).thenReturn(bootcamp);
                when(bootcampServicePort.registerBootcamp(bootcamp, MESSAGE_ID)).thenReturn(Mono.error(businessException));
                when(support.buildErrorResponse(HttpStatus.BAD_REQUEST, MESSAGE_ID, TechnicalMessage.BOOTCAMP_ALREADY_EXISTS))
                .thenReturn(Mono.just(expectedResponse));

        // When
        Mono<ServerResponse> responseMono = bootcampCreateHandler.handle(request);

        // Then
        StepVerifier.create(responseMono)
                .expectNext(expectedResponse)
                .verifyComplete();
                verify(support).buildErrorResponse(HttpStatus.BAD_REQUEST, MESSAGE_ID, TechnicalMessage.BOOTCAMP_ALREADY_EXISTS);
    }

    @Test
        void debeRetornarInternalServerErrorCuandoElServicioLanzaTechnicalException() {
        // Given
                BootcampDTO bootcampDTO = BootcampDTO.builder().name(BOOTCAMP_NAME).build();
                Bootcamp bootcamp = new Bootcamp(10L, BOOTCAMP_NAME, null, null, null, null);
        TechnicalException technicalException = new TechnicalException(TechnicalMessage.REPORT_SERVICE_UNAVAILABLE);
        ServerResponse expectedResponse = org.mockito.Mockito.mock(ServerResponse.class);

                when(support.getMessageId(request)).thenReturn(MESSAGE_ID);
        when(request.bodyToMono(BootcampDTO.class)).thenReturn(Mono.just(bootcampDTO));
        when(bootcampMapper.bootcampDTOToBootcamp(bootcampDTO)).thenReturn(bootcamp);
                when(bootcampServicePort.registerBootcamp(bootcamp, MESSAGE_ID)).thenReturn(Mono.error(technicalException));
                when(support.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, MESSAGE_ID, TechnicalMessage.REPORT_SERVICE_UNAVAILABLE))
                .thenReturn(Mono.just(expectedResponse));

        // When
        Mono<ServerResponse> responseMono = bootcampCreateHandler.handle(request);

        // Then
        StepVerifier.create(responseMono)
                .expectNext(expectedResponse)
                .verifyComplete();
                verify(support).buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, MESSAGE_ID, TechnicalMessage.REPORT_SERVICE_UNAVAILABLE);
    }

    @Test
        void debeRetornarInternalServerErrorCuandoOcurreUnErrorInesperado() {
        // Given
                BootcampDTO bootcampDTO = BootcampDTO.builder().name(BOOTCAMP_NAME).build();
                Bootcamp bootcamp = new Bootcamp(10L, BOOTCAMP_NAME, null, null, null, null);
        RuntimeException unexpectedException = new RuntimeException("unexpected");
        ServerResponse expectedResponse = org.mockito.Mockito.mock(ServerResponse.class);

                when(support.getMessageId(request)).thenReturn(MESSAGE_ID);
        when(request.bodyToMono(BootcampDTO.class)).thenReturn(Mono.just(bootcampDTO));
        when(bootcampMapper.bootcampDTOToBootcamp(bootcampDTO)).thenReturn(bootcamp);
                when(bootcampServicePort.registerBootcamp(bootcamp, MESSAGE_ID)).thenReturn(Mono.error(unexpectedException));
                when(support.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, MESSAGE_ID, TechnicalMessage.INTERNAL_ERROR))
                .thenReturn(Mono.just(expectedResponse));

        // When
        Mono<ServerResponse> responseMono = bootcampCreateHandler.handle(request);

        // Then
        StepVerifier.create(responseMono)
                .expectNext(expectedResponse)
                .verifyComplete();
        verify(support).buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, MESSAGE_ID, TechnicalMessage.INTERNAL_ERROR);
    }
}