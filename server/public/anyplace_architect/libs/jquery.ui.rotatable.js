/* globals define jQuery */
(function (factory) {
    if (typeof define === 'function' && define.amd) {
        // AMD. Register as an anonymous module.
        define(['jquery'], factory)
    } else {
        // Browser globals
        factory(jQuery)
    }
}(function ($) {
    $.widget('ui.rotatable', $.ui.mouse, {
        widgetEventPrefix: 'rotate',

        options: {
            angle: false,       // specify an angle in radians (for backward compatability)
            degrees: false,     // specify angle in degrees
            handle: false,      // an image to use for a handle
            handleOffset: {     // where the handle should appear
                top: 0,
                left: 0
            },
            radians: false,     // specify angle in radians
            rotate: null,       // a callback for during rotation
            rotationCenterOffset: {   // offset the center of the element for rotation
                top: 0,
                left: 0
            },
            snap: false,        // boolean flag, should the element snap to a certain rotation?
            start: null,        // callback when rotation starts
            step: 22.5,         // angle in degrees that the rotation should be snapped to
            stop: null,         // callback when rotation stops
            transforms: null,   // other transforms to performed on the element
            wheelRotate: true   // boolean flag, should the element rotate when the mousewheel is rotated?
        },

        // accessor for the angle in radians
        angle: function (angle) {
            if (angle === undefined) {
                return this.options.angle
            }
            this.options.angle = angle
            this.elementCurrentAngle = angle
            this._performRotation(this.options.angle)
        },

        // calculates the element center if needed and returns it
        getElementCenter: function () {
            this.elementCenter = this._calculateElementCenter()
            return this.elementCenter
        },

        // accessor for the handle
        handle: function (handle) {
            if (handle === undefined) {
                return this.options.handle
            }
            this.options.handle = handle
        },

        plugins: {},

        /* accessor for the center of rotation
        * takes an object with keys of top and left
        */
        rotationCenterOffset: function (offset) {
            if (offset === undefined) {
                return this.options.rotationCenterOffset
            }
            if (offset.top !== null) {
                this.options.rotationCenterOffset.top = offset.top
            }
            if (offset.left !== null) {
                this.options.rotationCenterOffset.left = offset.left
            }
        },

        // listener for rotating the element
        rotateElement: function (event) {
            if (!this.element || this.element.disabled || this.options.disabled) {
                return false
            }

            if (!event.which) {
                this.stopRotate(event)
                return false
            }

            var rotateAngle = this._calculateRotateAngle(event)
            var previousRotateAngle = this.elementCurrentAngle
            this.elementCurrentAngle = rotateAngle

            // Plugins callbacks need to be called first.
            this._propagate('rotate', event)

            if (this._propagate('rotate', event) === false) {
                this.elementCurrentAngle = previousRotateAngle
                return false
            }
            var ui = this.ui()
            if (this._trigger('rotate', event, ui) === false) {
                this.elementCurrentAngle = previousRotateAngle
                return false
            } else if (ui.angle.current !== rotateAngle) {
                rotateAngle = ui.angle.current
                this.elementCurrentAngle = rotateAngle
            }

            this._performRotation(rotateAngle)

            if (previousRotateAngle !== rotateAngle) {
                this.hasRotated = true
            }

            return false
        },

        // listener for starting rotation
        startRotate: function (event) {
            var center = this.getElementCenter()
            var startXFromCenter = event.pageX - center.x
            var startYFromCenter = event.pageY - center.y
            this.mouseStartAngle = Math.atan2(startYFromCenter, startXFromCenter)
            this.elementStartAngle = this.elementCurrentAngle
            this.hasRotated = false

            this._propagate('start', event)

            $(document).bind('mousemove', this.listeners.rotateElement)
            $(document).bind('mouseup', this.listeners.stopRotate)

            return false
        },

        // listener for stopping rotation
        stopRotate: function (event) {
            if (!this.element || this.element.disabled) {
                return
            }

            $(document).unbind('mousemove', this.listeners.rotateElement)
            $(document).unbind('mouseup', this.listeners.stopRotate)

            this.elementStopAngle = this.elementCurrentAngle

            this._propagate('stop', event)

            setTimeout(function () { this.element = false }, 10)
            return false
        },

        // listener for mousewheel rotation
        wheelRotate: function (event) {
            if (!this.element || this.element.disabled || this.options.disabled) {
                return
            }
            event.preventDefault()
            var angle = this._angleInRadians(Math.round(event.originalEvent.deltaY / 10))
            if (this.options.snap || event.shiftKey) {
                angle = this._calculateSnap(angle)
            }
            angle = this.elementCurrentAngle + angle
            this.angle(angle)
            this._trigger('rotate', event, this.ui())
        },

        // for callbacks
        ui: function () {
            return {
                api: this,
                element: this.element,
                angle: {
                    start: this.elementStartAngle,
                    current: this.elementCurrentAngle,
                    degrees: this._normalizeDegrees(this._angleInDegrees(this.elementCurrentAngle)),
                    stop: this.elementStopAngle
                }
            }
        },

        /* *********************** private functions ************************** */
        // calculates the radians for a given angle in degrees
        _angleInRadians: function (degrees) {
            return degrees * Math.PI / 180
        },

        // calculates the degrees for a given angle in radians
        _angleInDegrees: function (radians) {
            return radians * 180 / Math.PI
        },

        _normalizeDegrees: function (degrees) {
            return ((degrees % 360) + 360) % 360;
        },

        // calculates the center of the element
        _calculateElementCenter: function () {
            var elementOffset = this._getElementOffset()

            // Rotation center given via options
            if (this._isRotationCenterSet()) {
                return {
                    x: elementOffset.left + this.rotationCenterOffset().left,
                    y: elementOffset.top + this.rotationCenterOffset().top
                }
            }

            // Deduce rotation center from transform-origin
            if (this.element.css('transform-origin') !== undefined) {
                var originPx = this.element.css('transform-origin').match(/([\d.]+)px +([\d.]+)px/)
                if (originPx != null) {
                    return {
                        x: elementOffset.left + parseFloat(originPx[1]),
                        y: elementOffset.top + parseFloat(originPx[2])
                    }
                }
            }

            // Default rotation center: middle of the element
            return {
                x: elementOffset.left + this.element.width() / 2,
                y: elementOffset.top + this.element.height() / 2
            }
        },

        // calculates the angle that the element should snap to and returns it in radians
        _calculateSnap: function (radians) {
            var degrees = this._angleInDegrees(radians)
            degrees = Math.round(degrees / this.options.step) * this.options.step
            return this._angleInRadians(degrees)
        },

        // calculates the angle to rotate the element to, based on input
        _calculateRotateAngle: function (event) {
            var center = this.getElementCenter()

            var xFromCenter = event.pageX - center.x
            var yFromCenter = event.pageY - center.y
            var mouseAngle = Math.atan2(yFromCenter, xFromCenter)
            var rotateAngle = mouseAngle - this.mouseStartAngle + this.elementStartAngle

            if (this.options.snap || event.shiftKey) {
                rotateAngle = this._calculateSnap(rotateAngle)
            }

            return rotateAngle
        },

        // constructor
        _create: function () {
            var handle
            if (!this.options.handle) {
                handle = $(document.createElement('div'))
                handle.addClass('ui-rotatable-handle')
                if (this.options.handleOffset.top !== 0 || this.options.handleOffset.left !== 0) {
                    handle.css('position', 'relative')
                    handle.css('top', this.options.handleOffset.top + 'px')
                    handle.css('left', this.options.handleOffset.left + 'px')
                }
            } else {
                handle = this.options.handle
            }

            this.listeners = {
                rotateElement: $.proxy(this.rotateElement, this),
                startRotate: $.proxy(this.startRotate, this),
                stopRotate: $.proxy(this.stopRotate, this),
                wheelRotate: $.proxy(this.wheelRotate, this)
            }

            if (this.options.wheelRotate) {
                this.element.bind('wheel', this.listeners.wheelRotate)
            }

            handle.draggable({ helper: 'clone', start: this._dragStart, handle: handle })
            handle.bind('mousedown', this.listeners.startRotate)

            if (!handle.closest(this.element).length) {
                handle.appendTo(this.element)
            }
            this.rotationCenterOffset(this.options.rotationCenterOffset)

            if (this.options.degrees) {
                this.elementCurrentAngle = this._angleInRadians(this.options.degrees)
            }
            else {
                this.elementCurrentAngle = this.options.radians || this.options.angle || 0
            }
            this._performRotation(this.elementCurrentAngle)
        },

        // destructor
        _destroy: function () {
            this.element.removeClass('ui-rotatable')
            this.element.find('.ui-rotatable-handle').remove()

            if (this.options.wheelRotate) {
                this.element.unbind('wheel', this.listeners.wheelRotate)
            }
        },

        // used for the handle
        _dragStart: function (event) {
            if (this.element) {
                return false
            }
        },

        // retrieves the element offset
        _getElementOffset: function () {
            this._performRotation(0)
            var offset = this.element.offset()
            this._performRotation(this.elementCurrentAngle)
            return offset
        },

        _getTransforms: function (angle) {
            var transforms = 'rotate(' + angle + 'rad)'

            if (this.options.transforms) {
                transforms += ' ' + (function (transforms) {
                    var t = []
                    for (var i in transforms) {
                        if (transforms.hasOwnProperty(i) && transforms[i]) {
                            t.push(i + '(' + transforms[i] + ')')
                        }
                    }
                    return t.join(' ')
                }(this.options.transforms))
            }
            return transforms
        },

        // checks to see if the element has a rotationCenterOffset set
        _isRotationCenterSet: function () {
            return (this.options.rotationCenterOffset.top !== 0 || this.options.rotationCenterOffset.left !== 0)
        },

        // performs the actual rotation on the element
        _performRotation: function (angle) {
            if (this._isRotationCenterSet()) {
                this.element.css('transform-origin', this.options.rotationCenterOffset.left + 'px ' + this.options.rotationCenterOffset.top + 'px')
                this.element.css('-ms-transform-origin', this.options.rotationCenterOffset.left + 'px ' + this.options.rotationCenterOffset.top + 'px') /* IE 9 */
                this.element.css('-webkit-transform-origin', this.options.rotationCenterOffset.left + 'px ' + this.options.rotationCenterOffset + 'px') /* Chrome, Safari, Opera */
            }

            var transforms = this._getTransforms(angle)

            this.element.css('transform', transforms)
            this.element.css('-moz-transform', transforms)
            this.element.css('-webkit-transform', transforms)
            this.element.css('-o-transform', transforms)
        },

        // propagates events
        _propagate: function (n, event) {
            $.ui.plugin.call(this, n, [event, this.ui()]);
            (n !== 'rotate' && this._trigger(n, event, this.ui()))
        }
    })

    return $.ui.rotatable
}))