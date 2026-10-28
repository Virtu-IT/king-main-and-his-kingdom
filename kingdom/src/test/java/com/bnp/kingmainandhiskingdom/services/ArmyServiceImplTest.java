package com.bnp.kingmainandhiskingdom.services;

import com.bnp.kingmainandhiskingdom.domain.Knight;
import com.bnp.kingmainandhiskingdom.domain.Person;
import com.bnp.kingmainandhiskingdom.domain.Profession;
import com.bnp.kingmainandhiskingdom.domain.Wizard;
import com.bnp.kingmainandhiskingdom.exception.AlreadyExistsException;
import com.bnp.kingmainandhiskingdom.exception.ConflictException;
import com.bnp.kingmainandhiskingdom.exception.PersonNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ArmyServiceImplTest {

    @Autowired
    private ArmyService armyService;

    @Test
    void recruitPerson() {
        Person person = armyService.recruitPerson(new Person("Tom", 21));
        assertNotNull(person.getId());
        assertAll(
                () -> assertEquals("Tom", person.getName()),
                () -> assertEquals(21, person.getAge()),
                () -> assertNull(person.getProfession()));
    }

    @Test
    void recruitIgnoresAProfessionThatWasSent() {
        Person sneaky = new Person("Sneaky", 30);
        sneaky.setProfession(Profession.SOLDIER);
        assertNull(armyService.recruitPerson(sneaky).getProfession());
    }

    @Test
    void trainPersonGivesAnExistingPersonAProfession() {
        armyService.recruitPerson(new Person("Recruit", 25));

        Person trained = armyService.trainPerson("recruit", Profession.SOLDIER);

        assertEquals(Profession.SOLDIER, trained.getProfession());
        // it is stored: another lookup sees the profession
        assertEquals(Profession.SOLDIER, armyService.findPersonByName("Recruit").getProfession());
    }

    @Test
    void aPersonCanBeRetrained() {
        armyService.recruitPerson(new Person("Changer", 30));
        armyService.trainPerson("Changer", Profession.SOLDIER);
        assertEquals(Profession.NURSE, armyService.trainPerson("Changer", Profession.NURSE).getProfession());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when the person is too young for the profession")
    void trainPersonWhoIsTooYoung() {
        armyService.recruitPerson(new Person("Youngster", 17));

        assertThrows(IllegalArgumentException.class, () -> armyService.trainPerson("Youngster", Profession.SOLDIER));
        assertThrows(IllegalArgumentException.class, () -> armyService.trainPerson("Youngster", Profession.NURSE));
        // but a 17-year-old may become a farmer
        assertEquals(Profession.FARMER, armyService.trainPerson("Youngster", Profession.FARMER).getProfession());
    }

    @Test
    void aWizardCanNeverBecomeASoldier() {
        armyService.recruitPerson(new Wizard("Gandalf", 80, 100, "Istari"));

        assertThrows(IllegalArgumentException.class, () -> armyService.trainPerson("Gandalf", Profession.SOLDIER));
        assertEquals(Profession.NURSE, armyService.trainPerson("Gandalf", Profession.NURSE).getProfession());
    }

    @Test
    void aSoldierCanOnlyBeTrainedFromScratch_releaseFirst() {
        armyService.recruitPerson(new Person("Farmer Giles", 40));
        armyService.trainPerson("Farmer Giles", Profession.FARMER);

        assertThrows(ConflictException.class, () -> armyService.trainPerson("Farmer Giles", Profession.SOLDIER));

        assertNull(armyService.releasePerson("Farmer Giles").getProfession());
        assertEquals(Profession.SOLDIER, armyService.trainPerson("Farmer Giles", Profession.SOLDIER).getProfession());
        // already a soldier: again is a conflict, not a silent no-op
        assertThrows(ConflictException.class, () -> armyService.trainPerson("Farmer Giles", Profession.SOLDIER));
    }

    @Test
    void aBlacksmithNeedsToBe18() {
        armyService.recruitPerson(new Person("Apprentice", 17));
        assertThrows(IllegalArgumentException.class, () -> armyService.trainPerson("Apprentice", Profession.BLACKSMITH));
        armyService.recruitPerson(new Person("Smith", 30));
        assertEquals(Profession.BLACKSMITH, armyService.trainPerson("Smith", Profession.BLACKSMITH).getProfession());
    }

    @Test
    void onlyKnightsCarryEquipment() {
        armyService.recruitPerson(new Person("Walker", 30));
        assertThrows(IllegalArgumentException.class, () -> armyService.equipmentOf("Walker"));
        assertEquals(2, armyService.equipmentOf("Arthur").size());
    }

    @Test
    void aPieceOfEquipmentHasOneOwner() {
        armyService.recruitPerson(new Knight("Gaheris", 25, "Squire", "Dusty"));
        Long sword = armyService.equipmentOf("Arthur").get(0).getId();

        assertThrows(ConflictException.class, () -> armyService.giveEquipment("Gaheris", sword));
        assertThrows(com.bnp.kingmainandhiskingdom.exception.EquipmentNotFoundException.class,
                () -> armyService.giveEquipment("Gaheris", 424242L));
    }

    @Test
    void trainUnknownPersonThrowsNotFound() {
        assertThrows(PersonNotFoundException.class, () -> armyService.trainPerson("Nobody", Profession.FARMER));
    }

    @Test
    void updateDoesNotChangeTheProfession() {
        armyService.recruitPerson(new Person("Stable", 30));
        armyService.trainPerson("Stable", Profession.NURSE);

        Person updated = armyService.updatePerson("Stable", new Person("Stable", 31));

        assertEquals(31, updated.getAge());
        assertEquals(Profession.NURSE, updated.getProfession());
    }

    @Test
    void recruitTwiceIsRejected() {
        armyService.recruitPerson(new Person("Twin", 30));
        assertThrows(AlreadyExistsException.class, () -> armyService.recruitPerson(new Person("TWIN", 31)));
    }

    @Test
    void subclassesKeepTheirOwnFields() {
        armyService.recruitPerson(new Knight("Gareth", 24 + 1, "Squire", "Brownie"));
        armyService.recruitPerson(new Wizard("Nimue", 70, 90, "Lake"));

        assertEquals("Brownie", ((Knight) armyService.findPersonByName("gareth")).getHorseName());
        assertEquals(90, ((Wizard) armyService.findPersonByName("nimue")).getMana());
    }

    @Test
    void updateChangesAgeAndSubclassFields() {
        armyService.recruitPerson(new Knight("Kay", 30, "Squire", "Old"));
        Person updated = armyService.updatePerson("Kay", new Knight("Kay", 31, "Knight", "New"));

        assertEquals(31, updated.getAge());
        assertEquals("Knight", ((Knight) updated).getKnightRank());
        assertEquals("New", ((Knight) updated).getHorseName());
    }

    @Test
    void theTypeOfAPersonCannotChange() {
        armyService.recruitPerson(new Person("Plain", 30));
        assertThrows(IllegalArgumentException.class, () -> armyService.updatePerson("Plain", new Knight("Plain", 30, "Knight", "x")));
    }

    @Test
    void deleteUnknownPersonReturnsFalseAndFindThrows() {
        assertFalse(armyService.deletePerson("Nobody"));
        assertThrows(PersonNotFoundException.class, () -> armyService.findPersonByName("Nobody"));
    }

    @Test
    void missingNameGivesTheDefaultPerson() {
        Person person = armyService.findPersonByName(null);
        assertEquals("John", person.getName());
        assertEquals(20, person.getAge());
    }
}
