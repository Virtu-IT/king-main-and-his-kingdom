package com.bnp.kingmainandhiskingdom.controller;

import com.bnp.kingmainandhiskingdom.domain.Profession;
import com.bnp.kingmainandhiskingdom.dto.Mappers;
import com.bnp.kingmainandhiskingdom.dto.PersonDto;
import com.bnp.kingmainandhiskingdom.exception.PersonNotFoundException;
import com.bnp.kingmainandhiskingdom.services.ArmyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Reading needs a login (see SecurityConfig: /api/** is authenticated); changing needs the KING role
 * (method security); training also needs the KNIGHT role. A logged-in subject who tries it gets 403, an anonymous caller gets 401.
 */
@RestController
@RequestMapping(path = "/api/persons", produces = MediaType.APPLICATION_JSON_VALUE)
public class PersonRestController {

    private final ArmyService armyService;

    public PersonRestController(ArmyService armyService) {
        this.armyService = armyService;
    }

    @GetMapping
    public List<PersonDto> getAllPersons() {
        return armyService.getAllPersons().stream().map(Mappers::toDto).toList();
    }

    @GetMapping("/{name}")
    public ResponseEntity<PersonDto> getPersonByName(@PathVariable String name) {
        return ResponseEntity.ok(Mappers.toDto(armyService.findPersonByName(name)));
    }

    @GetMapping("/query")
    public ResponseEntity<PersonDto> queryPersonByName(@RequestParam String name) {
        return ResponseEntity.ok(Mappers.toDto(armyService.findPersonByName(name)));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('KING')")
    public ResponseEntity<PersonDto> createPerson(@Valid @RequestBody PersonDto person) {
        return new ResponseEntity<>(Mappers.toDto(armyService.recruitPerson(Mappers.toEntity(person))), HttpStatus.CREATED);
    }

    /** Trains an existing person to a soldier, farmer or nurse (the king and the knights may train). */
    @PostMapping(path = "/{name}/training", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('KING', 'KNIGHT')")
    public PersonDto trainPerson(@PathVariable String name, @Valid @RequestBody TrainingRequest request) {
        return Mappers.toDto(armyService.trainPerson(name, request.profession()));
    }

    public record TrainingRequest(@NotNull(message = "Profession is required") Profession profession) {
    }

    @PutMapping(path = "/{name}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('KING')")
    public ResponseEntity<PersonDto> updatePerson(@PathVariable String name, @Valid @RequestBody PersonDto person) {
        return ResponseEntity.ok(Mappers.toDto(armyService.updatePerson(name, Mappers.toEntity(person))));
    }

    @DeleteMapping("/{name}")
    @PreAuthorize("hasRole('KING')")
    public ResponseEntity<Void> deletePerson(@PathVariable String name) {
        if (!armyService.deletePerson(name)) {
            throw new PersonNotFoundException("Person with name '" + name + "' could not be found");
        }
        return ResponseEntity.noContent().build();
    }
}
