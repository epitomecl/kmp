package com.epitomecl.kmp.common;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.management.ManagementFactory;
import java.util.List;

import static com.epitomecl.kmp.common.AppConfInfo.CONFIG_SERVER_HOME;


public class HomeConfigurator {
    private static final Class<?> clazz = MethodHandles.lookup().lookupClass();

    private static HomeConfigurator instance;

    public static synchronized HomeConfigurator getInstance() {
        if (instance == null) {
            instance = new HomeConfigurator();
        }
        return instance;
    }

    private static String FS(String path) {
        return FilenameUtils.separatorsToSystem(path);
    }

    private static String home_dir;

    // changeLogbackConfiguration에서 사용하므로, logger사용 불가
    protected static synchronized String getHomeDir() {
        if (home_dir == null) {
            home_dir = System.getProperty(CONFIG_SERVER_HOME);
            if (home_dir == null) {
                home_dir = System.getenv(CONFIG_SERVER_HOME);
            }
            if (home_dir != null) {
                System.out.println(CONFIG_SERVER_HOME + "=" + home_dir);
            }
        }
        // 만약, CONFIG_SERVER_HOME 환경변수가 지정되어 있지 않다면,
        // 다음과 같이 home 디렉토리를 추정
        if (home_dir == null) {
            try {
                // 현재 경로에서 파일을 찾는다.
                String current_dir = clazz.getProtectionDomain().getCodeSource().getLocation().getPath();
                System.out.println("current_dir0=" + current_dir);
                File current_file = new File(current_dir);
                System.out.println("current_file=" + current_file.toString());
                if (current_file.exists()) {
                    if (current_file.isFile()) {
                        System.out.println("current_file is file");
                        current_dir = current_file.getParent();
                    } else if (current_file.isDirectory()) {
                        System.out.println("current_file is directory");
                        current_dir = current_file.getCanonicalPath();
                    }
                } else {
                    System.out.println("current_file is not exist");
                }
                System.out.println("current_dir=" + current_dir);

                String relative_path = null;

                // 1. 이 클래스가 lib에 존재
                if (current_dir.endsWith(FS("/lib"))) {
                    // 이 클래스가 설치 환경의 WEB-INF/lib에 존재 (
                    // ${CONFIG_SERVER_HOME}/webapps/"+projname+"/WEB-INF/lib/iaam-02-model-1.0.jar
                    if (current_dir.endsWith(FS("/WEB-INF/lib"))) {
                        relative_path = current_dir + "/../../../..";
                    }
                    // ERROR
                    else {
                        relative_path = null;
                    }
                }
                // 2. 이 클래스가 spring-boot 환경에 존재
                // file:/home/epitome/kmp/webapps/kmp-server-2.0.0.war!/WEB-INF/lib/kmp-common-2.0.0.jar!/
                else if (current_dir.contains(FS("kmp-server-2.0.0.war!"))) {
                    relative_path = current_file.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getPath().replaceFirst("^file:", "");
                }
                // 2. 이 클래스가 개발 환경의 target/classes에 존재
                // ${CONFIG_SERVER_HOME}/../"+projname/target/classes classpath
                else if (current_dir.endsWith(FS("/target/classes"))) {
                    relative_path = current_dir + "/../../../home";
                }
                // 5. 이 클래스가 개발 환경의 target에 존재
                // ${CONFIG_EPITOMECL_KMP_HOME}/../"+projname/target/dcl-05-service-1.0-jar-with-dependencies.jar
                else if (current_dir.endsWith(FS("/target"))) {
                    relative_path = current_dir + "/../../home";
                }
                // 6. 이 클래스가 M2 환경에 존재
                // mvn verify 수행시 ~/.m2/repository 아래
                else if (current_dir.contains(FS("/.m2/repository/"))) {
                    String currentDirectory = System.getProperty("user.dir");
                    relative_path = currentDirectory + "/../home";
                }
                System.out.println("relative_path=" + relative_path);

                File file = new File(relative_path);
                home_dir = file.getCanonicalPath();
            } catch (IOException e) {
                System.err.println(ExceptionUtils.getStackTrace(e));
            }
            System.out.println(CONFIG_SERVER_HOME + "=" + home_dir);

            String separator = System.getProperty("file.separator");
            String cmd = System.getProperty("java.home") + separator + "bin" + separator + "java";
            List<String> vmargs = ManagementFactory.getRuntimeMXBean().getInputArguments();
            String classpath = System.getProperty("java.class.path");
            System.out.println("cmd=" + cmd);
            System.out.println("vmargs=" + vmargs);
//			System.out.println("classpath=" + classpath);
        }

        return home_dir;
    }


    ///////////////////////////////////////////////
    // read only directories
    ///////////////////////////////////////////////
    public static String getConfDir() {
        return getHomeDir() + "/conf";
    }

    public static String getLibDir() {
        return getHomeDir() + "/lib";
    }

    public static String getTestResourcesDir() {
        return getHomeDir() + "/test/resources";
    }

    public static String getTestBinDir() {
        return getHomeDir() + "/test/bin";
    }

    public static String getProjectDir() {
        return getHomeDir() + "/..";
    }

    public static String getAppWorkerDir() {
        return getHomeDir() + "/../app-worker";
    }

    public static String getAppMpaaDir() {
        return getHomeDir() + "/../app-mpaa";
    }

    ///////////////////////////////////////////////
    // writable directories
    ///////////////////////////////////////////////
    public static String getLogsDir() {
        return getHomeDir() + "/data/logs";
    }

    public static String getDbDir() {
        return getHomeDir() + "/data/db";
    }

    public static String getActivemqDataDir() {
        return getHomeDir() + "/data/activemq-data";
    }

    public static String getTmpDir() { return getHomeDir() + "/data/tmp"; }

    public static String getWalletDir() { return getHomeDir() + "/data/wallet"; }

    // for logback
    public static void changeLogConfiguration() {
        // log 디렉토리가 없으면 생성
        File logDir = new File(getLogsDir());
        if (!logDir.exists()) {
            logDir.mkdir();
        }
//        if (logPropertyFile == null) {
        String config_file = getConfDir() + "/logback.xml";
        try {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            context.reset();
            System.setProperty(CONFIG_SERVER_HOME, getHomeDir());
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            configurator.doConfigure(config_file);
            System.out.println("Wow! I'm configured! - " + config_file);
        } catch (Exception e) {
            // DAMN! I'm not....
            e.printStackTrace();
        }
//            logPropertyFile = config_file;
//        }
    }

}
