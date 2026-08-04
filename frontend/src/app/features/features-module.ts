import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Login } from './auth/login/login';

@NgModule({
  declarations: [
    
  ],
  imports: [
    CommonModule,
    Login,
    RouterModule
  ],
  exports: [
    Login 
  ]
})
export class FeaturesModule {}