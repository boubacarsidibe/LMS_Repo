import { Component, OnInit } from '@angular/core';
import { FormateurService, FormateurProfile } from '../../services/formateur.service';

@Component({
  selector: 'app-dashboard-formateur',
  standalone: false,
  templateUrl: './dashboard-formateur.html',
  styleUrl: './dashboard-formateur.css',
})
export class DashboardFormateur implements OnInit {
  profile: FormateurProfile | null = null;

  constructor(private formateurService: FormateurService) {}

  ngOnInit(): void {
    this.formateurService.getProfile().subscribe({ next: p => this.profile = p, error: () => this.profile = null });
  }
}