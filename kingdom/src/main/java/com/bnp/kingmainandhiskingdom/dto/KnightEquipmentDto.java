package com.bnp.kingmainandhiskingdom.dto;

import java.util.List;

/** A knight and what he carries (exercise C4). */
public record KnightEquipmentDto(String knight, List<EquipmentDto> equipment) {
}
