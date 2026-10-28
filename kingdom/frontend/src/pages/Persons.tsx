import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { ApiError, api } from '../api';
import { ErrorBox } from '../components/ErrorBox';
import type { Person, PersonType, Profession } from '../models';

const ICONS: Record<PersonType, string> = { PERSON: '🧑', KNIGHT: '⚔️', WIZARD: '🧙' };
const PROFESSIONS: Profession[] = ['SOLDIER', 'FARMER', 'NURSE'];

export default function Persons() {
  const [persons, setPersons] = useState<Person[]>([]);
  const [error, setError] = useState<ApiError | null>(null);
  const [message, setMessage] = useState('');
  // the profession chosen in the dropdown of each row (by name); without a choice the dropdown shows the current one
  const [choices, setChoices] = useState<Record<string, Profession>>({});

  const [type, setType] = useState<PersonType>('KNIGHT');
  const [name, setName] = useState('');
  const [age, setAge] = useState('25');
  const [knightRank, setKnightRank] = useState('');
  const [horseName, setHorseName] = useState('');
  const [mana, setMana] = useState('50');
  const [school, setSchool] = useState('');

  const load = useCallback(async () => {
    try {
      setPersons(await api.persons());
    } catch (e) {
      setError(e as ApiError);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function run(call: () => Promise<unknown>, ok: string, after?: () => void) {
    setError(null);
    setMessage('');
    try {
      await call();
      setMessage(ok);
      after?.();
      await load();
    } catch (e) {
      setError(e as ApiError);
    }
  }

  function recruit(event: FormEvent) {
    event.preventDefault();
    const person: Person = { type, name: name.trim(), age: Number(age) };
    if (type === 'KNIGHT') {
      person.knightRank = knightRank;
      person.horseName = horseName;
    } else if (type === 'WIZARD') {
      person.mana = Number(mana);
      person.school = school;
    }
    void run(() => api.recruitPerson(person), `${person.name} was recruited.`, () => setName(''));
  }

  function train(person: Person) {
    const profession = choices[person.name] ?? person.profession ?? 'SOLDIER';
    void run(() => api.trainPerson(person.name, profession), `${person.name} is now trained as ${profession}.`);
  }

  function remove(person: Person) {
    if (!confirm(`Delete ${person.name}?`)) return;
    void run(() => api.deletePerson(person.name), `${person.name} was deleted.`);
  }

  return (
    <>
      <div className="info card panel">
        <h3>Inheritance: SINGLE_TABLE</h3>
        <p className="muted">
          Person, Knight and Wizard are stored in <strong>one</strong> table <code>person</code>. The column{' '}
          <code>person_type</code> says which class a row is; a knight has no <code>mana</code>, so that cell stays empty.
          Look at it in the H2 console (<code>/h2-console</code>, profile <code>local</code>).
        </p>
      </div>

      <ErrorBox error={error} />
      {message && <div className="status ok">{message}</div>}

      <div className="two-col">
        <div className="card panel">
          <h2>The people of the kingdom</h2>
          <table>
            <thead>
              <tr>
                <th></th>
                <th>Name</th>
                <th>Age</th>
                <th>Details</th>
                <th>Profession</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {persons.map((p) => (
                <tr key={p.id ?? p.name}>
                  <td aria-hidden="true">{ICONS[p.type]}</td>
                  <td>
                    <strong>{p.name}</strong>
                    <br />
                    <span className="tag">{p.type}</span>
                  </td>
                  <td>{p.age}</td>
                  <td className="muted">
                    {p.type === 'KNIGHT' && `${p.knightRank ?? ''} · horse ${p.horseName ?? ''}`}
                    {p.type === 'WIZARD' && `mana ${p.mana ?? ''} · ${p.school ?? ''}`}
                  </td>
                  <td className="nowrap">
                    {p.profession ? <span className="tag">{p.profession}</span> : <span className="muted">untrained</span>}
                    <div className="train">
                      <select
                        aria-label={`Profession for ${p.name}`}
                        value={choices[p.name] ?? p.profession ?? 'SOLDIER'}
                        onChange={(e) => setChoices({ ...choices, [p.name]: e.target.value as Profession })}
                      >
                        {PROFESSIONS.map((pr) => (
                          <option key={pr} value={pr}>
                            {pr}
                          </option>
                        ))}
                      </select>
                      <button type="button" className="secondary small" onClick={() => train(p)}>
                        Train
                      </button>
                    </div>
                  </td>
                  <td>
                    <button type="button" className="danger small" onClick={() => remove(p)}>
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
              {persons.length === 0 && (
                <tr>
                  <td colSpan={6} className="muted">
                    Nobody here.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <form className="card panel" onSubmit={recruit}>
          <h2>Recruit a person</h2>
          <p className="muted">
            Adds a new person to the kingdom, without a profession. Only the king may; try it as another user and watch
            the 403. Then <strong>train</strong> them in the table: SOLDIER from 21, NURSE from 18, FARMER from 16. The
            king and the knights may train.
          </p>
          <div className="grid">
            <label className="wide">
              Type
              <select value={type} onChange={(e) => setType(e.target.value as PersonType)}>
                <option value="PERSON">Person</option>
                <option value="KNIGHT">Knight</option>
                <option value="WIZARD">Wizard</option>
              </select>
            </label>
            <label>
              Name
              <input value={name} onChange={(e) => setName(e.target.value)} required />
            </label>
            <label>
              Age
              <input type="number" value={age} onChange={(e) => setAge(e.target.value)} required />
            </label>
            {type === 'KNIGHT' && (
              <>
                <label>
                  Rank
                  <input value={knightRank} onChange={(e) => setKnightRank(e.target.value)} />
                </label>
                <label>
                  Horse
                  <input value={horseName} onChange={(e) => setHorseName(e.target.value)} />
                </label>
              </>
            )}
            {type === 'WIZARD' && (
              <>
                <label>
                  Mana
                  <input type="number" value={mana} onChange={(e) => setMana(e.target.value)} />
                </label>
                <label>
                  School
                  <input value={school} onChange={(e) => setSchool(e.target.value)} />
                </label>
              </>
            )}
          </div>
          <button type="submit" className="btn" disabled={!name.trim() || age === ''}>
            Recruit
          </button>
        </form>
      </div>
    </>
  );
}
