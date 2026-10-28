package com.bnp.kingmainandhiskingdom.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("KNIGHT")
@Getter
@Setter
@NoArgsConstructor
public class Knight extends Person {

    /** SINGLE_TABLE: this column is nullable in the database because other Person rows have no rank. */
    private String knightRank;
    private String horseName;

    /**
     * C4: unidirectional @OneToMany with a join column: the table "equipment" gets a column knight_id.
     * LAZY (the default for collections): the equipment is only loaded when you touch the list, so outside
     * a transaction (open-in-view is off) touching it throws a LazyInitializationException.
     */
    @OneToMany
    @JoinColumn(name = "knight_id")
    private List<Equipment> equipment = new ArrayList<>();

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
