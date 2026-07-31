import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';


import { Login } from './auth/login/login';
import { RouterModule } from '@angular/router';


@NgModule({
  declarations: [
    
  ],
  imports: [
    CommonModule,
    Login,
    RouterModule,
  ],
  exports: [
    Login 
  ]
})
export class FeaturesModule {}