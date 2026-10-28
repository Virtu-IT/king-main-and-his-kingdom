package com.bnp.kingmainandhiskingdom.controller;

import com.bnp.kingmainandhiskingdom.dto.BuildingDto;
import com.bnp.kingmainandhiskingdom.dto.Mappers;
import com.bnp.kingmainandhiskingdom.repository.BuildingRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Exercise C3: everybody logged in may look at the buildings; only the king builds. */
@RestController
@RequestMapping(path = "/api/buildings", produces = MediaType.APPLICATION_JSON_VALUE)
public class BuildingRestController {

    private final BuildingRepository buildings;

    public BuildingRestController(BuildingRepository buildings) {
        this.buildings = buildings;
    }

    @GetMapping
    public List<BuildingDto> all() {
        return buildings.findAll().stream().map(Mappers::toDto).toList();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('KING')")
    public ResponseEntity<BuildingDto> build(@Valid @RequestBody BuildingDto building) {
        return new ResponseEntity<>(Mappers.toDto(buildings.save(Mappers.toEntity(building))), HttpStatus.CREATED);
    }
}
