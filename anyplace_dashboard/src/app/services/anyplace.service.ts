import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AppConstant } from '../constant';
// import { access } from 'fs';

@Injectable({
  providedIn: 'root'
})

export class AnyplaceService {

  apiUrl: string = AppConstant.BASE_API_URL;

  constructor(private http: HttpClient) { }

  getAllCategories() {
    
    let url = this.apiUrl + '/objcategories';
    
    return this.http.get(url);
  }

  

  getHistorySummary(objectId: string) {
    let url = this.apiUrl + '/history_summary';
    let body = { "obid": objectId }
    let headers = new HttpHeaders({ "Content-Type": "application/json" });

    return this.http.post(url, body, { headers: headers });
  }

  getSelectedItemHistory(objectId: string) {
    let url = this.apiUrl + '/history';
    let body = { "obid": objectId }
    let headers = new HttpHeaders({ "Content-Type": "application/json" });

    return this.http.post(url, body, { headers: headers });
  }

  getSelectedItemHistoryWithBuid(objectId: string, buid:string, floor_number:string) {
    let url = this.apiUrl + '/history';
    let body = { 
      "obid": objectId,
      "buid": buid,
      "floor": floor_number
    }
    let headers = new HttpHeaders({ "Content-Type": "application/json" });

    return this.http.post(url, body, { headers: headers });
  }

  getFloorPlan(buid: string, floorNo: number) {

    let url = this.apiUrl + '/floorplans64/' + buid + '/' + floorNo;
    let headers = new HttpHeaders();
    headers.append("Accept", "text/plain");
    headers.append("Content-Type", "application/json");

    let body = {
      "username": "username",
      "password": "password",
      "buid": buid,
      "access_token": "token",
      "floor_number": floorNo
    };

    return this.http.post(url, body, { headers, responseType: 'text' });

  }

  getFloorCoordinates() {
    let url = this.apiUrl + '/mapping/all_floors';

    return this.http.get(url);
  }

  getAllAccessPoints() {
    let url = this.apiUrl + '/mapping/accesspoints_all';
    return this.http.get(url);
  }

  toggleAccessPoints(accessPointList){
    let url = this.apiUrl + '/mapping/accesspoints_toggle';
    // console.log(url);
    let data = '[';
    accessPointList.forEach(element => {
      data += '"'+element+'",';
    });
    data = data.substr(0, data.length-1) + "]";
    console.log(data);
    // console.log('"["'+accessPointList+'"]"');
    
    let headers = new HttpHeaders();
    headers.append("Accept", "text/plain");
    headers.append("Content-Type", "application/json");

    let body = {
      "aplist": data
    };

    return this.http.post(url, body, {headers, responseType: 'text'});
  }

  //DASHBOARD 1: Get Location History of the object using Building/Floor
  getAllLocationHistoryBuidFnum(buid:string, floor_number:number) {
    let url = this.apiUrl + '/history_by_buid_floor';

    let headers = new HttpHeaders();
    headers.append("Accept", "text/plain");
    headers.append("Content-Type", "application/json");

    let body = {
      "buid": buid,
      "floor": floor_number
    };

    return this.http.post(url, body, {headers, responseType: 'text'});
  }

  getHeatmapBuidFloorWhitelisted(rss: number, buid:string, floor_number:string) {
    let url = this.apiUrl + '/mapping/radio/heatmap_building_floor_whitelisted';
    let body = { 
      "rss": rss,
      "buid": buid,
      "floor": floor_number
    }
    let headers = new HttpHeaders({ "Content-Type": "application/json" });

    return this.http.post(url, body, { headers: headers });
  }

  getHeatmapBuidFloorSSID(ssid: string, buid:string, floor_number:string) {
    let url = this.apiUrl + '/mapping/radio/heatmap_building_floor_ssid';
    let body = { 
      "ssid": ssid,
      "buid": buid,
      "floor": floor_number
    }
    let headers = new HttpHeaders({ "Content-Type": "application/json" });

    return this.http.post(url, body, { headers: headers });
  }


}
