$(()=> {
    const cesiumObj = cesiumInit.init();
    cesiumMouseEvt.init(cesiumObj);
    render.init();
});

const landScapeTypeSelect = {
    ele: '#landScapeTypeSelect',
    init: function() {
        $(this.ele).change(function() {
            if(this.value === 0) {
                render.renderType = RenderType.DOT
            } else {
                render.renderType = RenderType.LINE
            }

        });
    }
};

const cesiumInit = {
    ele: 'cesiumContainer',
    viewer: undefined,
    scene: undefined,
    canvas: undefined,
    init: function() {
        const extent = Cesium.Rectangle.fromDegrees(117.896284, 31.499028, 139.597380, 43.311528);

        Cesium.Camera.DEFAULT_VIEW_RECTANGLE = extent;
        Cesium.Camera.DEFAULT_VIEW_FACTOR = 0.7;
        Cesium.Ion.defaultAccessToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiO' +
            'iIxODQ0NTUxYi1mODg3LTQxZTEtYmU2Zi00NzQ0ODI3YjI1ZDIiLCJpZCI6MTUxODY' +
            'sInNjb3BlcyI6WyJhc2wiLCJhc3IiLCJhc3ciLCJnYyJdLCJpYXQiOjE1Njc0MDU5MDJ9.g' +
            'qA_lEtPeiKI_Tn6WbBKfcaSaiHmj0f1GmcD0VBtmPc';
        this.viewer = new Cesium.Viewer(this.ele,
            {
                terrainProvider: new Cesium.CesiumTerrainProvider({
                    url: Cesium.IonResource.fromAssetId(85669),
                }),
                timeline : true,
                animation : true,
                selectionIndicator : true,
                navigationHelpButton : false,
                infoBox : false,
                shadows: true,
                navigationInstructionsInitiallyVisible : true,
                baseLayerPicker : true
            });
        this.viewer.imageryLayers.addImageryProvider(
            new Cesium.IonImageryProvider({ assetId: 4 })
        );
        this.viewer.clock.shouldAnimate = true;
        this.scene = this.viewer.scene;
        this.canvas = this.viewer.canvas;
        return this;
    },
};

const cesiumMouseEvt = {
    viewer: undefined,
    scene: undefined,
    canvas: undefined,
    pos: {
        start: undefined,
        end: undefined,
        move: undefined
    },
    entity: {
        start: undefined,
        end: undefined,
        line: undefined
    },
    workType: undefined,
    init: function(cesiumObj) {
        cesiumMouseEvt.viewer = cesiumObj.viewer;
        cesiumMouseEvt.scene = cesiumObj.scene;
        cesiumMouseEvt.canvas = cesiumObj.canvas;
        cesiumMouseEvt.workType = LandsDirecWorkType.WAIT;
        cesiumMouseEvt.leftMouseDBClick();
        cesiumMouseEvt.mouseMove();
    },
    clearEntity: function() {
        cesiumMouseEvt.pos.start = undefined;
        cesiumMouseEvt.pos.end = undefined;
        debugger;
        const entityLine = cesiumMouseEvt.entity.line;
        const entityStartDot = cesiumMouseEvt.entity.start;
        const entityEndDot = cesiumMouseEvt.entity.end;

        if (entityLine !== undefined) {
            cesiumMouseEvt.viewer.entities.remove(entityLine);
            cesiumMouseEvt.entity.line = undefined;
        }
        if (entityStartDot !== undefined) {
            cesiumMouseEvt.viewer.entities.remove(entityStartDot);
            cesiumMouseEvt.entity.start = undefined;
        }
        if (entityEndDot !== undefined) {
            cesiumMouseEvt.viewer.entities.remove(entityEndDot);
            cesiumMouseEvt.entity.end = undefined;
        }
    },
    leftMouseDBClick: function() {
        const handler = new Cesium.ScreenSpaceEventHandler(cesiumMouseEvt.canvas);
        handler.setInputAction( (click) => {
                if ( render.renderType === RenderType.DOT ) {
                    cesiumMouseEvt.action.dot(click);
                } else {
                    cesiumMouseEvt.action.line(click);
                }
            },
            Cesium.ScreenSpaceEventType.LEFT_DOUBLE_CLICK
        );
    },
    mouseMove: function() {
        const handler = new Cesium.ScreenSpaceEventHandler(cesiumMouseEvt.canvas);
        handler.setInputAction( (move) => {
            console.log(move);
                cesiumMouseEvt.pos.move = cesiumMouseEvt.posByEvt(move.endPosition);
            },
            Cesium.ScreenSpaceEventType.MOUSE_MOVE
        );
    },
    action: {
        dot: function(click) {
            cesiumMouseEvt.clearEntity();
            const pos = cesiumMouseEvt.posByEvt(click.position);
            cesiumMouseEvt.pos.start = pos;
            cesiumMouseEvt.entity.start = render.dot(cesiumMouseEvt.viewer, pos);
        },
        line: function(click) {
            const pos = cesiumMouseEvt.posByEvt(click.position);
            const workType = cesiumMouseEvt.workType;
            if( workType === LandsDirecWorkType.WAIT) {
                cesiumMouseEvt.clearEntity();
                console.log(`wait`); // line drawing
                cesiumMouseEvt.pos.start = pos;
                cesiumMouseEvt.entity.start = render.dot(cesiumMouseEvt.viewer, pos);
                cesiumMouseEvt.entity.line = render.line(cesiumMouseEvt.viewer, pos);
                cesiumMouseEvt.workType = LandsDirecWorkType.RUN;
            } else if(workType === LandsDirecWorkType.RUN) {
                console.log(`run`);
                cesiumMouseEvt.workType = LandsDirecWorkType.WAIT;
                cesiumMouseEvt.entity.end = render.dot(cesiumMouseEvt.viewer, pos);
                cesiumMouseEvt.pos.end = pos;
            }
        }
    },
    posByEvt: function(e) {
        const cartesian = cesiumMouseEvt.scene.pickPosition(e);
        if (cartesian) {
            const cartographic = Cesium.Cartographic.fromCartesian(cartesian);
            const longitudeString = Cesium.Math.toDegrees(
                cartographic.longitude
            );
            const latitudeString = Cesium.Math.toDegrees(
                cartographic.latitude
            );
            const height = cartographic.height;
            return  {
                long: longitudeString,
                lat: latitudeString,
                alt: height
            };
        }
    }
};

const render = {
    renderType: undefined,
    init: function() {
        this.renderType = RenderType.LINE
    },
    dot: function(viewer, pos) {
        const result = viewer.entities.add({
            position: new Cesium.Cartesian3.fromDegrees(pos.long, pos.lat, pos.alt),
            ellipsoid: {
                radii: new Cesium.Cartesian3(10, 10, 10),
                material: Cesium.Color.RED,
            },
        });
        return result;
    },
    line: (viewer, pos) => {
        const result = viewer.entities.add({
            polyline: {
                // This callback updates positions each frame.
                positions: new Cesium.CallbackProperty(() => {
                    const resTimeMousePos = cesiumMouseEvt.pos.move;
                    let p;
                    if (cesiumMouseEvt.workType === LandsDirecWorkType.WAIT) {
                        p = Cesium.Cartesian3.fromDegreesArrayHeights(this.polyLinelastPos);
                        return p;
                    }
                    this.polyLinelastPos = [pos.long, pos.lat, pos.alt, resTimeMousePos.long,
                        resTimeMousePos.lat, resTimeMousePos.alt];
                    p = Cesium.Cartesian3.fromDegreesArrayHeights(
                        this.polyLinelastPos
                    );
                    return p;
                }, false),
                width: 10,
                clampToGround: true,
                material: new Cesium.PolylineOutlineMaterialProperty({
                    color: Cesium.Color.YELLOW,
                })
            },
        });
        return result;
    }
};
const LandsDirecWorkType = {
    WAIT : 0,
    RUN : 1,
    FINISH : 2,
    POLY : 3
};
const MouseEvtType = {
    UP : 0,
    DOWN : 1,
    MOVE : 2
};
const RenderType = {
    DOT : 0,
    LINE : 1
};
