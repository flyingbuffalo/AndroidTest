/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.wifidirect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.util.Log;

/**
 * A BroadcastReceiver that notifies of important wifi p2p events.
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager wifiP2pManager;
    private Channel channel;
    private WiFiDirectActivity wfdActivity;

    /**
     * @param wifiP2pManager WifiP2pManager system service
     * @param channel Wifi p2p channel
     * @param wfdActivity wfdActivity associated with the receiver
     */
    public WiFiDirectBroadcastReceiver(WifiP2pManager wifiP2pManager, Channel channel,
            WiFiDirectActivity wfdActivity) {
        super();
        this.wifiP2pManager = wifiP2pManager;
        this.channel = channel;
        this.wfdActivity = wfdActivity;
    }

    /*
     * (non-Javadoc)
     * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
     * android.content.Intent)
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

            // UI update to indicate wifi p2p status.
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi Direct mode is enabled
                wfdActivity.setIsWifiP2pEnabled(true);
            } else {
                wfdActivity.setIsWifiP2pEnabled(false);
                wfdActivity.resetData();

            }
            Log.d(WiFiDirectActivity.TAG, "P2P state changed with state " + state);
            
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

            // request available peers from the wifi p2p wifiP2pManager. This is an
            // asynchronous call and the calling wfdActivity is notified with a
            // callback on PeerListListener.onPeersAvailable()
        	//THISIS 피어 상태가 바뀌거나 할때 DeviceListFragment의 피어리스트 리스너의 메서드 호
            if (wifiP2pManager != null) {
                wifiP2pManager.requestPeers(channel, (PeerListListener) wfdActivity.getFragmentManager()
                        .findFragmentById(R.id.frag_list));
            }
            Log.d(WiFiDirectActivity.TAG, "P2P peers changed");
            
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
        	//THISIS 피어랑 커넥션 될때, 또는 끈켰을때
            if (wifiP2pManager == null) {
            	Log.d(WiFiDirectActivity.TAG, "P2P connection changed, but wifiP2pManager is null");
                return;
            }

            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {

                // we are connected with the other device, request connection
                // info to find group owner IP

                DeviceDetailFragment fragment = (DeviceDetailFragment) wfdActivity
                        .getFragmentManager().findFragmentById(R.id.frag_detail);
                //THISIS DetailFragment에 ConnectionInfoListner의 onConnectionInfoAvailable 호출
                wifiP2pManager.requestConnectionInfo(channel, fragment);
                Log.d(WiFiDirectActivity.TAG, "P2P connection is connected");
            } else {
                // It's a disconnect
                wfdActivity.resetData();
                Log.d(WiFiDirectActivity.TAG, "P2P connection is disconnected");
            }
            
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            DeviceListFragment fragment = (DeviceListFragment) wfdActivity.getFragmentManager()
                    .findFragmentById(R.id.frag_list);
            fragment.updateThisDevice((WifiP2pDevice) intent.getParcelableExtra(
                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));
            Log.d(WiFiDirectActivity.TAG, "P2P this device changed");
        }
    }
}
