import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable, firstValueFrom } from 'rxjs';
import { ApiService } from '../api.service';
import { ErrorBox } from '../error-box';
import { ApiError, Person, PersonType, Profession } from '../models';

const ICONS: Record<PersonType, string> = { PERSON: '🧑', KNIGHT: '⚔️', WIZARD: '🧙' };
const PROFESSIONS: Profession[] = ['SOLDIER', 'FARMER', 'NURSE'];

@Component({
  selector: 'app-persons',
  imports: [FormsModule, ErrorBox],
  template: `
    <div class="info card panel">
      <h3>Inheritance: SINGLE_TABLE</h3>
      <p class="muted">
        Person, Knight and Wizard are stored in <strong>one</strong> table <code>person</code>. The column
        <code>person_type</code> says which class a row is; a knight has no <code>mana</code>, so that cell stays empty.
        Look at it in the H2 console (<code>/h2-console</code>, profile <code>local</code>).
      </p>
    </div>

    <app-error-box [error]="error()" />
    @if (message()) {
      <div class="status ok">{{ message() }}</div>
    }

    <div class="two-col">
      <div class="card panel">
        <h2>The people of the kingdom</h2>
        <table>
          <thead>
            <tr><th></th><th>Name</th><th>Age</th><th>Details</th><th>Profession</th><th></th></tr>
          </thead>
          <tbody>
            @for (p of persons(); track p.id) {
              <tr>
                <td aria-hidden="true">{{ icon(p.type) }}</td>
                <td>
                  <strong>{{ p.name }}</strong><br />
                  <span class="tag">{{ p.type }}</span>
                </td>
                <td>{{ p.age }}</td>
                <td class="muted">
                  @switch (p.type) {
                    @case ('KNIGHT') { {{ p.knightRank }} · horse {{ p.horseName }} }
                    @case ('WIZARD') { mana {{ p.mana }} · {{ p.school }} }
                  }
                </td>
                <td class="nowrap">
                  @if (p.profession) {
                    <span class="tag">{{ p.profession }}</span>
                  } @else {
                    <span class="muted">untrained</span>
                  }
                  <div class="train">
                    <select #choice [attr.aria-label]="'Profession for ' + p.name">
                      @for (pr of professions; track pr) {
                        <option [value]="pr" [selected]="pr === p.profession">{{ pr }}</option>
                      }
                    </select>
                    <button type="button" class="secondary small" (click)="train(p, choice.value)">Train</button>
                  </div>
                </td>
                <td><button type="button" class="danger small" (click)="remove(p)">Delete</button></td>
              </tr>
            } @empty {
              <tr><td colspan="6" class="muted">Nobody here.</td></tr>
            }
          </tbody>
        </table>
      </div>

      <form class="card panel" (ngSubmit)="add()" #f="ngForm">
        <h2>Recruit a person</h2>
        <p class="muted">
          Adds a new person to the kingdom, without a profession. Only the king may; try it as another user and
          watch the 403. Then <strong>train</strong> them in the table: SOLDIER from 21, NURSE from 18, FARMER from 16.
          The king and the knights may train.
        </p>
        <div class="grid">
          <label class="wide">Type
            <select name="type" [(ngModel)]="type">
              <option value="PERSON">Person</option>
              <option value="KNIGHT">Knight</option>
              <option value="WIZARD">Wizard</option>
            </select>
          </label>
          <label>Name <input name="name" [(ngModel)]="name" required /></label>
          <label>Age (&gt; 20) <input name="age" type="number" [(ngModel)]="age" required /></label>
          @if (type === 'KNIGHT') {
            <label>Rank <input name="rank" [(ngModel)]="knightRank" /></label>
            <label>Horse <input name="horse" [(ngModel)]="horseName" /></label>
          }
          @if (type === 'WIZARD') {
            <label>Mana <input name="mana" type="number" [(ngModel)]="mana" /></label>
            <label>School <input name="school" [(ngModel)]="school" /></label>
          }
        </div>
        <button type="submit" class="btn" [disabled]="f.invalid">Recruit</button>
      </form>
    </div>
  `,
})
export class Persons implements OnInit {
  private readonly api = inject(ApiService);

  protected readonly persons = signal<Person[]>([]);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly message = signal('');
  protected readonly professions = PROFESSIONS;

  protected type: PersonType = 'KNIGHT';
  protected name = '';
  protected age: number | null = 25;
  protected knightRank = '';
  protected horseName = '';
  protected mana: number | null = 50;
  protected school = '';

  protected icon(type: PersonType): string {
    return ICONS[type];
  }

  ngOnInit(): Promise<void> {
    return this.load();
  }

  private async load(): Promise<void> {
    try {
      this.persons.set(await firstValueFrom(this.api.persons()));
    } catch (e) {
      this.error.set(e as ApiError);
    }
  }

  protected async add(): Promise<void> {
    const person: Person = { type: this.type, name: this.name.trim(), age: Number(this.age) };
    if (this.type === 'KNIGHT') {
      person.knightRank = this.knightRank;
      person.horseName = this.horseName;
    } else if (this.type === 'WIZARD') {
      person.mana = Number(this.mana);
      person.school = this.school;
    }
    await this.run(() => this.api.recruitPerson(person), `${person.name} was recruited.`, () => (this.name = ''));
  }

  protected async train(person: Person, profession: string): Promise<void> {
    await this.run(
      () => this.api.trainPerson(person.name, profession as Profession),
      `${person.name} is now trained as ${profession}.`,
    );
  }

  protected async remove(person: Person): Promise<void> {
    if (!confirm(`Delete ${person.name}?`)) return;
    await this.run(() => this.api.deletePerson(person.name), `${person.name} was deleted.`);
  }

  private async run(call: () => Observable<unknown>, ok: string, after?: () => void): Promise<void> {
    this.error.set(null);
    this.message.set('');
    try {
      await firstValueFrom(call());
      this.message.set(ok);
      after?.();
      await this.load();
    } catch (e) {
      this.error.set(e as ApiError);
    }
  }
}
