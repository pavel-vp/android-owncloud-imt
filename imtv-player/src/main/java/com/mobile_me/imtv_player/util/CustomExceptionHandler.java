package com.mobile_me.imtv_player.util;

        import java.io.BufferedWriter;
        import java.io.File;
        import java.io.FileWriter;
        import java.io.IOException;
        import java.io.PrintWriter;
        import java.io.StringWriter;
        import java.io.Writer;
        import java.lang.Thread.UncaughtExceptionHandler;
        import java.util.Calendar;

        import android.content.Context;
        import android.content.Intent;
        import android.os.Environment;
        import android.os.StatFs;
        import android.text.TextUtils;
        import android.util.Log;
        import android.os.Process;

        import com.mobile_me.imtv_player.dao.Dao;
        import com.mobile_me.imtv_player.service.LogUpload;
        import com.mobile_me.imtv_player.ui.IMTVApplication;
        import com.mobile_me.imtv_player.ui.LogoActivity;


public class CustomExceptionHandler implements UncaughtExceptionHandler
{
    private static CustomExceptionHandler Logger;
    private boolean isDebugVerboseLog = true;
    private int MB = 1024*1024;

    private UncaughtExceptionHandler defaultUEH;
    private String fileName;
    private BufferedWriter bos;

    public static class TLogLevel {
        public static final boolean Critical = false;
        public static final boolean DebugVerbose = true;

        public static final String LOGPREF_INFO = "INFO";
        public static final String LOGPREF_GPS = "GPS";
        public static final String LOGPREF_TAXSTATE = "TAX";
        public static final String LOGPREF_TIME = "TIME";
        public static final String LOGPREF_COST = "COST";
        public static final String LOGPREF_ONSTAND = "ONSTAND";
        public static final String LOGPREF_LOADTRIP = "LOADTRIP";


    }

    /*
     * if any of the parameters is null, the respective functionality will not be used
     */
    public CustomExceptionHandler()
    {
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
        this.fileName = Environment.getExternalStorageDirectory() + "/imtv";

        File f = new File(fileName);
        f.mkdirs();
        this.fileName = f.getAbsolutePath() + "/error.log";
        this.MB = 1024*1024;

        try
        {
            bos = new BufferedWriter(new FileWriter(fileName, true));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        Log(TLogLevel.DebugVerbose, "Свободной памяти: " + getAvailableSizeSD() + "byte");
    }

    public void Log(boolean level, String logStr) {
        if (isDebugVerboseLog == TLogLevel.Critical && level == TLogLevel.DebugVerbose)
            return;
        writeToFile(getLocation() + logStr);
        Log.d("IMTV", logStr);
    }

    public void LogExeption(boolean level, String logStr, Throwable e) {
        if (isDebugVerboseLog == TLogLevel.Critical && level == TLogLevel.DebugVerbose)
            return;

        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        e.printStackTrace(printWriter);
        String stacktrace = result.toString();
        printWriter.close();

        writeToFile(getLocation() + logStr + "\n\r" + stacktrace);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        writeToFile("uncaughtException : " + ex.getMessage());
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        ex.printStackTrace(printWriter);
        String stacktrace = result.toString();
        printWriter.close();

        writeToFile(ex.getMessage() + " : " + stacktrace);
        writeToFile("try to restart app");

        // отослать файл лога
        LogUpload.getInstance(Dao.getInstance(IMTVApplication.getInstance())).startUpload();

        Dao.getInstance(IMTVApplication.getInstance()).setTerminated(true);
        //IMTVApplication.setAlarmToStart(null);
        //Process.killProcess(Process.myPid());
        System.exit(0);
        //defaultUEH.uncaughtException(thread, ex);
    }

    private void writeToFile(String stacktrace)
    {
        isClearBigFile();

        try
        {
            if (bos == null)
                bos = new BufferedWriter(new FileWriter(fileName, true));

            if (bos != null) {
                synchronized (this) {
                    bos.write("\n" + Calendar.getInstance().getTime().toLocaleString() + " ");
                    bos.write(stacktrace);
                    bos.flush();
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private long getAvailableSizeSD() {
        long availablesize = 0;
        try {
            StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
            availablesize = (long)stat.getAvailableBlocks() * (long)stat.getBlockSize();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return availablesize;
    }

    // Удаляем файл, если он больших размеров
    private void isClearBigFile() {
        try {
            long limitSize = 5 * MB;
            if (isDebugVerboseLog == TLogLevel.DebugVerbose)
                limitSize = 20 * MB;

            File f = new File(fileName);
            if (f.exists() && (f.length() >= limitSize || getAvailableSizeSD() <= MB)) {
                if (bos != null) bos.close();
                f.delete();
            }
        }
        catch (Exception e)
        {
            //LogExeption(TLogLevel.Critical, "Ошибка при проверке размера файла и его удаления...", e);
        }
    }

    public synchronized void close() {
        if (Logger != null && Logger.bos != null) {
            try {
                Logger.bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            File logFile = new File(Logger.fileName);
            logFile.delete();
        }
        Logger = null;
    }

    public static CustomExceptionHandler getLog() {
        if (Logger == null)
            Logger = new CustomExceptionHandler();

        return Logger;
    }

    private static String getLocation() {
        final String className = CustomExceptionHandler.class.getName();
        final StackTraceElement[] traces = Thread.currentThread().getStackTrace();
        boolean found = false;

        for (int i = 0; i < traces.length; i++) {
            StackTraceElement trace = traces[i];

            try {
                if (found) {
                    if (!trace.getClassName().startsWith(className)) {
                        Class<?> clazz = Class.forName(trace.getClassName());
                        return "[" + getClassName(clazz) + ":" + trace.getMethodName() + ":" + trace.getLineNumber() + "]: ";
                    }
                }
                else if (trace.getClassName().startsWith(className)) {
                    found = true;
                    continue;
                }
            }
            catch (ClassNotFoundException e) {
            }
        }

        return "[]: ";
    }

    private static String getClassName(Class<?> clazz) {
        if (clazz != null) {
            if (!TextUtils.isEmpty(clazz.getSimpleName())) {
                return clazz.getSimpleName();
            }

            return getClassName(clazz.getEnclosingClass());
        }

        return "";
    }

    public void setDebugVerboseLog(boolean isDebugVerboseLog) {
        this.isDebugVerboseLog = isDebugVerboseLog;
    }

    public boolean isDebugVerboseLog() {
        return isDebugVerboseLog;
    }

    public String getFileName() {
        return fileName;
    }


    public static void log(String msg) {
        //Log.d("IMTV",msg);
        CustomExceptionHandler.getLog().Log(CustomExceptionHandler.TLogLevel.Critical, msg);
    }

    public static void logException(String msg, Exception e) {
        CustomExceptionHandler.getLog().LogExeption(CustomExceptionHandler.TLogLevel.Critical, msg, e);
    }

    public String switchToNewFile() {
        String fileTmp = this.fileName+".old";
        synchronized (this) {
            if (Logger != null && Logger.bos != null) {
                try {
                    Logger.bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                File logFile = new File(Logger.fileName);
                logFile.renameTo(new File(fileTmp));
            }
            Logger = null;
        }
        return fileTmp;
    }


}