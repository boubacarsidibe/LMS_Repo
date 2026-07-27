import { Routes } from '@angular/router';
import { Test1 } from './features/dashboard/test/test1/test1';
import { authGuard } from './core/auth/guards/auth-guard';
import { Login } from './features/auth/login/login';
import { Register } from './features/auth/register/register';
import { VerifyOtp } from './features/auth/verify-otp/verify-otp';

export const routes: Routes = [
    { path: 'login', component: Login }, // <-- LA ROUTE DOIT ÊTRE ICI
    { path: '', redirectTo: 'login', pathMatch: 'full' },
    { path: 'test1', component: Test1, canActivate: [authGuard] },
    { path:"register",component:Register},
    {path:"verify-otp",component:VerifyOtp}

];
