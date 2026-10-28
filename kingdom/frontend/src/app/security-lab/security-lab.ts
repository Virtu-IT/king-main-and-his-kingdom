import { HttpClient, HttpContext } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { JsonPipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { ANONYMOUS, AuthService, KEEP_SESSION } from '../auth.service';
import { ApiError } from '../models';

interface Probe {
  title: string;
  method: 'GET' | 'POST' | 'DELETE';
  url: string;
  body?: unknown;
  rule: string;
}

interface Result {
  status: number;
  message: string;
  anonymous: boolean;
}

const PROBES: Probe[] = [
  { title: 'Welcome message', method: 'GET', url: '/api/welcome?name=Visitor', rule: 'public (permitAll)' },
  { title: 'Who am I?', method: 'GET', url: '/api/auth/me', rule: 'any logged-in user' },
  { title: 'List persons', method: 'GET', url: '/api/persons', rule: 'any logged-in user' },
  {
    title: 'Add equipment',
    method: 'POST',
    url: '/api/equipment',
    body: { type: 'ARMOR', name: 'Lab shield', weight: 3, defense: 20, material: 'Wood' },
    rule: '@PreAuthorize: KING or KNIGHT',
  },
  { title: 'Delete a person', method: 'DELETE', url: '/api/persons/nobody-here', rule: '@PreAuthorize: KING (a king gets 404: allowed, but nobody to delete)' },
  { title: 'Actuator environment', method: 'GET', url: '/actuator/env', rule: 'URL rule: KING (not exposed, so a king gets 404)' },
  { title: 'Actuator health', method: 'GET', url: '/actuator/health', rule: 'public (permitAll)' },
];

/** Fires requests with and without a token so students see 200, 401, 403 and 404 for themselves. */
@Component({
  selector: 'app-security-lab',
  imports: [JsonPipe],
  template: `
    <div class="two-col">
      <div class="card panel">
        <h2>🔬 Security lab</h2>
        <p class="muted">
          Run each call twice: once <strong>with</strong> your token and once <strong>anonymously</strong>. Log in as
          different users to see the difference between <code>401</code> (who are you?) and <code>403</code>
          (you may not).
        </p>
        <table>
          <thead><tr><th>Call</th><th>Rule on the server</th><th></th><th>Result</th></tr></thead>
          <tbody>
            @for (p of probes; track p.title) {
              <tr>
                <td><strong>{{ p.title }}</strong><br /><code>{{ p.method }} {{ p.url }}</code></td>
                <td class="muted">{{ p.rule }}</td>
                <td class="nowrap">
                  <button type="button" class="small" [disabled]="!auth.loggedIn()" (click)="run(p, false)">With token</button>
                  <button type="button" class="secondary small" (click)="run(p, true)">Anonymous</button>
                </td>
                <td>
                  @for (mode of modes; track mode) {
                    @if (results()[p.title + '|' + mode]; as r) {
                      <div>
                        <span class="code" [class.good]="r.status >= 200 && r.status < 300" [class.bad]="r.status >= 400">{{ r.status }}</span>
                        <span class="muted"> {{ mode }}</span>
                        @if (r.message) { <div class="muted small-text">{{ r.message }}</div> }
                      </div>
                    }
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>

      <div class="card panel">
        <h2>🎫 Your token</h2>
        @if (auth.claims(); as claims) {
          <p class="muted">A JWT is just three Base64 parts. This is the middle one (the claims), decoded in your browser. The server trusts it only because of the signature (the third part).</p>
          <pre class="printout">{{ claims | json }}</pre>
          <p class="muted">Tamper with it and the server answers 401:</p>
          <button type="button" class="danger small" (click)="tamper()">Send a tampered token</button>
          @if (tampered(); as t) {
            <p><span class="code bad">{{ t.status }}</span> <span class="muted">{{ t.message }}</span></p>
          }
        } @else {
          <p class="muted">You are not logged in. Use the Log in button in the header.</p>
        }
      </div>
    </div>
  `,
})
export class SecurityLab {
  protected readonly auth = inject(AuthService);
  private readonly http = inject(HttpClient);

  protected readonly probes = PROBES;
  protected readonly modes = ['token', 'anonymous'];
  protected readonly results = signal<Record<string, Result>>({});
  protected readonly tampered = signal<ApiError | null>(null);

  protected async run(probe: Probe, anonymous: boolean): Promise<void> {
    const context = new HttpContext().set(ANONYMOUS, anonymous).set(KEEP_SESSION, true);
    let result: Result;
    try {
      await firstValueFrom(this.http.request(probe.method, probe.url, { body: probe.body, context, responseType: 'text' }));
      result = { status: 200, message: '', anonymous };
    } catch (e) {
      const err = e as ApiError;
      result = { status: err.status, message: err.message, anonymous };
    }
    this.results.update((r) => ({ ...r, [`${probe.title}|${anonymous ? 'anonymous' : 'token'}`]: result }));
  }

  protected async tamper(): Promise<void> {
    const token = this.auth.token();
    if (!token) return;
    const [header, , signature] = token.split('.');
    const fake = btoa(JSON.stringify({ ...this.auth.claims(), roles: ['KING'] })).replace(/=+$/, '');
    const forged = `${header}.${fake}.${signature}`;
    const context = new HttpContext().set(ANONYMOUS, true).set(KEEP_SESSION, true);
    try {
      await firstValueFrom(
        this.http.get('/api/persons', { context, headers: { Authorization: `Bearer ${forged}` }, responseType: 'text' }),
      );
      this.tampered.set({ status: 200, message: 'Accepted?! That should not happen.' });
    } catch (e) {
      this.tampered.set(e as ApiError);
    }
  }
}
