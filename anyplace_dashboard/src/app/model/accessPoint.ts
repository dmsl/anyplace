export class accessPoint {

    constructor(building_name: string, whitelisted: boolean, floor:number, ssid: string, mac: string, apid:string) {
        this.building_name = building_name;
        this.whitelisted = whitelisted;
        this.floor = floor;
        this.ssid = ssid;
        this.mac = mac;
        this.apid = apid;
    }
    building_name: string;
    whitelisted: boolean;
    floor:number;
    ssid: string;
    mac: string;
    apid: string;
}

export class accessPointGroupedBySsid{
    constructor(ssid: string, accessPointList: Array<accessPoint>){
        this.ssid = ssid;
        this.accessPointList = accessPointList;
    }
    ssid:string;
    accessPointList: Array<accessPoint>;
}