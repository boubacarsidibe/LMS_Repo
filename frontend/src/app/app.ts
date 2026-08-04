import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Login } from './features/auth/login/login';
import { FeaturesModule } from './features/features-module';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet,FeaturesModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('frontend');
}
