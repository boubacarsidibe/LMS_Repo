import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { UiConfirmationComponent } from './shared/components/ui/ui-confirmation.component';
import { UiNotificationsComponent } from './shared/components/ui/ui-notifications.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, UiConfirmationComponent, UiNotificationsComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('frontend');
}
