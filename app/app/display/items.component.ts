import {AfterViewInit, Component, ElementRef, OnInit, Renderer2, ViewChild} from "@angular/core";

import {Item} from "./item";
import {ItemService} from "./item.service";
import {TextDecoder} from "text-encoding";
import {Image} from "tns-core-modules/ui/image";
import {Peripheral, ReadResult} from "nativescript-bluetooth";
import bluetooth = require("nativescript-bluetooth");

const SCAN_DURATION_SECONDS: number = 4;
const SNAKE_MOVE_SERVICE_ID = "aab79343-9a83-4886-a1d3-32a800259937";
const SNAKE_MOVE_CHARACTERISTIC_ID = "e4990f35-28f4-40d8-bfa2-f05118720a28";
const ACCELEROMETER_SERVICE_ID = "e95d0753-251d-470a-a062-fa1922dfa9a8";
const ACCELEROMETER_DATA_CHARACTERISTIC_ID = "e95dca4b-251d-470a-a062-fa1922dfa9a8";
const ACCELEROMETER_PERIOD_CHARACTERISTIC_ID = "e95dfb24-251d-470a-a062-fa1922dfa9a8";

/**
 * Performs a low pass filter.
 * https://en.wikipedia.org/wiki/Low-pass_filter
 *
 * @param {number[]} a the new signal.
 * @param {number[]} b the old signal.
 * @returns {number[]} the filtered response.
 */
export function lowPass(a: number[], b: number[]): number[] {
    if (b == null) {
        return a;
    }

    for (let i = 0; i < a.length; i++) {
        b[i] += 0.1 * (a[i] - b[i]);
    }

    return b;
}

@Component({
    selector: "ns-items",
    moduleId: module.id,
    templateUrl: "./items.component.html",
})
export class ItemsComponent implements OnInit, AfterViewInit {
    @ViewChild("player1") player1Image: ElementRef;
    @ViewChild("player2") player2Image: ElementRef;
    items: Item[];
    scanning: boolean = false;
    scanningText: string = "Scanning";
    multiplayer: boolean = false;
    devicesFound: number = 0;
    connectingIds: Set<string> = new Set();
    previousAccelerometer: number[] = null;

    // This pattern makes use of Angular’s dependency injection implementation to inject an instance of the ItemService service into this class. 
    // Angular knows about this service because it is included in your app’s main NgModule, defined in app.module.ts.
    constructor(private itemService: ItemService,
                private rd: Renderer2,
                private el: ElementRef) {
    }

    ngOnInit(): void {
        this.items = this.itemService.getItems();

        setTimeout(() => {
            this.scan();
        }, 1000);
    }

    ngAfterViewInit() {
        // console.log(this.rd);
        // const image: android.widget.Button = new android.widget.Button(app.android.context);
        // console.log("hi" + JSON.stringify(this.el.nativeElement));
        // this.rd.appendChild(this.el.nativeElement, image);
    }

    public playGame() {
        alert("Game play is unsupported!");
    }

    public toggleMode() {
        this.multiplayer = !this.multiplayer;
    }

    public scan(): void {
        if (this.scanning) {
            console.log("You are already scanning!");
            return;
        }

        this.scanning = true;
        this.scanningText = "Scanning (" + SCAN_DURATION_SECONDS + " seconds remain)";

        for (let i = 1; i < SCAN_DURATION_SECONDS; i++) {
            setTimeout(() => {
                this.scanningText = "Scanning (" + i + " seconds remain)";
            }, (SCAN_DURATION_SECONDS - i) * 1000);
        }

        bluetooth.startScanning({
            // serviceUUIDs: [SERVICE_ID],
            serviceUUIDs: [], // Match any peripheral.
            seconds: SCAN_DURATION_SECONDS,
            onDiscovered: (peripheral: Peripheral) => this.onDiscovered(peripheral)
        }).then(() => {
            this.scanning = false;
            console.log("Scan complete, " + this.devicesFound + " devices found.");
        }, error => {
            this.scanning = false;
            console.log("Scanning error: " + error);
        });
    }

    public onDiscovered(peripheral: Peripheral): void {
        this.devicesFound++;
        this.connect(peripheral, true);
    }

    public connect(peripheral: Peripheral, msg: boolean): void {
        if (!peripheral.name || !peripheral.name.startsWith('BBC micro:bit')) {
            return;
        }

        if (this.connectingIds.has(peripheral.UUID)) {
            console.log("Already connecting to " + peripheral.name);
            return;
        }

        console.log("Connecting to: " + peripheral.name);
        this.connectingIds.add(peripheral.UUID);

        bluetooth.connect({
            UUID: peripheral.UUID,
            onConnected: (peripheral: Peripheral) => this.onConnected(peripheral),
            onDisconnected: () => this.onDisconnected(peripheral)
        });
    }

    public onConnected(peripheral: Peripheral): void {
        console.log("Connected to " + peripheral.name);
        this.connectingIds.delete(peripheral.UUID);
        this.subscribeAccelerometer(peripheral);
    }

    public onDisconnected(peripheral: Peripheral): void {
        this.connectingIds.delete(peripheral.UUID);
        console.log("Disconnected from " + peripheral.name);
    }

    public subscribeAccelerometer(peripheral: Peripheral): void {
        bluetooth.setCharacteristicLogging(false);

        bluetooth.startNotifying({
            peripheralUUID: peripheral.UUID,
            serviceUUID: SNAKE_MOVE_SERVICE_ID,
            characteristicUUID: SNAKE_MOVE_CHARACTERISTIC_ID,
            onNotify: (result: ReadResult) => this.onNotify(peripheral, result)
        }).then(() => {
            return new Promise((resolve => {
                setTimeout(resolve, 100);
            }));
        }).then(() => {
            return bluetooth.write({
                peripheralUUID: peripheral.UUID,
                serviceUUID: ACCELEROMETER_SERVICE_ID,
                characteristicUUID: ACCELEROMETER_PERIOD_CHARACTERISTIC_ID,
                value: new Uint16Array([640])
            });
        }).then(() => {
            return bluetooth.startNotifying({
                peripheralUUID: peripheral.UUID,
                serviceUUID: ACCELEROMETER_SERVICE_ID,
                characteristicUUID: ACCELEROMETER_DATA_CHARACTERISTIC_ID,
                onNotify: (result: ReadResult) => this.onNotify(peripheral, result)
            });
        }).then(() => {
            console.log("Notifications subscribed");
        });
    }

    public onNotify(peripheral: Peripheral, result: ReadResult): void {
        // Sadly, only 1 notification callback may be assigned per peripheral.
        // Due to this weird limitation provided by the nativescript-bluetooth
        // library, this is the adaption. Apologies if this is gross, I've not
        // had the time spare to create a PR to resolve this for that library.
        if (result.characteristicUUID == SNAKE_MOVE_CHARACTERISTIC_ID) {
            const data = new Int16Array(result.value);
            const moveX = data[0];
            const moveY = data[1];
            console.log("MOVEMENT RECEIVED - X: " + moveX + " Y: " + moveY);
        } else {
            const data = new Int16Array(result.value);

            const current: number[] = [data[0] / 1000, data[1] / 1000, data[2] / 1000];
            this.previousAccelerometer = lowPass(current, this.previousAccelerometer);

            const accelerometerX = this.previousAccelerometer[0];
            const accelerometerY = this.previousAccelerometer[1];
            const accelerometerZ = this.previousAccelerometer[2];

            // noinspection JSSuspiciousNameCombination
            let pitch = Math.atan(accelerometerX / Math.sqrt(Math.pow(accelerometerY, 2) + Math.pow(accelerometerZ, 2)));
            let roll = Math.atan(accelerometerY / Math.sqrt(Math.pow(accelerometerX, 2) + Math.pow(accelerometerZ, 2)));
            pitch *= (180.0 / Math.PI);
            roll *= -(180.0 / Math.PI);

            const thing: Image = this.player1Image.nativeElement;
            const image: android.widget.ImageView = thing.android;
            image.setRotationX(roll);
            image.setRotationY(pitch);
        }
    }
}
