package com.bnp.kingmainandhiskingdom.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotBlank;

/** REST representation of WarMachine ("type": "TANK" or "CATAPULT"). */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = WarMachineDto.Tank.class, name = "TANK"),
        @JsonSubTypes.Type(value = WarMachineDto.Catapult.class, name = "CATAPULT")
})
public sealed interface WarMachineDto permits WarMachineDto.Tank, WarMachineDto.Catapult {

    Long id();

    String name();

    int crewSize();

    record Tank(Long id, @NotBlank(message = "Name cannot be blank") String name, int crewSize,
                int armorThicknessMm) implements WarMachineDto {
    }

    record Catapult(Long id, @NotBlank(message = "Name cannot be blank") String name, int crewSize,
                    int rangeMeters) implements WarMachineDto {
    }
}
