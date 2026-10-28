package com.bnp.kingmainandhiskingdom.repository;

import com.bnp.kingmainandhiskingdom.domain.WarMachine;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

/** The class name must be the fragment interface name + "Impl" for Spring Data to pick it up. */
public class WarMachineRepositoryCustomImpl implements WarMachineRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<WarMachine> search(String nameContains, int minCrewSize) {
        String name = nameContains == null ? "" : nameContains.toLowerCase();
        return entityManager.createQuery(
                        "select w from WarMachine w where lower(w.name) like :name and w.crewSize >= :crew order by w.name",
                        WarMachine.class)
                .setParameter("name", "%" + name + "%")
                .setParameter("crew", minCrewSize)
                .getResultList();
    }
}
