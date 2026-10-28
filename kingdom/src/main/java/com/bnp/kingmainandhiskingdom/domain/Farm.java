package com.bnp.kingmainandhiskingdom.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("FARM")
@Getter
@Setter
@NoArgsConstructor
public class Farm extends Building {

    private Integer hectares;
    private String crop;

    public Farm(String name, Integer hectares, String crop) {
        setName(name);
        this.hectares = hectares;
        this.crop = crop;
    }
}
