package kr.dreamforone.hyunhaemap;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.normal.TedPermission;


import java.util.List;



/*
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;*/

public class SplashActivity extends AppCompatActivity {
  private static final int APP_PERMISSION_STORAGE = 9787;
  private final int APPS_PERMISSION_REQUEST=1000;
  final int SEC=1000;//다음 화면에 넘어가기 전에 머물 수 있는 시간(초)
  public static boolean isStart=true;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    //setContentView(R.layout.activity_splash);
    setContentView(R.layout.activity_splash);
    TedPermission.create()
            .setPermissions(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
            )
            .setRationaleMessage("이 앱은 권한설정을 하셔야 사용하실 수 있습니다.")
            .setDeniedMessage("권한설정에 거부하시면 앱설정에서 직접하셔야 합니다.")
            .setPermissionListener(permissionListener)
            .check();
  }
  PermissionListener permissionListener = new PermissionListener() {
    //퍼미션 설정을 하면
    @Override
    public void onPermissionGranted() {
      try{
        //30버전 이상일 때 백그라운드 gps가 있는지 확인
        //30버전부터는 항상 허용이 없기 때문에 설정에서 직접 설정을 해야 함
        if(30<=Build.VERSION.SDK_INT) {
          int permissionCheck2 = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION);
          //권한설정이 안 되어 있으면은
          if (permissionCheck2 == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(SplashActivity.this, "백그라운드 gps를 사용하시려면 위치를 항상 허용으로 변경하시면 됩니다.", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(SplashActivity.this,new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},0);
          }
        }

        goHandler();

                /*LocationPosition.act= mActivity;
                LocationPosition.setPosition(mActivity);
                if(LocationPosition.lng==0.0){
                    LocationPosition.setPosition(mActivity);
                }*/
      }catch(Exception e){

      }

    }
    //퍼미션 설정을 하지 않으면
    @Override
    public void onPermissionDenied(List<String> deniedPermissions) {

    }
  };

  //핸들러로 이용해서 3초간 머물고 이동이 됨
  public void goHandler() {
    Handler mHandler = new Handler();
    mHandler.postDelayed(new Runnable() {
      @Override
      public void run() {
        isStart=true;
        finish();
      }
    }, SEC);
  }
}
