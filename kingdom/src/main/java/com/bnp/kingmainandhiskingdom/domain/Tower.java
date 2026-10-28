package com.bnp.kingmainandhiskingdom.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("TOWER")
@Getter
@Setter
@NoArgsConstructor
public class Tower extends Building {

    private Integer heightMeters;
    private Integer crewSize;

    public Tower(String name, Integer heightMeters, Integer crewSize) {
        setName(name);
        this.heightMeters = heightMeters;
        this.crewSize = crewSize;
    }
}
