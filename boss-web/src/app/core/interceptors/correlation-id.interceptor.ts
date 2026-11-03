import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Tags every outgoing request with a correlation id the backend echoes back and
 * threads through its logs (see LogFields on the backend) - lets a user-reported
 * issue be traced end to end.
 */
export const correlationIdInterceptor: HttpInterceptorFn = (req, next) => {
  const correlationId = globalThis.crypto?.randomUUID ? globalThis.crypto.randomUUID() : `${Date.now()}-${Math.random()}`;
  return next(req.clone({ setHeaders: { 'X-Correlation-Id': correlationId } }));
};
