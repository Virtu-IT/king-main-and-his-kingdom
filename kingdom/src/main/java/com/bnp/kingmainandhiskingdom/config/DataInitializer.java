package com.bnp.kingmainandhiskingdom.config;

import com.bnp.kingmainandhiskingdom.domain.Armor;
import com.bnp.kingmainandhiskingdom.domain.Catapult;
import com.bnp.kingmainandhiskingdom.domain.Knight;
import com.bnp.kingmainandhiskingdom.domain.Person;
import com.bnp.kingmainandhiskingdom.domain.Profession;
import com.bnp.kingmainandhiskingdom.domain.Tank;
import com.bnp.kingmainandhiskingdom.domain.Weapon;
import com.bnp.kingmainandhiskingdom.domain.Wizard;
import com.bnp.kingmainandhiskingdom.repository.EquipmentRepository;
import com.bnp.kingmainandhiskingdom.repository.PersonRepository;
import com.bnp.kingmainandhiskingdom.repository.WarMachineRepository;
import com.bnp.kingmainandhiskingdom.security.AccountService;
import com.bnp.kingmainandhiskingdom.security.AppUserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Fills the (in-memory) database at startup. Switch it off with kingdom.seed.enabled=false.
 * The demo logins are DEMO ONLY: real passwords never belong in source code.
 */
@Component
@ConditionalOnProperty(name = "kingdom.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements ApplicationRunner {

    private final PersonRepository persons;
    private final EquipmentRepository equipment;
    private final WarMachineRepository warMachines;
    private final AppUserRepository users;
    private final AccountService accounts;

    public DataInitializer(PersonRepository persons, EquipmentRepository equipment, WarMachineRepository warMachines,
                           AppUserRepository users, AccountService accounts) {
        this.persons = persons;
        this.equipment = equipment;
        this.warMachines = warMachines;
        this.users = users;
        this.accounts = accounts;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (persons.count() == 0) {
            persons.save(new Knight("Arthur", 35, "King", "Llamrei"));
            Knight lancelot = new Knight("Lancelot", 30, "Captain", "Aspencade");
            lancelot.setProfession(Profession.SOLDIER);
            persons.save(lancelot);
            persons.save(new Person("Guinevere", 28));
            persons.save(new Wizard("Merlin", 65, 100, "Avalon"));
        }
        if (equipment.count() == 0) {
            equipment.save(new Weapon("Excalibur", 1.6, 95, "SLASHING"));
            equipment.save(new Weapon("Longbow", 1.1, 60, "PIERCING"));
            equipment.save(new Armor("Plate armor", 25.0, 80, "Steel"));
            equipment.save(new Armor("Leather vest", 4.5, 25, "Leather"));
        }
        if (warMachines.count() == 0) {
            warMachines.save(new Tank("Mark I", 8, 12));
            warMachines.save(new Catapult("Trebuchet", 6, 300));
            warMachines.save(new Catapult("Mangonel", 4, 150));
        }
        if (users.count() == 0) {
            accounts.create("king", "king1234", Set.of(AccountService.ROLE_KING, AccountService.ROLE_KNIGHT));
            accounts.create("lancelot", "lancelot1234", Set.of(AccountService.ROLE_KNIGHT));
            accounts.create("peasant", "peasant1234", Set.of(AccountService.ROLE_SUBJECT));
        }
    }
}
