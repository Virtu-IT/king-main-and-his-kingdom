package com.bnp.kingmainandhiskingdom.services;

import com.bnp.kingmainandhiskingdom.domain.Person;
import com.bnp.kingmainandhiskingdom.domain.Profession;

import java.util.List;

public interface ArmyService {
    /** Adds a new person to the kingdom (without a profession). */
    Person recruitPerson(Person person);

    /** Takes an existing person from the database and trains them to a soldier, farmer or nurse. */
    Person trainPerson(String name, Profession profession);

    List<Person> getAllPersons();
    Person findPersonByName(String name);
    Person updatePerson(String name, Person person);
    boolean deletePerson(String name);
}
