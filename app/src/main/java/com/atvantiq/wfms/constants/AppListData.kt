package com.atvantiq.wfms.constants

import com.atvantiq.wfms.models.reimbursement.TravelModeOption

object AppListData {
    val purposes = listOf(
        "Installation",
        "Maintenance",
        "Repair"
    )
    val  sites = listOf(
        "Site A",
        "Site B",
        "Site C"
    )

    val localTravelModes: List<TravelModeOption> = listOf(
        TravelModeOption(label = "Auto / Local Taxi", value = "AUTO_LOCAL_TAXI"),
        TravelModeOption(label = "Bus", value = "BUS"),
        TravelModeOption(label = "Bike (Auto Fetch KM)", value = "BIKE_AUTO_KM"),
        TravelModeOption(label = "Cab (App Based)", value = "CAB_APP"),
        TravelModeOption(label = "Car", value = "CAR"),
    )

    val outstationTravelModes: List<TravelModeOption> = listOf(
        TravelModeOption(label = "Auto / Local Taxi", value = "AUTO_LOCAL_TAXI"),
        TravelModeOption(label = "Bus", value = "BUS"),
        TravelModeOption(label = "Bike (Auto Fetch KM)", value = "BIKE_AUTO_KM"),
        TravelModeOption(label = "Cab (App Based)", value = "CAB_APP"),
        TravelModeOption(label = "Car", value = "CAR"),
        TravelModeOption(label = "Train", value = "TRAIN"),
        TravelModeOption(label = "Air", value = "AIR"),
    )

}