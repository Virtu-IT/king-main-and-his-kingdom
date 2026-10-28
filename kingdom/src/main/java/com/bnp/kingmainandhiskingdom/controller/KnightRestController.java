package com.bnp.kingmainandhiskingdom.controller;

import com.bnp.kingmainandhiskingdom.dto.KnightEquipmentDto;
import com.bnp.kingmainandhiskingdom.dto.Mappers;
import com.bnp.kingmainandhiskingdom.services.ArmyService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Exercise C4: all knights with their equipment, loaded with ONE query (see KnightRepository.findAllWithEquipment). */
@RestController
@RequestMapping(path = "/api/knights", produces = MediaType.APPLICATION_JSON_VALUE)
public class KnightRestController {

    private final ArmyService armyService;

    public KnightRestController(ArmyService armyService) {
        this.armyService = armyService;
    }

    @GetMapping
    public List<KnightEquipmentDto> knights() {
        return armyService.knightsWithEquipment().stream()
                .map(k -> new KnightEquipmentDto(k.getName(), k.getEquipment().stream().map(Mappers::toDto).toList()))
                .toList();
    }
}
