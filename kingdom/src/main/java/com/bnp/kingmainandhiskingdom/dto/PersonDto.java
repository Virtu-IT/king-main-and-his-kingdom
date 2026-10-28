package com.bnp.kingmainandhiskingdom.dto;

import com.bnp.kingmainandhiskingdom.domain.Profession;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * What the REST API sends and receives for a person. The entities stay inside the application:
 * the JSON contract can change without touching the database mapping, and the other way round.
 * The "type" property selects the subclass ("PERSON", "KNIGHT", "WIZARD" or "PEASANT"); "id" and "profession"
 * are ignored on input: a profession is only given by training (POST /api/persons/{name}/training).
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", defaultImpl = PersonDto.Plain.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = PersonDto.Plain.class, name = "PERSON"),
        @JsonSubTypes.Type(value = PersonDto.Knight.class, name = "KNIGHT"),
        @JsonSubTypes.Type(value = PersonDto.Wizard.class, name = "WIZARD"),
        @JsonSubTypes.Type(value = PersonDto.Peasant.class, name = "PEASANT")
})
public sealed interface PersonDto permits PersonDto.Plain, PersonDto.Knight, PersonDto.Wizard, PersonDto.Peasant {

    Long id();

    String name();

    int age();

    Profession profession();

    record Plain(Long id, @NotBlank(message = "Name cannot be blank") String name,
                 @PositiveOrZero(message = "Age cannot be negative") int age,
                 Profession profession) implements PersonDto {
    }

    /** C5: a knight must have a rank, and his horse has a short name. */
    record Knight(Long id, @NotBlank(message = "Name cannot be blank") String name,
                  @PositiveOrZero(message = "Age cannot be negative") int age, Profession profession,
                  @NotBlank(message = "A knight needs a rank") String knightRank,
                  @Size(max = 30, message = "The name of the horse can have at most 30 characters") String horseName) implements PersonDto {
    }

    record Wizard(Long id, @NotBlank(message = "Name cannot be blank") String name,
                  @PositiveOrZero(message = "Age cannot be negative") int age, Profession profession,
                  Integer mana, String school) implements PersonDto {
    }

    /** C1: the fourth subclass. */
    record Peasant(Long id, @NotBlank(message = "Name cannot be blank") String name,
                   @PositiveOrZero(message = "Age cannot be negative") int age, Profession profession,
                   String crop) implements PersonDto {
    }
}
