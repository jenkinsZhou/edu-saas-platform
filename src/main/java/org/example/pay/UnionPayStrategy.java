package org.example.pay;

public class UnionPayStrategy implements PayStrategy {
    @Override
    public String type() {
        return "pay_union";
    }

    @Override
    public void pay() {
        System.out.println("正在支付~");
        sleep();
        System.out.println("银联支付成功~");
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
