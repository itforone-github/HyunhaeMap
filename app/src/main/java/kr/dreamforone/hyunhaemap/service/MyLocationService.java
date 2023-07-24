package kr.dreamforone.hyunhaemap.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import kr.dreamforone.hyunhaemap.R;
import util.Common;


public class MyLocationService extends Service {
    private static final int SERVICE_NOTIFICATION_ID = 1;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Location lastKnownLocation;
    private static final long MIN_TIME_BETWEEN_UPDATES = 3000; // 5초마다 (밀리초 단위)
    double latitude = 0.0;//위도
    double longitude = 0.0;//경도
    Socket socket;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String socketUrl = getString(R.string.socket_url);//res/values/string.xml에 설정
        //소켓통신하기
        try{

            socket = IO.socket(socketUrl+"/gps");//소켓 객체 생성하기

            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.d("connect1","connect");
                }
            }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.d("disconnect","disconnect");
                }
            });
            // Socket.IO 서버와 연결합니다.
            socket.connect();
        }catch (Exception e){
            Toast.makeText(MyLocationService.this, "소켓통신", Toast.LENGTH_SHORT).show();
            Log.e("socket-error",e.getStackTrace().toString());
        }
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                // 위치 정보 변경될 때마다 호출되는 콜백 메서드
                latitude = location.getLatitude();//위도
                longitude = location.getLongitude();//경도
                //현재 위치를 저장하기
                Common.savePref(getApplicationContext(),"lat",(float)latitude);
                Common.savePref(getApplicationContext(),"lng",(float)longitude);


                //움직임이 감지가 됐을 때
                if (lastKnownLocation == null || hasMoved(lastKnownLocation, location)) {
                    // 마지막 위치가 없거나 현재 위치가 이동한 경우 작업 수행
                    lastKnownLocation = location;
                    performTaskWithLocation(location);
                    JSONObject data = new JSONObject();
                    Log.d("gps1", latitude + ":" + longitude);
                    try{
                        //소켓통신으로 데이터 보내기
                        data.put("mb_id", Common.getPref(getApplicationContext(),"mb_id",""));
                        data.put("mb_name", Common.getPref(getApplicationContext(),"mb_name",""));
                        data.put("lat",latitude);
                        data.put("lng",longitude);
                        data.put("is_move",true);
                    }catch (Exception e){
                        Log.d("error",e.getStackTrace().toString());
                    }
                    // 이벤트 전송
                    socket.emit("location", data.toString());
                //움직임이 감지가 없을 때
                }else{
                    performTaskWithLocation(lastKnownLocation);

                    JSONObject data = new JSONObject();

                    Log.d("gps1", latitude + ":" + longitude);
                    try{
                        //소켓통신으로 데이터 보내기
                        data.put("mb_id", Common.getPref(getApplicationContext(),"mb_id",""));
                        data.put("mb_name", Common.getPref(getApplicationContext(),"mb_name",""));
                        data.put("lat",latitude);
                        data.put("lng",longitude);
                        data.put("is_move",false);
                    }catch (Exception e){
                        Log.d("error",e.getStackTrace().toString());
                    }
                    // 이벤트 전송
                    socket.emit("location", data.toString());
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.d("onStatusChanged",status+"");
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };

        // 위치 업데이트 시작
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BETWEEN_UPDATES, 0, locationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BETWEEN_UPDATES, 0, locationListener);
        }

        // Foreground 서비스로 실행
        startForeground(SERVICE_NOTIFICATION_ID, buildNotification());


        return START_STICKY;
    }

    private boolean hasMoved(Location lastLocation, Location currentLocation) {
        // 마지막 위치와 현재 위치를 비교하여 움직임 여부 확인
        float distance = lastLocation.distanceTo(currentLocation);
        return distance > 0;
    }
    //노티피케이션이 있어야 백그라운드 실행이 됨
    private Notification buildNotification() {
        // Foreground 서비스를 위한 알림 채널 생성 (Android 8.0 이상)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("channel_id", "Channel Name", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        // Foreground 서비스를 위한 알림 생성
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channel_id")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("백그라운드 서비스 실행 중")
                .setContentText("실시간 관제중입니다.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true);//알림을 제거하지 못하게
        return builder.build();
    }

    private void performTaskWithLocation(Location location) {
        // 좌표값을 사용하여 원하는 작업 수행
        latitude = location.getLatitude();
        longitude = location.getLongitude();

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 위치 업데이트 중지
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
        if (socket != null) {
            socket.disconnect();
            Log.d("disconnect1","disconnect");
        }

    }
    private void updateNotification() {
        Notification notification = buildNotification();
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.notify(SERVICE_NOTIFICATION_ID, notification);
    }
}