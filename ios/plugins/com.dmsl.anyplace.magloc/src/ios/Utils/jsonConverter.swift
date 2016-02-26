//
//  jsonConverter.swift
//  AnyplaceSwift
//
//  Created by Antria Kkoushi on 25/04/15.
//  Copyright (c) 2015 Antria Kkoushi. All rights reserved.
//

import Foundation

func JSONToString(value: AnyObject, pretty: Bool = false) -> String? {
    let options: NSJSONWritingOptions = pretty ? NSJSONWritingOptions.PrettyPrinted : []
    do {
        let data = try NSJSONSerialization.dataWithJSONObject(value, options: options)
        let string = NSString(data: data, encoding: NSUTF8StringEncoding) as NSString! as String
        return string
    } catch {
        return nil
    }
   
}

func StringToJSON (string : NSString) -> NSDictionary? {
    let data = string.dataUsingEncoding(NSUTF8StringEncoding)
    let jsonData: NSData = data!
    do {
        let jsonDict = try NSJSONSerialization.JSONObjectWithData(jsonData, options: NSJSONReadingOptions.MutableContainers) as! NSDictionary
        return jsonDict
    } catch {
        return nil
    }
}




