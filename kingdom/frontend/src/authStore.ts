import { useSyncExternalStore } from 'react';
import type { TokenResponse } from './models';

const STORAGE_KEY = 'kingdom.session';

/**
 * Keeps the JWT after login. It lives in sessionStorage: gone when the tab closes, but readable by any
 * script on the page (XSS!). A HttpOnly cookie would be safer against XSS, but then CSRF protection is needed.
 *
 * A tiny store outside React, so that the API client (not a component) can read the token and log out on a
 * 401; components follow it with useSession().
 */
let session: TokenResponse | null = read();
const listeners = new Set<() => void>();

export function getSession(): TokenResponse | null {
  return session;
}

export function setSession(next: TokenResponse | null): void {
  session = next;
  try {
    if (next) sessionStorage.setItem(STORAGE_KEY, JSON.stringify(next));
    else sessionStorage.removeItem(STORAGE_KEY);
  } catch {
    /* storage not available: the login just does not survive a refresh */
  }
  listeners.forEach((listener) => listener());
}

function subscribe(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

/** The current session; the component renders again when it changes. */
export function useSession(): TokenResponse | null {
  return useSyncExternalStore(subscribe, getSession);
}

export function hasRole(role: string): boolean {
  return session?.roles.includes(role) ?? false;
}

/** The claims inside a token, decoded in the browser (the signature is NOT checked here, the server does that). */
export function decodeClaims(token: string | null | undefined): Record<string, unknown> | null {
  if (!token) return null;
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(atob(payload));
  } catch {
    return null;
  }
}

/** After "log in with the identity provider": the server hands us its own JWT, we only keep it. */
export function completeSso(token: string): boolean {
  const claims = decodeClaims(token);
  if (!claims || typeof claims['sub'] !== 'string' || typeof claims['exp'] !== 'number') {
    return false;
  }
  setSession({
    token,
    tokenType: 'Bearer',
    expiresAt: new Date(claims['exp'] * 1000).toISOString(),
    username: claims['sub'],
    roles: Array.isArray(claims['roles']) ? (claims['roles'] as string[]) : [],
  });
  return true;
}

function read(): TokenResponse | null {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const stored = JSON.parse(raw) as TokenResponse;
    return new Date(stored.expiresAt).getTime() > Date.now() ? stored : null;
  } catch {
    return null;
  }
}
