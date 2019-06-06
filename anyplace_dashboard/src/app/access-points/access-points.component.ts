import { Component, OnInit } from '@angular/core';
import { AnyplaceService } from '../services/anyplace.service';
import { toggleAccessPointList } from '../model/toggleAccessPointList';
import { accessPoint, accessPointGroupedBySsid } from '../model/accessPoint';
import { async } from 'q';

@Component({
  selector: 'app-access-points',
  templateUrl: './access-points.component.html',
  styleUrls: ['./access-points.component.css']
})
export class AccessPointsComponent implements OnInit {

  constructor(private anyplaceService: AnyplaceService) { }
  
  // accessPoints: accessPointList;
  accessPointVal: Array<accessPoint> = new Array<accessPoint>();
  accessPointBySsid : Array<accessPointGroupedBySsid> = new Array<accessPointGroupedBySsid>();
  filteredItems:  Array<accessPointGroupedBySsid> = new Array<accessPointGroupedBySsid>();
  togglePoints: toggleAccessPointList;
  ssid_model: string;
  isLoading: boolean;
  building_model: string;
  canSubmit: boolean;

  ngOnInit() {
    this.isLoading = false;
    this.fetchData();  
  }

  fetchData(){
    this.accessPointBySsid = new Array<accessPointGroupedBySsid>();
    this.canSubmit = false;
    this.anyplaceService.getAllAccessPoints()
      .subscribe((response: any) => {
        response.accesspoints.forEach(element => {
          
          let ids = Object.values(element)[0];
          let ssid = '';
          let accessPointValTemp = new Array<accessPoint>();

          for(let iter = 0; iter < Object.keys(ids).length; iter++){
            ssid = ids[iter].ssid;
            accessPointValTemp.push(new accessPoint(ids[iter].building_name, ids[iter].whitelisted == "true" ? true:false, 
                                      ids[iter].floor, ids[iter].ssid, ids[iter].mac, ids[iter].apid));
          }
          
          this.accessPointBySsid.push(Object.create(new accessPointGroupedBySsid(ssid, Object.create(accessPointValTemp))));
          this.accessPointVal = this.accessPointVal.concat(accessPointValTemp);

        });
      });
      this.togglePoints = new toggleAccessPointList();
      this.filteredItems = this.accessPointBySsid;
  }

  toggleAccessListSsid(ssid){
    let filterSsid = ssid;
    let toggleSsidList = this.accessPointVal.filter(function(item, index, arr){ 
      if(item.ssid == filterSsid){
        return true;
      }
     });
     toggleSsidList.forEach(element => {
       this.toggleAccessList(element.apid);
     });
  }

  toggleAccessList(event)
  {
    if(this.togglePoints.pointList.indexOf(event.apid) > -1){
      this.togglePoints.pointList =this.removeFromArray(this.togglePoints.pointList, event.apid);
    }else{
      this.togglePoints.pointList.push(event.apid);
    }
  }

  removeFromArray(arrayVals, val){
    const index = arrayVals.indexOf(val, 0);
    if (index > -1) {
      arrayVals.splice(index, 1);
    }
    return arrayVals;
  }

  clickedRow(ap: any){
    ap.whitelisted = !ap.whitelisted;
    this.toggleAccessList(ap);
    if(this.toggleAccessList.length>0){
      this.canSubmit = true;
    }else{
      this.canSubmit = false;
    }
  }

  filterItem(value:any, filterType:string){
    let filter_val = value.target.value;
    if(!filter_val){
      this.filteredItems = this.accessPointBySsid;
    } // when nothing has typed

    if(filterType == "SSID"){
      this.building_model = "";
      this.filteredItems = Object.assign([], this.accessPointBySsid).filter(
        item => item.ssid.toLowerCase().indexOf(filter_val.toLowerCase()) > -1);
    }

    else if(filterType == "BUILDING"){
      this.ssid_model = "";
      let filteredItemsTemp = new Array<accessPointGroupedBySsid>();
      let total_length = Object.keys(this.accessPointBySsid).length;
      for (let iterT = 0; iterT < total_length; iterT++){
        filteredItemsTemp.push( 
                      new accessPointGroupedBySsid( 
                        this.accessPointBySsid[iterT].ssid, 
                        this.accessPointBySsid[iterT].accessPointList
                        .filter(function(lowval, index, arr){
                          return lowval.building_name
                                .toLowerCase()
                                .indexOf(filter_val.toLowerCase()) > -1;
                              }
                        )
                      )
                    )
      };

      this.filteredItems = filteredItemsTemp.filter(function(val, index, arr){
        if(val.accessPointList.length>0){
          return true;
        }
      });
    }
 }

  process(){
    this.isLoading = true;
    this.anyplaceService.toggleAccessPoints(this.togglePoints.pointList).subscribe(
      suc => {
        this.isLoading = false;
        document.getElementById('successModel').click();
        this.fetchData();
      },
      err => { 
        console.log("Error Occured. Please try again.")
      });
    
  }

 reset(){
  this.fetchData();
 }

}