package org.example.pay;

public class NormalPay {

    public void pay(String type) {
        System.out.println("正在支付...");
        loading();
        if (type.equalsIgnoreCase("pay_ali")) {
            System.out.println("支付宝支付成功");
        } else if (type.equalsIgnoreCase("pay_wechat")) {
            System.out.println("微信支付成功");
        } else if (type.equalsIgnoreCase("pay_union")) {
            System.out.println("银联支付成功");
        } else {
            System.out.println("未匹配到支付类型");
        }
    }

    private void loading() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            System.out.println("loading出错:" + e);
        }
    }


}
