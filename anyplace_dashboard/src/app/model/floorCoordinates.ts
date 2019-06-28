export class floorCoordinates {
    buid: string;
    floor_name: string;
    top_right_lat: number;
    is_published: string;
    username_creator?: any;
    bottom_left_lat: number;
    description: string;
    floor_number: number;
    fuid: string;
    zoom: string;
    top_right_lng: number;
    bottom_left_lng: number;
    building_name:string;
}

export class floorCoordinatesClass{
    // constructor(buid: string, floor_name: string, top_right_lat: number,is_published: string, username_creator?: any,bottom_left_lat: number,description: string, floor_number: number,fuid: string,zoom: string,top_right_lng: number,bottom_left_lng: number){
    constructor(val:any){
        this.buid = val.buid;
        this.floor_name = val.floor_name;
        this.top_right_lat = val.top_right_lat;
        this.top_right_lng = val.top_right_lng;
        this.is_published = val.is_published;
        this.username_creator = val.username_creator;
        this.bottom_left_lat = val.bottom_left_lat;
        this.bottom_left_lng = val.bottom_left_lng;
        this.description = val.description;
        this.floor_number = val.floor_number;
        this.fuid = val.fuid;
        this.zoom = val.zoom;        
    }

    buid: string;
    floor_name: string;
    top_right_lat: number;
    is_published: string;
    username_creator?: any;
    bottom_left_lat: number;
    description: string;
    floor_number: number;
    fuid: string;
    zoom: string;
    top_right_lng: number;
    bottom_left_lng: number;
}