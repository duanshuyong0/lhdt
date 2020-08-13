var AnalsBuildHeight = function(viewer, magoInstance) {
    var magoManager = magoInstance.getMagoManager();
    this._viewer = viewer;
    this._scene = viewer.scene;

    this._polylines = [];
    this._labels = [];
    this._polyPoint = [];

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

    // magoManager.on(Mago3D.MagoManager.EVENT_TYPE.SMARTTILELOADEND, smartTileLoaEndCallbak);
    // magoManager.on(Mago3D.MagoManager.EVENT_TYPE.F4DLOADEND, echoLoadEndCallback);
    // 	면적 측정 버튼
    $('#heightAnalsObserverPoint').click(function() {
        var statusChecked = $("#heightAnalsObserverPoint").is(":checked");
        if(statusChecked) {
            drawingMode = 'polygon';

            startDrawPolyLine();
        } else {
            drawingMode = undefined;
        }
    });

    function getColor(v, min, max) {
        function getC(f, l, r) {
            return {
                r: Math.floor((1 - f) * l.r + f * r.r),
                g: Math.floor((1 - f) * l.g + f * r.g),
                b: Math.floor((1 - f) * l.b + f * r.b),
            };
        }

        var left = { r: 255, g: 0, b: 0 },
            middle = { r: 0, g: 255, b: 0 },
            right = { r: 0, g: 0, b: 255 },
            mid = (max - min) / 2;

        return v < min + mid ?
            getC((v - min) / mid, left, middle) :
            getC((v - min - mid) / mid, middle, right);
    }

    $('#heightAnalsObserverBtn').click(function() {
        $.ajax({
            url: "http://localhost:8090/anals/gis/get_data_info_by_poly"
        }).done(function(data) {
            const jsonData = JSON.parse(data);

            min = 0;
            max = jsonData.length;
            for(var p in jsonData) {
                var obj = jsonData[p];
                var color = getColor(p, min, max);
                console.log(color);
                changeColorAPI(magoInstance, obj.data_group_id, obj.data_key, null,
                    'isPhysical=true', color.r + ',' + color.g + ',' + color.b)
            }
        })
    })

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
                this._polyPoint.push({
                    lon: Cesium.Math.toDegrees(cartographic.longitude),
                    lat: Cesium.Math.toDegrees(cartographic.latitude)
                });
                this._polylines.push(createPoint(tempPosition));
            }
        }, Cesium.ScreenSpaceEventType.LEFT_CLICK);

        handler.setInputAction(function (event) {
            terminateShape();
        }, Cesium.ScreenSpaceEventType.RIGHT_CLICK);
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

    function getLineLength(positions) {
        lengthInMeters = 0;
        for (var i = 1, len = positions.length; i < len; i++) {
            var startPoint = positions[i - 1];
            var endPoint = positions[i];

            lengthInMeters += Cesium.Cartesian3.distance(startPoint, endPoint);
        }
        return formatDistance(lengthInMeters);
    }

};