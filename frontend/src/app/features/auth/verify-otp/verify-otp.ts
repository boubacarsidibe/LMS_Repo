import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Auth } from '../../../core/auth/services/authService';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-verify-otp',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './verify-otp.html',
  styleUrl: './verify-otp.css',
})
export class VerifyOtp implements OnInit {
  otpForm: FormGroup;
  email: string = '';
  token: string = '';
  errorMessage: string = '';
  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private authService: Auth
  ) {
    this.otpForm = this.fb.group({
      code: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]],
    });
  }

  ngOnInit(): void {
    this.email = this.route.snapshot.queryParamMap.get('email') || '';
    this.token = this.route.snapshot.queryParamMap.get('token') || '';
  }

  onVerify() {
  if (this.otpForm.invalid) {
    this.otpForm.markAllAsTouched();
    return;
  }

  const { code } = this.otpForm.value;

  this.authService.verifyOtp(this.email, code).subscribe({
    next: (res) => {
      this.authService.setToken(this.token);
      this.router.navigate(['/test1']);
    },
    error: (err) => {
      if (err.status === 400 || err.status === 401) {
        this.errorMessage = 'Code OTP invalide ou expiré.';
      } else {
        this.errorMessage = 'Une erreur est survenue. Veuillez réessayer.';
      }
    }
  });
}
  onResendCode() {
    this.authService.getOtp(this.email).subscribe({
      next: () => {
        this.errorMessage = '';
        console.log('Un nouveau code OTP a été envoyé à', this.email);
      },
      error: () => {
        this.errorMessage = 'Impossible de renvoyer le code. Veuillez réessayer.';
      }
    });
  }
}