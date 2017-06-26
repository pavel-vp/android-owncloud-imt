package com.mobile_me.imtv_player.service;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;

import com.mobile_me.imtv_player.BuildConfig;
import com.mobile_me.imtv_player.R;
import com.mobile_me.imtv_player.dao.Dao;
import com.mobile_me.imtv_player.model.MTPlayList;
import com.mobile_me.imtv_player.model.MTPlayListRec;
import com.mobile_me.imtv_player.util.CustomExceptionHandler;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.stericson.rootshell.exceptions.RootDeniedException;
import com.stericson.rootshell.execution.Command;
import com.stericson.roottools.RootTools;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by pasha on 7/26/16.
 */
public class Updater implements IMTCallbackEvent {

    private Context ctx;
    private MTOwnCloudHelper helper;
    public Updater(Context ctx) {
        this.ctx = ctx;
        helper = new MTOwnCloudHelper(Dao.getInstance(ctx).getRemoteUpdateFilePath(), ctx, this);
        CustomExceptionHandler.log("Updater created");
    }

    public void startGetInfoUpdate() {
        CustomExceptionHandler.log("Updater start get info");
        helper.loadFileInfoFromServer();
    }

    private void startDownLoadUpdate() {
        CustomExceptionHandler.log("Updater start download");
        helper.loadUpdateFromServer();
    }

    public boolean downloadNewVersion(final String remotePath, final String fileName, final String localDir) {
        boolean res = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                Thread.setDefaultUncaughtExceptionHandler(CustomExceptionHandler.getLog());

                try {

                    URL url = new URL(remotePath + "/" + fileName);
                    final URLConnection conn = url.openConnection();
                    conn.connect();


                    File outputDirs = new File(localDir);
                    outputDirs.mkdirs();
                    String localFileName = "tmp.apk";
                    File outputFile = new File(localDir, localFileName);
                    FileOutputStream fos = new FileOutputStream(outputFile);
                    //начинаем закачку в буфер

                    InputStream is = conn.getInputStream();

                    byte[] buffer = new byte[1024];
                    int len1 = 0;
                    while ((len1 = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, len1);
                    }
                    fos.close();
                    is.close();
                    Runtime.getRuntime().exec("chmod 777 " + outputFile);

                    // запустить установку
                    if (outputFile.exists()) {
                        installAPKonRooted(outputFile.getAbsolutePath(), Updater.this.ctx);
/*                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.fromFile(outputFile), "application/vnd.android.package-archive");
                        ctx.startActivity(intent);
*/
                    }

                } catch (Exception e) {
                    //res = false;
                    CustomExceptionHandler.logException("download new version error ", e);
                    e.printStackTrace();
                }
            }
        }).start();
        return res;
    }

    public static void checkVersionAndInstall(File newFile, Context ctx) throws Exception {
        final PackageManager pm = ctx.getPackageManager();
        PackageInfo newInfo = pm.getPackageArchiveInfo(newFile.getAbsolutePath(), PackageManager.GET_META_DATA);
        CustomExceptionHandler.log("Updater newFile version= " + newInfo.versionCode + ", current version=" + BuildConfig.VERSION_CODE);
        if (newInfo.versionCode != BuildConfig.VERSION_CODE) {
            CustomExceptionHandler.log("Updater start to update app");
//                installApk2(newFile.getAbsolutePath());
            Updater.installAPKonRooted(newFile.getAbsolutePath(), ctx);
        }
    }

    public static void installAPKonRooted(String filename, Context ctx) throws Exception {
        File file = new File(filename);
        if (file.exists()) {
                final String libs = "LD_LIBRARY_PATH=/vendor/lib:/system/lib";
                String command;
//                command = "adb install -r " + filename + " \n";

                command = "pm install -r " + filename;
            Process proc = Runtime.getRuntime().exec(new String[] { "su", "-c", command });
//            Process proc = Runtime.getRuntime().exec(new String[] { "su", command });
            //Process proc = Runtime.getRuntime().exec( command );
            int res = 0;
                res=proc.waitFor();
//                res = instalarApk(command);
                CustomExceptionHandler.log("res procWait2="+res);
            if (res == 0) {
                //IMTVApplication.setAlarmToStart(ctx);
                System.exit(0);
            }
        }
    }

    private int instalarApk( String commands ) {

        try {

            Process p = Runtime.getRuntime().exec("sudo");
            InputStream es = p.getErrorStream();
            DataOutputStream os = new DataOutputStream(p.getOutputStream());

            os.writeBytes(commands + "\n");

            os.writeBytes("exit\n");
            os.flush();

            int read;
            byte[] buffer = new byte[4096];
            String output = new String();
            while ((read = es.read(buffer)) > 0) {
                output += new String(buffer, 0, read);
            }
            //CustomExceptionHandler.log("output="+output);
            return p.waitFor();

        } catch (IOException e) {
            e.printStackTrace();
            CustomExceptionHandler.logException("installarApk error", e);
        } catch (InterruptedException e) {
            e.printStackTrace();
            CustomExceptionHandler.logException("InstallarApk error2", e);
        }
        return 999;
    }

    private void installApk2(String file) throws TimeoutException, RootDeniedException, IOException {
        Thread.setDefaultUncaughtExceptionHandler(CustomExceptionHandler.getLog());
        Command command = new Command(0, "pm install -r " + file) {
            @Override
            public void commandCompleted(int arg0, int arg1) {
                CustomExceptionHandler.log("App installation is completed");
                //IMTVApplication.setAlarmToStart(ctx);
                System.exit(0);
            }

            @Override
            public void commandOutput(int arg0, String line) {
                CustomExceptionHandler.log("commandOutput = "+line);
            }

            @Override
            public void commandTerminated(int arg0, String arg1) {
                CustomExceptionHandler.log("commandTerminated = "+arg1);
            }
        };
        RootTools.debugMode = true;
        RootTools.getShell(true).add(command);
    }

    @Override
    public void onPlayListLoaded(MTPlayList playList, MTOwnCloudHelper ownCloudHelper) {

    }

    @Override
    public void onVideoFileLoaded(MTPlayListRec file, MTOwnCloudHelper ownCloudHelper) {

    }

    @Override
    public void onUpdateFileLoaded(MTOwnCloudHelper ownCloudHelper) {
        // вызвать установку
        try {
            CustomExceptionHandler.log("Updater success downloading");
            String path = Environment.getExternalStorageDirectory() + "/imtv";
            File newFile = new File(path, Dao.getInstance(ctx).getRemoteUpdateFilePath());
            CustomExceptionHandler.log("Updater newFile= " + newFile + " check is need to install");
            // нужно проверить надо ли установить, т.к.  иначе уйдем в бесконечный цикл
            Updater.checkVersionAndInstall(newFile, ctx);
            CustomExceptionHandler.log("Updater finished");
        } catch (Exception e) {
            CustomExceptionHandler.logException("Error updating", e);
            e.printStackTrace();
        }
        reLaunchUpdater();

    }

    @Override
    public void onError(int mode, MTOwnCloudHelper ownCloudHelper, RemoteOperationResult result) {
        CustomExceptionHandler.log("Updater onError download occured");
        reLaunchUpdater();
    }

    @Override
    public void onUploadLog(String file) {

    }

    @Override
    public void onSimpleFileLoaded(MTOwnCloudHelper ownCloudHelper, File file) {

    }

    @Override
    public void onFileInfoLoaded(RemoteFile fileInfo) {
        // Проверить, есть ли уже такой скачанный файл (по длине)
        String path = Environment.getExternalStorageDirectory() + "/imtv";
        File newFile = new File(path, Dao.getInstance(ctx).getRemoteUpdateFilePath());
        CustomExceptionHandler.log("onFileInfoLoaded check file: exists:"+newFile.exists()+", fileLength="+newFile.length()+", newFileLength="+fileInfo.getLength());
        if (newFile.exists() && newFile.length() == fileInfo.getLength()) { // TODO: потом сделать по MD5
            // ничего не делаем, т.к. файлы иентичные
            CustomExceptionHandler.log("onFileInfoLoaded files are identical, try to check version number with current");
            try {
                Updater.checkVersionAndInstall(newFile, ctx);
            } catch (Exception e) {
                CustomExceptionHandler.logException("Error updating", e);
                e.printStackTrace();
            }
        } else {
            // Если нету - запустить скачку
            this.startDownLoadUpdate();
        }
    }

    private void reLaunchUpdater() {
        if (!Dao.getInstance(ctx).getTerminated()) {
            CustomExceptionHandler.log("reLaunchUpdater next try after " + ctx.getResources().getString(R.string.updateapk_interval_minutes) + " mins");
            Dao.getInstance(ctx).getExecutor().schedule(new Runnable() {
                @Override
                public void run() {
                    Thread.setDefaultUncaughtExceptionHandler(CustomExceptionHandler.getLog());
                    Updater.this.startGetInfoUpdate();
                }
            }, Integer.parseInt(ctx.getResources().getString(R.string.updateapk_interval_minutes)), TimeUnit.MINUTES);
        }
    }
}
