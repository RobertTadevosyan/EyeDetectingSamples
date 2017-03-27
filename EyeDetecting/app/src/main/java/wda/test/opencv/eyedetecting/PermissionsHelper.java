package wda.test.opencv.eyedetecting;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anahit on 3/21/17.
 */

public class PermissionsHelper {

    public static void gettingPermissions(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int hasCameraPermission = activity.checkSelfPermission(Manifest.permission.CAMERA);
            int hasWriteExternalStoragePermission = activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int hasReadExternalStoragePermission = activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);

            List<String> permissions = new ArrayList<String>();

            if (hasCameraPermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.CAMERA);

            }

            if (hasWriteExternalStoragePermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

            }

            if (hasReadExternalStoragePermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);

            }

            if (!permissions.isEmpty()) {
                activity.requestPermissions(permissions.toArray(new String[permissions.size()]), 111);
            }
        }
    }
}
