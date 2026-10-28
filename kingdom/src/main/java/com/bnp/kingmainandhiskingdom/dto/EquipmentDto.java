package com.bnp.kingmainandhiskingdom.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** REST representation of Equipment ("type": "WEAPON" or "ARMOR"). */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = EquipmentDto.Weapon.class, name = "WEAPON"),
        @JsonSubTypes.Type(value = EquipmentDto.Armor.class, name = "ARMOR")
})
public sealed interface EquipmentDto permits EquipmentDto.Weapon, EquipmentDto.Armor {

    Long id();

    String name();

    double weight();

    record Weapon(Long id, @NotBlank(message = "Name cannot be blank") String name, double weight,
                  int damage, @NotBlank(message = "Damage type cannot be blank") String damageType) implements EquipmentDto {
    }

    record Armor(Long id, @NotBlank(message = "Name cannot be blank") String name, double weight,
                 int defense, @NotNull(message = "Material is required") String material) implements EquipmentDto {
    }
}
