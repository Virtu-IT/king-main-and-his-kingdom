package com.bnp.kingmainandhiskingdom.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.bnp.kingmainandhiskingdom.domain.Profession;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * What the REST API sends and receives for a person. The entities stay inside the application:
 * the JSON contract can change without touching the database mapping, and the other way round.
 * The "type" property selects the subclass ("PERSON", "KNIGHT" or "WIZARD"); "id" and "profession" are ignored
 * on input: a profession is only given by training (POST /api/persons/{name}/training).
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", defaultImpl = PersonDto.Plain.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = PersonDto.Plain.class, name = "PERSON"),
        @JsonSubTypes.Type(value = PersonDto.Knight.class, name = "KNIGHT"),
        @JsonSubTypes.Type(value = PersonDto.Wizard.class, name = "WIZARD")
})
public sealed interface PersonDto permits PersonDto.Plain, PersonDto.Knight, PersonDto.Wizard {

    Long id();

    String name();

    int age();

    Profession profession();

    record Plain(Long id, @NotBlank(message = "Name cannot be blank") String name,
                 @PositiveOrZero(message = "Age cannot be negative") int age,
                 Profession profession) implements PersonDto {
    }

    record Knight(Long id, @NotBlank(message = "Name cannot be blank") String name,
                  @PositiveOrZero(message = "Age cannot be negative") int age, Profession profession,
                  String knightRank, String horseName) implements PersonDto {
    }

    record Wizard(Long id, @NotBlank(message = "Name cannot be blank") String name,
                  @PositiveOrZero(message = "Age cannot be negative") int age, Profession profession,
                  Integer mana, String school) implements PersonDto {
    }
}
