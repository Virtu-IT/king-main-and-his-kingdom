package com.bnp.kingmainandhiskingdom.services;

import com.bnp.kingmainandhiskingdom.domain.Equipment;
import com.bnp.kingmainandhiskingdom.domain.Knight;
import com.bnp.kingmainandhiskingdom.domain.Person;
import com.bnp.kingmainandhiskingdom.domain.Profession;

import java.util.List;

public interface ArmyService {
    /** Adds a new person to the kingdom (without a profession). */
    Person recruitPerson(Person person);

    /** Takes an existing person from the database and trains them to a soldier, farmer or nurse. */
    Person trainPerson(String name, Profession profession);

    /** C6: takes the profession away again (needed before someone can become a soldier after another profession). */
    Person releasePerson(String name);

    /** C4: the equipment a knight carries. */
    List<Equipment> equipmentOf(String name);

    /** C4: gives a piece of equipment to a knight; it can only be carried by one knight at a time. */
    List<Equipment> giveEquipment(String name, Long equipmentId);

    /** C4: all knights with their equipment, loaded with one query. */
    List<Knight> knightsWithEquipment();

    List<Person> getAllPersons();
    Person findPersonByName(String name);
    Person updatePerson(String name, Person person);
    boolean deletePerson(String name);
}
