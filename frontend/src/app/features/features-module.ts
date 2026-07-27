import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

// Tes imports de composants
import { Login } from './auth/login/login';
// import { Register } from './auth/register/register';
// import { VerifyOtp } from './auth/verify-otp/verify-otp';

@NgModule({
  declarations: [
  ],
  imports: [
    CommonModule,
    Login 
  ],
  exports: [
    Login 
  ]
})
export class FeaturesModule {}