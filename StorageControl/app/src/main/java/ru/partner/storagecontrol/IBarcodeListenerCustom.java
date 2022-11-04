package ru.partner.storagecontrol;

public interface IBarcodeListenerCustom {
    void onBarcodeEvent(String barCode, String type);
    void onFailureEvent();
}
