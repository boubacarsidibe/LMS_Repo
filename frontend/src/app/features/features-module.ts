import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {  ReactiveFormsModule } from '@angular/forms';
import { Login } from './auth/login/login';




@NgModule({
  declarations: [
    Login
  ],
  imports: [
    CommonModule,ReactiveFormsModule
  ],exports: [
    Login
  ]
})
export class FeaturesModule { }
