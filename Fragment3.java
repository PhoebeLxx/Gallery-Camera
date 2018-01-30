package com.syberos.sdkdemo.fragment;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.syberos.imkit.SyberosIm;
import com.syberos.imkit.entity.UserInformation;
import com.syberos.imkit.listener.ModifyUserPortraitCallback;
import com.syberos.sdkdemo.R;
import com.syberos.sdkdemo.chatui.enity.FullImageInfo;
import com.syberos.sdkdemo.chatui.ui.activity.FullImageActivity;
import com.syberos.sdkdemo.utils.LogUtils;
import com.syberos.sdkdemo.utils.PhotoUtils;
import com.syberos.sdkdemo.utils.ToastUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;

import static android.app.Activity.RESULT_OK;

/**
 * Created by ML on 2018/1/11.
 */

public class Fragment3 extends Fragment {
    @Bind(R.id.imageView)
    ImageView imageView;
    @Bind(R.id.tvName)
    TextView name;
    @Bind(R.id.tvPhoneNum)
    TextView phone;
    @Bind(R.id.tvDepartment)
    TextView department;
    @Bind(R.id.tvEmail)
    TextView email;
    @Bind(R.id.tvId)
    TextView id;

    private static final int CODE_GALLERY_REQUEST = 0xa0;
    private static final int CODE_CAMERA_REQUEST = 0xa1;
    private static final int CODE_RESULT_REQUEST = 0xa2;
    private static final int CAMERA_PERMISSIONS_REQUEST_CODE = 0x03;
    private static final int STORAGE_PERMISSIONS_REQUEST_CODE = 0x04;
    private File fileUri = new File(Environment.getExternalStorageDirectory().getPath() + "/photo.jpg");
    private File fileCropUri = new File(Environment.getExternalStorageDirectory().getPath() + "/crop_photo.jpg");
    private Uri imageUri;
    private Uri cropImageUri;
    private static final int OUTPUT_X = 480;
    private static final int OUTPUT_Y = 480;

    private UserInformation user;
    private AlertDialog.Builder dialog;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_3, container, false);
        ButterKnife.bind(this, view);
        user = SyberosIm.getInstance()
                .getUserInformation(SyberosIm.getInstance().getCurrentUserId());
        showUser(user);

        return view;
    }

    public void showUser(UserInformation user) {
        Glide.with(this)
                .load(user.iconUrl)
                .override(200, 200)
                .error(android.R.drawable.sym_def_app_icon)
                .into(new SimpleTarget<GlideDrawable>() {
                    @Override
                    public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                        imageView.setImageDrawable(resource);
                    }
                });
        name.setText(user.name);
        department.setText(user.department);
        phone.setText(user.phoneNum);
        email.setText(user.email);
        id.setText(user.userId);
    }


    /**
     * 动态申请sdcard读写权限
     */
    private void autoObtainStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSIONS_REQUEST_CODE);
            } else {
                PhotoUtils.openPic(this, CODE_GALLERY_REQUEST);
            }
        } else {
            PhotoUtils.openPic(this, CODE_GALLERY_REQUEST);
        }
    }

    /**
     * 申请访问相机权限
     */
    private void autoObtainCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.CAMERA)) {
                    ToastUtils.showToast(getActivity(), "您已经拒绝过一次");
                }
                requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE}, CAMERA_PERMISSIONS_REQUEST_CODE);
            }
        } else {//有权限直接调用系统相机拍照
            if (hasSdcard()) {
                imageUri = Uri.fromFile(fileUri);
                //通过FileProvider创建一个content类型的Uri
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    imageUri = FileProvider.getUriForFile(getActivity(), "com.zz.fileprovider", fileUri);
                }
                PhotoUtils.takePicture(this, imageUri, CODE_CAMERA_REQUEST);
            } else {
                ToastUtils.showToast(getActivity(), "设备没有SD卡！");
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            //调用系统相机申请拍照权限回调
            case CAMERA_PERMISSIONS_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (hasSdcard()) {
                        imageUri = Uri.fromFile(fileUri);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            //通过FileProvider创建一个content类型的Uri
                            imageUri = FileProvider.getUriForFile(getActivity(), "com.zz.fileprovider", fileUri);
                        }
                        PhotoUtils.takePicture(this, imageUri, CODE_CAMERA_REQUEST);
                    } else {
                        ToastUtils.showToast(getActivity(), "设备没有SD卡！");
                    }
                } else {
                    ToastUtils.showToast(getActivity(), "请允许打开相机！！");
                }
                break;
            }
            //调用系统相册申请Sdcard权限回调
            case STORAGE_PERMISSIONS_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    PhotoUtils.openPic(this, CODE_GALLERY_REQUEST);
                } else {
                    ToastUtils.showToast(getActivity(), "请允许打操作SDCard！！");
                }
                break;
            default:
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            //相机返回
            case CODE_CAMERA_REQUEST:
                cropImageUri = Uri.fromFile(fileCropUri);
                PhotoUtils.cropImageUri(this, imageUri, cropImageUri, 1, 1, OUTPUT_X, OUTPUT_Y, CODE_RESULT_REQUEST);
                break;
            //相册返回
            case CODE_GALLERY_REQUEST:
                if (hasSdcard()) {
                    cropImageUri = Uri.fromFile(fileCropUri);
                    Uri newUri = Uri.parse(PhotoUtils.getPath(getActivity(), data.getData()));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        newUri = FileProvider.getUriForFile(getActivity(), "com.zz.fileprovider", new File(newUri.getPath()));
                    }
                    PhotoUtils.cropImageUri(this, newUri, cropImageUri, 1, 1, OUTPUT_X, OUTPUT_Y, CODE_RESULT_REQUEST);
                } else {
                    ToastUtils.showToast(getActivity(), "设备没有SD卡！");
                }
                break;
            //裁剪返回
            case CODE_RESULT_REQUEST:
                Bitmap bitmap = PhotoUtils.getBitmapFromUri(cropImageUri, getActivity());
                if (bitmap != null) {
                    showImages(bitmap);
                }
                break;
            default:
        }
        String s = String.valueOf(cropImageUri);
        SyberosIm.getInstance().modifyUserPortrait(String.valueOf(cropImageUri), new ModifyUserPortraitCallback() {

            @Override
            public void onSuccess(String path) {
                LogUtils.d("tag", "Success");
            }

            @Override
            public void onFailed(int errorCode, String errorMsg) {
                LogUtils.d("tag", "Fail");
            }
        });
        String s1 = user.iconUrl;
    }

    private void showImages(Bitmap bitmap) {
        imageView.setImageBitmap(bitmap);
    }

    /**
     * 检查设备是否存在SDCard的工具方法
     */
    public static boolean hasSdcard() {
        String state = Environment.getExternalStorageState();
        return state.equals(Environment.MEDIA_MOUNTED);
    }

    @OnClick(R.id.imageView)
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onClick(View v) {
        int location[] = new int[2];
        v.getLocationOnScreen(location);
        FullImageInfo fullImageInfo = new FullImageInfo();
        fullImageInfo.setLocationX(location[0]);
        fullImageInfo.setLocationY(location[1]);
        fullImageInfo.setWidth(v.getWidth());
        fullImageInfo.setHeight(v.getHeight());
        fullImageInfo.setImageUrl(user.iconUrl);
        EventBus.getDefault().postSticky(fullImageInfo);
        startActivity(new Intent(getActivity(), FullImageActivity.class));
    }

    @OnLongClick(R.id.imageView)
    public boolean onLongClick() {
        dialog = new AlertDialog.Builder(getActivity());
        dialog.setPositiveButton("拍照", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    autoObtainCameraPermission();
                }
            })
            .setNeutralButton("从相册选取照片", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    autoObtainStoragePermission();
                }
            });
        dialog.show();
        return true;
    }
}