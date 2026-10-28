package com.bnp.kingmainandhiskingdom.services;

import com.bnp.kingmainandhiskingdom.repository.EquipmentRepository;
import com.bnp.kingmainandhiskingdom.repository.PersonRepository;
import com.bnp.kingmainandhiskingdom.repository.WarMachineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class KingdomStatsService {

    private final PersonRepository persons;
    private final EquipmentRepository equipment;
    private final WarMachineRepository warMachines;

    public KingdomStatsService(PersonRepository persons, EquipmentRepository equipment, WarMachineRepository warMachines) {
        this.persons = persons;
        this.equipment = equipment;
        this.warMachines = warMachines;
    }

    public Stats stats() {
        return new Stats(persons.count(), equipment.count(), warMachines.count());
    }

    public record Stats(long persons, long equipment, long warMachines) {
    }
}
