package com.bnp.kingmainandhiskingdom.repository;

import com.bnp.kingmainandhiskingdom.domain.WarMachine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarMachineRepository extends JpaRepository<WarMachine, Long>, WarMachineRepositoryCustom {
}
