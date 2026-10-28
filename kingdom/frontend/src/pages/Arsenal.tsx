import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { ApiError, api } from '../api';
import { ErrorBox } from '../components/ErrorBox';
import type { Equipment, EquipmentType, WarMachine, WarMachineType } from '../models';

export default function Arsenal() {
  const [equipment, setEquipment] = useState<Equipment[]>([]);
  const [machines, setMachines] = useState<WarMachine[]>([]);
  const [error, setError] = useState<ApiError | null>(null);
  const [message, setMessage] = useState('');

  const [query, setQuery] = useState('');
  const [minCrew, setMinCrew] = useState(0);

  const [eType, setEType] = useState<EquipmentType>('WEAPON');
  const [eName, setEName] = useState('');
  const [eWeight, setEWeight] = useState('1');
  const [eDamage, setEDamage] = useState('40');
  const [eDamageType, setEDamageType] = useState('SLASHING');
  const [eDefense, setEDefense] = useState('30');
  const [eMaterial, setEMaterial] = useState('Iron');

  const [mType, setMType] = useState<WarMachineType>('CATAPULT');
  const [mName, setMName] = useState('');
  const [mCrew, setMCrew] = useState('4');
  const [mArmor, setMArmor] = useState('10');
  const [mRange, setMRange] = useState('200');

  const loadEquipment = useCallback(async () => {
    try {
      setEquipment(await api.equipment());
    } catch (e) {
      setError(e as ApiError);
    }
  }, []);

  useEffect(() => {
    void loadEquipment();
  }, [loadEquipment]);

  // search the war machines again whenever the filter changes (ignoring an answer that arrives too late)
  const [reload, setReload] = useState(0);
  useEffect(() => {
    let current = true;
    api
      .warMachines(query, minCrew)
      .then((list) => current && setMachines(list))
      .catch((e) => current && setError(e as ApiError));
    return () => {
      current = false;
    };
  }, [query, minCrew, reload]);

  async function run(call: () => Promise<unknown>, ok: string, after: () => void) {
    setError(null);
    setMessage('');
    try {
      await call();
      setMessage(ok);
      after();
    } catch (e) {
      setError(e as ApiError);
    }
  }

  function addEquipment(event: FormEvent) {
    event.preventDefault();
    const item: Equipment =
      eType === 'WEAPON'
        ? { type: 'WEAPON', name: eName.trim(), weight: Number(eWeight), damage: Number(eDamage), damageType: eDamageType }
        : { type: 'ARMOR', name: eName.trim(), weight: Number(eWeight), defense: Number(eDefense), material: eMaterial };
    void run(() => api.addEquipment(item), `${item.name} added to the equipment.`, () => {
      setEName('');
      void loadEquipment();
    });
  }

  function addMachine(event: FormEvent) {
    event.preventDefault();
    const machine: WarMachine =
      mType === 'TANK'
        ? { type: 'TANK', name: mName.trim(), crewSize: Number(mCrew), armorThicknessMm: Number(mArmor) }
        : { type: 'CATAPULT', name: mName.trim(), crewSize: Number(mCrew), rangeMeters: Number(mRange) };
    void run(() => api.addWarMachine(machine), `${machine.name} was built.`, () => {
      setMName('');
      setReload((n) => n + 1);
    });
  }

  return (
    <>
      <ErrorBox error={error} />
      {message && <div className="status ok">{message}</div>}

      <div className="two-col">
        <div className="card panel">
          <h2>🗡️ Equipment</h2>
          <p className="muted">
            <strong>JOINED</strong>: shared columns in table <code>equipment</code>, weapon and armor columns in{' '}
            <code>weapon</code> / <code>armor</code>. Knights and the king may add.
          </p>
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Weight</th>
                <th>Details</th>
              </tr>
            </thead>
            <tbody>
              {equipment.map((e) => (
                <tr key={e.id}>
                  <td>
                    <strong>{e.name}</strong>
                    <br />
                    <span className="tag">{e.type}</span>
                  </td>
                  <td>{e.weight} kg</td>
                  <td className="muted">
                    {e.type === 'WEAPON' ? `damage ${e.damage} · ${e.damageType}` : `defense ${e.defense} · ${e.material}`}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <form className="card panel" onSubmit={addEquipment}>
          <h2>Add equipment</h2>
          <div className="grid">
            <label className="wide">
              Type
              <select value={eType} onChange={(e) => setEType(e.target.value as EquipmentType)}>
                <option value="WEAPON">Weapon</option>
                <option value="ARMOR">Armor</option>
              </select>
            </label>
            <label>
              Name
              <input value={eName} onChange={(e) => setEName(e.target.value)} required />
            </label>
            <label>
              Weight (kg)
              <input type="number" step="0.1" value={eWeight} onChange={(e) => setEWeight(e.target.value)} required />
            </label>
            {eType === 'WEAPON' ? (
              <>
                <label>
                  Damage
                  <input type="number" value={eDamage} onChange={(e) => setEDamage(e.target.value)} required />
                </label>
                <label>
                  Damage type
                  <input value={eDamageType} onChange={(e) => setEDamageType(e.target.value)} required />
                </label>
              </>
            ) : (
              <>
                <label>
                  Defense
                  <input type="number" value={eDefense} onChange={(e) => setEDefense(e.target.value)} required />
                </label>
                <label>
                  Material
                  <input value={eMaterial} onChange={(e) => setEMaterial(e.target.value)} required />
                </label>
              </>
            )}
          </div>
          <button type="submit" className="btn" disabled={!eName.trim()}>
            Add
          </button>
        </form>
      </div>

      <div className="two-col spaced">
        <div className="card panel">
          <h2>💣 War machines</h2>
          <p className="muted">
            <strong>TABLE_PER_CLASS</strong>: no table <code>war_machine</code>; <code>tank</code> and{' '}
            <code>catapult</code> each hold every column. The search is a custom repository fragment. Only the king may
            add.
          </p>
          <div className="grid">
            <label>
              Name contains
              <input value={query} onChange={(e) => setQuery(e.target.value)} />
            </label>
            <label>
              Min. crew
              <input type="number" min={0} value={minCrew} onChange={(e) => setMinCrew(Number(e.target.value) || 0)} />
            </label>
          </div>
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Crew</th>
                <th>Details</th>
              </tr>
            </thead>
            <tbody>
              {machines.map((m) => (
                <tr key={m.id}>
                  <td>
                    <strong>{m.name}</strong>
                    <br />
                    <span className="tag">{m.type}</span>
                  </td>
                  <td>{m.crewSize}</td>
                  <td className="muted">
                    {m.type === 'TANK' ? `armor ${m.armorThicknessMm} mm` : `range ${m.rangeMeters} m`}
                  </td>
                </tr>
              ))}
              {machines.length === 0 && (
                <tr>
                  <td colSpan={3} className="muted">
                    No war machines match.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <form className="card panel" onSubmit={addMachine}>
          <h2>Build a war machine</h2>
          <div className="grid">
            <label className="wide">
              Type
              <select value={mType} onChange={(e) => setMType(e.target.value as WarMachineType)}>
                <option value="CATAPULT">Catapult</option>
                <option value="TANK">Tank</option>
              </select>
            </label>
            <label>
              Name
              <input value={mName} onChange={(e) => setMName(e.target.value)} required />
            </label>
            <label>
              Crew
              <input type="number" value={mCrew} onChange={(e) => setMCrew(e.target.value)} required />
            </label>
            {mType === 'TANK' ? (
              <label>
                Armor (mm)
                <input type="number" value={mArmor} onChange={(e) => setMArmor(e.target.value)} required />
              </label>
            ) : (
              <label>
                Range (m)
                <input type="number" value={mRange} onChange={(e) => setMRange(e.target.value)} required />
              </label>
            )}
          </div>
          <button type="submit" className="btn" disabled={!mName.trim()}>
            Build
          </button>
        </form>
      </div>
    </>
  );
}
