package com.bnp.kingmainandhiskingdom.controller;

import com.bnp.kingmainandhiskingdom.dto.EquipmentDto;
import com.bnp.kingmainandhiskingdom.dto.Mappers;
import com.bnp.kingmainandhiskingdom.dto.WarMachineDto;
import com.bnp.kingmainandhiskingdom.services.ArsenalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Everybody logged in may look at the arsenal; the king, his knights and the steward may add equipment. */
@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class ArsenalRestController {

    private final ArsenalService arsenalService;

    public ArsenalRestController(ArsenalService arsenalService) {
        this.arsenalService = arsenalService;
    }

    @GetMapping("/equipment")
    public List<EquipmentDto> equipment() {
        return arsenalService.allEquipment().stream().map(Mappers::toDto).toList();
    }

    @PostMapping(path = "/equipment", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('KING', 'KNIGHT', 'STEWARD')")
    public ResponseEntity<EquipmentDto> addEquipment(@Valid @RequestBody EquipmentDto equipment) {
        return new ResponseEntity<>(Mappers.toDto(arsenalService.addEquipment(Mappers.toEntity(equipment))), HttpStatus.CREATED);
    }

    @GetMapping("/war-machines")
    public List<WarMachineDto> warMachines(@RequestParam(defaultValue = "") String name,
                                        @RequestParam(defaultValue = "0") int minCrew) {
        return arsenalService.searchWarMachines(name, minCrew).stream().map(Mappers::toDto).toList();
    }

    @PostMapping(path = "/war-machines", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('KING')")
    public ResponseEntity<WarMachineDto> addWarMachine(@Valid @RequestBody WarMachineDto warMachine) {
        return new ResponseEntity<>(Mappers.toDto(arsenalService.addWarMachine(Mappers.toEntity(warMachine))), HttpStatus.CREATED);
    }
}
