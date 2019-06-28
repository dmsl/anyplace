import { Component } from '@angular/core';
import { Router } from '@angular/router'

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {

  constructor(private router: Router) { }

  activeLink: string;
  logging: boolean;
  ngOnInit() {
    this.router.navigate(['/dashboard'], {skipLocationChange: true});
    this.activeLink = 'dashboard';
    this.logging = true;
  }

  onClickMethod(param: any) {
    this.activeLink = param;
  }
}