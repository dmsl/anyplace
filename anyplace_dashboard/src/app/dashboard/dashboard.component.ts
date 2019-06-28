import { Component, OnInit } from '@angular/core';
import { AnyplaceService } from '../services/anyplace.service';
import { tileLayer, latLng, marker, CRS, MarkerOptions, DivIcon, LatLng, imageOverlay, latLngBounds, Layer, Map, control, Control, DomUtil } from 'leaflet';
import { category } from '../model/category';
import { objectHistory, objectHisoryList } from '../model/objectHistory';
import { floorCoordinates, floorCoordinatesClass} from '../model/floorCoordinates';
import { formatDate } from '@angular/common';
import { interval } from 'rxjs';
import { flatMap } from 'rxjs/operators'
import { buildingFloorList } from '../model/buildingFloorList';
@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {

  constructor(private anyplaceService: AnyplaceService) { }

  categories: Array<category> = new Array<category>();
  selectedCatList: Array<string>;
  selectObjList: Array<string>;
  selectedItem: string;
  locationCount: number = 10;
  maxLocationCount: number;
  options: any;
  blnCategorySelected: boolean = false;
  blnObjectSelected: boolean = false;
  defaultZoom: number = 10;
  maxZoom: number = 21;
  mapCenter: LatLng = latLng(46.879966, -121.726909)
  markerLayers: Array<Layer> = [];
  floorLayer: Layer;
  showFloorLayer: boolean = false;
  objLHistory: Array<objectHistory> = [];
  objLHistoryFiltered: Array<objectHistory> = [];
  currentFloor: number;
  currentBuilding: string;
  currentTimestamp: string;
  allFloorCoordinates: any = [];
  map: any;
  markerColorCodes = ['rgb(255,253,228)', 'rgb(0,85,170)'];
  legend: any;
  blnLegend: boolean = false;
  startColor = "#0B5D14";
  endColor = "AD0D17";
  buildingFloorObj: Array<buildingFloorList>;
  filterModelTable: string;
  hideTableLayout: boolean;
  objectHistoryList: Array<objectHisoryList>;
  objectHistoryArray: Array<objectHistory>;
  hideFloorFilters: boolean;
  hideCategoryTableLayout:boolean;
  hideObjectTableLayout:boolean;
  filteredValues: any;
  objectTypeFilter: any;
  startDateFilter: any;
  endDateFilter: any;
  selectObjectForHistory: string;
  resetProps:string;
  objHistory:boolean;
  objHistorySlider:boolean;
  resetPropsObj:string;
  objHistoryLength:number;
  objLHistorySlider: Array<objectHistory> = [];
  visibleElements:number;

  ngOnInit() {
    this.initializeLegend();
    this.hideTableLayout = true;
    this.hideFloorFilters = true;
    this.hideObjectTableLayout = true;
    this.hideCategoryTableLayout = false;
    this.objHistory = true;
    this.objHistorySlider = false;
    this.resetProps = 'Select Building and Floor';
    this.resetPropsObj = 'Select a Object';
    this.anyplaceService.getAllCategories()
      .subscribe((response: any) => {
        if (response && response.categories) {
          
          let oTempVal = new Array<string>();
          response.categories.forEach(element => {
            let tempVal:any = Object.values(element)[0];  
            tempVal.forEach(function(t){
              if(oTempVal.indexOf(t) === -1) {
                oTempVal.push(t);
              }
            });
            
            // this.selectObjList.push(...Object.values(element)[0]);
            this.categories.push(new category(Object.keys(element).toString(), Object.values(element)));
          });
          this.selectObjList = Object.assign([], oTempVal);
        }
      });

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

  }

  //to show Zoom Control on Map
  mapReady(map: Map) {
    map.addControl(control.zoom({ position: 'bottomright' }));
    this.map = map;

  }

  getItems() {
    let locationCounters: Array<number> = [];
    this.maxLocationCount = (this.maxLocationCount > 30) ? 30 : this.maxLocationCount;
    for (let i = 0; i <= this.maxLocationCount; i += 5) {
      locationCounters.push(i);
    }
    return locationCounters;
  }

  categorySelectionChange(selectedCategory: any) {
    if (selectedCategory.target.value) {
      // this.selectedCatList = this.categories.find(x => x.name === selectedCategory.target.value).items;
      // this.blnCategorySelected = true;
      this.anyplaceService.getHistorySummary(selectedCategory.target.value)
          .subscribe((response:any) =>{
            let tempEle = [];
            response.lHistorySummary.forEach((element:any)=>{
              var d = new Date(parseInt(element.timestamp));
              element.timestampDate = d.toLocaleDateString() + ' ' + d.toLocaleTimeString(); 
              tempEle.push(element);
            });
            this.filteredValues = tempEle;
            this.hideTableLayout = false;
            this.hideFloorFilters = true;
            this.hideObjectTableLayout = false;
            this.hideCategoryTableLayout = true;
            this.objHistory = false;
            this.objHistorySlider = false;
            this.selectObjectForHistory = selectedCategory.target.value;
            this.resetProps = 'Select Building and Floor';
          });
      this.resetProperties();
    }
  }

  triggerCategoryChange(selectedCategory: any) {
    
      // this.selectedCatList = this.categories.find(x => x.name === selectedCategory.target.value).items;
      // this.blnCategorySelected = true;
    this.anyplaceService.getHistorySummary(selectedCategory)
        .subscribe((response:any) =>{
          let tempEle = [];
          response.lHistorySummary.forEach((element:any)=>{
            var d = new Date(parseInt(element.timestamp));
            element.timestampDate = d.toLocaleDateString() + ' ' + d.toLocaleTimeString();
            tempEle.push(element);
          });
          this.filteredValues = tempEle;
          this.hideTableLayout = false;
          this.hideFloorFilters = true;
          this.hideObjectTableLayout = false;
          this.hideCategoryTableLayout = true;
        });
    this.resetProperties();
    
  }

  locationCountChange(number: any) {
    this.locationCount = number;
    this.setMarkerLayer();
  }

  objectSelectionChange(value: any) {

    let objHistory: Array<objectHistory> = [];
    if (value) {
      this.anyplaceService.getSelectedItemHistory(value)
        .subscribe((response: any) => {
          if (response && response.lHistory) {
            this.defaultZoom = this.maxZoom;
            this.sortObjectHistory(response, objHistory);
          }
        });
    }
  }

  sortObjectHistory(response: any, objHistory: Array<objectHistory>) {

    response.lHistory.forEach((element: any) => {
      let object: objectHistory = element;
      objHistory.push(object);
    });
    let sortedObjectHistory = objHistory.sort((a: any, b: any) =>
      new Date(formatDate(b.timestamp, 'medium', 'en-US')).getTime() - new Date(formatDate(a.timestamp, 'medium', 'en-US')).getTime()
    );

    this.objLHistory = sortedObjectHistory;
    this.maxLocationCount = sortedObjectHistory.length;
    this.getFloorCoordinates();
  }

  sortObjectHistoryFloor(objHistory: Array<objectHistory>) {

    // response.lHistory.forEach((element: any) => {
    //   let object: objectHistory = element;
    //   objHistory.push(object);
    // });
    let sortedObjectHistory = objHistory.sort((a: any, b: any) =>
      new Date(formatDate(b.timestamp, 'medium', 'en-US')).getTime() - new Date(formatDate(a.timestamp, 'medium', 'en-US')).getTime()
    );

    this.objLHistory = sortedObjectHistory;
    this.objLHistoryFiltered = sortedObjectHistory;
    this.maxLocationCount = sortedObjectHistory.length;
    this.getFloorCoordinates();
  }
  
  floorSelected(event){
    if(event.target.value == "Select Building and Floor"){
      return;
    }
    let info = event.target.value.split(",");
    this.anyplaceService.getAllLocationHistoryBuidFnum(info[0], info[1])
      .subscribe((response: any)=>{
        let parsedResponse = JSON.parse(response).lHistory;
        let objectHistoryTemp = new Array<objectHistory>();
        let objectHistoryListTemp = new Array<objectHisoryList>();
        this.objectHistoryList =  new Array<objectHisoryList>();
        this.hideTableLayout = false;
        this.hideFloorFilters = true;
        this.hideObjectTableLayout = true;
        this.hideCategoryTableLayout = false;
        this.objHistory = true;
        this.objHistorySlider = false;
        this.resetPropsObj = 'Select a Object';

        // document.getElementById('toggleClick').click();
        if(Object.keys(parsedResponse).length)
        {
          parsedResponse.forEach(ele => {
              let ids = Object.keys(ele)[0];
              let objectHistoryTempInner = new Array<objectHistory>();
              let arrayObj:any = Object.values(ele)[0];
              arrayObj.forEach(element => {
                var d = new Date(parseInt(element.timestamp));
                element.timestampDate = d.toLocaleDateString() + ' ' + d.toLocaleTimeString();
                objectHistoryTempInner.push(element);  
              });
              // objectHistoryTemp.push(...objectHistoryTempInner);
              objectHistoryTemp.push(objectHistoryTempInner[0]);
              objectHistoryListTemp.push(new objectHisoryList(ids, objectHistoryTempInner));
          });
          // this.objectHistoryArray = objectHistoryTemp;          
          this.objectHistoryList = objectHistoryListTemp;
          this.filteredValues = objectHistoryListTemp;
          this.defaultZoom = this.maxZoom;
          if (this.blnLegend) {
            this.legend.remove();
            this.blnLegend = false;
          }
          this.sortObjectHistoryFloor(objectHistoryTemp);
        }else{
          this.filteredValues = null;
          this.hideFloorFilters = false;
          this.resetProperties();
        }

      });
  }

  getFloorCoordinates() {

    if (this.objLHistory.length > 0) {
      
      this.currentBuilding = this.objLHistory[0].buid;
      this.currentFloor = this.objLHistory[0].floor;
      this.currentTimestamp = this.objLHistory[0].timestamp;
      

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
  }

  getFloorPlan(allFloors: any) {
    let floorCoordinates: floorCoordinates;
    floorCoordinates = allFloors.find(x => (x.buid === this.currentBuilding && x.floor_number === this.currentFloor.toString()));

    if (sessionStorage.getItem(this.currentBuilding + ';' + this.currentFloor)) {
      this.setFloorLayer(floorCoordinates, sessionStorage.getItem(this.currentBuilding + ';' + this.currentFloor));
    } else {
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

    //setting floor layer for selected building/floor
    this.showFloorLayer = true;
    let imageBounds = latLngBounds(latLng(floorCoordinates.bottom_left_lat, floorCoordinates.bottom_left_lng),
      latLng(floorCoordinates.top_right_lat, floorCoordinates.top_right_lng));
    this.floorLayer = imageOverlay('data:image/png;base64,' + imageBase64, imageBounds, { interactive: true })
      .bindPopup('<b>Building Name: </b>' + floorCoordinates.building_name + '<br/><b>Floor Number: </b>' + floorCoordinates.floor_number);
    this.setMapProperties(this.defaultZoom, imageBounds.getCenter())
    
    this.setMarkerLayer();
  }

  setMarkerLayer() {
    //setting marker layer for selected object
    let title;
    this.markerLayers = [];
    this.blnObjectSelected = true;
    let showObjectHistory = this.objLHistory.filter((item, idx) => {
      return idx < this.locationCount;
    });
    let total_Count = (200/showObjectHistory.length);

    showObjectHistory.forEach((item: any, i:number) => {
      let obj = item.obid? item.obid:this.selectObjectForHistory;
      title = obj + ' ; ' + formatDate(item.timestamp, 'medium', 'en-IN');// + ' ; ' + 'Near to Pillar 7';
      let c = this.markerColorCodes[1];
      if(this.objHistorySlider)
        {c = this.RGB_Linear_Blend(total_Count*i/200, this.markerColorCodes[1], this.markerColorCodes[0]);}
        // this.blnLegend = false;
      this.markerLayers.push(marker([item.coordinates_lat, item.coordinates_lon], this.getMarkerOptions(c, title, 1000-i)));  //green 
    });
    
  }

  private resetProperties() {
    this.blnObjectSelected = false;
    this.selectedItem = '';
    this.currentBuilding = '';
    this.currentTimestamp = '';
    this.markerLayers = [];
    this.showFloorLayer = false;
    this.defaultZoom = 10;
    if (this.blnLegend) {
      this.legend.remove();
      this.blnLegend = false;
    }
  }

  private setMapProperties(zoom: number, center: LatLng) {
    this.defaultZoom = zoom;
    this.mapCenter = center;
  }

  private getMarkerOptions(color: string, title?: string, zIndexNUmber?: number): MarkerOptions {

    let myCustomColor = color;
    let markerHtmlStyles = `
      background-color: ${myCustomColor};
      width: 3rem;
      height: 3rem;
      display: block;
      left: -1.5rem;
      top: -1.5rem;
      position: relative;
      border-radius: 3rem 3rem 0;
      transform: rotate(45deg);
      `
    let markerOptions: MarkerOptions = {
      title: title,
      zIndexOffset: zIndexNUmber,
      icon: new DivIcon({
        className: 'my-custom-pin',
        iconAnchor: [0, 24],
        iconSize: [-6, 0],
        popupAnchor: [0, -36],
        html: `<span class='customdesign' style="${markerHtmlStyles}" />`
      })
    }
    return markerOptions;
  }

  private initializeLegend() {
    this.legend = new Control({ position: 'bottomleft' });
    let colorCodes = this.markerColorCodes;

    this.legend.onAdd = function (map) {
      let div = DomUtil.create('div', 'info legend');
      let labels = ['<strong>Marker Color Categories</strong>'],
        categories = ['Oldest location', 'Most recent location'];

      for (let i = 0; i < categories.length; i++) {
        div.innerHTML +=
          labels.push(
            '<i style="background:' + colorCodes[i] + ';color:' + colorCodes[i] + ';border:1px solid black;">color</i> ' +
            (categories[i] ? categories[i] : '+'));

      }
      div.innerHTML = labels.join('<br>');
      return div;
    };
  }

  parseDate(input) {
    var parts = input.split('-');
    // new Date(year, month [, day [, hours[, minutes[, seconds[, ms]]]]])
    return new Date(parts[0], parts[1]-1, parts[2]); // Note: months are 0-based
  }

  filterItem(){
    this.filteredValues = Object.assign([], this.objectHistoryList);
    this.objLHistory = Object.assign([], this.objLHistoryFiltered);

    if(this.objectTypeFilter){
      this.filteredValues = Object.assign([], this.filteredValues).filter(
        item => item.objName.toLowerCase().indexOf(this.objectTypeFilter.toLowerCase()) > -1 || item.history[0].objcat.toLowerCase().indexOf(this.objectTypeFilter.toLowerCase()) > -1
      );
      this.objLHistory = Object.assign([], this.objLHistory).filter(
        item => item.obid.toLowerCase().indexOf(this.objectTypeFilter.toLowerCase()) > -1 || item.objcat.toLowerCase().indexOf(this.objectTypeFilter.toLowerCase()) > -1
      );
    }
    
    if(this.endDateFilter){
      this.filteredValues = Object.assign([], this.filteredValues).filter(
        item => item.history[0].timestamp < new Date(this.parseDate(this.endDateFilter))
        );

      this.objLHistory = Object.assign([], this.objLHistory).filter(
        item => item.timestamp < new Date(this.parseDate(this.endDateFilter))
        )
    }
    
    if(this.startDateFilter){
      this.filteredValues = Object.assign([], this.filteredValues).filter(
        item => item.history[0].timestamp > new Date(this.parseDate(this.startDateFilter))
        );

      this.objLHistory = Object.assign([], this.objLHistory).filter(
        item => item.timestamp > new Date(this.parseDate(this.startDateFilter))
        );
    }
    
    this.setMarkerLayer();
  }
  resetClick(){
    this.filteredValues = Object.assign([], this.objectHistoryList);
    this.objectTypeFilter = this.startDateFilter = this.endDateFilter = '';
    this.objLHistory = Object.assign([], this.objLHistoryFiltered);
    this.setMarkerLayer();
  }

  loadHistorySummary(cat){
    // document.getElementById("toggleClick").click();
    this.selectObjectForHistory = cat.objName;
    this.objHistory = false;
    this.triggerCategoryChange(cat.objName);
    //call the flow of the left design.
  }

  loadHistory(cat){
    this.anyplaceService.getSelectedItemHistoryWithBuid(this.selectObjectForHistory, cat.buid, cat.floor)
        .subscribe((response:any)=>{
          let objectHistoryTempInner = new Array<objectHistory>();

          response.lHistory.forEach(element=>{
            var d = new Date(parseInt(element.timestamp));
            element.timestampDate = d.toLocaleDateString() + ' ' + d.toLocaleTimeString();
            objectHistoryTempInner.push(element);
          });
          this.objHistoryLength = objectHistoryTempInner.length;
          this.defaultZoom = this.maxZoom;
          this.objHistorySlider = true;
          this.visibleElements = this.objHistoryLength;
          this.objLHistorySlider = Object.assign([], objectHistoryTempInner);
          this.legend.addTo(this.map);
          this.blnLegend = true;
          this.sortObjectHistoryFloor(objectHistoryTempInner);
        });
  }

  sliderRange(event){
    this.visibleElements = event.target.value;
    this.sortObjectHistoryFloor(this.objLHistorySlider.slice(0,event.target.value));
  }

  //TAKEN FROM https://github.com/PimpTrizkit/PJs/wiki/12.-Shade,-Blend-and-Convert-a-Web-Color-(pSBC.js)#stackoverflow-archive-begin
  RGB_Linear_Blend(p,c0,c1){
    let i=parseInt,r=Math.round,P=1-p,[a,b,c,d]=c0.split(","),[e,f,g,h]=c1.split(","),x=d||h;
    return"rgb"+(x?"a(":"(")+r(i(a[3]=="a"?a.slice(5):a.slice(4))*P+i(e[3]=="a"?e.slice(5):e.slice(4))*p)+","+r(i(b)*P+i(f)*p)+","+r(i(c)*P+i(g)*p)+")";
  }

}