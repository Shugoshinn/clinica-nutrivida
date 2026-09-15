import { Component, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MsalService } from '@azure/msal-angular';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: `<router-outlet></router-outlet>`
})
export class AppComponent implements OnInit {

  constructor(private authService: MsalService) {}

  ngOnInit(): void {
    this.authService.instance.initialize().then(() => {
      this.authService.handleRedirectObservable().subscribe({
        next: (result) => {
          if (result) {
            this.authService.instance.setActiveAccount(result.account);
          }
        },
        error: (error) => console.error('Error en MSAL redirect:', error)
      });
    });
  }
}