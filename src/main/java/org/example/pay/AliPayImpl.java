package org.example.pay;

public class AliPayImpl implements PayStrategy {
    @Override
    public String type() {
        return "pay_ali";
    }

    @Override
    public void pay() {
        System.out.println("正在支付~");
        sleep();
        System.out.println("支付宝支付成功~");
    }

    private void sleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
