package com.bnp.kingmainandhiskingdom.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import lombok.Getter;
import lombok.Setter;

/**
 * Inheritance demo 4: TABLE_PER_CLASS.
 * There is NO table for WarMachine: "tank" and "catapult" each hold all columns (name and crewSize too).
 * No joins when you query one subclass, but a query on WarMachine becomes a UNION of all tables,
 * and the id cannot be IDENTITY (hence the sequence). Rarely a good idea; shown here for completeness.
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Getter
@Setter
public abstract class WarMachine extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private int crewSize;
}
