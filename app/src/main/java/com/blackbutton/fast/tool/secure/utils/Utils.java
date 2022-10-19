package com.blackbutton.fast.tool.secure.utils;

import static androidx.work.impl.utils.ProcessUtils.getProcessName;

import static com.xuexiang.xutil.app.ActivityUtils.startActivity;
import static com.xuexiang.xutil.app.AppUtils.getPackageManager;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout.LayoutParams;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

import com.blackbutton.fast.tool.secure.bean.ProfileBean;
import com.github.shadowsocks.R;
import com.xuexiang.xutil.tip.ToastUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public final class Utils {

    private Utils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    /**
     * 得到设备屏幕的宽度
     */
    public static int getScreenWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    /**
     * 得到设备屏幕的高度
     */
    public static int getScreenHeight(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    private static final String STATUS_BAR_HEIGHT_RES_NAME = "status_bar_height";

    /**
     * 计算状态栏高度高度 getStatusBarHeight
     *
     * @return
     */
    public static int getStatusBarHeight() {
        return getInternalDimensionSize(Resources.getSystem(),
                STATUS_BAR_HEIGHT_RES_NAME);
    }

    private static int getInternalDimensionSize(Resources res, String key) {
        int result = 0;
        int resourceId = res.getIdentifier(key, "dimen", "android");
        if (resourceId > 0) {
            result = res.getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /**
     * get ListView height according to every children
     *
     * @param view
     * @return
     */
    public static int getListViewHeightBasedOnChildren(ListView view) {
        int height = getAbsListViewHeightBasedOnChildren(view);
        ListAdapter adapter;
        int adapterCount;
        if (view != null && (adapter = view.getAdapter()) != null
                && (adapterCount = adapter.getCount()) > 0) {
            height += view.getDividerHeight() * (adapterCount - 1);
        }
        return height;
    }

    /**
     * get AbsListView height according to every children
     *
     * @param view
     * @return
     */
    public static int getAbsListViewHeightBasedOnChildren(AbsListView view) {
        ListAdapter adapter;
        if (view == null || (adapter = view.getAdapter()) == null) {
            return 0;
        }

        int height = 0;
        for (int i = 0; i < adapter.getCount(); i++) {
            View item = adapter.getView(i, null, view);
            if (item instanceof ViewGroup) {
                item.setLayoutParams(new LayoutParams(
                        LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            }
            item.measure(0, 0);
            height += item.getMeasuredHeight();
        }
        height += view.getPaddingTop() + view.getPaddingBottom();
        return height;
    }

    /**
     * View设备背景
     *
     * @param context
     * @param v
     * @param res
     */
    @SuppressWarnings("deprecation")
    public static void setBackground(Context context, View v, int res) {
        Bitmap bm = BitmapFactory.decodeResource(context.getResources(), res);
        BitmapDrawable bd = new BitmapDrawable(context.getResources(), bm);
        v.setBackgroundDrawable(bd);
    }

    /**
     * 释放图片资源
     *
     * @param v
     */
    public static void recycleBackground(View v) {
        Drawable d = v.getBackground();
        //别忘了把背景设为null，避免onDraw刷新背景时候出现used a recycled bitmap错误
        v.setBackgroundResource(0);
        if (d != null && d instanceof BitmapDrawable) {
            Bitmap bmp = ((BitmapDrawable) d).getBitmap();
            if (bmp != null && !bmp.isRecycled()) {
                bmp.recycle();
            }
        }
        if (d != null) {
            d.setCallback(null);
        }
    }

    /**
     * 遍历View,清除所有ImageView的缓存
     *
     * @param view
     */
    public static void clearImageView(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) view;
            int count = parent.getChildCount();
            for (int i = 0; i < count; i++) {
                clearImageView(parent.getChildAt(i));
            }
        } else if (view instanceof ImageView) {
            clearImgMemory((ImageView) view);
        }
    }

    /**
     * 清空图片的内存
     */
    public static void clearImgMemory(ImageView imageView) {
        Drawable d = imageView.getDrawable();
        if (d != null && d instanceof BitmapDrawable) {
            Bitmap bmp = ((BitmapDrawable) d).getBitmap();
            if (bmp != null && !bmp.isRecycled()) {
                bmp.recycle();
            }
        }
        imageView.setImageBitmap(null);
        if (d != null) {
            d.setCallback(null);
        }
    }

    /**
     * 放大缩小图片
     *
     * @param bitmap 源Bitmap
     * @param w      宽
     * @param h      高
     * @return 目标Bitmap
     */
    public static Bitmap zoom(Bitmap bitmap, int w, int h) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Matrix matrix = new Matrix();
        float scaleWidth = ((float) w / width);
        float scaleHeight = ((float) h / height);
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }

    /**
     * 安静关闭 IO
     *
     * @param closeables closeables
     */
    public static void closeIOQuietly(final Closeable... closeables) {
        if (closeables == null) return;
        for (Closeable closeable : closeables) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Indicates if this file represents a file on the underlying file system.
     *
     * @param filePath 文件路径
     * @return 是否存在文件
     */
    public static boolean isFileExist(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return false;
        }

        File file = new File(filePath);
        return (file.exists() && file.isFile());
    }

    /**
     * 获取bitmap
     *
     * @param filePath 文件路径
     * @return bitmap
     */
    public static Bitmap getBitmap(String filePath) {
        if (!isFileExist(filePath)) {
            return null;
        }
        return BitmapFactory.decodeFile(filePath);
    }

    /**
     * 检查是否为空指针
     *
     * @param object
     * @param hint
     */
    public static void checkNull(Object object, String hint) {
        if (null == object) {
            throw new NullPointerException(hint);
        }
    }

    /**
     * 检查是否为空指针
     *
     * @param t
     * @param message
     */
    public static <T> T checkNotNull(T t, String message) {
        if (t == null) {
            throw new NullPointerException(message);
        }
        return t;
    }

    /**
     * 旋转图片
     *
     * @param angle  旋转角度
     * @param bitmap 要旋转的图片
     * @return 旋转后的图片
     */
    public static Bitmap rotate(Bitmap bitmap, int angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, true);
    }

    /**
     * 将Drawable转化为Bitmap
     *
     * @param drawable Drawable
     * @return Bitmap
     */
    public static Bitmap getBitmapFromDrawable(Drawable drawable) {
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, drawable
                .getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * 将Drawable转化为Bitmap
     *
     * @param drawable Drawable
     * @return Bitmap
     */
    public static Bitmap getBitmapFromDrawable(Drawable drawable, int color) {
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, drawable
                .getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(color, PorterDuff.Mode.SRC_IN);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);

        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        canvas = new Canvas(bitmap);
        canvas.drawColor(color, PorterDuff.Mode.SRC_IN);
        return bitmap;
    }

    /**
     * 获取应用的图标
     *
     * @param context
     * @return
     */
    public static Drawable getAppIcon(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(context.getPackageName(), 0);
            return info.loadIcon(pm);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 支持?attrs属性  http://stackoverflow.com/questions/27986204  ：As mentioned here on API < 21 you can't use attrs to color in xml drawable.
     *
     * @return
     */
    public static boolean isSupportColorAttrs() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static boolean isLight(int color) {
        return Math.sqrt(
                Color.red(color) * Color.red(color) * .241 +
                        Color.green(color) * Color.green(color) * .691 +
                        Color.blue(color) * Color.blue(color) * .068) > 130;
    }

    public static boolean isNullOrEmpty(@Nullable CharSequence string) {
        return string == null || string.length() == 0;
    }

    /**
     * 获取数值的位数，例如9返回1，99返回2，999返回3
     *
     * @param number 要计算位数的数值，必须>0
     * @return 数值的位数，若传的参数小于等于0，则返回0
     */
    public static int getNumberDigits(int number) {
        if (number <= 0) return 0;
        return (int) (Math.log10(number) + 1);
    }

    /**
     * 设置Drawable的颜色
     * <b>这里不对Drawable进行mutate()，会影响到所有用到这个Drawable的地方，如果要避免，请先自行mutate()</b>
     */
    public static ColorFilter setDrawableTintColor(Drawable drawable, @ColorInt int tintColor) {
        LightingColorFilter colorFilter = new LightingColorFilter(Color.argb(255, 0, 0, 0), tintColor);
        if (drawable != null) {
            drawable.setColorFilter(colorFilter);
        }
        return colorFilter;
    }

    /**
     * 进程判断
     * @param context
     * @return
     */
    public static boolean isMainProcess(Context context) {
        try {
            if (null != context) {
                return context.getPackageName().equals(getProcessName(context));
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }
    /**
     * 国旗转化
     */
    public static int FlagConversion(String ufo_country) {
        if(ufo_country.contains("Faster server")){
            return R.mipmap.ic_country;
        }else if (ufo_country.contains("Japan")) {
            return R.mipmap.japan;
        }else if(ufo_country.contains("United Kingdom")){
            return R.mipmap.unitedkingdom;
        }else if (ufo_country.contains("Australia")) {
            return R.mipmap.australia;
        }else if (ufo_country.contains("Belgium")) {
            return R.mipmap.belgium;
        }else if (ufo_country.contains("Brazil")) {
            return R.mipmap.brazil;
        }else if (ufo_country.contains("Canada")) {
            return R.mipmap.canada;
        }else if (ufo_country.contains("France")) {
            return R.mipmap.france;
        }else if (ufo_country.contains("Germany")) {
            return R.mipmap.germany;
        }else if (ufo_country.contains("Hongkong")) {
            return R.mipmap.hongkong;
        }else if (ufo_country.contains("India")) {
            return R.mipmap.india;
        }else if (ufo_country.contains("Ireland")) {
            return R.mipmap.ireland;
        }
        else if (ufo_country.contains("italy")) {
            return R.mipmap.italy;
        }else if (ufo_country.contains("Koreasouth")) {
            return R.mipmap.koreasouth;
        }else if (ufo_country.contains("Netherlands")) {
            return R.mipmap.netherlands;
        }else if (ufo_country.contains("Newzealand")) {
            return R.mipmap.newzealand;
        }else if (ufo_country.contains("Norway")) {
            return R.mipmap.norway;
        }else if (ufo_country.contains("Russianfederation")) {
            return R.mipmap.russianfederation;
        }else if (ufo_country.contains("Singapore")) {
            return R.mipmap.singapore;
        }else if (ufo_country.contains("Sweden")) {
            return R.mipmap.sweden;
        }else if (ufo_country.contains("Switzerland")) {
            return R.mipmap.switzerland;
        }else if (ufo_country.contains("tw")) {
            return R.mipmap.tw;
        }else {
            return R.mipmap.ic_country;
        }
    }

    /**
     * 添加最佳线路
     */
    public static ProfileBean.SafeLocation addTheBestRoute(ProfileBean.SafeLocation data) {
        ProfileBean.SafeLocation bestRoute;
        bestRoute = new ProfileBean.SafeLocation();
        bestRoute.setUfo_country(data.getUfo_country());
        bestRoute.setUfo_city(data.getUfo_city());
        bestRoute.setUfo_ip(data.getUfo_ip());
        bestRoute.setUfo_method(data.getUfo_method());
        bestRoute.setUfo_port(data.getUfo_port());
        bestRoute.setUfo_pwd(data.getUfo_pwd());
        bestRoute.setCheek_state(true);
        bestRoute.setBestServer(true);
        return bestRoute;
    }

}
