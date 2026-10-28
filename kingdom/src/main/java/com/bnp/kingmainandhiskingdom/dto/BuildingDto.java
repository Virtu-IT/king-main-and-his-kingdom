package com.bnp.kingmainandhiskingdom.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Exercise C3. The columns of the subclasses are nullable in the database (SINGLE_TABLE), so the rules that
 * "a castle has towers" are validated here, on the way in.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BuildingDto.Castle.class, name = "CASTLE"),
        @JsonSubTypes.Type(value = BuildingDto.Farm.class, name = "FARM"),
        @JsonSubTypes.Type(value = BuildingDto.Tower.class, name = "TOWER")
})
public sealed interface BuildingDto permits BuildingDto.Castle, BuildingDto.Farm, BuildingDto.Tower {

    Long id();

    String name();

    record Castle(Long id, @NotBlank(message = "Name cannot be blank") String name,
                  @NotNull(message = "Towers is required") @Positive Integer towers,
                  @NotNull(message = "hasMoat is required") Boolean hasMoat) implements BuildingDto {
    }

    record Farm(Long id, @NotBlank(message = "Name cannot be blank") String name,
                @NotNull(message = "Hectares is required") @Positive Integer hectares,
                String crop) implements BuildingDto {
    }

    record Tower(Long id, @NotBlank(message = "Name cannot be blank") String name,
                 @NotNull(message = "Height is required") @Positive Integer heightMeters,
                 @NotNull(message = "Crew size is required") @Positive Integer crewSize) implements BuildingDto {
    }
}
