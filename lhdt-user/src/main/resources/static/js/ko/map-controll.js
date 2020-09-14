var lengthInMeters = 0;
var areaInMeters = 0;

function MapControll(viewer, option) {
	this._viewer = viewer;
	this._scene = viewer.scene;

	//viewer.scene.globe.depthTestAgainstTerrain = true;
	viewer.cesiumWidget.screenSpaceEventHandler.removeInputAction(Cesium.ScreenSpaceEventType.LEFT_DOUBLE_CLICK);

	this._polylines = [];
	this._labels = [];   

	var that = this;
	var handler = null;

	var drawingMode = 'line';
	var activeShapePoints = [];
	var activeShape;
	var activeLabel;
	
	var ellipsoid = viewer.scene.globe.ellipsoid;
	var flags = {
			looking : false,
			moveForward : false,
			moveBackward : false,
			moveUp : false,
			moveDown : false,
			moveLeft : false,
			moveRight : false
		};

	function getFlagForKeyCode(keyCode) {
		switch (keyCode) {
		case 'W'.charCodeAt(0):
			return 'moveForward';
		case 'S'.charCodeAt(0):
			return 'moveBackward';
		case 'Q'.charCodeAt(0):
			return 'moveUp';
		case 'E'.charCodeAt(0):
			return 'moveDown';
		case 'D'.charCodeAt(0):
			return 'moveRight';
		case 'A'.charCodeAt(0):
			return 'moveLeft';
		default:
			return undefined;
		}
	}
	
	var preventTags = ['INPUT','SELECT','TEXTAREA'];
	document.addEventListener('keydown', function(e) {
		var tagName = e.target.tagName;
		if(preventTags.includes(tagName)){
			return;
		}
		var flagName = getFlagForKeyCode(e.keyCode);
		if (typeof flagName !== 'undefined') {
			flags[flagName] = true;
		}
	}, false);

	document.addEventListener('keyup', function(e) {
		var flagName = getFlagForKeyCode(e.keyCode);
		if (typeof flagName !== 'undefined') {
			flags[flagName] = false;
		}
	}, false);

	viewer.clock.onTick.addEventListener(function(clock) {
		var camera = viewer.camera;

		if (flags.looking) {
			var width = canvas.clientWidth;
			var height = canvas.clientHeight;

			// Coordinate (0.0, 0.0) will be where the mouse was clicked.
			var x = (mousePosition.x - startMousePosition.x) / width;
			var y = -(mousePosition.y - startMousePosition.y) / height;

			var lookFactor = 0.05;
			camera.lookRight(x * lookFactor);
			camera.lookUp(y * lookFactor);
		}

		// Change movement speed based on the distance of the camera to the surface of the ellipsoid.
		var cameraHeight = ellipsoid.cartesianToCartographic(camera.position).height;
		var moveRate = cameraHeight / 100.0;

		if (flags.moveForward) {
			camera.moveForward(moveRate);
		}
		if (flags.moveBackward) {
			camera.moveBackward(moveRate);
		}
		if (flags.moveUp) {
			camera.moveUp(moveRate);
		}
		if (flags.moveDown) {
			camera.moveDown(moveRate);
		}
		if (flags.moveLeft) {
			camera.moveLeft(moveRate);
		}
		if (flags.moveRight) {
			camera.moveRight(moveRate);
		}
	});

	/**
	 * 나침반 동작
	 */
	viewer.scene.postRender.addEventListener(function () {
		var camera = this._viewer.camera;
		var angle = Cesium.Math.toDegrees(camera.heading);
		if (angle > 359.9 || angle < .1) {
			$("#rotateReset").addClass('on');
			$("#rotateReset").css({
				'-webkit-transform': 'rotate(0)',
				'-moz-transform': 'rotate(0)',
				'-ms-transform': 'rotate(0)',
				'transform': 'rotate(0)'
			});
		}
		else {
			$("#rotateReset").removeClass('on');
			$("#rotateReset").css({
				'-webkit-transform': 'rotate(' + -angle + 'deg)',
				'-moz-transform': 'rotate(' + -angle + 'deg)',
				'-ms-transform': 'rotate(' + -angle + 'deg)',
				'transform': 'rotate(' + -angle + 'deg)'
			});
		}
	});


	function createPoint(worldPosition) {
		var entity = viewer.entities.add({
			position: worldPosition,
			point: {
				color: Cesium.Color.YELLOW,
				pixelSize: 5,
				outlineColor: Cesium.Color.BLACK,
				outlineWidth: 2,
				disableDepthTestDistance: Number.POSITIVE_INFINITY,
				heightReference: Cesium.HeightReference.CLAMP_TO_GROUND
			}
		});
		return entity;
	}

	var dynamicCenter = new Cesium.CallbackProperty(function () {
		var bs = Cesium.BoundingSphere.fromPoints(activeShapePoints);
		return Cesium.Ellipsoid.WGS84.scaleToGeodeticSurface(bs.center);
	}, false);

	var dynamicLabel = new Cesium.CallbackProperty(function () {
		return getArea(activeShapePoints);
	}, false);

	function drawShape(positionData) {
		var shape;
		if (drawingMode === 'line') {
			shape = viewer.entities.add({
				corridor: {
					// polyline: {
						positions: positionData,
						material: new Cesium.ColorMaterialProperty(Cesium.Color.YELLOW),
						//heightReference: Cesium.HeightReference.CLAMP_TO_GROUND,
						// followSurface: true,
						// clampToGround : true,
						width: 3
				}
			});
		}
		else if (drawingMode === 'polygon') {
			shape = viewer.entities.add({
				name     : "Polygon for area measurement",
				polygon: {
					hierarchy: positionData,
					material: new Cesium.ColorMaterialProperty(Cesium.Color.YELLOW.withAlpha(0.3)),
					/* height: 0.1, */
					//heightReference: Cesium.HeightReference.CLAMP_TO_GROUND
				}
			});
		}
		return shape;
	}

	// use scratch object to avoid new allocations per frame.
	var startCartographic = new Cesium.Cartographic();
	var endCartographic = new Cesium.Cartographic();
	var scratch = new Cesium.Cartographic();
	var geodesic = new Cesium.EllipsoidGeodesic();

	function getLineLength(positions) {
		lengthInMeters = 0;
		for (var i = 1, len = positions.length; i < len; i++) {
			var startPoint = positions[i - 1];
			var endPoint = positions[i];

			lengthInMeters += Cesium.Cartesian3.distance(startPoint, endPoint);
		}
		return formatDistance(lengthInMeters);
	}

	function getArea(positions) {
		areaInMeters = 0;
		if (positions.length >= 3)
		{
			var points = [];
			for(var i = 0, len = positions.length; i < len; i++)
			{
				// points.push(Cesium.Cartesian2.fromCartesian3(positions[i]));
				var cartographic = Cesium.Cartographic.fromCartesian(positions[i]);
				points.push(new Cesium.Cartesian2(cartographic.longitude, cartographic.latitude));
			}
			if(Cesium.PolygonPipeline.computeWindingOrder2D(points) === Cesium.WindingOrder.CLOCKWISE)
			{
				points.reverse();
			}

			var triangles = Cesium.PolygonPipeline.triangulate(points);

			for(var i = 0, len = triangles.length; i < len; i+=3)
			{
				// areaInMeters +=
					// Cesium.PolygonPipeline.computeArea2D([points[triangles[i]],
				// points[triangles[i + 1]], points[triangles[i + 2]]]);
				areaInMeters += calArea(points[triangles[i]], points[triangles[i + 1]], points[triangles[i + 2]]);
			}
		}
		return formatArea(areaInMeters);
	}
	function calArea(t1, t2, t3, i) {
		var r = Math.abs(t1.x * (t2.y - t3.y) + t2.x * (t3.y - t1.y) + t3.x * (t1.y - t2.y)) / 2;
		var cartographic = new Cesium.Cartographic((t1.x + t2.x + t3.x) / 3, (t1.y + t2.y + t3.y) / 3);
		var cartesian = viewer.scene.globe.ellipsoid.cartographicToCartesian(cartographic);
		var magnitude = Cesium.Cartesian3.magnitude(cartesian);
		return r * magnitude * magnitude * Math.cos(cartographic.latitude)
	}

	function drawLabel(positionData) {
		var label;
		// if (drawingMode === 'line') {
		label = viewer.entities.add({
			position: positionData,
			label: {
				text: getLineLength(activeShapePoints),
				font: 'bold 20px sans-serif',
				fillColor: Cesium.Color.YELLOW,
				style: Cesium.LabelStyle.FILL,
				verticalOrigin: Cesium.VerticalOrigin.BOTTOM,
				disableDepthTestDistance: Number.POSITIVE_INFINITY,
				heightReference: Cesium.HeightReference.CLAMP_TO_GROUND/*
				 * ,
				 * pixelOffset :
				 * new
				 * Cesium.Cartesian2(5,
				 * 20)
				 */
			}
		});
		// }
		return label;
	}

	function drawAreaLabel() {
		var label;
		var bs = Cesium.BoundingSphere.fromPoints(activeShapePoints);
		var position = Cesium.Ellipsoid.WGS84.scaleToGeodeticSurface(bs.center);
		var text = getArea(activeShapePoints);

		label = viewer.entities.add({
			name     : "Label for area measurement",
			position: position,
			label: {
				text: text,
				font: 'bold 20px sans-serif',
				fillColor: Cesium.Color.BLUE,
				style: Cesium.LabelStyle.FILL,
				verticalOrigin: Cesium.VerticalOrigin.BOTTOM,
				disableDepthTestDistance: Number.POSITIVE_INFINITY,
				heightReference: Cesium.HeightReference.CLAMP_TO_GROUND
			}
		});

		return label;
	}

	// Redraw the shape so it's not dynamic and remove the dynamic shape.
	function terminateShape() {
		// activeShapePoints.pop();
		lengthInMeters = 0;
		areaInMeters = 0
		this._polylines.push(drawShape(activeShapePoints));
		if (drawingMode === 'polygon')  this._labels.push(drawAreaLabel());

		viewer.entities.remove(activeShape);
		viewer.entities.remove(activeLabel);

		activeShape = undefined;
		activeLabel = undefined;
		activeShapePoints = [];
	}

	this.clearMap = function () {
		lengthInMeters = 0;
		areaInMeters = 0
		if (Cesium.defined(handler)) {
			handler.destroy();
			handler = null;
		}
		for (var i = 0, len = this._polylines.length; i < len; i++) {
			viewer.entities.remove(this._polylines[i]);
		}
		for (var i = 0, len = this._labels.length; i < len; i++) {
			viewer.entities.remove(this._labels[i]);
		}

		viewer.entities.remove(activeShape);
		viewer.entities.remove(activeLabel);

		activeShape = undefined;
		activeLabel = undefined;
		activeShapePoints = [];

		this._polylines = [];
		this._labels = [];
	}
	$("#rotateReset").click(function() {
		var camera = that._scene.camera;
		camera.flyTo({
			destination: camera.positionWC,
			duration : 0
		});

		$(this).css({
			'-webkit-transform': 'rotate(0)',
			'-moz-transform': 'rotate(0)',
			'-ms-transform': 'rotate(0)',
			'transform': 'rotate(0)'
		});
		$('#rotateInput').val(0);
		$('#pitchInput').val(-90);
	});

	$('#rotateLeft').click(function(){
		that._scene.camera.twistLeft(Cesium.Math.toRadians(10));
		var currentHeading = Math.round(Cesium.Math.toDegrees(viewer.camera.heading));
		if(currentHeading > 180){
			currentHeading = currentHeading - 360;
		}
		$('#rotateInput').val(currentHeading);
	});

	$('#rotateRight').click(function(){
		that._scene.camera.twistRight(Cesium.Math.toRadians(10));
		var currentHeading = Math.round(Cesium.Math.toDegrees(viewer.camera.heading));
		if(currentHeading > 180){
			currentHeading = currentHeading - 360;
		}
		$('#rotateInput').val(currentHeading);
	});

	/*
	 * viewer.screenSpaceEventHandler.setInputAction(function
	 * onMouseMove(movement){
	 * console.log(Cesium.Math.toDegrees(viewer.camera.heading)); var
	 * currentHeading = Cesium.Math.toDegrees(viewer.camera.heading);
	 * if(currentHeading > 180){ currentHeading = currentHeading - 360; }
	 * $('#rotateInput').val(currentHeading); },
	 * Cesium.ScreenSpaceEventType.LEFT_UP);
	 * 
	 */

	viewer.camera.changed.addEventListener(function() {
		var currentHeading = Math.round( Cesium.Math.toDegrees(viewer.camera.heading));
		//console.log('Heading:', currentHeading)

		if(currentHeading > 180){
			currentHeading = currentHeading - 360;
		}

		$('#rotateInput').val(currentHeading);


		var deg = Math.round( Cesium.Math.toDegrees(viewer.camera.pitch))
		//console.log('Pitch:', deg)

		$('#pitchInput').val(deg);

	});

	$('#mapCtrlReset').click(function () {
		console.log("맵컨트롤 : 초기화");
		that._scene.camera.flyTo({
			destination: Cesium.Cartesian3.fromDegrees(parseFloat(Mago3D.MagoConfig.getPolicy().initLongitude),
					parseFloat(Mago3D.MagoConfig.getPolicy().initLatitude),
					parseFloat(Mago3D.MagoConfig.getPolicy().initAltitude)),
					duration: parseInt(Mago3D.MagoConfig.getPolicy().initDuration)
		});
	});

	$('#mapCtrlAll').click(function (){
		that._scene.camera.flyTo({
			destination: new Cesium.Cartesian3(-3158185.8634899906, 4713784.056940694, 4516771.367915208), //대략적인 한반도 좌표..
			duration: parseInt(Mago3D.MagoConfig.getPolicy().initDuration)
		});
	});


	// 거리 측정 버튼
	$('#mapCtrlDistance').click(function() {
		$(this).toggleClass('on'); // 버튼 색 변경
		$('#mapCtrlArea').removeClass('on');
		$(this).trigger('afterClick');
	});

	$('#mapCtrlDistance').bind('afterClick', function () {
		console.log("맵컨트롤 : 거리");
		that.clearMap();
		drawingMode = 'line';

		if ($(this).hasClass('on')) {
			startDrawPolyLine();
		}
	});

	// 	면적 측정 버튼
	$('#mapCtrlArea').click(function() {
		$(this).toggleClass('on'); // 버튼 색 변경
		$('#mapCtrlDistance').removeClass('on');
		$(this).trigger('afterClick');
	});

    $('#mapCtrlArea').bind('afterClick', function () {
        console.log("맵컨트롤 : 면적");
        that.clearMap();
        drawingMode = 'polygon';

        if ($(this).hasClass('on')) {
            startDrawPolyLine();
        }
    });
    
    $('#mapCapture').click(function() {
    	// console.log("맵컨트롤 : 저장");
        var targetResolutionScale = 1.0;
        var timeout = 1000; // in ms

        var scene =that._scene;
        if (!scene) {
            console.error("No scene");
        }

        // define callback functions
        var prepareScreenshot = function(){
            var canvas = scene.canvas;
            viewer.resolutionScale = targetResolutionScale;
            scene.preRender.removeEventListener(prepareScreenshot);
            // take snapshot after defined timeout to allow scene update (ie. loading data)
            startLoading();
            setTimeout(function(){
                scene.postRender.addEventListener(takeScreenshot);
            }, timeout);
        }

        var takeScreenshot = function(){
            scene.postRender.removeEventListener(takeScreenshot);
            var canvas = scene.canvas;
            canvas.toBlob(function(blob){
                var url = URL.createObjectURL(blob);
                downloadURI(url, "snapshot-" + moment().format("YYYYMMDDHHmmss") + ".png");
                // reset resolutionScale
                viewer.resolutionScale = 1.0;
                stopLoading();
            });
        }

        scene.preRender.addEventListener(prepareScreenshot);
    });
    
    function downloadURI(uri, name) {
        var link = document.createElement("a");
        link.download = name;
        link.href = uri;
        // mimic click on "download button"
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        delete link;
    }
    
    $('#mapCtrlZoomIn').click(function () {
        console.log("맵컨트롤 : 확대");
        var altitude = getCameraCurrentPositionAPI(MAGO3D_INSTANCE).alt;
        that._scene.camera.zoomIn(altitude * 0.1);
    });

    $('#mapCtrlZoomOut').click(function () {
        console.log("맵컨트롤 : 축소");
        var altitude = getCameraCurrentPositionAPI(MAGO3D_INSTANCE).alt;
        that._scene.camera.zoomOut(altitude * 0.1);
    });

    $('#distanceLayer button.focusA').click(function () {
        that.clearMap();
        $('#mapCtrlDistance').trigger('afterClick');
    });

    $('#areaLayer button.focusA').click(function () {
        that.clearMap();
        $('#mapCtrlArea').trigger('afterClick');
    });
    
    
    //카메라 좌우회전
    //deprecated.
    $('#mapCtrlCamRightRot, #mapCtrlCamLeftRot').click(function(e) {
    	var id = e.target.id;
    	var degree = (id === 'mapCtrlCamLeftRot') ? -180 : 180;
    	var camera = that._viewer.camera;
    	camera.cancelFlight();
    	var curCamPos = camera.position;
    	var curCartoPos = Cesium.Cartographic.fromCartesian(curCamPos);
    	var maximumHeight = 300;
    	if(curCartoPos.height > maximumHeight) curCartoPos.height = maximumHeight;
    	
    	var moveToPos = Cesium.Cartesian3.fromRadians(curCartoPos.longitude, curCartoPos.latitude, curCartoPos.height);
    	
    	camera.flyTo({
    		destination: moveToPos,
    		maximumHeight : maximumHeight,
    		orientation: {
    			heading: 0,
    			pitch: 0,
    			roll: 0
    		},
    		duration:1,
    		easingFunction : Cesium.EasingFunction.LINEAR_NONE,
    		complete : function() {
    			camera.flyTo({
    				destination: moveToPos,
    				maximumHeight : maximumHeight,
    				orientation: {
    					heading: Cesium.Math.toRadians(degree),
    					pitch: 0,
    					roll: 0
    				},
    				duration:5,
    				easingFunction : Cesium.EasingFunction.LINEAR_NONE,
    				complete : function(){
    					camera.flyTo({
    						destination: moveToPos,
    						maximumHeight : maximumHeight,
    						orientation: {
    							heading: Cesium.Math.toRadians(degree*2),
    							pitch: 0,
    							roll: 0
    						},
    						duration:5,
    						easingFunction : Cesium.EasingFunction.LINEAR_NONE,
    					})
    				}
    			});
    		}
    	});
    });

    function startDrawPolyLine() {
        handler = new Cesium.ScreenSpaceEventHandler(viewer.canvas);
        var dynamicPositions = new Cesium.CallbackProperty(function () {
        	if(drawingMode === 'polygon') {
        		return new Cesium.PolygonHierarchy(activeShapePoints);
        	} else {
        		return activeShapePoints;
        	}
        }, false);
        
        handler.setInputAction(function (event) {
            var earthPosition = viewer.scene.pickPosition(event.position);
            if (Cesium.defined(earthPosition)) {
                var cartographic = Cesium.Cartographic.fromCartesian(earthPosition);
                var tempPosition = Cesium.Cartesian3.fromDegrees(Cesium.Math.toDegrees(cartographic.longitude), Cesium.Math.toDegrees(cartographic.latitude));
                activeShapePoints.push(tempPosition);
                
                if (activeShapePoints.length === 1) {
                    activeShape = drawShape(dynamicPositions);
                    if (drawingMode === 'polygon') {
	                    activeLabel = viewer.entities.add({
	                        name     : "TempLabel for area measurement",
	                        position: dynamicCenter,
	                        label: {
	                            text: dynamicLabel,
	                            font: 'bold 20px sans-serif',
	                            fillColor: Cesium.Color.BLUE,
	                            style: Cesium.LabelStyle.FILL,
	                            verticalOrigin: Cesium.VerticalOrigin.BOTTOM,
	                            disableDepthTestDistance: Number.POSITIVE_INFINITY,
	                            heightReference: Cesium.HeightReference.CLAMP_TO_GROUND
	                        }
	                    });
                    }
                }
                else {
                    this._labels.push(drawLabel(tempPosition));
                }
                this._polylines.push(createPoint(tempPosition));
            }
        }, Cesium.ScreenSpaceEventType.LEFT_CLICK);

        handler.setInputAction(function (event) {
            terminateShape();
        }, Cesium.ScreenSpaceEventType.RIGHT_CLICK);

    }
}

var formatDistance = function (_length) {
	var unitFactor = 1;//parseFloat($('#distanceFactor option:selected').val());
	var unitName = 'm (미터)';//$('#distanceFactor option:selected').text();
	var output= Math.round(_length / unitFactor * 100) / 100 + " " + unitName.substring(0, unitName.indexOf('('));
	return output;
};

var formatArea = function (_area) {
	var unitFactor = 1;//parseFloat($('#areaFactor option:selected').val());
	var unitName = 'm² (제곱미터)' //$('#areaFactor option:selected').text();
		var output= Math.round(_area / unitFactor * 100) / 100 + " " + unitName.substring(0, unitName.indexOf('('));
	return output;
};

$("#mapPolicy").click(function(){
	$("#mapPolicy").toggleClass("on");
	$("#mago3DSettingLabelLayer").toggle();
});
$(".layerClose").click(function(){
	$("#mapPolicy").removeClass("on");
	$("#mago3DSettingLabelLayer").hide();
});

// 가이드 팝업 띄우기
function goMagoAPIGuide(url) {
	var width = 1200;
	var height = 800;

	// 만들 팝업창 좌우 크기의 1/2 만큼 보정값으로 빼주었음
	var popupX = (window.screen.width / 2) - (width / 2);
	var popupY = (window.screen.height / 2) - (height / 2);
	
	var popWin = window.open(url, "", "toolbar=no, width=" + width + " ,height=" + height + ", top=" + popupY + ", left=" + popupX + 
			", directories=no,status=yes,scrollbars=no,menubar=no,location=no");
	return false;
}

var Compass = function(cViewer, div) {
	this.compassElement = div;
	
	this._viewer = cViewer;
	this._scene = this._viewer.scene;
	this._camera = this._scene.camera;
	this.scratch2d = new Cesium.Cartesian2();
	
	this.isRotating = false;
	this.rotateIsLook = false;
	this.rotateInitialCursorAngle = 0;
	this.rotateInitialCameraAngle = 0;
	this.rotateInitialCameraDistance = 0;
	this.rotateFrame;
	
	this.isOrbiting = false;
	this.orbitLastTimestamp;
	this.orbitFrame;
	this.orbitIsLook = false;
	this.orbitCursorAngle = 0;
	this.orbitCursorOpacity = 0;
	
	this.rotateMouseMove = this.rotateMouseMove.bind(this);
	this.rotateMouseUp = this.rotateMouseUp.bind(this);
	
	this.orbitMouseMove = this.orbitMouseMove.bind(this);
	this.orbitMouseUp = this.orbitMouseUp.bind(this);
	this.orbitTick = this.orbitTick.bind(this);
	
	var that = this;
	this.compassElement.addEventListener(
		'mousedown',
		function(e){
			that.handleDown(e);
		}
		,false
	);
	
	this.compassElement.addEventListener(
		'dblclick',
		function(e){
			that.handleDblClick(e);
		}
		,false
	);

	this._viewer.clock.onTick.addEventListener(this.setSvgRotate.bind(this));
}
Compass.prototype.handleDblClick = function(e) {
	this.scratch2d.x = this._scene.canvas.clientWidth / 2;
	this.scratch2d.y = this._scene.canvas.clientHeight / 2;
	
	var pickRay = this._camera.getPickRay(this.scratch2d);
	var pick = this._scene.globe.pick(pickRay, this._scene);
	
	if(!pick) this._camera.flyHome();
	
	var frame = Cesium.Transforms.eastNorthUpToFixedFrame(pick);
	var subtract = Cesium.Cartesian3.subtract(pick, this._camera.position, new Cesium.Cartesian3());
	var tween = Cesium.CameraFlightPath.createTween(this._scene, {
		destination : Cesium.Matrix4.multiplyByPoint(frame, new Cesium.Cartesian3(0, 0, Cesium.Cartesian3.magnitude(subtract)), new Cesium.Cartesian3()),
		direction : Cesium.Matrix4.multiplyByPointAsVector(frame, new Cesium.Cartesian3(0,0,-1), new Cesium.Cartesian3()),
		up : Cesium.Matrix4.multiplyByPointAsVector(frame,  new Cesium.Cartesian3(0,1,0), new Cesium.Cartesian3()),
		duraion : 1.2
	});
	this._scene.tweens.add(tween);
}
Compass.prototype.handleDown = function(e) {
	var tagName = e.target.tagName;
	if(tagName === 'DIV') {
		this.setOrbitMode(e);
	} else {
		this.setRotateMode(e);
	}
}

Compass.prototype.setOrbitMode = function(e) {
	this.isOrbiting = true;
	this.orbitLastTimestamp = this.getTimeStamp();
	
	this.scratch2d.x = this._scene.canvas.clientWidth / 2;
	this.scratch2d.y = this._scene.canvas.clientHeight / 2;
	
	var pickRay = this._camera.getPickRay(this.scratch2d);
	var pick = this._scene.globe.pick(pickRay, this._scene);
	
	if(pick) {
		this.orbitFrame = Cesium.Transforms.eastNorthUpToFixedFrame(pick, Cesium.Ellipsoid.WGS84);
		this.orbitIsLook = false;
	} else {
		this.orbitFrame = Cesium.Transforms.eastNorthUpToFixedFrame(this._camera.positionWC, Cesium.Ellipsoid.WGS84);
		this.orbitIsLook = true;
	}
	
	var br = this.compassElement.getBoundingClientRect();
	var divCenter = new Cesium.Cartesian2((br.right - br.left) / 2, (br.bottom - br.top) / 2);
	var offset = new Cesium.Cartesian2(e.clientX - br.left, e.clientY - br.top);
	
	var subtract = Cesium.Cartesian2.subtract(offset, divCenter, this.scratch2d);
	this.setOrbitCursor(subtract);
	
	var fan = this.compassElement.getElementsByClassName('bearingInFan').item(0);
	fan.style.display = 'block';
	
	document.addEventListener("pointermove", this.orbitMouseMove, false);
    document.addEventListener("pointerup", this.orbitMouseUp, false);
	this._viewer.clock.onTick.addEventListener(this.orbitTick);
}

Compass.prototype.orbitMouseMove = function(e) {
	var br = this.compassElement.getBoundingClientRect();
	var divCenter = new Cesium.Cartesian2((br.right - br.left) / 2, (br.bottom - br.top) / 2);
	var offset = new Cesium.Cartesian2(e.clientX - br.left, e.clientY - br.top);
	
	var subtract = Cesium.Cartesian2.subtract(offset, divCenter, this.scratch2d);
	this.setOrbitCursor(subtract);
}

Compass.prototype.orbitMouseUp = function(e) {
	
	this.isOrbiting = false;
	this.orbitLastTimestamp = undefined;
	
	document.removeEventListener("pointermove", this.orbitMouseMove, false);
    document.removeEventListener("pointerup", this.orbitMouseUp, false);
	this._viewer.clock.onTick.removeEventListener(this.orbitTick);
	
	var fan = this.compassElement.getElementsByClassName('bearingInFan').item(0);
	fan.style.display = 'none';
}

Compass.prototype.orbitTick = function(e) {
	var currentTime = this.getTimeStamp();
	var offsetTime = (currentTime - this.orbitLastTimestamp) * (2.5 * (this.orbitCursorOpacity - 0.5) / 1e3);
	var angle1 = this.orbitCursorAngle + Cesium.Math.PI_OVER_TWO;
	
	var cos = Math.cos(angle1) * offsetTime;
	var sin = Math.sin(angle1) * offsetTime;
	
	var transform = Cesium.Matrix4.clone(this._camera.transform);
	this._camera.lookAtTransform(this.orbitFrame);
	if(this.orbitIsLook) { 
		this._camera.look(Cesium.Cartesian3.UNIT_Z, -cos);
		this._camera.look(this._camera.right, -sin);
	} else {
		this._camera.rotateLeft(cos);
		this._camera.rotateUp(sin);
	}
	this._camera.lookAtTransform(transform);
	this.orbitLastTimestamp = currentTime;
	
	var fan = this.compassElement.getElementsByClassName('bearingInFan').item(0);
	fan.style.transform = `rotate(-${this.orbitCursorAngle}rad)`;
}

Compass.prototype.setOrbitCursor = function (sub) {
	var br = this.compassElement.getBoundingClientRect();
	var o = Math.atan2(-sub.y, sub.x);
	this.orbitCursorAngle = Cesium.Math.zeroToTwoPi(o - Cesium.Math.PI_OVER_TWO);
	var magnitude = Cesium.Cartesian2.magnitude(sub);
	var s = br.width / 2;
	var min = Math.min(magnitude / s, 1);
	var opacity = 0.5 * min * min + 0.5;
	this.orbitCursorOpacity = opacity;
}

Compass.prototype.setRotateMode = function(e) {
	this.isRotating = true;
	var br = this.compassElement.getBoundingClientRect();
	this.rotateInitialCursorAngle = Math.atan2(-br.y, br.x);
	this.scratch2d.x = this._scene.canvas.clientWidth / 2;
	this.scratch2d.y = this._scene.canvas.clientHeight / 2;
	
	var pickRay = this._camera.getPickRay(this.scratch2d);
	var pick = this._scene.globe.pick(pickRay, this._scene);
	
	if(pick) {
		this.rotateFrame = Cesium.Transforms.eastNorthUpToFixedFrame(pick, Cesium.Ellipsoid.WGS84);
		this.rotateIsLook = false;
	} else {
		this.rotateFrame = Cesium.Transforms.eastNorthUpToFixedFrame(this._camera.positionWC, Cesium.Ellipsoid.WGS84);
		this.rotateIsLook = true;
	}
	
	var transform = Cesium.Matrix4.clone(this._camera.transform);
	
	this._camera.lookAtTransform(this.rotateFrame);	
	this.rotateInitialCameraAngle = Math.atan2(this._camera.position.y, this._camera.position.x);
    this.rotateInitialCameraDistance = Cesium.Cartesian3.magnitude(new Cesium.Cartesian3(this._camera.position.x, this._camera.position.y,0));
    this._camera.lookAtTransform(transform);
	
	document.addEventListener("pointermove", this.rotateMouseMove, false);
    document.addEventListener("pointerup", this.rotateMouseUp, false);
}

Compass.prototype.rotateMouseMove = function(e) {
	var divRect = this.compassElement.getBoundingClientRect();
	var divCenter = new Cesium.Cartesian2((divRect.right - divRect.left) / 2, (divRect.bottom - divRect.top) / 2);
	var offset = new Cesium.Cartesian2(e.clientX - divRect.left, e.clientY - divRect.top);

	var subtract = Cesium.Cartesian2.subtract(offset, divCenter, this.scratch2d);
	var angle1 = Math.atan2(-subtract.y, subtract.x) - this.rotateInitialCursorAngle;
	var angle2 = Cesium.Math.zeroToTwoPi(this.rotateInitialCameraAngle - angle1);
	
	var transform = Cesium.Matrix4.clone(this._camera.transform);
	
	this._camera.lookAtTransform(this.rotateFrame);
	var atan2 = Math.atan2(this._camera.position.y, this._camera.position.x);
	this._camera.rotateRight(angle2 - atan2);
	this._camera.lookAtTransform(transform);
}

Compass.prototype.rotateMouseUp = function(e) {
	this.isRotating = false;
	
	document.removeEventListener("pointermove", this.rotateMouseMove, false);
    document.removeEventListener("pointerup", this.rotateMouseUp, false);
}

Compass.prototype.getTimeStamp = function() {
	return new Date().getTime();
}

Compass.prototype.setSvgRotate = function() {
	this.compassElement.getElementsByTagName('svg').item(1).style.transform = `rotate(-${this._camera.heading}rad)`;
}

$(document).ready(function() {
	// 처음
	$('#mapCtrlHome').click(function() {
		var magoManager = MAGO3D_INSTANCE.getMagoManager();
		if (magoManager.isCesiumGlobe()) {
			var config = magoManager.configInformation;
			if (config.initCameraEnable) {
				var lon = parseFloat(config.initLongitude);
				var lat = parseFloat(config.initLatitude);
				var height = parseFloat(config.initAltitude);
				var duration = parseInt(config.initDuration);
				if (isNaN(lon) || isNaN(lat) || isNaN(height)) {
					throw new Error('Longitude, Latitude, Height must number type.');
				}
				if (isNaN(duration)) {
					duration = 3;
				}
				magoManager.flyTo(lon, lat, height, duration);
			}
		}
	});

	// 전체화면
	$('#mapCtrlFullScreen').click(function() {
		$(this).toggleClass('on');

		var magoManager = MAGO3D_INSTANCE.getMagoManager();
		var target = document.getElementById(magoManager.config.getContainerId());
		if (this.full) {
			if (isFullScreen()) {
				exitFullScreen();
				this.full = false;
			}
		} else {
			if (isFullScreenSupported()) {
				requestFullScreen(target);
				this.full = true;
			}
		}

		function isFullScreenSupported() {
			var body = document.body;
			return !!(
				body.webkitRequestFullscreen ||
				(body.msRequestFullscreen && document.msFullscreenEnabled) ||
				(body.requestFullscreen && document.fullscreenEnabled)
			);
		}

		function isFullScreen() {
			return !!(
				document.webkitIsFullScreen ||
				document.msFullscreenElement ||
				document.fullscreenElement
			);
		}

		function requestFullScreen(element) {
			if (element.requestFullscreen) {
				element.requestFullscreen();
			} else if (element.msRequestFullscreen) {
				element.msRequestFullscreen();
			} else if (element.webkitRequestFullscreen) {
				element.webkitRequestFullscreen();
			}
		}

		function exitFullScreen() {
			if (document.exitFullscreen) {
				document.exitFullscreen();
			} else if (document.msExitFullscreen) {
				document.msExitFullscreen();
			} else if (document.webkitExitFullscreen) {
				document.webkitExitFullscreen();
			}
		}

	});

	// 측정
	$('#mapCtrlMeasure').click(function() {
		$(this).toggleClass('on');
		$('#controlMeasureWrap').toggle();
	});

	// 거리 측정
	$('#mapCtrlMeasureDistance').click(function() {
		$(this).siblings().removeClass('on');
		$(this).toggleClass('on');
	});

	// 면적 측정
	$('#mapCtrlMeasureArea').click(function() {
		$(this).siblings().removeClass('on');
		$(this).toggleClass('on');
	});

	// 높이 측정
	$('#mapCtrlMeasureHeight').click(function() {
		$(this).siblings().removeClass('on');
		$(this).toggleClass('on');
	});

	// 설정
	$('#mapCtrlSetting').click(function() {
		$(this).toggleClass('on');
		if ($(this).hasClass('on')) {
			$('#mapSettingWrap').css('width', '340px');
			$('#mapCtrlWrap').css('right', '340px');
			$('#mapCtrlCompassOut').css('right', '340px');
			$('.mago3d-overlayContainer-defaultControl').css('right', '340px');
		} else {
			$('#mapSettingWrap').css('width', '0');
			$('#mapCtrlWrap').css('right', '0');
			$('#mapCtrlCompassOut').css('right', '0');
			$('.mago3d-overlayContainer-defaultControl').css('right', '0');
		}
	});

	// 확대
	$('#mapCtrlZoomIn').click(function() {
		var magoManager = MAGO3D_INSTANCE.getMagoManager();
		if (magoManager.isCesiumGlobe()) {
			var scene = magoManager.scene;
			var camera = scene.camera;
			var cartographicPosition = Cesium.Cartographic.fromCartesian(camera.position);
			var alt = cartographicPosition.height;
			scene.camera.zoomIn(alt * 0.1);

		}
	});

	// 축소
	$('#mapCtrlZoomOut').click(function() {
		var magoManager = MAGO3D_INSTANCE.getMagoManager();
		if (magoManager.isCesiumGlobe()) {
			var scene = magoManager.scene;
			var camera = scene.camera;
			var cartographicPosition = Cesium.Cartographic.fromCartesian(camera.position);
			var alt = cartographicPosition.height;
			scene.camera.zoomOut(alt * 0.1);
		}
	});
});



