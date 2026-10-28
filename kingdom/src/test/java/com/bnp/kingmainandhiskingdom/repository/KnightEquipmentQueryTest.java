package com.bnp.kingmainandhiskingdom.repository;

import com.bnp.kingmainandhiskingdom.domain.Knight;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.LazyInitializationException;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercise C4: the N+1 problem and lazy loading, proven by counting the SQL statements Hibernate sends.
 * (Hibernate statistics are switched on for this test with a property.)
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
class KnightEquipmentQueryTest {

    @Autowired
    private KnightRepository knightRepository;
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void plainFindAllCostsOneExtraSelectPerKnight_theEntityGraphNeedsOne() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();

        // the naive way: 1 select for the knights, then one select for the equipment of EACH knight
        entityManager.clear();
        statistics.clear();
        List<Knight> knights = knightRepository.findAll();
        knights.forEach(knight -> knight.getEquipment().size());
        long naive = statistics.getPrepareStatementCount();
        assertTrue(knights.size() >= 2);
        assertEquals(1 + knights.size(), naive, "N+1: one select for the knights and one for each knight");

        // with the entity graph: one select with a left join
        entityManager.clear();
        statistics.clear();
        List<Knight> fetched = knightRepository.findAllWithEquipment();
        fetched.forEach(knight -> knight.getEquipment().size());
        assertEquals(1, statistics.getPrepareStatementCount(), "everything in one query, however many knights there are");
        assertEquals(knights.size(), fetched.size(), "the join must not duplicate knights");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void outsideATransactionTheLazyCollectionCannotBeLoaded() {
        // open-in-view is off and there is no transaction: the entity is detached as soon as findAll returns
        List<Knight> knights = knightRepository.findAll();

        assertThrows(LazyInitializationException.class, () -> knights.get(0).getEquipment().size());
    }
}
