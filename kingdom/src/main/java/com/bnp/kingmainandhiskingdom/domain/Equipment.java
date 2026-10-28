package com.bnp.kingmainandhiskingdom.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Inheritance demo 3: JOINED.
 * Table "equipment" holds the shared columns (id, name, weight); "weapon" and "armor" hold only
 * their own columns and share the primary key. Subclass columns can be NOT NULL, but every
 * query on a subclass needs a join.
 */
@Entity
@Table(name = "equipment")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
public abstract class Equipment extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private double weight;
}
