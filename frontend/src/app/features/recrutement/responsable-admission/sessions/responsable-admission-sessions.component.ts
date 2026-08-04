import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-responsable-admission-sessions',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './responsable-admission-sessions.component.html',
})
export class ResponsableAdmissionSessionsComponent {
  drawerOpen = false;

  openDrawer(): void { this.drawerOpen = true; }
  closeDrawer(): void { this.drawerOpen = false; }
}