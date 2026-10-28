package com.bnp.kingmainandhiskingdom.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("CASTLE")
@Getter
@Setter
@NoArgsConstructor
public class Castle extends Building {

    // wrapper types: in SINGLE_TABLE these columns must be nullable (a farm has no towers), a primitive would be NOT NULL
    private Integer towers;
    private Boolean hasMoat;

    public Castle(String name, Integer towers, Boolean hasMoat) {
        setName(name);
        this.towers = towers;
        this.hasMoat = hasMoat;
    }
}
