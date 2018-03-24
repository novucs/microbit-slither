import {AfterViewInit, Component, ElementRef, OnInit, Renderer2, ViewChild} from "@angular/core";

import {Item} from "./item";
import {ItemService} from "./item.service";

import {
    Characteristic,
    connect,
    Peripheral,
    ReadResult,
    Service,
    startNotifying,
    startScanning
} from "nativescript-bluetooth";
import {TextDecoder} from "text-encoding";
import * as app from "tns-core-modules/application";
import * as platform from "tns-core-modules/platform";
import {Image} from "tns-core-modules/ui/image";
import ImageView = org.nativescript.widgets.ImageView;
import {Button} from "tns-core-modules/ui/button";


const SCAN_DURATION_SECONDS: number = 4;
const UART_SERVICE_ID = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
const UART_RX_CHARACTERISTIC_ID = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
const UART_TX_CHARACTERISTIC_ID = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";

@Component({
    selector: "ns-items",
    moduleId: module.id,
    templateUrl: "./items.component.html",
})
export class ItemsComponent implements OnInit, AfterViewInit {
    @ViewChild("foo") microbitImage: ElementRef;
    @ViewChild("bar") bar: ElementRef;
    items: Item[];
    scanning: boolean = false;
    devicesFound: number = 0;
    connectingIds: Set<string> = new Set();

    // This pattern makes use of Angular’s dependency injection implementation to inject an instance of the ItemService service into this class. 
    // Angular knows about this service because it is included in your app’s main NgModule, defined in app.module.ts.
    constructor(private itemService: ItemService,
                private rd: Renderer2,
                private el: ElementRef) {
    }

    ngOnInit(): void {
        this.items = this.itemService.getItems();

        setTimeout(() => {
            const thing: Image = this.microbitImage.nativeElement;
            console.log(thing);
            console.log(thing.android);
            console.log(thing.ios);
            console.log(thing.imageSource);
            console.log(thing.src);
            console.log(thing.stretch);

            const image: android.widget.ImageView = thing.android;
            image.setRotationX(3.0);
            image.setRotationY(3.0);
        }, 2000);
    }

    ngAfterViewInit() {
        // console.log(this.rd);
        // const image: android.widget.Button = new android.widget.Button(app.android.context);
        // console.log("hi" + JSON.stringify(this.el.nativeElement));
        // this.rd.appendChild(this.el.nativeElement, image);
    }

    public scan(): void {
//         const javaLangPkg = java.lang;
//         const androidPkg = android;
//         const androidViewPkg = android.view;
//
// // access classes from inside the packages later on
//
//         const View = androidViewPkg.View;
//         const image: android.widget.Button = android.app;
//         image.setRotationX(10);
//         image.setRotationY(10);
//
//
//         const Object = javaLangPkg.Object; // === java.lang.Object;


        if (this.scanning) {
            console.log("You are already scanning!");
            return;
        }

        for (let i = 0; i < SCAN_DURATION_SECONDS; i++) {
            setTimeout(() => {
                console.log("Scanning, " + (SCAN_DURATION_SECONDS - i) + " seconds remaining");
            }, i * 1000);
        }

        startScanning({
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

        connect({
            UUID: peripheral.UUID,
            onConnected: (peripheral: Peripheral) => this.onConnected(peripheral),
            onDisconnected: () => this.onDisconnected(peripheral)
        });
    }

    public onConnected(peripheral: Peripheral): void {
        console.log("Connected to " + peripheral.name);
        this.subscribe(peripheral);
    }

    public onDisconnected(peripheral: Peripheral): void {
        this.connectingIds.delete(peripheral.UUID);
        console.log("Disconnected from " + peripheral.name);
    }


    public subscribe(peripheral: Peripheral): void {
        const uartService: Service = peripheral.services.find((service: Service) =>
            service.UUID == UART_SERVICE_ID);
        const uartTxCharacteristic = uartService.characteristics.find((characteristic: Characteristic) =>
            characteristic.UUID == UART_RX_CHARACTERISTIC_ID);
        startNotifying({
            peripheralUUID: peripheral.UUID,
            serviceUUID: uartService.UUID,
            characteristicUUID: uartTxCharacteristic.UUID,
            onNotify: (result: ReadResult) => this.onNotify(peripheral, result)
        }).then(() => {
            console.log("Notifications subscribed");
        });
    }

    public onNotify(peripheral: Peripheral, result: ReadResult): void {
        const text = new TextDecoder("UTF-8").decode(result.value);
        console.log("Received message: " + text);
    }
}
