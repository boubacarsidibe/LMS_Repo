import { Component, Input } from '@angular/core';
import { Formation } from '../../models/formation';

@Component({
  selector: 'app-formation-card',
  standalone: false,
  templateUrl: './formation-card.html',
  styleUrl: './formation-card.css'
})
export class FormationCard {
  @Input() formation!: Formation;
}
