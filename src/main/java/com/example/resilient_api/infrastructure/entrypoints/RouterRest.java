package com.example.resilient_api.infrastructure.entrypoints;


import com.example.resilient_api.infrastructure.entrypoints.dto.BootcampDTO;
import com.example.resilient_api.infrastructure.entrypoints.dto.BootcampIdsRequestDTO;
import com.example.resilient_api.infrastructure.entrypoints.handler.BootcampHandlerImpl;
import com.example.resilient_api.infrastructure.entrypoints.util.APIResponse;
import com.example.resilient_api.infrastructure.entrypoints.util.Constants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.DELETE;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterRest {
    @Bean
    @RouterOperations({
        @RouterOperation(
            path = "/bootcamp",
            method = RequestMethod.POST,
            beanClass = BootcampHandlerImpl.class,
            beanMethod = "createBootcamp",
            operation = @Operation(
                operationId = "createBootcamp",
                summary = "Create a bootcamp",
                description = "Creates a bootcamp and returns the persisted resource.",
                tags = {"Bootcamp"},
                parameters = {
                    @Parameter(
                        in = ParameterIn.HEADER,
                        name = Constants.X_MESSAGE_ID,
                        required = false,
                        description = "Correlation identifier for tracing"
                    )
                },
                requestBody = @RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = BootcampDTO.class))
                ),
                responses = {
                    @ApiResponse(
                        responseCode = "201",
                        description = "Bootcamp created",
                        content = @Content(schema = @Schema(implementation = APIResponse.class))
                    )
                }
            )),
        @RouterOperation(
            path = "/bootcamp/validate",
            method = RequestMethod.POST,
            beanClass = BootcampHandlerImpl.class,
            beanMethod = "validateBootcamps",
            operation = @Operation(
                operationId = "validateBootcamps",
                summary = "Validate bootcamps",
                description = "Validates a list of bootcamp identifiers.",
                tags = {"Bootcamp"},
                parameters = {
                    @Parameter(
                        in = ParameterIn.HEADER,
                        name = Constants.X_MESSAGE_ID,
                        required = false,
                        description = "Correlation identifier for tracing"
                    )
                },
                requestBody = @RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = BootcampIdsRequestDTO.class))
                ),
                responses = {
                    @ApiResponse(
                        responseCode = "200",
                        description = "Bootcamps validated",
                        content = @Content(schema = @Schema(implementation = APIResponse.class))
                    )
                }
            )),
        @RouterOperation(
            path = "/bootcamp",
            method = RequestMethod.GET,
            beanClass = BootcampHandlerImpl.class,
            beanMethod = "listBootcamps",
            operation = @Operation(
                operationId = "listBootcamps",
                summary = "List bootcamps",
                description = "Returns the paginated list of bootcamps.",
                tags = {"Bootcamp"},
                parameters = {
                    @Parameter(
                        in = ParameterIn.HEADER,
                        name = Constants.X_MESSAGE_ID,
                        required = false,
                        description = "Correlation identifier for tracing"
                    ),
                    @Parameter(in = ParameterIn.QUERY, name = "page", required = false, description = "Page number starting at zero"),
                    @Parameter(in = ParameterIn.QUERY, name = "size", required = false, description = "Page size"),
                    @Parameter(in = ParameterIn.QUERY, name = "sortBy", required = false, description = "Field used to sort results"),
                    @Parameter(in = ParameterIn.QUERY, name = "sortDirection", required = false, description = "Sort direction: asc or desc")
                },
                responses = {
                    @ApiResponse(
                        responseCode = "200",
                        description = "Bootcamps listed",
                        content = @Content(schema = @Schema(implementation = APIResponse.class))
                    )
                }
            )),
        @RouterOperation(
            path = "/bootcamp/{id}",
            method = RequestMethod.DELETE,
            beanClass = BootcampHandlerImpl.class,
            beanMethod = "deleteBootcamp",
            operation = @Operation(
                operationId = "deleteBootcamp",
                summary = "Delete a bootcamp",
                description = "Deletes a bootcamp by its identifier.",
                tags = {"Bootcamp"},
                parameters = {
                    @Parameter(
                        in = ParameterIn.HEADER,
                        name = Constants.X_MESSAGE_ID,
                        required = false,
                        description = "Correlation identifier for tracing"
                    ),
                    @Parameter(
                        in = ParameterIn.PATH,
                        name = "id",
                        required = true,
                        description = "Bootcamp identifier",
                        schema = @Schema(type = "integer", format = "int64")
                    )
                },
                responses = {
                    @ApiResponse(
                        responseCode = "200",
                        description = "Bootcamp deleted",
                        content = @Content(schema = @Schema(implementation = APIResponse.class))
                    )
                }
            ))
    })
    public RouterFunction<ServerResponse> routerFunction(BootcampHandlerImpl bootcampHandler) {
    return route(POST("/bootcamp"), bootcampHandler::createBootcamp)
            .andRoute(POST("/bootcamp/validate"), bootcampHandler::validateBootcamps)
            .andRoute(GET("/bootcamp"), bootcampHandler::listBootcamps)
            .andRoute(DELETE("/bootcamp/{id}"), bootcampHandler::deleteBootcamp);
    }
}
