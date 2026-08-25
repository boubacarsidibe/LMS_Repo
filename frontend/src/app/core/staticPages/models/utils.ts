export interface TocSection {
    id: string;
    title: string;
    icon?: string;
}
export interface TocItem {
  id: string;        // Doit correspondre exactement à l'id HTML de la section
  title: string;     // Libellé affiché dans le sommaire
  icon?: string;     // Icône optionnelle
}