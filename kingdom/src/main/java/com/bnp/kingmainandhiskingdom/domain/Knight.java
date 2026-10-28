package com.bnp.kingmainandhiskingdom.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("KNIGHT")
@Getter
@Setter
@NoArgsConstructor
public class Knight extends Person {

    /** SINGLE_TABLE: this column is nullable in the database because other Person rows have no rank. */
    private String knightRank;
    private String horseName;

    public Knight(String name, int age, String knightRank, String horseName) {
        super(name, age);
        this.knightRank = knightRank;
        this.horseName = horseName;
    }

    @Override
    public void copyFrom(Person other) {
        super.copyFrom(other);
        if (other instanceof Knight knight) {
            this.knightRank = knight.knightRank;
            this.horseName = knight.horseName;
        }
    }
}
