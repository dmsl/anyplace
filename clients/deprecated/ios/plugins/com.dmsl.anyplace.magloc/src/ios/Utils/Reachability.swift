import Foundation
import SystemConfiguration

protocol ReachabilityDelegate {
    func reachabilityChanged( reachabilityStatus: ReachabilityStatus )
}

enum ReachabilityStatus: CustomStringConvertible {
    
    case NotReachable, ReachableViaWiFi, ReachableViaWWAN
    
    var isReachable: Bool {
        switch(self) {
        case .NotReachable: return false
        case .ReachableViaWiFi: fallthrough
        case .ReachableViaWWAN: return true
        }
    }
    
    var description: String {
        switch self {
        case .ReachableViaWWAN:
            return "Cellular"
        case .ReachableViaWiFi:
            return "WiFi"
        case .NotReachable:
            return "No Connection"
        }
    }
}

class Reachability{
    
    init( reachabilityDelegate: ReachabilityDelegate ) {
        self.reachabilityDelegate = reachabilityDelegate
    }
    
    
    static func networkReachabilityStatus() -> ReachabilityStatus {
        var zeroAddress = sockaddr_in(sin_len: 0, sin_family: 0, sin_port: 0, sin_addr: in_addr(s_addr: 0), sin_zero: (0, 0, 0, 0, 0, 0, 0, 0))
        zeroAddress.sin_len = UInt8(sizeofValue(zeroAddress))
        zeroAddress.sin_family = sa_family_t(AF_INET)
        
        let defaultRouteReachability = withUnsafePointer(&zeroAddress) {
            SCNetworkReachabilityCreateWithAddress(nil, UnsafePointer($0))
        }
        return reachabilityStatus(defaultRouteReachability)
    }
    
    static func hostReachabilityStatus(host: String) -> ReachabilityStatus {
        let route = host.cStringUsingEncoding(NSUTF8StringEncoding)
        let routeReachability = SCNetworkReachabilityCreateWithName(nil, route!)
        return reachabilityStatus(routeReachability)
    }
    
    private static func reachabilityStatus(reachabilityRoute: SCNetworkReachability?) -> ReachabilityStatus {
        var flags: SCNetworkReachabilityFlags = []
        if SCNetworkReachabilityGetFlags(reachabilityRoute!, &flags) == false {
            return .NotReachable
        }
        
        let needsConnection = flags.contains(.ConnectionRequired)
        if needsConnection {
            return .NotReachable
        }
        
        let status: ReachabilityStatus
        if flags.contains(.IsWWAN) {
            status = .ReachableViaWWAN
        } else if flags.contains(.Reachable) {
            status = .ReachableViaWiFi
        } else {
            status = .NotReachable
        }
        return status
    }
    
    static func isConnectedToNetwork() -> Bool {
        return networkReachabilityStatus().isReachable
    }
    
    static func isHostAvailable(host: String) -> Bool {
        return hostReachabilityStatus(host).isReachable
    }
    
    private var timer: dispatch_source_t! = nil
    private var reachabilityStatus: ReachabilityStatus = .NotReachable
    
    
    func startNetworkReachibilityNotifier() {
        startNotifier() { Reachability.networkReachabilityStatus() }
    }
    
    func startHostReachibilityNotifier( host: String ) {
        startNotifier() { Reachability.hostReachabilityStatus(host) }
    }
    
    private func startNotifier ( reachability: () -> ReachabilityStatus ) {
        if timer != nil {
            stopNotifier()
        }
        self.reachabilityStatus = .NotReachable
        
        
        let timer_queue = dispatch_queue_create("cy.ac.ucy.cs.Reachability", DISPATCH_QUEUE_CONCURRENT)
        timer = dispatch_source_create(DISPATCH_SOURCE_TYPE_TIMER, 0, 0, timer_queue)
        dispatch_source_set_timer(timer, dispatch_walltime(nil, 0), 500 * NSEC_PER_MSEC, 100 * NSEC_PER_MSEC)
        dispatch_source_set_event_handler(timer) { [weak self] in
            self?.timer_fired(reachability)
        }
        dispatch_resume(timer)
    }
    
    private func timer_fired( reachability: () -> ReachabilityStatus ) {
        let status = reachability()
        if self.reachabilityStatus != status {
            self.reachabilityStatus = status
            dispatch_async(dispatch_get_main_queue(), { [weak self] in
                self?.reachabilityDelegate.reachabilityChanged(status)
            })
        }
    }
    
    func stopNotifier() {
        dispatch_source_cancel(timer)
        timer = nil
    }
    
    deinit {
        stopNotifier()
    }
    
    private let reachabilityDelegate: ReachabilityDelegate
    
}
