import {Component, OnInit} from "@angular/core";

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


const SCAN_DURATION_SECONDS: number = 4;
const UART_SERVICE_ID = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
const UART_RX_CHARACTERISTIC_ID = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
const UART_TX_CHARACTERISTIC_ID = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";

@Component({
    selector: "ns-items",
    moduleId: module.id,
    templateUrl: "./items.component.html",
})
export class ItemsComponent implements OnInit {
    items: Item[];
    scanning: boolean = false;
    devicesFound: number = 0;
    connectingIds: Set<string> = new Set();

    // This pattern makes use of Angular’s dependency injection implementation to inject an instance of the ItemService service into this class. 
    // Angular knows about this service because it is included in your app’s main NgModule, defined in app.module.ts.
    constructor(private itemService: ItemService) {
    }

    ngOnInit(): void {
        this.items = this.itemService.getItems();
    }

    public scan(): void {
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
