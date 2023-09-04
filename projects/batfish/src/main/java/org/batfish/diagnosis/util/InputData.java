package org.batfish.diagnosis.util;

import org.apache.commons.io.FileUtils;


import java.io.File;
import java.io.IOException;
import java.util.*;

public class InputData {

    public static String concatFilePath(String rootPath, String sub) {
        if (sub!=null && !sub.equals("")) {
            return rootPath + "/" + sub;
        } else {
            return rootPath;
        }
    }

    public static String findFilePathWithMatchedName(String rootPath, String matchedName) {
        if (rootPath==null) {
            int a =0;
        }
        File rootDir = new File(rootPath);
        if (!rootDir.exists()) {
            System.out.println("ERROR!!!!ERROR!!!!!");
            System.out.println(rootPath);
            System.out.println(matchedName);
            assert false;
        }
        File[] files = rootDir.listFiles();
        for (File file: files) {
            String aa = file.getName();
            if (file.getName().toLowerCase().contains(matchedName.toLowerCase())) {
                return file.getAbsolutePath();
            }
        }
        return "";
    }

    public static String projectRootPath = System.getProperty("user.dir");

    public InputData() {}

    public static String filterInvalidFilePath(String filePath) {
        File file = new File(filePath);
        assert (file.exists());
        return filePath;

    }


    /**
     * 得到修复后配置的根路径，每个路径下会存放【修改设备（增量）的配置文件】和【result.txt文件用于说明所有设备的配置改动】
     *
     * @return {@link String}
     */// 待读取的目录
    public static String getRepairedCfgRootPath(String oldConfigRootPath) {
        return concatFilePath(oldConfigRootPath, "patch");
    }


}
