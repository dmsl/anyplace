
import { floorCoordinatesClass } from './floorCoordinates';

export class buildingFloorList{
    constructor(building_name: string, floor_obj:Array<floorCoordinatesClass>){
        this.building_name = building_name;
        this.floor_obj = floor_obj;
    }

    building_name:string;
    floor_obj:Array<floorCoordinatesClass>;
}