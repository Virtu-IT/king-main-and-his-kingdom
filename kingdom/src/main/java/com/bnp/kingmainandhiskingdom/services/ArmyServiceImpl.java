package com.bnp.kingmainandhiskingdom.services;

import com.bnp.kingmainandhiskingdom.domain.Person;
import com.bnp.kingmainandhiskingdom.domain.Profession;
import com.bnp.kingmainandhiskingdom.exception.AlreadyExistsException;
import com.bnp.kingmainandhiskingdom.exception.PersonNotFoundException;
import com.bnp.kingmainandhiskingdom.repository.PersonRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ArmyServiceImpl implements ArmyService {

    private final PersonRepository personRepository;
    private final String defaultPersonName;
    private final int defaultPersonAge;

    public ArmyServiceImpl(PersonRepository personRepository,
                           @Value("${person.default-name}") String defaultPersonName,
                           @Value("${person.default-age}") int defaultPersonAge) {
        this.personRepository = personRepository;
        this.defaultPersonName = defaultPersonName;
        this.defaultPersonAge = defaultPersonAge;
    }

    @Override
    public Person recruitPerson(Person person) {
        if (person == null) {
            throw new IllegalArgumentException("Person cannot be null");
        }
        person.setProfession(null); // a profession is only given by training
        if (person.getName() == null) {
            person.setName(defaultPersonName);
        }
        if (personRepository.existsByNameIgnoreCase(person.getName())) {
            throw new AlreadyExistsException("Person with name '" + person.getName() + "' already exists");
        }
        return personRepository.save(person);
    }

    @Override
    public Person trainPerson(String name, Profession profession) {
        if (profession == null) {
            throw new IllegalArgumentException("Profession cannot be null");
        }
        Person person = personRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new PersonNotFoundException("Person with name '" + name + "' could not be found"));
        if (person.getAge() < profession.getMinimumAge()) {
            throw new IllegalArgumentException("Too young to become a " + profession + ": minimum age is "
                    + profession.getMinimumAge());
        }
        person.setProfession(profession); // the entity is managed: the change is saved when the transaction commits
        return person;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Person> getAllPersons() {
        return personRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Person findPersonByName(String name) {
        if (name == null) {
            return new Person(defaultPersonName, defaultPersonAge);
        }
        return personRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new PersonNotFoundException("Person with name '" + name + "' could not be found"));
    }

    @Override
    public Person updatePerson(String name, Person person) {
        if (person == null) {
            throw new IllegalArgumentException("Person cannot be null");
        }
        Person existing = findPersonByName(name);
        if (person.getName() != null && !person.getName().equalsIgnoreCase(existing.getName())
                && personRepository.existsByNameIgnoreCase(person.getName())) {
            throw new AlreadyExistsException("Person with name '" + person.getName() + "' already exists");
        }
        if (existing.getClass() != person.getClass()) {
            throw new IllegalArgumentException("The type of a person cannot be changed ("
                    + existing.getClass().getSimpleName() + " -> " + person.getClass().getSimpleName() + ")");
        }
        existing.copyFrom(person);
        return existing;
    }

    @Override
    public boolean deletePerson(String name) {
        if (name == null) {
            return false;
        }
        return personRepository.findByNameIgnoreCase(name)
                .map(p -> {
                    personRepository.delete(p);
                    return true;
                })
                .orElse(false);
    }
}
