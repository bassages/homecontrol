import {Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs/Observable";
import {Energiecontract} from "./energiecontract";
import * as moment from "moment";

@Injectable()
export class EnergiecontractService {

  constructor(private http: HttpClient) { }

  public getAll(): Observable<Energiecontract[]> {
    return this.http.get<BackendEnergiecontract[]>('/api/energiecontract')
                    .map(EnergiecontractService.allToEnergieContract);
  }

  public delete(id: number): Observable<Object> {
    return this.http.delete(`/api/energiecontract/${id}`);
  }

  public save(energieContract: Energiecontract): Observable<Energiecontract> {
    return this.http.post<BackendEnergiecontract>('/api/energiecontract', EnergiecontractService.toBackendEnergieContract(energieContract))
                    .map(EnergiecontractService.toEnergieContract);
  }

  static toEnergieContract(backendEnergieContract: BackendEnergiecontract): Energiecontract {
    const energiecontract: Energiecontract = new Energiecontract();
    energiecontract.id = backendEnergieContract.id;
    energiecontract.leverancier = backendEnergieContract.leverancier;
    energiecontract.stroomPerKwhDalTarief = backendEnergieContract.stroomPerKwhDalTarief;
    energiecontract.stroomPerKwhNormaalTarief = backendEnergieContract.stroomPerKwhNormaalTarief;
    energiecontract.gasPerKuub = backendEnergieContract.gasPerKuub;
    energiecontract.validFrom = moment(backendEnergieContract.validFrom, 'YYYY-MM-DD');
    energiecontract.validTo = moment(backendEnergieContract.validTo, 'YYYY-MM-DD');
    return energiecontract;
  }

  private static allToEnergieContract(backendEnegiecontracten: BackendEnergiecontract[]): Energiecontract[] {
    return backendEnegiecontracten.map(EnergiecontractService.toEnergieContract);
  }

  private static toBackendEnergieContract(energieContract: Energiecontract) {
    const backendEnergiecontract: BackendEnergiecontract = new BackendEnergiecontract();
    backendEnergiecontract.id = energieContract.id;
    backendEnergiecontract.validFrom = energieContract.validFrom.format('YYYY-MM-DD');
    backendEnergiecontract.leverancier = energieContract.leverancier;
    backendEnergiecontract.gasPerKuub = energieContract.gasPerKuub;
    backendEnergiecontract.stroomPerKwhNormaalTarief = energieContract.stroomPerKwhNormaalTarief;
    backendEnergiecontract.stroomPerKwhDalTarief = energieContract.stroomPerKwhDalTarief;
    return backendEnergiecontract;
  }
}

class BackendEnergiecontract {
  id: number;
  validFrom: string;
  validTo: string;
  stroomPerKwhNormaalTarief: number;
  stroomPerKwhDalTarief: number;
  gasPerKuub: number;
  leverancier: string;
}