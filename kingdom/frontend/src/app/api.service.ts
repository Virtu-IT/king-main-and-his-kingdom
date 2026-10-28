import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Equipment, Person, Profession, WarMachine } from './models';

/** All REST endpoints of the Spring Boot API. */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  persons(): Observable<Person[]> {
    return this.http.get<Person[]>('/api/persons');
  }

  recruitPerson(person: Person): Observable<Person> {
    return this.http.post<Person>('/api/persons', person);
  }

  /** Trains an existing person to a soldier, farmer or nurse. */
  trainPerson(name: string, profession: Profession): Observable<Person> {
    return this.http.post<Person>(`/api/persons/${encodeURIComponent(name)}/training`, { profession });
  }

  deletePerson(name: string): Observable<void> {
    return this.http.delete<void>(`/api/persons/${encodeURIComponent(name)}`);
  }

  equipment(): Observable<Equipment[]> {
    return this.http.get<Equipment[]>('/api/equipment');
  }

  addEquipment(equipment: Equipment): Observable<Equipment> {
    return this.http.post<Equipment>('/api/equipment', equipment);
  }

  warMachines(name = '', minCrew = 0): Observable<WarMachine[]> {
    const params = new HttpParams().set('name', name).set('minCrew', minCrew);
    return this.http.get<WarMachine[]>('/api/war-machines', { params });
  }

  addWarMachine(machine: WarMachine): Observable<WarMachine> {
    return this.http.post<WarMachine>('/api/war-machines', machine);
  }
}
