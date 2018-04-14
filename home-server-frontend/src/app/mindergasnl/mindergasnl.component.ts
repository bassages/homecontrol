import {Component, OnInit} from '@angular/core';
import {MindergasnlService} from "./mindergasnl.service";
import {LoadingIndicatorService} from "../loading-indicator/loading-indicator.service";
import {ErrorHandingService} from "../error-handling/error-handing.service";
import {FormControl, FormGroup, Validators} from "@angular/forms";

const authenticatieTokenMaxLengthValidator = Validators.maxLength(255);

@Component({
  selector: 'mindergasnl',
  templateUrl: './mindergasnl.component.html',
  styleUrls: ['./mindergasnl.component.scss']
})
export class MindergasnlComponent implements OnInit {

  public form: FormGroup;
  public showSavedMessage: boolean = false;

  private _editmode: boolean;

  set editMode(editmode: boolean) {
    this._editmode = editmode;
    if (editmode) {
      this.automatischUploaden.enable();
      this.authenticatietoken.enable();
    } else {
      this.automatischUploaden.disable();
      this.authenticatietoken.disable();
    }
  }

  get editMode(): boolean {
    return this._editmode;
  }

  constructor(private mindergasnlService: MindergasnlService,
              private loadingIndicatorService: LoadingIndicatorService,
              private errorHandlingService: ErrorHandingService) { }

  ngOnInit() {
    this.form  = new FormGroup({
      automatischUploaden: new FormControl(),
      authenticatietoken: new FormControl('', authenticatieTokenMaxLengthValidator)
    });

    this.automatischUploaden.valueChanges.subscribe(value => this.setAuthenticatieTokenValidators(value));
    this.editMode = false;

    setTimeout(() => { this.getMinderGasNlSettings(); },0);
  }

  get automatischUploaden(): FormControl {
    return this.form.get('automatischUploaden') as FormControl;
  }

  get authenticatietoken(): FormControl {
    return this.form.get('authenticatietoken') as FormControl;
  }

  private getMinderGasNlSettings(): void {
    this.loadingIndicatorService.open();
    this.mindergasnlService.get().subscribe(
      minderGasNlSettings => this.form.setValue(minderGasNlSettings),
      error => this.errorHandlingService.handleError("De instellingen voor MinderGas.nl konden nu niet opgehaald worden", error),
      () => this.loadingIndicatorService.close()
    );
  }

  public save(): void {
    if (this.form.valid) {
      this.loadingIndicatorService.open();
      this.mindergasnlService.update(this.form.getRawValue()).subscribe(
        () => this.flashSavedMessage(),
        error => this.errorHandlingService.handleError("De instellingen konden niet opgeslagen worden", error),
        () => {
          this.loadingIndicatorService.close();
          this.editMode = false;
        }
      );
    }
  }

  private flashSavedMessage(): void {
    this.showSavedMessage = true;
    setTimeout(() => { this.showSavedMessage = false; },2500);
  }

  private setAuthenticatieTokenValidators(automatischUploaden: boolean): void {
    if (automatischUploaden) {
      this.authenticatietoken.setValidators([authenticatieTokenMaxLengthValidator, Validators.required]);
    } else {
      this.authenticatietoken.setValidators(authenticatieTokenMaxLengthValidator);
    }
    this.authenticatietoken.updateValueAndValidity();
  }
}
