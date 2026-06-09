package org.example.pay;

public class WeiChatPayImpl implements PayStrategy {
    @Override
    public String type() {
        return "pay_wechat";
    }

    @Override
    public void pay() {
        System.out.println("正在支付~");
        sleep();
        System.out.println("微信支付成功~");
    }

    private void sleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("支付被中断", e);
        }
    }
}
