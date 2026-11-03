export const environment = {
  production: true,
  // Overridden at deploy time via a generated config or build-time replacement -
  // never bake a production API host into the bundle for every environment.
  apiBaseUrl: '/api/v1',
};
