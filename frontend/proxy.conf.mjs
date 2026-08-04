/**
 * Configuration du proxy Angular.
 * Fichier .mjs obligatoire pour désactiver la limite bodyParser
 * du dev-server (413 Payload Too Large sur les uploads multipart).
 */
export default [
  {
    context: ['/api'],
    target: 'http://backend:8080',
    secure: false,
    changeOrigin: true,
    logLevel: 'debug',
    onProxyReq: null,
  },
];