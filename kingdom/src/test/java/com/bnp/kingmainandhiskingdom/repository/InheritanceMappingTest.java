package com.bnp.kingmainandhiskingdom.repository;

import com.bnp.kingmainandhiskingdom.domain.Armor;
import com.bnp.kingmainandhiskingdom.domain.Catapult;
import com.bnp.kingmainandhiskingdom.domain.Equipment;
import com.bnp.kingmainandhiskingdom.domain.Knight;
import com.bnp.kingmainandhiskingdom.domain.Person;
import com.bnp.kingmainandhiskingdom.domain.Tank;
import com.bnp.kingmainandhiskingdom.domain.WarMachine;
import com.bnp.kingmainandhiskingdom.domain.Weapon;
import com.bnp.kingmainandhiskingdom.domain.Wizard;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One test per inheritance strategy. Besides the Java side (polymorphic queries) every test also looks
 * at the database side with native SQL, to SEE what the strategy does to the tables.
 * Run with the "local" profile to read the generated SQL in the console.
 */
@SpringBootTest(properties = "kingdom.seed.enabled=false") // start with empty tables
@ActiveProfiles("test")
@Transactional // every test is rolled back
class InheritanceMappingTest {

    @Autowired
    private EntityManager em;
    @Autowired
    private PersonRepository personRepository;
    @Autowired
    private KnightRepository knightRepository;
    @Autowired
    private EquipmentRepository equipmentRepository;
    @Autowired
    private WeaponRepository weaponRepository;
    @Autowired
    private WarMachineRepository warMachineRepository;

    private long count(String sql) {
        return ((Number) em.createNativeQuery(sql).getSingleResult()).longValue();
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    // ---- 1. @MappedSuperclass ------------------------------------------------------------------

    @Test
    void mappedSuperclassFieldsEndUpInEveryTable() {
        Person saved = personRepository.save(new Person("Bors", 40));
        flushAndClear();

        assertTrue(saved.getId() != null);
        assertTrue(saved.getCreatedAt() != null);
        // BaseEntity has no table of its own, its columns are part of PERSON
        assertEquals(1, count("select count(*) from information_schema.columns where table_name = 'PERSON' and column_name = 'CREATED_AT'"));
        assertEquals(0, count("select count(*) from information_schema.tables where table_name = 'BASE_ENTITY'"));
    }

    // ---- 2. SINGLE_TABLE -----------------------------------------------------------------------

    @Test
    void singleTableKeepsAllPersonsInOneTableWithADiscriminator() {
        personRepository.save(new Person("Bors", 40));
        personRepository.save(new Knight("Percival", 25, "Knight", "Grey"));
        personRepository.save(new Wizard("Morgana", 50, 80, "Avalon"));
        flushAndClear();

        assertEquals(3, count("select count(*) from person"));
        assertEquals(1, count("select count(*) from person where person_type = 'KNIGHT'"));
        assertEquals(1, count("select count(*) from person where person_type = 'WIZARD'"));
        // a knight has no mana: that column is simply NULL in the shared table
        assertEquals(1, count("select count(*) from person where person_type = 'KNIGHT' and mana is null"));
    }

    @Test
    void singleTablePolymorphicQueryReturnsTheRealSubclasses() {
        personRepository.save(new Person("Bors", 40));
        personRepository.save(new Knight("Percival", 25, "Knight", "Grey"));
        personRepository.save(new Wizard("Morgana", 50, 80, "Avalon"));
        flushAndClear();

        List<Person> all = personRepository.findAll();
        assertEquals(3, all.size());
        assertEquals(1, all.stream().filter(p -> p instanceof Knight).count());
        assertEquals(1, all.stream().filter(p -> p instanceof Wizard).count());

        Person percival = personRepository.findByNameIgnoreCase("percival").orElseThrow();
        assertInstanceOf(Knight.class, percival);
        assertEquals("Grey", ((Knight) percival).getHorseName());
    }

    @Test
    void aSubclassRepositoryOnlySeesItsOwnRows() {
        personRepository.save(new Person("Bors", 40));
        personRepository.save(new Knight("Percival", 25, "Knight", "Grey"));
        personRepository.save(new Wizard("Morgana", 50, 80, "Avalon"));
        flushAndClear();

        List<Knight> knights = knightRepository.findAll();
        assertEquals(1, knights.size());
        assertEquals("Percival", knights.get(0).getName());
        assertEquals(1, knightRepository.findByKnightRank("Knight").size());
    }

    // ---- 3. JOINED -----------------------------------------------------------------------------

    @Test
    void joinedSplitsTheColumnsOverParentAndChildTables() {
        equipmentRepository.save(new Weapon("Dagger", 0.4, 30, "PIERCING"));
        equipmentRepository.save(new Armor("Shield", 6.0, 40, "Oak"));
        flushAndClear();

        assertEquals(2, count("select count(*) from equipment"));
        assertEquals(1, count("select count(*) from weapon"));
        assertEquals(1, count("select count(*) from armor"));
        // name lives in the parent table only, damage in the child table only
        assertEquals(1, count("select count(*) from information_schema.columns where table_name = 'EQUIPMENT' and column_name = 'NAME'"));
        assertEquals(0, count("select count(*) from information_schema.columns where table_name = 'WEAPON' and column_name = 'NAME'"));
        assertEquals(1, count("select count(*) from information_schema.columns where table_name = 'WEAPON' and column_name = 'DAMAGE'"));
    }

    @Test
    void joinedSubclassColumnsCanBeNotNull() {
        Weapon incomplete = new Weapon("Broken", 1.0, 10, null); // damageType is NOT NULL in table WEAPON
        assertThrows(Exception.class, () -> {
            equipmentRepository.save(incomplete);
            em.flush();
        });
    }

    @Test
    void joinedPolymorphicQueries() {
        equipmentRepository.save(new Weapon("Dagger", 0.4, 30, "PIERCING"));
        equipmentRepository.save(new Weapon("Axe", 3.0, 70, "SLASHING"));
        equipmentRepository.save(new Armor("Shield", 6.0, 40, "Oak"));
        flushAndClear();

        List<Equipment> everything = equipmentRepository.findAll();
        assertEquals(3, everything.size());
        assertEquals(2, equipmentRepository.findAllWeapons().size());
        assertEquals(1, weaponRepository.findByDamageGreaterThanEqual(50).size());
    }

    // ---- 4. TABLE_PER_CLASS --------------------------------------------------------------------

    @Test
    void tablePerClassHasNoTableForTheAbstractParent() {
        warMachineRepository.save(new Tank("Mark II", 5, 20));
        warMachineRepository.save(new Catapult("Ballista", 3, 120));
        flushAndClear();

        assertEquals(0, count("select count(*) from information_schema.tables where table_name = 'WAR_MACHINE'"));
        assertEquals(1, count("select count(*) from tank"));
        assertEquals(1, count("select count(*) from catapult"));
        // the "inherited" columns are repeated in every table
        assertEquals(1, count("select count(*) from information_schema.columns where table_name = 'TANK' and column_name = 'NAME'"));
        assertEquals(1, count("select count(*) from information_schema.columns where table_name = 'CATAPULT' and column_name = 'NAME'"));
    }

    @Test
    void tablePerClassPolymorphicQueryAndCustomRepositoryFragment() {
        warMachineRepository.save(new Tank("Mark II", 5, 20));
        warMachineRepository.save(new Catapult("Ballista", 3, 120));
        warMachineRepository.save(new Catapult("Trebuchet", 6, 300));
        flushAndClear();

        List<WarMachine> all = warMachineRepository.findAll(); // a UNION over both tables
        assertEquals(3, all.size());

        List<WarMachine> big = warMachineRepository.search("", 5); // our own implementation
        assertEquals(2, big.size());
        assertEquals("Mark II", big.get(0).getName());
        assertInstanceOf(Catapult.class, big.get(1));
    }
}
