package com.bnp.kingmainandhiskingdom.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Exercise C3: buildings, mapped with SINGLE_TABLE. Why this strategy: every subclass has only two extra
 * columns of its own, so one table stays narrow; the list of all buildings (the most common query) is one
 * select without joins; and nothing in the subclasses must be NOT NULL at database level (the DTOs validate that).
 * Choose JOINED instead when subclasses have many columns that must be NOT NULL.
 */
@Entity
@Table(name = "building")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "building_type", discriminatorType = DiscriminatorType.STRING, length = 20)
@Getter
@Setter
public abstract class Building extends BaseEntity {

    @Column(nullable = false)
    private String name;
}
