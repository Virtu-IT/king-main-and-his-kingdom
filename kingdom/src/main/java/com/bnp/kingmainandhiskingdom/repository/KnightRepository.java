package com.bnp.kingmainandhiskingdom.repository;

import com.bnp.kingmainandhiskingdom.domain.Knight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** A repository for a subclass only returns the rows of that subclass (WHERE person_type = 'KNIGHT'). */
public interface KnightRepository extends JpaRepository<Knight, Long> {
    List<Knight> findByKnightRank(String knightRank);
}
