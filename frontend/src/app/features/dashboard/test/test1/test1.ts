import { Component, OnInit } from '@angular/core';
import { User } from '../../../../core/auth/models/user.model';
import { Observable } from 'rxjs/internal/Observable';
import { AsyncPipe, CommonModule } from '@angular/common';
import { Auth } from '../../../../core/auth/services/authService';



@Component({
  selector: 'app-test1',
  templateUrl: './test1.html',
  styleUrl: './test1.css',
  standalone: true,
  imports: [AsyncPipe,CommonModule]
})
export class Test1 implements OnInit {
  user$: Observable<User | null> | null = null;
  constructor(private authService: Auth) { }

  ngOnInit(): void {
    this.user$ = this.authService.getCurrentUser();
  }

  logout() {
    confirm('Êtes-vous sûr de vouloir vous déconnecter ?') 
    this.authService.logout();
    console.log('Déconnexion réussie');
    window.location.reload();
    
    
  }
}
