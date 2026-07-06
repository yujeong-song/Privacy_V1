package com.example.privacy_v1.model;

import java.util.List;

//public class BrandData {
//    public String brand;
//    public String lastUpdated;
//    public List<CarGroup> cars;
//
//    public static class CarGroup {
//        public String name;            // 2번째 화면 항목(예: "싼타페", "그랜저IG")
//        public List<ModelInfo> models; // 3번째 화면 항목
//    }
//
//    public static class ModelInfo {
//        public String modelname;       // 3번째 화면 항목(예: "더 뉴 그랜져IG")
//        public List<Integer> years;    // 4번째 화면 항목
//        public int maxConnect;
//        public String timeOut;
//    }
//}
public class BrandData {
    // 파일마다 brand 또는 oem이 있을 수 있어 둘 다 받게 해둠
    public String brand;
    public String oem;
    public String lastUpdated;

    public List<Car> cars;

    public String getDisplayBrand() {
        if (oem != null && !oem.isEmpty()) return oem;
        if (brand != null && !brand.isEmpty()) return brand;
        return "";
    }
}