import { Component, effect, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
})
export class App {
  protected readonly auth = inject(AuthService);
  protected readonly dark = signal(readStoredTheme() === 'dark');

  constructor() {
    effect(() => {
      const theme = this.dark() ? 'dark' : 'light';
      document.documentElement.dataset['theme'] = theme;
      try {
        localStorage.setItem('theme', theme);
      } catch {
        /* storage not available: the choice just isn't remembered */
      }
    });
  }

  protected toggleTheme(): void {
    this.dark.update((d) => !d);
  }
}

function readStoredTheme(): string | null {
  try {
    return localStorage.getItem('theme');
  } catch {
    return null;
  }
}
