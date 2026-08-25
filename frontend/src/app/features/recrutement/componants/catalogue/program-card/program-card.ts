import { Component, Input } from '@angular/core';
import { Program } from '../../../models/program';

@Component({
  selector: 'app-program-card',
  standalone: false,
  templateUrl: './program-card.html',
  styleUrl: './program-card.css',
})
export class ProgramCard {

  @Input() prog!: Program;
}
