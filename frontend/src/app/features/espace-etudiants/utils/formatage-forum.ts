/**
 * Convertit une syntaxe "markdown allege" (saisie via la barre d'outils du forum)
 * en HTML. Le texte est d'abord echappe pour empecher toute injection : les balises
 * generees ensuite (gras, italique, code, lien, liste) sont les seules autorisees.
 */
export function versHtmlForum(texte: string): string {
  const echappe = texte
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');

  const avecInlines = echappe
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/(?<!\*)\*([^*]+)\*(?!\*)/g, '<em>$1</em>')
    .replace(/`([^`]+)`/g, '<code class="px-1 py-0.5 bg-surface-container-high rounded text-[13px]">$1</code>')
    .replace(
      /\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)/g,
      '<a href="$2" target="_blank" rel="noopener noreferrer" class="text-secondary underline">$1</a>'
    );

  const lignes = avecInlines.split('\n');
  const morceaux: string[] = [];
  let dansListe = false;

  for (const ligne of lignes) {
    const puce = ligne.match(/^-\s+(.*)$/);
    if (puce) {
      if (!dansListe) {
        morceaux.push('<ul class="list-disc pl-5 my-1">');
        dansListe = true;
      }
      morceaux.push(`<li>${puce[1]}</li>`);
    } else {
      if (dansListe) {
        morceaux.push('</ul>');
        dansListe = false;
      }
      morceaux.push(ligne === '' ? '<br>' : `${ligne}<br>`);
    }
  }
  if (dansListe) {
    morceaux.push('</ul>');
  }

  return morceaux.join('');
}
