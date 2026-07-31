import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LandingComponent } from './catalogue/landing/landing.component';
import { RouterModule } from '@angular/router';
import { ProgramCard } from './componants/catalogue/program-card/program-card';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { FooterComponent } from '../../shared/components/footer/footer.component';


@NgModule({
  declarations: [
    ProgramCard
  ],
  imports: [
    CommonModule, RouterModule,NavbarComponent,FooterComponent
  ],
  exports: [
    ProgramCard
  ]
})
export class RecrutementModule {}