package cn.faury.android.library.zxing.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 图片工具类，拍照和相册选择调用
 */
public class ImageUtils {

    public static final int GET_IMAGE_BY_CAMERA = 5001;
    public static final int GET_IMAGE_FROM_PHONE = 5002;
    public static final int CROP_IMAGE = 5003;
    public static Uri imageUriFromCamera;
    public static Uri cropImageUri;
    public static String urlString;

    public static final String PUBLIC_CAMERA_PATH = ".cn.faury/camera/.nomedia/"; // 手机相册存储路径片段

    public static void openCameraImage(final Activity activity) {
        ImageUtils.imageUriFromCamera = ImageUtils.createImagePathUri();

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // MediaStore.EXTRA_OUTPUT参数不设置时,系统会自动生成一个uri,但是只会返回�?个缩略图
        // 返回图片在onActivityResult中�?�过以下代码获取
        // Bitmap bitmap = (Bitmap) data.getExtras().get("data");
//		intent.putExtra("crop", "true"); // 解决直接拍照旋转问题
        intent.putExtra(MediaStore.EXTRA_OUTPUT, ImageUtils.imageUriFromCamera);
        intent.putExtra(MediaStore.Images.Media.ORIENTATION, 0);
        ImageUtils.urlString = ImageUtils.imageUriFromCamera.getPath();
        activity.startActivityForResult(intent, ImageUtils.GET_IMAGE_BY_CAMERA);

        Log.e("ImageUtils.urlString", "ImageUtils.urlString:" + ImageUtils.urlString);
    }

    /**
     * 本地图片单选模式
     *
     * @param activity
     */
    public static void openLocalImage(final Activity activity) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_PICK);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                "image/*");
        activity.startActivityForResult(intent, ImageUtils.GET_IMAGE_FROM_PHONE);

    }

//	/**
//	 * 本地图片多�?�模�?
//	 * 
//	 * @param activity
//	 */
//	public static void multiOpenLocalImages(Activity activity) {
//		Intent intent = new Intent(activity, ImageAlbumShowActivity.class);
//		activity.startActivity(intent);
//	}

    public static void cropImage(Activity activity, Uri srcUri, int type) {

        ImageUtils.cropImageUri = ImageUtils.createImagePathUri();
        Intent intent = new Intent("com.android.camera.action.CROP");
        ImageUtils.urlString = srcUri.getPath();

        intent.setDataAndType(srcUri, "image/*");
        intent.putExtra("crop", "true");

        // //////////////////////////////////////////////////////////////
        // 1.宽高和比例都不设置时,裁剪框可以自行调�?(比例和大小都可以随意调整)
        // //////////////////////////////////////////////////////////////
        // 2.只设置裁剪框宽高�?(aspect)�?,裁剪框比例固定不可调�?,只能调整大小
        // //////////////////////////////////////////////////////////////
        // 3.裁剪后生成图片宽�?(output)的设置和裁剪框无�?,只决定最终生成图片大�?
        // //////////////////////////////////////////////////////////////
        // 4.裁剪框宽高比�?(aspect)可以和裁剪后生成图片比例(output)不同,此时,
        // 会以裁剪框的宽为�?,按照裁剪宽高比例生成�?个图�?,该图和框选部分可能不�?,
        // 不同的情况可能是截取框�?�的�?部分,也可能超出框选部�?,向下延伸补足
        // //////////////////////////////////////////////////////////////

        // aspectX aspectY 是裁剪框宽高的比�?
//		intent.putExtra("aspectX", 1);
//		intent.putExtra("aspectY", 1);
        // outputX outputY 是裁剪后生成图片的宽�?
        intent.putExtra("outputX", 368);
        intent.putExtra("outputY", 368);

        // return-data为true�?,会直接返回bitmap数据,但是大图裁剪时会出现问题,推荐下面为false时的方式
        // return-data为false�?,不会返回bitmap,但需要指定一个MediaStore.EXTRA_OUTPUT保存图片uri
        intent.putExtra(MediaStore.EXTRA_OUTPUT, ImageUtils.cropImageUri);
        intent.putExtra("return-data", false);

        ImageUtils.urlString = ImageUtils.cropImageUri.getPath();

        activity.startActivityForResult(intent, CROP_IMAGE);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String getPath(Context context,Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/"
                            + split[1];
                }

            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection,
                        selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * 直接拍照 判断是否旋转角度，并产生新的bitmap
     *
     * @param path
     * @return
     */
    public static Bitmap getResultImage(String path) {
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = 8; // 保持原图 不失�?
        Bitmap cameraBitmap = BitmapFactory.decodeFile(path, bitmapOptions);
        // 获取旋转角度
        int degree = readPictureDegree(path);
        if (degree == 0) {
            return null;
        } else {
            return rotaingImageView(readPictureDegree(path), cameraBitmap);
        }

    }

    // 旋转图片
    public static Bitmap rotaingImageView(int angle, Bitmap bitmap) {
        // 旋转图片 动作
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        // 创建新的图片
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return resizedBitmap;
    }

    // 读取角度
    public static int readPictureDegree(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri,
                                       String selection, String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection,
                    selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri
                .getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri
                .getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri
                .getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri
                .getAuthority());
    }

    /**
     * 创建�?条图片地�?uri,用于保存拍照后的照片
     * @return 图片的uri
     */

    private static Uri createImagePathUri() {
        Uri imageFilePath = null;
        SimpleDateFormat timeFormatter = new SimpleDateFormat(
                "yyyyMMdd_HHmmss", Locale.CHINA);
        long time = System.currentTimeMillis();
        String imageName = timeFormatter.format(new Date(time)) + ".jpg";
        File file = new File(Environment.getExternalStorageDirectory(),
                PUBLIC_CAMERA_PATH);
        if (!file.exists()) {
            file.mkdirs();
        }
        imageFilePath = Uri.fromFile(new File(file.toString() + "/" + imageName));
        return imageFilePath;
    }

    // 压缩图片
    public static Bitmap yasuoBitmap(String path) {
        if ("".equals(path) || path == null) {
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        // 获取这个图片的宽和高
        Bitmap bitmap = BitmapFactory.decodeFile(path, options); // 此时返回bm为空
        options.inJustDecodeBounds = false;
        // 计算缩放�?
        int be = (int) (options.outHeight / (float) 200);
        if (be <= 0)
            be = 1;
        options.inSampleSize = be;
        // 重新读入图片，注意这次要把options.inJustDecodeBounds 设为 false�?
        bitmap = BitmapFactory.decodeFile(path, options);
        // 由于本地可能存在无法载入的异常图片文件，这里产生的Bitmap对象操作会抛出NullPointerException异常
        return bitmap;
    }

    /**
     * 将Bitmap对象保存为本地文件方�? 路径为�?�DCIM/Camera/Format/XXX.jpg�?
     *
     * @param bitmap  �?保存的Bitmap对象
     * @param quality 图片压缩比例，范围为0~100，数值越高，保存文件越大
     * @return File 输出文件对象
     */
    public static File convertBitmapToFile(Bitmap bitmap, int quality) {
        SimpleDateFormat timeFormatter = new SimpleDateFormat(
                "yyyyMMdd_HHmmss", Locale.CHINA);
        long time = System.currentTimeMillis();
        String imageName = timeFormatter.format(new Date(time)) + ".jpg";
        File file = new File(Environment.getExternalStorageDirectory(),
                PUBLIC_CAMERA_PATH + "bitmap/");
        try {
            if (!file.exists()) {
                file.mkdir();
            }
            FileOutputStream outStream = new FileOutputStream(new File(file,
                    imageName));
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outStream);
            outStream.flush();
            outStream.close();
            return new File(file, imageName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (NullPointerException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getImage(String srcPath) {
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        //开始读入图片，此时把options.inJustDecodeBounds 设回true了
        newOpts.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(srcPath,newOpts);//此时返回bm为空

        newOpts.inJustDecodeBounds = false;
        int w = newOpts.outWidth;
        int h = newOpts.outHeight;
        //现在主流手机比较多是800*480分辨率，所以高和宽我们设置为
        float hh = 800f;//这里设置高度为800f
        float ww = 400f;//这里设置宽度为480f
        //缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
        int be = 1;//be=1表示不缩放
        if (w > h && w > ww) {//如果宽度大的话根据宽度固定大小缩放
            be = (int) (newOpts.outWidth / ww);
        } else if (w < h && h > hh) {//如果高度高的话根据宽度固定大小缩放
            be = (int) (newOpts.outHeight / hh);
        }
        if (be <= 0)
            be = 1;
        newOpts.inSampleSize = be;//设置缩放比例
        //重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
        bitmap = BitmapFactory.decodeFile(srcPath, newOpts);
        return compressImage(srcPath,bitmap);//压缩好比例大小后再进行质量压缩
    }

    /**
     * 压缩图片
     */
    public static String compressImage(String imagePath) {
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        return compressImage(imagePath, bitmap);
    }

    /**
     * 压缩图片
     */
    public static String compressImage(Bitmap bitmap) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = new Date();
        return compressImage(sdf.format(date) + ".jpg", bitmap);
    }

    /**
     * @param imagePath
     * @param bitmap
     * @return
     */
    private static String compressImage(String imagePath, Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        int options = 90;
        while (baos.toByteArray().length / 1024 > 300) { // 循环判断如果压缩后图片是否大�?300kb,大于继续压缩
            baos.reset();// 重置baos即清空baos
            bitmap.compress(Bitmap.CompressFormat.JPEG, options, baos);// 这里压缩options%，把压缩后的数据存放到baos�?
            options -= 10;
        }
        bitmap.recycle();
        int index = imagePath.lastIndexOf("/");
        String name = "";
        if (index == -1) {
            name = imagePath;
        } else {
            name = imagePath.substring(index + 1);
        }
        return saveImage(baos, name);
    }

    private static String saveImage(ByteArrayOutputStream baos, String path) {
        try {
            File dir = new File(Environment.getExternalStorageDirectory(),
                    PUBLIC_CAMERA_PATH + "compress/");
            if(!dir.exists()){
                dir.mkdirs();
            }
            File file = new File(dir, path);
            FileOutputStream out = new FileOutputStream(file);
            try {
                out.write(baos.toByteArray());
                return file.getAbsolutePath();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 从本地path中获取bitmap，压缩后保存小图片到本地
     *
     * @param context
     * @param path    图片存放的路径
     *
     * @return 返回压缩后图片的存放路径
     */
    public static String compressAndSavePicture(Context context, String path) {
        String compressdPicPath = "";

        //★★★★★★★★★★★★★★重点★★★★★★★★★★★★★
        /*
         * //★如果不压缩直接从path获取bitmap，这个bitmap会很大，下面在压缩文件到100kb时，会循环很多次，
         * // ★而且会因为迟迟达不到100k，options一直在递减为负数，直接报错
         * //★ 即使原图不是太大，options不会递减为负数，也会循环多次，UI会卡顿，所以不推荐不经过压缩，直接获取到bitmap
         * Bitmap bitmap=BitmapFactory.decodeFile(path);
         * */
//      ★★★★★★★★★★★★★★重点★★★★★★★★★★★★★

//        建议先将图片压缩到控件所显示的尺寸大小后，再压缩图片质量
//        首先得到手机屏幕的高宽，根据此来压缩图片，当然想要获取跟精确的控件显示的高宽（更加节约内存）,可以使用getImageViewSize();
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int width = displayMetrics.widthPixels;  // 屏幕宽度（像素）
        int height = displayMetrics.heightPixels;  // 屏幕高度（像素）
//        获取按照屏幕高宽压缩比压缩后的bitmap
        Bitmap bitmap = decodeSampledBitmapFromPath(path, width, height);

        String oldName = path.substring(path.lastIndexOf("/"), path.lastIndexOf("."));
        String name = oldName + "_compress.jpg";//★很奇怪oldName之前不能拼接字符串，只能拼接在后面，否则图片保存失败
        String saveDir = PUBLIC_CAMERA_PATH + "compress/";
        File dir = new File(Environment.getExternalStorageDirectory(),saveDir);
        if (!dir.exists()) {
            dir.mkdir();
        }
        // 保存入sdCard
        File file = new File(dir, name);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        /* options表示 如果不压缩是100，表示压缩率为0。如果是70，就表示压缩率是70，表示压缩30%; */
        int options = 100;
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

        while (baos.toByteArray().length / 1024 > 200) {
            // 循环判断如果压缩后图片是否大于200kb继续压缩

            baos.reset();
            options -= 10;
            if (options < 11) {//为了防止图片大小一直达不到500kb，options一直在递减，当options<0时，下面的方法会报错
                // 也就是说即使达不到500kb，也就压缩到10了
                bitmap.compress(Bitmap.CompressFormat.JPEG, options, baos);
                break;
            }
            // 这里压缩options%，把压缩后的数据存放到baos中
            bitmap.compress(Bitmap.CompressFormat.JPEG, options, baos);
        }

        try {
            FileOutputStream out = new FileOutputStream(file);
            out.write(baos.toByteArray());
            out.flush();
            out.close();
            compressdPicPath = file.getAbsolutePath();

        } catch (IOException e) {
            e.printStackTrace();
        }

        File oldFile = new File(path);
        if (oldFile.exists()) {
            oldFile.delete();
        }
        return compressdPicPath;
    }

    /**
     * 根据图片要显示的宽和高，对图片进行压缩，避免OOM
     *
     * @param path
     * @param width  要显示的imageview的宽度
     * @param height 要显示的imageview的高度
     * @return
     */
    private static Bitmap decodeSampledBitmapFromPath(String path, int width, int height) {

//      获取图片的宽和高，并不把他加载到内存当中
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        options.inSampleSize = caculateInSampleSize(options, width, height);
//      使用获取到的inSampleSize再次解析图片(此时options里已经含有压缩比 options.inSampleSize，再次解析会得到压缩后的图片，不会oom了 )
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        return bitmap;

    }

    /**
     * 根据需求的宽和高以及图片实际的宽和高计算SampleSize
     *
     * @param options
     * @param reqWidth  要显示的imageview的宽度
     * @param reqHeight 要显示的imageview的高度
     * @return
     * @compressExpand 这个值是为了像预览图片这样的需求，他要比所要显示的imageview高宽要大一点，放大才能清晰
     */
    private static int caculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int width = options.outWidth;
        int height = options.outHeight;

        int inSampleSize = 1;

        if (width >= reqWidth || height >= reqHeight) {

            int widthRadio = Math.round(width * 1.0f / reqWidth);
            int heightRadio = Math.round(width * 1.0f / reqHeight);

            inSampleSize = Math.max(widthRadio, heightRadio);

        }

        return inSampleSize;
    }

    /**
     * 通过Base32将Bitmap转换成Base64字符串
     * @param bit
     * @return
     */
    public static String convertBitmapToBase64(Bitmap bit){
        ByteArrayOutputStream bos=new ByteArrayOutputStream();
        bit.compress(Bitmap.CompressFormat.JPEG, 100, bos);//参数100表示不压缩
        byte[] bytes=bos.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }
}
