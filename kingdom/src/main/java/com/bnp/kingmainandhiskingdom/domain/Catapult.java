package com.bnp.kingmainandhiskingdom.domain;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Catapult extends WarMachine {

    private int rangeMeters;

    public Catapult(String name, int crewSize, int rangeMeters) {
        setName(name);
        setCrewSize(crewSize);
        this.rangeMeters = rangeMeters;
    }
}
