package rsc.journal;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;

public class MyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        // Create intent on action
        Intent intentMain = new Intent(context, DayActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, intentMain, PendingIntent.FLAG_UPDATE_CURRENT);

        // Create Notification
        Notification.Builder notification_builder;
        NotificationManager notification_manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // For new devices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String chanel_id = "j1";
            NotificationChannel mChannel = new NotificationChannel(chanel_id, "Напоминания", NotificationManager.IMPORTANCE_HIGH);
            notification_manager.createNotificationChannel(mChannel);
            notification_builder = new Notification.Builder(context, chanel_id);
        } else {
            notification_builder = new Notification.Builder(context);
        }

        notification_builder
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Напоминание")
                .setContentText(intent.getStringExtra("data"))
                .setContentIntent(resultPendingIntent)
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(Notification.PRIORITY_MAX)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setAutoCancel(true);

        notification_manager.notify(0, notification_builder.build());
    }
}
