package com.bnp.kingmainandhiskingdom.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "armor")
@Getter
@Setter
@NoArgsConstructor
public class Armor extends Equipment {

    @Column(nullable = false)
    private int defense;

    @Column(nullable = false)
    private String material;

    public Armor(String name, double weight, int defense, String material) {
        setName(name);
        setWeight(weight);
        this.defense = defense;
        this.material = material;
    }
}
