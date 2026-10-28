package com.bnp.kingmainandhiskingdom.domain;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Tank extends WarMachine {

    private int armorThicknessMm;

    public Tank(String name, int crewSize, int armorThicknessMm) {
        setName(name);
        setCrewSize(crewSize);
        this.armorThicknessMm = armorThicknessMm;
    }
}
