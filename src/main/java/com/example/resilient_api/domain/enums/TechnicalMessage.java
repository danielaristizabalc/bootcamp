package com.example.resilient_api.domain.enums;

import com.example.resilient_api.domain.constants.Constants;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum TechnicalMessage {

    INTERNAL_ERROR("500","Something went wrong, please try again", ""),
    INTERNAL_ERROR_IN_ADAPTERS("PRC501","Something went wrong in adapters, please try again", ""),
    INVALID_REQUEST("400", "Bad Request, please verify data", ""),
    INVALID_PARAMETERS(INVALID_REQUEST.getCode(), "Bad Parameters, please verify data", ""),
    INVALID_EMAIL("403", "Invalid email, please verify", "email"),
    INVALID_MESSAGE_ID("404", "Invalid Message ID, please verify", "messageId"),
    UNSUPPORTED_OPERATION("501", "Method not supported, please try again", ""),
    USER_CREATED("201", "User created successfully", ""),
    ADAPTER_RESPONSE_NOT_FOUND("404-0", "invalid email, please verify", ""),
    USER_ALREADY_EXISTS("400","El usuario ya está registrado." ,"" ),
    BOOTCAMP_CREATED("201", "Bootcamp creado exitosamente", ""),
    BOOTCAMP_ALREADY_EXISTS("400", "El bootcamp ya está registrado", ""),
    INVALID_CAPABILITIES_COUNT("400", "Un bootcamp debe tener entre 1 y 4 capacidades", Constants.CAPABILITIES_PARAM),
    CAPABILITIES_ALREADY_ASSIGNED("400", "Algunas capacidades ya están asociadas a otro bootcamp", Constants.CAPABILITIES_PARAM),
    CAPABILITIES_NOT_FOUND("400", "Algunas capacidades no existen", Constants.CAPABILITIES_PARAM),
    CAPABILITY_SERVICE_UNAVAILABLE("503", "Servicio de capacidades no disponible", "");

    private final String code;
    private final String message;
    private final String param;
}