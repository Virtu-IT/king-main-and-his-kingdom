export interface TokenResponse {
  token: string;
  tokenType: string;
  expiresAt: string;
  username: string;
  roles: string[];
}

export interface UserInfo {
  username: string;
  roles: string[];
}

export type PersonType = 'PERSON' | 'KNIGHT' | 'WIZARD';

export type Profession = 'SOLDIER' | 'FARMER' | 'NURSE';

/** One interface for the whole SINGLE_TABLE hierarchy: the subclass fields are simply optional. */
export interface Person {
  id?: number;
  type: PersonType;
  name: string;
  age: number;
  profession?: Profession | null;
  knightRank?: string;
  horseName?: string;
  mana?: number;
  school?: string;
}

export type EquipmentType = 'WEAPON' | 'ARMOR';

export interface Equipment {
  id?: number;
  type: EquipmentType;
  name: string;
  weight: number;
  damage?: number;
  damageType?: string;
  defense?: number;
  material?: string;
}

export type WarMachineType = 'TANK' | 'CATAPULT';

export interface WarMachine {
  id?: number;
  type: WarMachineType;
  name: string;
  crewSize: number;
  armorThicknessMm?: number;
  rangeMeters?: number;
}
