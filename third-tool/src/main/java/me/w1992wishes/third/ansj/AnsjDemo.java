package me.w1992wishes.third.ansj;

import org.ansj.library.AmbiguityLibrary;
import org.ansj.library.DicLibrary;
import org.ansj.splitWord.analysis.*;

public class AnsjDemo {

    public static void main(String[] args) {
//        String str = "洁面仪配合洁面深层清洁毛孔 清洁鼻孔面膜碎觉使劲挤才能出一点点皱纹 脸颊毛孔修复的看不见啦 草莓鼻历史遗留问题没辙 脸和脖子差不多颜色的皮肤才是健康的 长期使用安全健康的比同龄人显小五到十岁 28岁的妹子看看你们的鱼尾纹" ;
//
//        System.out.println(BaseAnalysis.parse(str));
//        System.out.println(ToAnalysis.parse(str));
//        System.out.println(DicAnalysis.parse(str));
//        System.out.println(IndexAnalysis.parse(str));
//        System.out.println(NlpAnalysis.parse(str));

        //DicLibrary.insert("dic", "订单号", "/v", 49999);
        DicLibrary.insert("dic", "单号", "/v", 1000);
        DicLibrary.insert("dic", "订单", "/v", 1001);
        DicLibrary.insert("dic", "陆金所", "/v", 1001);
        DicLibrary.insert("dic", "陆金", "/v", 20001);

        AmbiguityLibrary.insert("ambiguity","陆金");
        System.out.println(DicAnalysis.parse("陆金所属订单号"));
    }


}
