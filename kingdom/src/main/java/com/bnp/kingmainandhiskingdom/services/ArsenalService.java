package com.bnp.kingmainandhiskingdom.services;

import com.bnp.kingmainandhiskingdom.domain.Equipment;
import com.bnp.kingmainandhiskingdom.domain.WarMachine;
import com.bnp.kingmainandhiskingdom.repository.EquipmentRepository;
import com.bnp.kingmainandhiskingdom.repository.WarMachineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** The royal arsenal: equipment (JOINED inheritance) and war machines (TABLE_PER_CLASS inheritance). */
@Service
@Transactional
public class ArsenalService {

    private final EquipmentRepository equipmentRepository;
    private final WarMachineRepository warMachineRepository;

    public ArsenalService(EquipmentRepository equipmentRepository, WarMachineRepository warMachineRepository) {
        this.equipmentRepository = equipmentRepository;
        this.warMachineRepository = warMachineRepository;
    }

    @Transactional(readOnly = true)
    public List<Equipment> allEquipment() {
        return equipmentRepository.findAll();
    }

    public Equipment addEquipment(Equipment equipment) {
        return equipmentRepository.save(equipment);
    }

    @Transactional(readOnly = true)
    public List<WarMachine> searchWarMachines(String nameContains, int minCrewSize) {
        return warMachineRepository.search(nameContains, minCrewSize);
    }

    public WarMachine addWarMachine(WarMachine warMachine) {
        return warMachineRepository.save(warMachine);
    }
}
