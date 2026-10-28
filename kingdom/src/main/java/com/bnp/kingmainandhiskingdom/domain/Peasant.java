package com.bnp.kingmainandhiskingdom.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Exercise C1: one more subclass in the SINGLE_TABLE hierarchy of Person: a new value in person_type, one new nullable column. */
@Entity
@DiscriminatorValue("PEASANT")
@Getter
@Setter
@NoArgsConstructor
public class Peasant extends Person {

    private String crop;

    public Peasant(String name, int age, String crop) {
        super(name, age);
        this.crop = crop;
    }

    @Override
    public void copyFrom(Person other) {
        super.copyFrom(other);
        if (other instanceof Peasant peasant) {
            this.crop = peasant.crop;
        }
    }
}
