package com.sysui.batt;

interface IExtractSubjectCallback {
    void onStart(String message);
    void onResult(boolean success, String message);
}