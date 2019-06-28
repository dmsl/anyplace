export class objectHistory{
    buid: string;
    objectCat: string;
    dvid: string;
    coordinates_lat: number;
    coordinates_lon: number;
    floor: number;
    timestamp: string;
    timestampDate: any;
}

export class objectHisoryList{
    constructor(objName:string, history:Array<objectHistory>){
        this.objName = objName;
        this.history = history;       
    }
    objName:string;
    history:Array<objectHistory>;
}