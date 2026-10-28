package com.bnp.kingmainandhiskingdom.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("WIZARD")
@Getter
@Setter
@NoArgsConstructor
public class Wizard extends Person {

    private Integer mana;
    private String school;

    public Wizard(String name, int age, Integer mana, String school) {
        super(name, age);
        this.mana = mana;
        this.school = school;
    }

    @Override
    public void copyFrom(Person other) {
        super.copyFrom(other);
        if (other instanceof Wizard wizard) {
            this.mana = wizard.mana;
            this.school = wizard.school;
        }
    }
}
