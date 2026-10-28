import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';
import { ErrorBox } from '../error-box';
import { ApiError } from '../models';

/** The seeded demo users (see DataInitializer). Demo only: never ship passwords in a frontend. */
const DEMO_USERS = [
  { username: 'king', password: 'king1234', label: '👑 King', hint: 'KING + KNIGHT: may do everything' },
  { username: 'lancelot', password: 'lancelot1234', label: '🛡️ Lancelot', hint: 'KNIGHT: may add equipment' },
  { username: 'peasant', password: 'peasant1234', label: '🌾 Peasant', hint: 'SUBJECT: may only look' },
];

@Component({
  selector: 'app-login',
  imports: [FormsModule, ErrorBox],
  template: `
    <div class="two-col">
      <form class="card panel" (ngSubmit)="login()" #lf="ngForm">
        <h2>Log in</h2>
        <app-error-box [error]="error()" />
        <div class="grid">
          <label class="wide">Username <input name="username" [(ngModel)]="username" autocomplete="username" required /></label>
          <label class="wide">Password <input name="password" type="password" [(ngModel)]="password" autocomplete="current-password" required /></label>
        </div>
        <button type="submit" class="btn" [disabled]="lf.invalid || busy()">Log in</button>

        <h3 class="spaced">Single sign-on</h3>
        <p class="muted">Needs the fake identity provider (port 9000) and the profile <code>idp</code>.</p>
        <a class="btn secondary-btn" href="/oauth2/authorization/kingdom-idp">Log in with the identity provider</a>

        <h3 class="spaced">Demo logins</h3>
        <div class="actions">
          @for (u of demoUsers; track u.username) {
            <button type="button" class="secondary small" [title]="u.hint" (click)="fill(u.username, u.password)">{{ u.label }}</button>
          }
        </div>
      </form>

      <form class="card panel" (ngSubmit)="register()" #rf="ngForm">
        <h2>New subject</h2>
        <p class="muted">Registering always makes you a plain SUBJECT.</p>
        @if (registered()) {
          <div class="status ok">Welcome, {{ registered() }}! You can log in now.</div>
        }
        <div class="grid">
          <label class="wide">Username (3-30) <input name="newUsername" [(ngModel)]="newUsername" required minlength="3" maxlength="30" /></label>
          <label class="wide">Password (min. 8) <input name="newPassword" type="password" [(ngModel)]="newPassword" required minlength="8" autocomplete="new-password" /></label>
        </div>
        <button type="submit" class="secondary" [disabled]="rf.invalid || busy()">Register</button>
      </form>
    </div>
  `,
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly demoUsers = DEMO_USERS;
  protected readonly error = signal<ApiError | null>(null);
  protected readonly busy = signal(false);
  protected readonly registered = signal<string | null>(null);

  protected username = '';
  protected password = '';
  protected newUsername = '';
  protected newPassword = '';

  protected fill(username: string, password: string): void {
    this.username = username;
    this.password = password;
  }

  protected async login(): Promise<void> {
    this.busy.set(true);
    this.error.set(null);
    try {
      await this.auth.login(this.username.trim(), this.password);
      await this.router.navigate(['/persons']);
    } catch (e) {
      this.error.set(e as ApiError);
    } finally {
      this.busy.set(false);
    }
  }

  protected async register(): Promise<void> {
    this.busy.set(true);
    this.error.set(null);
    this.registered.set(null);
    try {
      const user = await this.auth.register(this.newUsername.trim(), this.newPassword);
      this.registered.set(user.username);
      this.username = user.username;
      this.newUsername = this.newPassword = '';
    } catch (e) {
      this.error.set(e as ApiError);
    } finally {
      this.busy.set(false);
    }
  }
}
