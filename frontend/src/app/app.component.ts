import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from './shared/header/header.component';
import { FooterComponent } from './shared/footer/footer.component';
import { MsalService } from '@azure/msal-angular';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, HeaderComponent, FooterComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  constructor(private authService: MsalService) {}

  login() {
    this.authService.loginPopup().subscribe({
      next: (result) => {
        console.log('Login exitoso:', result);
      },
      error: (error) => {
        console.error('Error en el login:', error);
      }
    });
  }
}