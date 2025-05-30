package com.finals.appdev50;

// NoInternet.java
import android.content.Context;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;
import com.google.android.material.snackbar.Snackbar;

import org.imaginativeworld.oopsnointernet.callbacks.ConnectionCallback;
import org.imaginativeworld.oopsnointernet.dialogs.pendulum.DialogPropertiesPendulum;
import org.imaginativeworld.oopsnointernet.dialogs.pendulum.NoInternetDialogPendulum;
import org.imaginativeworld.oopsnointernet.dialogs.signal.DialogPropertiesSignal;
import org.imaginativeworld.oopsnointernet.dialogs.signal.NoInternetDialogSignal;
import org.imaginativeworld.oopsnointernet.snackbars.fire.NoInternetSnackbarFire;
import org.imaginativeworld.oopsnointernet.snackbars.fire.SnackbarPropertiesFire;

public class noInternet {

    public static void showPendulumDialog(Context context, Lifecycle lifecycle) {
        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            NoInternetDialogPendulum.Builder builder = new NoInternetDialogPendulum.Builder(activity, lifecycle);
            DialogPropertiesPendulum properties = builder.getDialogProperties();

            properties.setConnectionCallback(new ConnectionCallback() {
                @Override
                public void hasActiveConnection(boolean hasActiveConnection) {
                    // Optional: Handle connection change
                }
            });

            properties.setCancelable(false); // Optional
            properties.setNoInternetConnectionTitle("No Internet");
            properties.setNoInternetConnectionMessage("Check your Internet connection and try again");
            properties.setShowInternetOnButtons(true);
            properties.setPleaseTurnOnText("Please turn on");
            properties.setWifiOnButtonText("Wifi");
            properties.setMobileDataOnButtonText("Mobile data");

            properties.setOnAirplaneModeTitle("No Internet");
            properties.setOnAirplaneModeMessage("You have turned on the airplane mode.");
            properties.setPleaseTurnOffText("Please turn off");
            properties.setAirplaneModeOffButtonText("Airplane mode");
            properties.setShowAirplaneModeOffButtons(true);

            builder.build();
        }
    }

    public static void showSignalDialog(Context context, Lifecycle lifecycle) {
        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            NoInternetDialogSignal.Builder builder = new NoInternetDialogSignal.Builder(activity, lifecycle);
            DialogPropertiesSignal properties = builder.getDialogProperties();

            properties.setConnectionCallback(new ConnectionCallback() {
                @Override
                public void hasActiveConnection(boolean hasActiveConnection) {
                    // Optional: Handle connection change
                }
            });

            properties.setCancelable(false); // Optional
            properties.setNoInternetConnectionTitle("No Internet");
            properties.setNoInternetConnectionMessage("Check your Internet connection and try again");
            properties.setShowInternetOnButtons(true);
            properties.setPleaseTurnOnText("Please turn on");
            properties.setWifiOnButtonText("Wifi");
            properties.setMobileDataOnButtonText("Mobile data");

            properties.setOnAirplaneModeTitle("No Internet");
            properties.setOnAirplaneModeMessage("You have turned on the airplane mode.");
            properties.setPleaseTurnOffText("Please turn off");
            properties.setAirplaneModeOffButtonText("Airplane mode");
            properties.setShowAirplaneModeOffButtons(true);

            builder.build();
        }
    }

    public static void showSnackbarFire(Context context, Lifecycle lifecycle, ViewGroup mainContainer) {
        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            NoInternetSnackbarFire.Builder builder = new NoInternetSnackbarFire.Builder(mainContainer, lifecycle);
            SnackbarPropertiesFire properties = builder.getSnackbarProperties();

            properties.setConnectionCallback(new ConnectionCallback() {
                @Override
                public void hasActiveConnection(boolean hasActiveConnection) {
                    // Optional: Handle connection change
                }
            });

            properties.setDuration(Snackbar.LENGTH_INDEFINITE); // Optional
            properties.setNoInternetConnectionMessage("No active Internet connection!");
            properties.setOnAirplaneModeMessage("You have turned on the airplane mode!");
            properties.setSnackbarActionText("Settings");
            properties.setShowActionToDismiss(false); // Optional
            properties.setSnackbarDismissActionText("OK");

            builder.build();
        }
    }
}
