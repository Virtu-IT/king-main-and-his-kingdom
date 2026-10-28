import { getSession, setSession } from './authStore';
import type { Equipment, Person, Profession, TokenResponse, UserInfo, WarMachine } from './models';

/** What every failed call turns into. The server answers errors with a picture; the reason is in X-Error-Message. */
export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

export interface Options {
  method?: string;
  body?: unknown;
  /** Do not send the Authorization header (to show what a 401 looks like). */
  anonymous?: boolean;
  /** A 401 does not log the user out (the security lab provokes them on purpose). */
  keepSession?: boolean;
  headers?: Record<string, string>;
}

export interface RawResponse {
  status: number;
  text: string;
}

/**
 * The one place that talks to the server, like an interceptor:
 * - adds the Bearer token (unless the request is anonymous)
 * - adds X-Requested-With, so the server does not trigger the browser's own Basic-auth popup on a 401
 * - turns every failure into an ApiError
 * - a 401 while we were logged in means the token is gone/expired: log out (the router shows the login page)
 */
export async function raw(url: string, options: Options = {}): Promise<RawResponse> {
  const token = getSession()?.token ?? null;
  const sendToken = token !== null && !options.anonymous;

  const headers: Record<string, string> = { 'X-Requested-With': 'XMLHttpRequest' };
  if (options.body !== undefined) headers['Content-Type'] = 'application/json';
  if (sendToken) headers['Authorization'] = `Bearer ${token}`;
  Object.assign(headers, options.headers);

  let response: Response;
  try {
    response = await fetch(url, {
      method: options.method ?? 'GET',
      headers,
      body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
    });
  } catch {
    throw new ApiError(0, 'Cannot reach the server. Is the application running?');
  }

  if (!response.ok) {
    if (response.status === 401 && sendToken && !options.keepSession) {
      setSession(null);
    }
    throw new ApiError(
      response.status,
      response.headers.get('X-Error-Message') || response.statusText || `HTTP ${response.status}`,
    );
  }
  return { status: response.status, text: await response.text() };
}

export async function request<T>(url: string, options: Options = {}): Promise<T> {
  const { text } = await raw(url, options);
  return (text ? JSON.parse(text) : undefined) as T;
}

/** All REST endpoints of the Spring Boot API. */
export const api = {
  async login(username: string, password: string): Promise<void> {
    const session = await request<TokenResponse>('/api/auth/login', {
      method: 'POST',
      body: { username, password },
      anonymous: true,
    });
    setSession(session);
  },

  register(username: string, password: string): Promise<UserInfo> {
    return request<UserInfo>('/api/auth/register', { method: 'POST', body: { username, password }, anonymous: true });
  },

  persons: () => request<Person[]>('/api/persons'),

  recruitPerson: (person: Person) => request<Person>('/api/persons', { method: 'POST', body: person }),

  /** Trains an existing person to a soldier, farmer or nurse. */
  trainPerson: (name: string, profession: Profession) =>
    request<Person>(`/api/persons/${encodeURIComponent(name)}/training`, { method: 'POST', body: { profession } }),

  deletePerson: (name: string) =>
    request<void>(`/api/persons/${encodeURIComponent(name)}`, { method: 'DELETE' }),

  equipment: () => request<Equipment[]>('/api/equipment'),

  addEquipment: (equipment: Equipment) => request<Equipment>('/api/equipment', { method: 'POST', body: equipment }),

  warMachines: (name = '', minCrew = 0) =>
    request<WarMachine[]>(`/api/war-machines?name=${encodeURIComponent(name)}&minCrew=${minCrew}`),

  addWarMachine: (machine: WarMachine) =>
    request<WarMachine>('/api/war-machines', { method: 'POST', body: machine }),
};
