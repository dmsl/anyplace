# CHANGELOG: nneofy01 thesis:  
You can use the Anyplace service or Postman [Instructions](./POSTMAN.md) in some cases to evaluate the bellow endpoints.

### Icon map:
- BUGFIX: <img src="https://www.iconsdb.com/icons/preview/green/bug-2-xxl.png" height="20" />
- IMPROVED: <img src="https://uploads-ssl.webflow.com/593720e6eee8942f4c1ba6e5/5b1af17ac3b6110f42ee34c6_Talix-Icon-Library.2-82.png" height="18" />
- EXTENDED: <img src="https://www.iconsdb.com/icons/preview/green/new-badge-3-xxl.png" height="18" />

# IMPLEMENTED (51):
<details> 
<summary>
list (51)
</summary>

##### Users
> - /anyplace/mapping/accounts/sign
>     + users have roles and first user becomes an `admin`
> - /anyplace/debug/accounts_all<img src="https://www.iconsdb.com/icons/preview/green/new-badge-3-xxl.png" height="18" />
>     + used only by 'users' that are 'admins'

##### Buildings
> - /anyplace/mapping/building/add
> - /anyplace/mapping/building/update<img src="https://www.iconsdb.com/icons/preview/green/bug-2-xxl.png" height="20" />
> - /anyplace/mapping/building/delete<img src="https://www.iconsdb.com/icons/preview/green/bug-2-xxl.png" height="20" />
> - /anyplace/mapping/building/all 
> - /anyplace/mapping/building/all_owner 
> - /anyplace/mapping/building/coordinates<img src="https://uploads-ssl.webflow.com/593720e6eee8942f4c1ba6e5/5b1af17ac3b6110f42ee34c6_Talix-Icon-Library.2-82.png" height="18" />
>     + `owner_id` not required
>     +  accepts a range, with a reasonable default one now (e.g. 100 meters)
> - /anyplace/mapping/building/coowners<img src="https://www.iconsdb.com/icons/preview/green/bug-2-xxl.png" height="20" />
> - /anyplace/mapping/building/all_bucode
> - /anyplace/mapping/building/newowner<img src="https://www.iconsdb.com/icons/preview/green/bug-2-xxl.png" height="20" /><img src="https://uploads-ssl.webflow.com/593720e6eee8942f4c1ba6e5/5b1af17ac3b6110f42ee34c6_Talix-Icon-Library.2-82.png" height="18" />
    
##### Floors
> - /anyplace/mapping/floor/add<img src="https://www.iconsdb.com/icons/preview/green/bug-2-xxl.png" height="20" />
> - /anyplace/mapping/floor/delete<img src="https://www.iconsdb.com/icons/preview/green/bug-2-xxl.png" height="20" />
> - /anyplace/mapping/floor/uploadWithZoom<img src="https://www.iconsdb.com/icons/preview/green/bug-2-xxl.png" height="20" />
> - /anyplace/mapping/floor/all<img src="https://www.iconsdb.com/icons/preview/green/bug-2-xxl.png" height="20" />
> - /anyplace/mapping/floor/update
> - /anyplace/mapping/floor/upload

##### POIs 
> - /anyplace/mapping/pois/add<img src="https://www.iconsdb.com/icons/preview/green/bug-2-xxl.png" height="20" />
> - /anyplace/mapping/pois/delete<img src="https://www.iconsdb.com/icons/preview/green/bug-2-xxl.png" height="20" />
> - /anyplace/mapping/pois/update<img src="https://www.iconsdb.com/icons/preview/green/bug-2-xxl.png" height="20" />
> - /anyplace/mapping/pois/all_floor<img src="https://www.iconsdb.com/icons/preview/green/bug-2-xxl.png" height="20" /><img src="https://uploads-ssl.webflow.com/593720e6eee8942f4c1ba6e5/5b1af17ac3b6110f42ee34c6_Talix-Icon-Library.2-82.png" height="18" />
> - /anyplace/mapping/pois/all_building<img src="https://www.iconsdb.com/icons/preview/green/bug-2-xxl.png" height="20" /><img src="https://uploads-ssl.webflow.com/593720e6eee8942f4c1ba6e5/5b1af17ac3b6110f42ee34c6_Talix-Icon-Library.2-82.png" height="18" />
> - /anyplace/mapping/pois/all_pois_nconnectors

##### Campuses 
> - /anyplace/mapping/campus/add
> - /anyplace/mapping/campus/update
> - /anyplace/mapping/campus/delete
> - /anyplace/mapping/campus/all_cucode<img src="https://www.iconsdb.com/icons/preview/green/bug-2-xxl.png" height="20" /><img src="https://uploads-ssl.webflow.com/593720e6eee8942f4c1ba6e5/5b1af17ac3b6110f42ee34c6_Talix-Icon-Library.2-82.png" height="18" />
> - /anyplace/mapping/campus/all_owner

##### Connections 
> - /anyplace/mapping/connection/add<img src="https://www.iconsdb.com/icons/preview/green/bug-2-xxl.png" height="20" />
> - /anyplace/mapping/connection/delete<img src="https://www.iconsdb.com/icons/preview/green/bug-2-xxl.png" height="20" />
> - /anyplace/mapping/connection/all_floor<img src="https://www.iconsdb.com/icons/preview/green/bug-2-xxl.png" height="20" />
> - /anyplace/mapping/connection/all_floors<img src="https://www.iconsdb.com/icons/preview/green/bug-2-xxl.png" height="20" />
> - /anyplace/mapping/connection/update

##### Navigation
> - /anyplace/navigation/route_xy<img src="https://www.iconsdb.com/icons/preview/green/bug-2-xxl.png" height="20" /><img src="https://uploads-ssl.webflow.com/593720e6eee8942f4c1ba6e5/5b1af17ac3b6110f42ee34c6_Talix-Icon-Library.2-82.png" height="18" />
> - /anyplace/navigation/route<img src="https://www.iconsdb.com/icons/preview/green/bug-2-xxl.png" height="20" />
> - /anyplace/navigation/building/id<img src="https://www.iconsdb.com/icons/preview/green/bug-2-xxl.png" height="20" />
> - /anyplace/navigation/pois/id<img src="https://www.iconsdb.com/icons/preview/green/bug-2-xxl.png" height="20" />

##### Position
> - /anyplace/position/radio_upload<img src="https://www.iconsdb.com/icons/preview/green/bug-2-xxl.png" height="20" /><img src="https://uploads-ssl.webflow.com/593720e6eee8942f4c1ba6e5/5b1af17ac3b6110f42ee34c6_Talix-Icon-Library.2-82.png" height="18" />
> - /anyplace/position/radio_by_building_floor<img src="https://www.iconsdb.com/icons/preview/green/bug-2-xxl.png" height="20" /> 
> - /anyplace/position/radio/delete<img src="https://www.iconsdb.com/icons/preview/green/bug-2-xxl.png" height="20" />
> - /anyplace/position/radio/delete/time<img src="https://www.iconsdb.com/icons/preview/green/bug-2-xxl.png" height="20" />
> - /anyplace/position/radio/time
> - /anyplace/position/radio_by_building_floor_all<img src="https://www.iconsdb.com/icons/preview/green/bug-2-xxl.png" height="20" />
> - /anyplace/position/radio/APs_building_floor<img src="https://www.iconsdb.com/icons/preview/green/bug-2-xxl.png" height="20" /><img src="https://uploads-ssl.webflow.com/593720e6eee8942f4c1ba6e5/5b1af17ac3b6110f42ee34c6_Talix-Icon-Library.2-82.png" height="18" />
   
#### IMPLEMENTED > crashed or did not work well in CouchDB (2): <img src="https://www.iconsdb.com/icons/preview/green/bug-2-xxl.png" height="20" /><img src="https://uploads-ssl.webflow.com/593720e6eee8942f4c1ba6e5/5b1af17ac3b6110f42ee34c6_Talix-Icon-Library.2-82.png" height="20" />
<details> 
<summary>
list (2)
</summary>
    
> - /anyplace/position/radio_download_floor
> - /anyplace/position/radio_by_floor_bbox
    
</details>
   
#### IMPLEMENTED > Changed response structure (5): <img src="https://www.iconsdb.com/icons/preview/green/bug-2-xxl.png" height="20" />
<details> 
<summary>
list (5)
</summary>
    
JSon response was optimized/fixed, but JavaScript has to be updated.  
The response was a JSon `{x: "", y:"", w:"embedded_String_JSon"}`.  
Now the response is a JSon `{"location": GeoJson, "count":int, "sum": int, "avg":double}`.  
They can be checked with tools like postman. 

> - /anyplace/mapping/radio/heatmap_building_floor
> - /anyplace/position/radio/heatmap_building_floor_average_1<img src="https://uploads-ssl.webflow.com/593720e6eee8942f4c1ba6e5/5b1af17ac3b6110f42ee34c6_Talix-Icon-Library.2-82.png" height="18" />
> - /anyplace/position/radio/heatmap_building_floor_average_2<img src="https://uploads-ssl.webflow.com/593720e6eee8942f4c1ba6e5/5b1af17ac3b6110f42ee34c6_Talix-Icon-Library.2-82.png" height="18" />
> - /anyplace/position/radio/heatmap_building_floor_average_3<img src="https://uploads-ssl.webflow.com/593720e6eee8942f4c1ba6e5/5b1af17ac3b6110f42ee34c6_Talix-Icon-Library.2-82.png" height="18" />
> - /anyplace/position/radio/heatmap_building_floor_average_3_tiles<img src="https://uploads-ssl.webflow.com/593720e6eee8942f4c1ba6e5/5b1af17ac3b6110f42ee34c6_Talix-Icon-Library.2-82.png" height="18" />

</details>
    
</details>

  
# UNIMPLEMENTED (15)
<details> 
<summary>
list (15)
</summary>
    
#### UNIMPLEMENTED: Not working in Couchbase (5):

<details> 
<summary>
list (5)
</summary>
    
Couchbase views are **disabled** for these because storage space runs out.  
A study and some initial work was made to support these optimally in MongoDB.  

> - /anyplace/position/radio/heatmap_building_floor_timestamp
> - /anyplace/position/radio/heatmap_building_floor_timestamp_average_1
> - /anyplace/position/radio/heatmap_building_floor_timestamp_average_2
> - /anyplace/position/radio/heatmap_building_floor_timestamp_average_3
> - /anyplace/position/radio/heatmap_building_floor_timestamp_tiles
</details>

#### UNIMPLEMENTED: Not in use (10):

<details> 
<summary>
list (10)
</summary>
    
Will not be implemented as they are **not** in use by the web or the Android apps.   
After reworking the database they are not necessary.
  
> - /accounts/oauth2/token
> - /anyplace/position/estimate_position
> - /anyplace/position/path_add
> - /anyplace/position/path_delete
> - /anyplace/position/paths_by_floor
> - /anyplace/position/paths_by_buid
> - /anyplace/position/milestones_add
> - /anyplace/position/milestones_by_floor
> - /anyplace/mapping/radio/radio_buid_floor
> - /anyplace/mapping/maintenance

</details>    
    
</details>
 