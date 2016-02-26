//
//  fileSystem.swift
//  AnyplaceSwift
//
//  Created by Antria Kkoushi on 25/04/15.
//  Copyright (c) 2015 Antria Kkoushi. All rights reserved.
//

import Foundation
import UIKit

class FileHelper {
    static func getApplicationSupportFilePath(filename: String, dir: String!) -> String {
        let fm = NSFileManager.defaultManager()
        let appSupportDir = try! fm.URLForDirectory(NSSearchPathDirectory.ApplicationSupportDirectory, inDomain: NSSearchPathDomainMask.UserDomainMask, appropriateForURL: nil, create: true).path!
        let path = ((appSupportDir + (dir == nil ? "/" : ("/" + dir! + "/")) + filename) as NSString).stringByStandardizingPath
        return path
    }
    
    static func getApplicationTemporaryFilePath(filename: String, dir: String!) -> String {
        return ((NSTemporaryDirectory() + (dir == nil ? "/" : ("/" + dir! + "/")) + filename) as NSString).stringByStandardizingPath
    }
    
    static func getApplicationTemporaryDirPath(dir: String!) -> String {
        return ((NSTemporaryDirectory() + (dir == nil ? "/" : ("/" + dir! + "/"))) as NSString).stringByStandardizingPath
    }
    
    static func moveFile(oldPath: String, newPath: String) -> Bool {
        let fm = NSFileManager.defaultManager()
        if !fm.fileExistsAtPath(oldPath) || !fm.isReadableFileAtPath(oldPath) {
            return false
        }
        
        let dir = (newPath as NSString).stringByDeletingLastPathComponent
        var isDir: ObjCBool = false
        let exists = fm.fileExistsAtPath(dir, isDirectory: &isDir)
        
        if !exists || !isDir {
            do {
                try fm.createDirectoryAtPath(dir, withIntermediateDirectories: true, attributes: nil)
            } catch {
                return false
            }
        }
        
        do {
            if fm.fileExistsAtPath(newPath) {
                try fm.removeItemAtPath(newPath)
            }
            try fm.copyItemAtPath(oldPath, toPath: newPath)
        } catch {
            return false
        }
        return true
    }
    
    static func isDirectory(path: String) -> Bool {
        var isDirectory: ObjCBool = true
        let exists = NSFileManager.defaultManager().fileExistsAtPath(path, isDirectory: &isDirectory)
        return Bool(isDirectory) && exists
    }

    
    static func isFileExist(path: String) -> Bool {
        return NSFileManager.defaultManager().fileExistsAtPath(path)
    }
    
    static func createFile(filePath: String) -> Bool {
        let fm = NSFileManager.defaultManager()
        let dirPath = (filePath as NSString).stringByDeletingLastPathComponent
        
        var isDir: ObjCBool = false
        let dirExists = fm.fileExistsAtPath(dirPath, isDirectory: &isDir)
        
        if !dirExists || !isDir {
            do {
                try fm.createDirectoryAtPath(dirPath, withIntermediateDirectories: true, attributes: nil)
            } catch {
                return false
            }
        } else {
            if fm.fileExistsAtPath(filePath) && !fm.isWritableFileAtPath(filePath) {
                do {
                    try fm.removeItemAtPath(filePath)
                } catch {
                    return false
                }
            }
        }
        return fm.createFileAtPath(filePath, contents: nil, attributes: nil)
    }
    
    static func createFile(filename: String, dir: String!) -> Bool {
        let filePath = getApplicationSupportFilePath(filename, dir: dir)
        return createFile(filePath)
    }
    
    static func deleteFile(path: String) -> Bool {
        let fm = NSFileManager.defaultManager()
        do {
            try fm.removeItemAtPath(path)
            return true
        } catch {
            return false
        }
    }
    
    static func imageFromBase64File(path: String) -> UIImage? {
        guard let base64data = NSData(contentsOfFile: path) else {
            return nil
        }
        guard let imageData = NSData( base64EncodedData: base64data, options: NSDataBase64DecodingOptions.IgnoreUnknownCharacters) else {
            return nil
        }
        return UIImage(data: imageData)
    }
    
    static func unzip(path: String) -> Bool {
        return true
    }
    
    static func sizeOfFile(path: String) throws -> UInt64 {
        let attributes: NSDictionary = try NSFileManager.defaultManager().attributesOfItemAtPath(path)
        return attributes.fileSize()
    }
    
    static func dateModified(path: String) throws -> NSDate? {
        let attributes: NSDictionary = try NSFileManager.defaultManager().attributesOfItemAtPath(path)
        return attributes.fileModificationDate()
    }
    
    static func readFile(filename : String) -> NSString {
        
        //    let filemgr = NSFileManager.defaultManager()
        
        //    let currentPath = filemgr.currentDirectoryPath
        //println(currentPath)
        
        
        let dirPaths = NSSearchPathForDirectoriesInDomains(.DocumentDirectory,
            .UserDomainMask, true)
        
        let docsDir = dirPaths[0] as String
        
        
        let newDir = (docsDir as NSString).stringByAppendingPathComponent("data")
        
        let filepath = (newDir as NSString).stringByAppendingPathComponent(filename)
        print(filepath)
        
        //Read file
        let file: NSFileHandle? = NSFileHandle(forReadingAtPath: filepath)
        
        if file == nil {
            print("File open failed")
            return ""
        } else {
            
            let buf = file?.readDataToEndOfFile()
            let string = NSString(data: buf!, encoding: NSUTF8StringEncoding)
            //println(string1!)
            file?.closeFile()
            return string!
        }
        
    }
    
    static func writeFile(path: String, data: String, append: Bool = false) -> Bool {
        
        if !isFileExist(path) {
            if !createFile(path) {
                return false
            }
        }
        
        if let outputStream = NSOutputStream(toFileAtPath: path, append: append) {
            let data: NSData = data.dataUsingEncoding(NSUTF8StringEncoding)!
            outputStream.open()
            outputStream.write(UnsafePointer<UInt8>(data.bytes), maxLength: data.length)
            outputStream.close()
            return true
        } else {
            deleteFile(path)
            return false
        }
    }
    
    static func listChildren(path: String) throws -> [String]? {
        if FileHelper.isDirectory(path) {
            let fm = NSFileManager.defaultManager()
            return try fm.contentsOfDirectoryAtPath(path)
        } else {
            return nil
        }
    }
//    static func createAndWriteFile (filename : String, jsonData : String){
//        let filemgr = NSFileManager.defaultManager()
//        print(filemgr)
//        
//        let currentPath = filemgr.currentDirectoryPath
//        print(currentPath)
//        
//        
//        let dirPaths = NSSearchPathForDirectoriesInDomains(.DocumentDirectory,.UserDomainMask, true)
//        
//        let docsDir = dirPaths[0] as String
//        
//        
//        
//        let newDir = docsDir.stringByAppendingPathComponent("data")
//        print(newDir)
//        
//        //    var error: NSError?
//        
//        //Create Directory
//        do {
//            try filemgr.createDirectoryAtPath(newDir,withIntermediateDirectories: true,
//                attributes: nil)
//        }
//        catch let error as NSError {
//            print("Failed to create dir: \(error.localizedDescription)")
//        }
//        
//        let filepath = newDir.stringByAppendingPathComponent(filename)
//        
//        if !filemgr.fileExistsAtPath(filepath) {
//            let data = ("aaa" as NSString).dataUsingEncoding(NSUTF8StringEncoding)
//            
//            //      let ok = filemgr.createFileAtPath(filepath,contents:data,attributes:nil)
//            filemgr.createFileAtPath(filepath,contents:data,attributes:nil)
//            filemgr.createFileAtPath(filepath, contents: nil, attributes: nil)
//            
//            //      let databuffer = filemgr.contentsAtPath(filepath)
//            filemgr.contentsAtPath(filepath)
//            
//            
//            let file: NSFileHandle? = NSFileHandle(forUpdatingAtPath:  filepath)
//            
//            if file == nil {
//                print("File open failed")
//            } else {
//                file?.truncateFileAtOffset(0)
//                let data = (jsonData as NSString).dataUsingEncoding(NSUTF8StringEncoding)
//                
//                file?.writeData(data!)
//                
//                file?.closeFile()
//            }
//        }
//    }

}

