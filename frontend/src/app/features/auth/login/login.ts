import { Component } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Auth } from '../../../core/auth/services/auth';
import { AuthResponse } from '../../../core/auth/models/auth-response';




@Component({
  selector: 'app-login',
  standalone: false,
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  errorMessage: string = '';
  form:FormGroup;

  constructor(
    private formBuilder: FormBuilder,private authService: Auth
  ) {
    this.form = this.formBuilder.group({
      username: '',
      password: '',
    });
    
  }

  onSubmit() {
    if (this.form.invalid) {
      return;
    }

    this.authService.login(this.form.value.username, this.form.value.password).subscribe(
      (response:AuthResponse) => {
        const token = response.token;
        localStorage.setItem('token', token);
        this.errorMessage = '';
        console.log('Login successful. Token:', token);
      },
      (error) => {
        this.errorMessage = 'Invalid username or password';
      }
    );
    }

  }
  




