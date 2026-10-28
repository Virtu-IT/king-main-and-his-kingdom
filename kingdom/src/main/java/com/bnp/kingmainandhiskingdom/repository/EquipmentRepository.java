package com.bnp.kingmainandhiskingdom.repository;

import com.bnp.kingmainandhiskingdom.domain.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    /** Polymorphic JPQL: type(e) filters on the class, no matter in which table the data is stored. */
    @Query("select e from Equipment e where type(e) = Weapon order by e.name")
    List<Equipment> findAllWeapons();
}
