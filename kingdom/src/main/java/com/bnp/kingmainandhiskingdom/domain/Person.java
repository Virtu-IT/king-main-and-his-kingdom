package com.bnp.kingmainandhiskingdom.domain;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Inheritance demo 2: SINGLE_TABLE (the default strategy).
 * Person, Knight and Wizard all live in ONE table "person"; the column PERSON_TYPE says which
 * class a row belongs to. Fast (no joins), but the columns of the subclasses must be nullable.
 * The REST API never exposes these entities: it uses the DTOs in the dto package.
 */
@Entity
@Table(name = "person")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "person_type", discriminatorType = DiscriminatorType.STRING, length = 20)
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Person extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private int age;

    /** Null until the person is trained (see ArmyService.trainPerson). */
    @Enumerated(EnumType.STRING)
    private Profession profession;

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    /** Copies the editable fields of another person (not the profession: that only changes by training). */
    public void copyFrom(Person other) {
        if (other.getName() != null) {
            this.name = other.getName();
        }
        this.age = other.getAge();
    }
}
