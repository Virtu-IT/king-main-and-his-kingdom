package com.bnp.kingmainandhiskingdom.repository;

import com.bnp.kingmainandhiskingdom.domain.WarMachine;

import java.util.List;

/** A custom repository fragment: your own implementation (see the Impl class) mixed into the Spring Data repository. */
public interface WarMachineRepositoryCustom {
    List<WarMachine> search(String nameContains, int minCrewSize);
}
