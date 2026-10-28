package com.bnp.kingmainandhiskingdom.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "weapon")
@Getter
@Setter
@NoArgsConstructor
public class Weapon extends Equipment {

    @Column(nullable = false)
    private int damage;

    @Column(nullable = false)
    private String damageType;

    public Weapon(String name, double weight, int damage, String damageType) {
        setName(name);
        setWeight(weight);
        this.damage = damage;
        this.damageType = damageType;
    }
}
