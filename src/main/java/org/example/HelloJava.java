package org.example;

import org.example.pay.PayContext;

public class HelloJava {
    public static void main(String[] args) {
        PayContext payContext = new PayContext();

        System.out.println("已加载策略: " + payContext.strategies().keySet());
        payContext.payWithType("pay_ali");
//        payContext.payWithType("pay_wechat");
//        payContext.payWithType("pay_union");
//        payContext.payWithType("pay_xxx");
    }
}
