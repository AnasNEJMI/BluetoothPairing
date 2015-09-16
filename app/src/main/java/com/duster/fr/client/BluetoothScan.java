package com.duster.fr.client;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Anas on 15/09/2015.
 */
public class BluetoothScan extends Fragment {

    public View OnCreateView(LayoutInflater inflater,ViewGroup container, Bundle savedInstanceState){
        return inflater.inflate(R.layout.activity_main,container,false);
    }
}