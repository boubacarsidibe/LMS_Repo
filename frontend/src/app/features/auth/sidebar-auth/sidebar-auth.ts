import { Component } from '@angular/core';

@Component({
  selector: 'app-sidebar-auth',
  imports: [],
  templateUrl: './sidebar-auth.html',
  styleUrl: './sidebar-auth.css',
  host: {
    'class': 'hidden md:flex md:w-1/2 bg-primary-container relative flex-col justify-between p-12 text-on-primary-container overflow-hidden'
  }
})
export class SidebarAuth {

}
