import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterOutlet } from '@angular/router';
import { SidebarAuth } from "./../../sidebar-auth/sidebar-auth";
@Component({
  selector: 'app-auth-layout',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterOutlet, SidebarAuth],
  templateUrl: './auth-layout.html'
})
export class AuthLayoutComponent {}