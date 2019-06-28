import { Component, OnInit } from '@angular/core';
import { AnyplaceService } from '../services/anyplace.service';
import { tileLayer, latLng, marker, CRS, MarkerOptions, DivIcon, LatLng, imageOverlay, latLngBounds, Layer, Map, control, Control, DomUtil } from 'leaflet';
import { buildingFloorList } from '../model/buildingFloorList';
import {radioPoint} from '../model/radioPoints';
import { floorCoordinates, floorCoordinatesClass} from '../model/floorCoordinates';
import * as L from 'leaflet';
import HeatmapOverlay from 'leaflet-heatmap/leaflet-heatmap';
import { Options } from 'ng5-slider';

@Component({
  selector: 'app-others',
  templateUrl: './others.component.html',
  styleUrls: ['./others.component.css']
})


export class OthersComponent implements OnInit {

  constructor(private anyplaceService: AnyplaceService) { }

  buildingFloorObj: Array<buildingFloorList>;
  options: any;
  allFloorCoordinates: any = [];
  map: any;
  mapCopy: any;
  maxZoom: number = 21;
  resetProps:string;
  radioPointList: Array<radioPoint>;
  filteredRadioPointList: Array<radioPoint>;
  currentBuilding:string;
  currentFloor:number;
  showFloorLayer:boolean;
  floorLayer: Layer;
  defaultZoom: number = 10;
  mapCenter: LatLng = latLng(46.879966, -121.726909)
  markerLayers: Array<Layer> = [];
  serviceType:string;
  ssidList: Array<string>;
  ssidVal:string;
  pointsLength:number;
  sliderMaxVal: number;
  sliderMaxVal2: number;
  hm:any;
  showSlider:boolean;
  sliderOption:Options;

  ngOnInit() {
    this.resetProps = 'Select Building and Floor';
    this.showFloorLayer = false;
    this.serviceType = "Whitelist";
    this.ssidList = new Array<string>();
    this.ssidVal = 'Select SSID';
    this.sliderMaxVal = 0;
    this.sliderMaxVal2 = 110;
    this.showSlider = false;
    this.sliderOption = {
      floor: 0,
      ceil: 110,
      noSwitching: true
    }

    this.options = {
      layers: [
        tileLayer('https://cartodb-basemaps-a.global.ssl.fastly.net/light_all/{z}/{x}/{y}.png', { maxZoom: this.maxZoom, attribution: '...' })
      ],
      zoomControl: false,
    };

    let buildingFloorTemp;
    this.buildingFloorObj = new Array<buildingFloorList>();
    this.anyplaceService.getFloorCoordinates()
          .subscribe((data: any) => {
            if (data && data.floors) {
              this.allFloorCoordinates = data.floors;    
              // this.getFloorPlan(data.floors); // get showing the info in table;
              buildingFloorTemp = new Array<buildingFloorList>();
              let builTemp = Array<string>();

              this.allFloorCoordinates.forEach(function(val){
                if(builTemp.indexOf(val.building_name)>=0){
                  
                  let floor_obj = buildingFloorTemp[builTemp.indexOf(val.building_name)].floor_obj;
                  if(floor_obj.length >=2){
                    buildingFloorTemp[builTemp.indexOf(val.building_name)].floor_obj[floor_obj.length] = val;
                  }
                  else {
                    buildingFloorTemp[builTemp.indexOf(val.building_name)].floor_obj = [floor_obj[0], val];}
                }
                else{
                  buildingFloorTemp.push(new buildingFloorList(val.building_name, [val]))
                  builTemp.push(val.building_name);
                }
              });   
              this.buildingFloorObj = buildingFloorTemp;
            }
          });

    this.anyplaceService.getAllAccessPoints()
    .subscribe((response: any) => {
      response.accesspoints.forEach(element => {
        this.ssidList.push(Object.keys(element)[0]);
      });
    });
  }

  mapReady(map: Map) {
    map.addControl(control.zoom({ position: 'bottomright' }));
    this.map = map;
  }

  //TRIGGERED WHEN SUBMIT BUTTON IS PRESSED: Also checks if signal strength is related to whitelisted or SSID.
  floorSelected(){
    if(this.resetProps == "Select Building and Floor"){
      alert("Please Select a Building");
      this.showSlider = false;
      return;
    }
    if(this.serviceType == "SSID"){
      if(this.ssidVal == "Select SSID"){
        this.showSlider = false;
        alert("Please Select SSID");
        return;
      }
    }
    this.showSlider = true;
    
    let info = this.resetProps.split(",");
    this.currentBuilding=info[0];
    this.currentFloor = parseInt(info[1]);
    this.radioPointList = new Array<radioPoint>();
    
    this.resetProperties();
    if(this.serviceType == "Whitelist")
    {
      this.anyplaceService.getHeatmapBuidFloorWhitelisted(-110,info[0], info[1])
      .subscribe((response: any)=>{ this.serviceParsing(response, "w") });
    }else
    {
      this.anyplaceService.getHeatmapBuidFloorSSID(this.ssidVal ,info[0], info[1])
      .subscribe((response: any)=>{ this.serviceParsing(response, "s") });
    }
  }
  //Gets the reponse from service and then parses the response for visualization
  serviceParsing(response, serviceType){
    let parsedResponse = response.radioPoints;
    if(parsedResponse.length)
    {
      parsedResponse.forEach(ele => {
        if(serviceType=='s'){
          this.radioPointList.push(new radioPoint(ele.x, ele.y, ele.max_rss+110, 0));
        }else{
          ele.rss.forEach(element => {
            this.radioPointList.push(new radioPoint(ele.x, ele.y, element+110, ele.rss.length));
          });
        }
      }); 
      this.sliderValueR();
      this.filteredRadioPointList = Object.assign([], this.radioPointList);
      this.pointsLength = this.filteredRadioPointList.length;
      // this.sortObjectHistoryFloor();
      //Trigers visualization
      this.getFloorCoordinates();
    }
  }

  sortObjectHistoryFloor() {
    this.getFloorCoordinates();
  }

  getFloorCoordinates() {
    if (this.allFloorCoordinates.length > 0) {
      this.getFloorPlan(this.allFloorCoordinates);
    } else {
      this.anyplaceService.getFloorCoordinates()
        .subscribe((data: any) => {
          if (data && data.floors) {
            this.allFloorCoordinates = data.floors;    
            this.getFloorPlan(data.floors); // get showing the info in table;
          }
        });
    }
  }

  getFloorPlan(allFloors: any) {
    let floorCoordinates: floorCoordinates;
    floorCoordinates = allFloors.find(x => (x.buid === this.currentBuilding && x.floor_number === this.currentFloor.toString()));

    if (sessionStorage.getItem(this.currentBuilding + ';' + this.currentFloor)) {
      this.setFloorLayer(floorCoordinates, sessionStorage.getItem(this.currentBuilding + ';' + this.currentFloor));
    } 
    else {
      this.anyplaceService.getFloorPlan(this.currentBuilding, this.currentFloor)
        .subscribe((imageBase64: any) => {

          if (imageBase64) {
            sessionStorage.setItem(this.currentBuilding + ';' + this.currentFloor, imageBase64);
            this.setFloorLayer(floorCoordinates, imageBase64);
          }
      });
    }
  }

  setFloorLayer(floorCoordinates: floorCoordinates, imageBase64: string) {
    this.showFloorLayer = true;
    let imageBounds = latLngBounds(latLng(floorCoordinates.bottom_left_lat, floorCoordinates.bottom_left_lng),
      latLng(floorCoordinates.top_right_lat, floorCoordinates.top_right_lng));
    this.floorLayer = imageOverlay('data:image/png;base64,' + imageBase64, imageBounds, { interactive: true })
      .bindPopup('<b>Building Name: </b>' + floorCoordinates.building_name + '<br/><b>Floor Number: </b>' + floorCoordinates.floor_number);
    
    this.setMarkerLayer();
    this.setMapProperties(21, imageBounds.getCenter())
  }

  private setMapProperties(zoom: number, center: LatLng) {
    this.defaultZoom = zoom;
    this.mapCenter = center;
  }

  setMarkerLayer() {
    this.markerLayers = [];        
    let testData = {
      max: 400,
      min: 1,
      data: []
    };

    this.filteredRadioPointList.forEach((item: any, i:number) => {     
      if(this.serviceType == 's'){ 
        testData.data.push({lat:item.x, lng:item.y, count:item.macList});}
      else{
        testData.data.push({lat:item.x, lng:item.y, count:item.countPoints});}
    });
    
    const cfg = {
      'radius': 30,
      'maxOpacity': 1,
      'scaleRadius': false,
      'useLocalExtrema': true,
      latField: 'lat',
      lngField: 'lng',
      valueField: 'count'
    };
    this.mapCopy = Object.assign([], this.map);
    this.hm = new HeatmapOverlay(cfg);
    this.hm.setData(testData);
    this.hm.onAdd(this.map);
  }

  private resetProperties() {
    this.markerLayers = [];
    this.showFloorLayer = false;
    this.defaultZoom = 10;
    if(document.getElementsByClassName('leaflet-zoom-hide').length>0)
      {
        document.getElementsByClassName('leaflet-zoom-hide')[0].remove();
      }
  }

  sliderValueR(){
    if(document.getElementsByClassName('ng5-slider-floor').length>0)
      document.getElementsByClassName('ng5-slider-floor')[0].remove();
    if(document.getElementsByClassName('ng5-slider-ceil').length>0)
      document.getElementsByClassName('ng5-slider-ceil')[0].remove();
    if(document.getElementsByClassName('ng5-slider-model-value').length>0)
      document.getElementsByClassName('ng5-slider-model-value')[0].remove();
    if(document.getElementsByClassName('ng5-slider-model-high').length>0)
      document.getElementsByClassName('ng5-slider-model-high')[0].remove();
    if(document.getElementsByClassName('ng5-slider-combined').length>0)
      document.getElementsByClassName('ng5-slider-combined')[0].remove();
  }

  sliderRange(){
    this.sliderValueR();
    this.filteredRadioPointList = new Array<radioPoint>();
    let item = new Array<radioPoint>();
    let min = this.sliderMaxVal ;
    let max = this.sliderMaxVal2;
    this.radioPointList.forEach(function(ele){
      if(ele.macList < max && ele.macList > min){
        item.push(ele);
      }
    });
    this.filteredRadioPointList = item;
    this.resetProperties();
    this.getFloorCoordinates();
  }

  resetPropsO(){
    this.filteredRadioPointList = new Array<radioPoint>();
    this.radioPointList = new Array<radioPoint>();
    this.showSlider = false;
    if(this.currentBuilding && this.currentFloor){
      this.resetProperties();
      this.getFloorCoordinates();
    }
  }
}

// private getMarkerOptions(color: string, title?: string, zIndexNUmber?: number): MarkerOptions {

//   let myCustomColor = color;
//   let markerHtmlStyles = `
//     background-color: ${myCustomColor};
//     width: 3rem;
//     height: 3rem;
//     display: block;
//     left: -1.5rem;
//     top: -1.5rem;
//     position: relative;
//     border-radius: 3rem 3rem 0;
//     transform: rotate(45deg);
//     `
//   let markerOptions: MarkerOptions = {
//     title: title,
//     zIndexOffset: zIndexNUmber,
//     icon: new DivIcon({
//       className: 'my-custom-pin',
//       iconAnchor: [0, 24],
//       iconSize: [-6, 0],
//       popupAnchor: [0, -36],
//       html: `<span class='customdesign' style="${markerHtmlStyles}" >${title}</span>`
//     })
//   }
//   return markerOptions;
// }