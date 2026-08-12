/**
 * Déclenche le téléchargement d'un Blob (ex. réponse HTTP { responseType: 'blob' })
 * comme un fichier nommé, via un lien <a> temporaire — pattern standard en
 * l'absence d'API native de téléchargement direct depuis un Blob en mémoire.
 */
export function downloadBlob(blob: Blob, filename: string): void {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  window.URL.revokeObjectURL(url);
}
