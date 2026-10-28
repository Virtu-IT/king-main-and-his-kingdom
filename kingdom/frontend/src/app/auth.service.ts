import { HttpClient, HttpContext, HttpContextToken } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { TokenResponse, UserInfo } from './models';

/** Requests with this flag are sent without the Authorization header (to show what a 401 looks like). */
export const ANONYMOUS = new HttpContextToken<boolean>(() => false);
/** Requests with this flag never log the user out on a 401 (the security lab provokes them on purpose). */
export const KEEP_SESSION = new HttpContextToken<boolean>(() => false);

const STORAGE_KEY = 'kingdom.session';

/**
 * Keeps the JWT after login. It lives in sessionStorage: gone when the tab closes, but readable by any
 * script on the page (XSS!). A HttpOnly cookie would be safer against XSS, but then CSRF protection is needed.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  readonly session = signal<TokenResponse | null>(readSession());
  readonly token = computed(() => this.session()?.token ?? null);
  readonly username = computed(() => this.session()?.username ?? null);
  readonly roles = computed(() => this.session()?.roles ?? []);
  readonly loggedIn = computed(() => this.session() !== null);
  /** The claims inside the token, decoded in the browser (the signature is NOT checked here, the server does that). */
  readonly claims = computed(() => decodeClaims(this.token()));

  hasRole(role: string): boolean {
    return this.roles().includes(role);
  }

  async login(username: string, password: string): Promise<void> {
    const context = new HttpContext().set(ANONYMOUS, true);
    const session = await firstValueFrom(
      this.http.post<TokenResponse>('/api/auth/login', { username, password }, { context }),
    );
    this.session.set(session);
    store(session);
  }

  async register(username: string, password: string): Promise<UserInfo> {
    const context = new HttpContext().set(ANONYMOUS, true);
    return firstValueFrom(this.http.post<UserInfo>('/api/auth/register', { username, password }, { context }));
  }

  /** After "log in with the identity provider": the server hands us its own JWT, we only keep it. */
  completeSso(token: string): boolean {
    const claims = decodeClaims(token);
    if (!claims || typeof claims['sub'] !== 'string' || typeof claims['exp'] !== 'number') {
      return false;
    }
    const session: TokenResponse = {
      token,
      tokenType: 'Bearer',
      expiresAt: new Date(claims['exp'] * 1000).toISOString(),
      username: claims['sub'],
      roles: Array.isArray(claims['roles']) ? (claims['roles'] as string[]) : [],
    };
    this.session.set(session);
    store(session);
    return true;
  }

  logout(redirect = true): void {
    this.session.set(null);
    store(null);
    if (redirect) {
      void this.router.navigate(['/login']);
    }
  }
}

function readSession(): TokenResponse | null {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const session = JSON.parse(raw) as TokenResponse;
    return new Date(session.expiresAt).getTime() > Date.now() ? session : null;
  } catch {
    return null;
  }
}

function store(session: TokenResponse | null): void {
  try {
    if (session) sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    else sessionStorage.removeItem(STORAGE_KEY);
  } catch {
    /* storage not available: the login just does not survive a refresh */
  }
}

function decodeClaims(token: string | null): Record<string, unknown> | null {
  if (!token) return null;
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(atob(payload));
  } catch {
    return null;
  }
}
