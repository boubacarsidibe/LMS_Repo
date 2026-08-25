import { Component } from '@angular/core';

@Component({
  selector: 'app-sidebar-auth',
  imports: [],
  templateUrl: './sidebar-auth.html',
  styleUrl: './sidebar-auth.css',
  host: {
    'class': 'hidden md:flex md:w-1/2 relative flex-col justify-between p-12 overflow-hidden'
  },
  styles: `
    :host {
      background-color: #131741;
      color: #7c80b0;
    }
  `
})
export class SidebarAuth {

}
