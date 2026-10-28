import { Component, input, signal } from '@angular/core';
import { ApiError } from './models';

/** Shows a failed call: status code, reason and the http.cat picture of that status (hidden when offline). */
@Component({
  selector: 'app-error-box',
  template: `
    @if (error(); as e) {
      <div class="status err" role="alert">
        @if (e.status > 0 && !imageFailed()) {
          <img
            class="cat"
            [src]="'https://http.cat/' + e.status + '.jpg'"
            [alt]="'HTTP ' + e.status"
            (error)="imageFailed.set(true)"
          />
        }
        <div>
          <strong>{{ e.status || 'No connection' }}</strong>
          <div>{{ e.message }}</div>
        </div>
      </div>
    }
  `,
})
export class ErrorBox {
  readonly error = input<ApiError | null>(null);
  protected readonly imageFailed = signal(false);
}
