import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { SidebarAuth } from "./../../sidebar-auth/sidebar-auth";
import { LogoInpgComponent } from '../../../../shared/components/logo-inpg/logo-inpg.component';
@Component({
  selector: 'app-auth-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, SidebarAuth, LogoInpgComponent],
  templateUrl: './auth-layout.html'
})
export class AuthLayoutComponent {}
