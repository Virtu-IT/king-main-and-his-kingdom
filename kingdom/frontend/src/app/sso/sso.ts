import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../auth.service';

/**
 * Landing page after the single sign-on: the server redirects to /sso#token=...
 * A fragment (#) is never sent to a server, so the token does not end up in access logs.
 */
@Component({
  selector: 'app-sso',
  imports: [RouterLink],
  template: `
    <div class="card panel">
      @if (failed()) {
        <h2>Single sign-on failed</h2>
        <p class="muted">No valid token was received. <a routerLink="/login">Back to the login page</a></p>
      } @else {
        <h2>Signing you in...</h2>
      }
    </div>
  `,
})
export class Sso implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly failed = signal(false);

  ngOnInit(): void {
    const token = new URLSearchParams(window.location.hash.replace(/^#/, '')).get('token');
    // remove the token from the address bar and the browser history right away
    history.replaceState(null, '', '/sso');
    if (token && this.auth.completeSso(token)) {
      void this.router.navigate(['/persons']);
    } else {
      this.failed.set(true);
    }
  }
}
