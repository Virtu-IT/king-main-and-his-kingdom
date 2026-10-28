import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable, firstValueFrom } from 'rxjs';
import { ApiService } from '../api.service';
import { ErrorBox } from '../error-box';
import { ApiError, Equipment, EquipmentType, WarMachine, WarMachineType } from '../models';

@Component({
  selector: 'app-arsenal',
  imports: [FormsModule, ErrorBox],
  template: `
    <app-error-box [error]="error()" />
    @if (message()) {
      <div class="status ok">{{ message() }}</div>
    }

    <div class="two-col">
      <div class="card panel">
        <h2>🗡️ Equipment</h2>
        <p class="muted">
          <strong>JOINED</strong>: shared columns in table <code>equipment</code>, weapon and armor columns in
          <code>weapon</code> / <code>armor</code>. Knights and the king may add.
        </p>
        <table>
          <thead><tr><th>Name</th><th>Weight</th><th>Details</th></tr></thead>
          <tbody>
            @for (e of equipment(); track e.id) {
              <tr>
                <td><strong>{{ e.name }}</strong><br /><span class="tag">{{ e.type }}</span></td>
                <td>{{ e.weight }} kg</td>
                <td class="muted">
                  @if (e.type === 'WEAPON') { damage {{ e.damage }} · {{ e.damageType }} }
                  @else { defense {{ e.defense }} · {{ e.material }} }
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>

      <form class="card panel" (ngSubmit)="addEquipment()" #ef="ngForm">
        <h2>Add equipment</h2>
        <div class="grid">
          <label class="wide">Type
            <select name="etype" [(ngModel)]="eType">
              <option value="WEAPON">Weapon</option>
              <option value="ARMOR">Armor</option>
            </select>
          </label>
          <label>Name <input name="ename" [(ngModel)]="eName" required /></label>
          <label>Weight (kg) <input name="eweight" type="number" step="0.1" [(ngModel)]="eWeight" required /></label>
          @if (eType === 'WEAPON') {
            <label>Damage <input name="edamage" type="number" [(ngModel)]="eDamage" required /></label>
            <label>Damage type <input name="edtype" [(ngModel)]="eDamageType" required /></label>
          } @else {
            <label>Defense <input name="edefense" type="number" [(ngModel)]="eDefense" required /></label>
            <label>Material <input name="ematerial" [(ngModel)]="eMaterial" required /></label>
          }
        </div>
        <button type="submit" class="btn" [disabled]="ef.invalid">Add</button>
      </form>
    </div>

    <div class="two-col spaced">
      <div class="card panel">
        <h2>💣 War machines</h2>
        <p class="muted">
          <strong>TABLE_PER_CLASS</strong>: no table <code>war_machine</code>; <code>tank</code> and <code>catapult</code>
          each hold every column. The search is a custom repository fragment. Only the king may add.
        </p>
        <div class="grid">
          <label>Name contains <input name="q" [ngModel]="query" (ngModelChange)="query = $event; search()" /></label>
          <label>Min. crew <input name="mc" type="number" min="0" [ngModel]="minCrew" (ngModelChange)="minCrew = $event ?? 0; search()" /></label>
        </div>
        <table>
          <thead><tr><th>Name</th><th>Crew</th><th>Details</th></tr></thead>
          <tbody>
            @for (m of machines(); track m.id) {
              <tr>
                <td><strong>{{ m.name }}</strong><br /><span class="tag">{{ m.type }}</span></td>
                <td>{{ m.crewSize }}</td>
                <td class="muted">
                  @if (m.type === 'TANK') { armor {{ m.armorThicknessMm }} mm }
                  @else { range {{ m.rangeMeters }} m }
                </td>
              </tr>
            } @empty {
              <tr><td colspan="3" class="muted">No war machines match.</td></tr>
            }
          </tbody>
        </table>
      </div>

      <form class="card panel" (ngSubmit)="addMachine()" #mf="ngForm">
        <h2>Build a war machine</h2>
        <div class="grid">
          <label class="wide">Type
            <select name="mtype" [(ngModel)]="mType">
              <option value="CATAPULT">Catapult</option>
              <option value="TANK">Tank</option>
            </select>
          </label>
          <label>Name <input name="mname" [(ngModel)]="mName" required /></label>
          <label>Crew <input name="mcrew" type="number" [(ngModel)]="mCrew" required /></label>
          @if (mType === 'TANK') {
            <label>Armor (mm) <input name="marmor" type="number" [(ngModel)]="mArmor" required /></label>
          } @else {
            <label>Range (m) <input name="mrange" type="number" [(ngModel)]="mRange" required /></label>
          }
        </div>
        <button type="submit" class="btn" [disabled]="mf.invalid">Build</button>
      </form>
    </div>
  `,
})
export class Arsenal implements OnInit {
  private readonly api = inject(ApiService);

  protected readonly equipment = signal<Equipment[]>([]);
  protected readonly machines = signal<WarMachine[]>([]);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly message = signal('');

  protected query = '';
  protected minCrew = 0;

  protected eType: EquipmentType = 'WEAPON';
  protected eName = '';
  protected eWeight: number | null = 1;
  protected eDamage: number | null = 40;
  protected eDamageType = 'SLASHING';
  protected eDefense: number | null = 30;
  protected eMaterial = 'Iron';

  protected mType: WarMachineType = 'CATAPULT';
  protected mName = '';
  protected mCrew: number | null = 4;
  protected mArmor: number | null = 10;
  protected mRange: number | null = 200;

  async ngOnInit(): Promise<void> {
    await Promise.all([this.loadEquipment(), this.search()]);
  }

  private async loadEquipment(): Promise<void> {
    try {
      this.equipment.set(await firstValueFrom(this.api.equipment()));
    } catch (e) {
      this.error.set(e as ApiError);
    }
  }

  protected async search(): Promise<void> {
    try {
      this.machines.set(await firstValueFrom(this.api.warMachines(this.query, this.minCrew)));
    } catch (e) {
      this.error.set(e as ApiError);
    }
  }

  protected async addEquipment(): Promise<void> {
    const item: Equipment =
      this.eType === 'WEAPON'
        ? { type: 'WEAPON', name: this.eName.trim(), weight: Number(this.eWeight), damage: Number(this.eDamage), damageType: this.eDamageType }
        : { type: 'ARMOR', name: this.eName.trim(), weight: Number(this.eWeight), defense: Number(this.eDefense), material: this.eMaterial };
    await this.run(() => this.api.addEquipment(item), `${item.name} added to the equipment.`, async () => {
      this.eName = '';
      await this.loadEquipment();
    });
  }

  protected async addMachine(): Promise<void> {
    const machine: WarMachine =
      this.mType === 'TANK'
        ? { type: 'TANK', name: this.mName.trim(), crewSize: Number(this.mCrew), armorThicknessMm: Number(this.mArmor) }
        : { type: 'CATAPULT', name: this.mName.trim(), crewSize: Number(this.mCrew), rangeMeters: Number(this.mRange) };
    await this.run(() => this.api.addWarMachine(machine), `${machine.name} was built.`, async () => {
      this.mName = '';
      await this.search();
    });
  }

  private async run(call: () => Observable<unknown>, ok: string, after: () => Promise<void>): Promise<void> {
    this.error.set(null);
    this.message.set('');
    try {
      await firstValueFrom(call());
      this.message.set(ok);
      await after();
    } catch (e) {
      this.error.set(e as ApiError);
    }
  }
}
