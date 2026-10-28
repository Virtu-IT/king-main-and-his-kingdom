package com.bnp.kingmainandhiskingdom.dto;

import com.bnp.kingmainandhiskingdom.domain.Armor;
import com.bnp.kingmainandhiskingdom.domain.Building;
import com.bnp.kingmainandhiskingdom.domain.Castle;
import com.bnp.kingmainandhiskingdom.domain.Farm;
import com.bnp.kingmainandhiskingdom.domain.Peasant;
import com.bnp.kingmainandhiskingdom.domain.Tower;
import com.bnp.kingmainandhiskingdom.domain.Catapult;
import com.bnp.kingmainandhiskingdom.domain.Equipment;
import com.bnp.kingmainandhiskingdom.domain.Knight;
import com.bnp.kingmainandhiskingdom.domain.Person;
import com.bnp.kingmainandhiskingdom.domain.Tank;
import com.bnp.kingmainandhiskingdom.domain.WarMachine;
import com.bnp.kingmainandhiskingdom.domain.Weapon;
import com.bnp.kingmainandhiskingdom.domain.Wizard;

/** Hand-written entity <-> DTO mapping (MapStruct would generate the same). */
public final class Mappers {

    private Mappers() {
    }

    public static PersonDto toDto(Person person) {
        return switch (person) {
            case Knight k -> new PersonDto.Knight(k.getId(), k.getName(), k.getAge(), k.getProfession(), k.getKnightRank(), k.getHorseName());
            case Wizard w -> new PersonDto.Wizard(w.getId(), w.getName(), w.getAge(), w.getProfession(), w.getMana(), w.getSchool());
            case Peasant p -> new PersonDto.Peasant(p.getId(), p.getName(), p.getAge(), p.getProfession(), p.getCrop());
            default -> new PersonDto.Plain(person.getId(), person.getName(), person.getAge(), person.getProfession());
        };
    }

    public static Person toEntity(PersonDto dto) {
        return switch (dto) {
            case PersonDto.Knight k -> new Knight(k.name(), k.age(), k.knightRank(), k.horseName());
            case PersonDto.Wizard w -> new Wizard(w.name(), w.age(), w.mana(), w.school());
            case PersonDto.Peasant p -> new Peasant(p.name(), p.age(), p.crop());
            case PersonDto.Plain p -> new Person(p.name(), p.age());
        };
    }

    public static BuildingDto toDto(Building building) {
        return switch (building) {
            case Castle c -> new BuildingDto.Castle(c.getId(), c.getName(), c.getTowers(), c.getHasMoat());
            case Farm f -> new BuildingDto.Farm(f.getId(), f.getName(), f.getHectares(), f.getCrop());
            case Tower t -> new BuildingDto.Tower(t.getId(), t.getName(), t.getHeightMeters(), t.getCrewSize());
            default -> throw new IllegalArgumentException("Unknown building " + building.getClass().getSimpleName());
        };
    }

    public static Building toEntity(BuildingDto dto) {
        return switch (dto) {
            case BuildingDto.Castle c -> new Castle(c.name(), c.towers(), c.hasMoat());
            case BuildingDto.Farm f -> new Farm(f.name(), f.hectares(), f.crop());
            case BuildingDto.Tower t -> new Tower(t.name(), t.heightMeters(), t.crewSize());
        };
    }

    public static EquipmentDto toDto(Equipment equipment) {
        return switch (equipment) {
            case Weapon w -> new EquipmentDto.Weapon(w.getId(), w.getName(), w.getWeight(), w.getDamage(), w.getDamageType());
            case Armor a -> new EquipmentDto.Armor(a.getId(), a.getName(), a.getWeight(), a.getDefense(), a.getMaterial());
            default -> throw new IllegalArgumentException("Unknown equipment " + equipment.getClass().getSimpleName());
        };
    }

    public static Equipment toEntity(EquipmentDto dto) {
        return switch (dto) {
            case EquipmentDto.Weapon w -> new Weapon(w.name(), w.weight(), w.damage(), w.damageType());
            case EquipmentDto.Armor a -> new Armor(a.name(), a.weight(), a.defense(), a.material());
        };
    }

    public static WarMachineDto toDto(WarMachine machine) {
        return switch (machine) {
            case Tank t -> new WarMachineDto.Tank(t.getId(), t.getName(), t.getCrewSize(), t.getArmorThicknessMm());
            case Catapult c -> new WarMachineDto.Catapult(c.getId(), c.getName(), c.getCrewSize(), c.getRangeMeters());
            default -> throw new IllegalArgumentException("Unknown war machine " + machine.getClass().getSimpleName());
        };
    }

    public static WarMachine toEntity(WarMachineDto dto) {
        return switch (dto) {
            case WarMachineDto.Tank t -> new Tank(t.name(), t.crewSize(), t.armorThicknessMm());
            case WarMachineDto.Catapult c -> new Catapult(c.name(), c.crewSize(), c.rangeMeters());
        };
    }
}
