import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormateurService, FormateurProfile } from '../../services/formateur.service';

@Component({
  selector: 'app-dashboard-formateur',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard-formateur.html',
  styleUrl: './dashboard-formateur.css',
})
export class DashboardFormateur implements OnInit {
  profile: FormateurProfile | null = null;
  loading = false;

  constructor(private formateurService: FormateurService) {}

  ngOnInit(): void {
    this.loading = true;
    this.formateurService.getProfile().subscribe({
      next: p => { this.profile = p; this.loading = false; },
      error: () => { this.profile = null; this.loading = false; }
    });
  }
}