import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { AppComponent } from './app.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { OthersComponent } from './others/others.component';
import { AccessPointsComponent } from './access-points/access-points.component';

const routes: Routes = [
  { path: 'dashboard', component: DashboardComponent},
  { path: 'points', component: AccessPointsComponent },
  { path: 'others', component: OthersComponent },
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: '*', redirectTo: '/dashboard', pathMatch: 'full' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes, {onSameUrlNavigation: 'reload'})],
  exports: [RouterModule]
})
export class AppRoutingModule { }
