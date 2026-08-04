import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { SidebarAuth } from "./../../sidebar-auth/sidebar-auth";
@Component({
  selector: 'app-auth-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, SidebarAuth],
  templateUrl: './auth-layout.html'
})
export class AuthLayoutComponent {}