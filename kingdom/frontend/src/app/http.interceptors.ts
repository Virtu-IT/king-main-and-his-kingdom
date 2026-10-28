import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ANONYMOUS, AuthService, KEEP_SESSION } from './auth.service';
import { ApiError } from './models';

/**
 * - adds the Bearer token (unless the request is marked ANONYMOUS)
 * - adds X-Requested-With, so the server does not trigger the browser's own Basic-auth popup on a 401
 * - turns every failure into an ApiError; the server answers errors with a picture, the reason is in X-Error-Message
 * - a 401 while we were logged in means the token is gone/expired: back to the login page
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token();
  const sendToken = token !== null && !req.context.get(ANONYMOUS);

  const request = req.clone({
    setHeaders: {
      'X-Requested-With': 'XMLHttpRequest',
      ...(sendToken ? { Authorization: `Bearer ${token}` } : {}),
    },
  });

  return next(request).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401 && sendToken && !req.context.get(KEEP_SESSION)) {
        auth.logout();
      }
      return throwError(() => toApiError(err));
    }),
  );
};

function toApiError(err: HttpErrorResponse): ApiError {
  if (err.status === 0) {
    return { status: 0, message: 'Cannot reach the server. Is the application running?' };
  }
  const header = err.headers?.get('X-Error-Message');
  const body = err.error;
  const detail = header || (body && typeof body === 'object' ? body.message : undefined);
  return { status: err.status, message: detail || err.statusText || 'request failed' };
}
