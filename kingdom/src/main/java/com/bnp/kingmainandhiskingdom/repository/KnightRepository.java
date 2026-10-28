package com.bnp.kingmainandhiskingdom.repository;

import com.bnp.kingmainandhiskingdom.domain.Knight;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/** A repository for a subclass only returns the rows of that subclass (WHERE person_type = 'KNIGHT'). */
public interface KnightRepository extends JpaRepository<Knight, Long> {
    List<Knight> findByKnightRank(String knightRank);

    /** The knight who carries a piece of equipment, if any. */
    Optional<Knight> findByEquipmentId(Long equipmentId);

    /**
     * C4: ALL knights with their equipment in ONE query. With plain findAll() every knight.getEquipment() costs
     * an extra select (the N+1 problem). The entity graph turns it into one select with a left join.
     */
    @EntityGraph(attributePaths = "equipment")
    @Query("select k from Knight k order by k.name")
    List<Knight> findAllWithEquipment();
}
