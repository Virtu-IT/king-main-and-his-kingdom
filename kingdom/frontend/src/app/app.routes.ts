import { Routes } from '@angular/router';
import { Arsenal } from './arsenal/arsenal';
import { authGuard } from './auth.guard';
import { Login } from './login/login';
import { Persons } from './persons/persons';
import { SecurityLab } from './security-lab/security-lab';
import { Sso } from './sso/sso';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'persons' },
  { path: 'login', component: Login, title: 'Login - Kingdom' },
  { path: 'sso', component: Sso, title: 'Signing in - Kingdom' },
  { path: 'persons', component: Persons, canActivate: [authGuard], title: 'Persons - Kingdom' },
  { path: 'arsenal', component: Arsenal, canActivate: [authGuard], title: 'Arsenal - Kingdom' },
  { path: 'security-lab', component: SecurityLab, title: 'Security lab - Kingdom' },
  { path: '**', redirectTo: 'persons' },
];
