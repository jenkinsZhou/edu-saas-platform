# 订单与支付扩展设计

## 当前边界

当前阶段已经具备订单、报名履约、人工收款、退款记录和测试渠道支付回调能力。

订单负责记录销售事实和金额快照，报名负责教学履约：

```text
创建报名订单 -> 确认订单 -> 生成或激活班级报名 -> 登记收款流水 -> 更新付款状态
```

这样设计的好处是，后面接微信、支付宝、线下转账、分期、退款时，不需要推翻课程和报名模型。

## 状态拆分

订单状态和付款状态分开：

```text
order_status: CREATED / CONFIRMED / CANCELLED
pay_status:   UNPAID / PART_PAID / PAID / PART_REFUNDED / REFUNDED
```

这能覆盖常见教培场景：

- 先报名后付款
- 先收定金后补尾款
- 线下收款人工确认
- 后续退款或转班

## 核心表

```text
enrollment_order       订单主表
enrollment_order_item  订单明细，保存课程、班级、学员和价格快照
class_enrollment       班级报名履约结果
```

订单明细保存快照而不是只存外键，是为了避免课程改名、改价后影响历史订单。

## 支付模块演进

当前已经新增过程单和流水表：

```text
payment_order      支付单，面向支付渠道
refund_order       退款单
payment_record     收款成功后的财务流水
refund_record      退款成功后的财务流水
finance_receipt    收据/发票申请
```

支付成功后更新 `enrollment_order.pay_status`，但不直接改课程数据。课程履约仍通过订单确认或支付回调触发报名。

`finance_receipt` 后续可继续补齐，用于收据、发票申请和财务归档。

## 已落地收款流水

当前已经新增：

```text
payment_record
refund_record
```

用于保存每一笔实际收款：

- 所属订单
- 收款流水号
- 收款类型
- 收款方式
- 收款金额
- 渠道交易号
- 收款时间
- 备注

登记收款后，系统会重新计算订单：

```text
paid_amount
outstanding_amount
pay_status
```

付款状态规则：

```text
0 元已收              -> UNPAID
已收小于应收          -> PART_PAID
已收等于应收          -> PAID
已退小于已收          -> PART_REFUNDED
已退等于已收且净收=0  -> REFUNDED
```

后续接微信/支付宝时，可以把渠道回调转换为 `payment_record`，再走同一套订单状态刷新逻辑。

退款则同样只写 `refund_record`，再统一刷新订单的已收、已退、净收和付款状态。

## 已落地审计

当前订单中心的关键写操作会写入 `operation_log`，并额外写入订单维度的 `order_audit_log`：

```text
CREATE_ORDER
CONFIRM_ORDER
CANCEL_ORDER
CREATE_PAYMENT
CREATE_REFUND
```

日志会记录：

- 租户
- 操作账号
- 操作模块
- 动作
- 业务对象类型
- 业务对象 ID
- requestId
- 金额或订单号摘要

排查账务问题时，可以先按 `requestId` 精确查接口链路，再按订单号、流水 ID 或操作人追溯业务动作。

订单详情可通过接口查看审计轨迹：

```text
GET /api/orders/{id}/audits
```

## 已落地支付回调底座

当前新增了支付回调入口：

```text
POST /api/orders/callbacks/payment
Header: X-Callback-Secret
```

回调会先做幂等判断，再锁定订单行处理：

```text
callback_no + channel_trade_no 去重
SUCCESS 状态 -> 写 payment_record -> 刷新订单付款状态 -> 写 order_audit_log
非 SUCCESS 状态 -> 只写 payment_callback_log 和审计，不入账
```

回调日志表：

```text
payment_callback_log
```

用于追踪：

- 渠道编码
- 渠道交易号
- 回调编号
- 回调状态
- 原始 payload
- 处理结果
- requestId

这只是支付渠道底座。真正接微信/支付宝时，还需要把 `X-Callback-Secret` 替换为渠道验签，并接入通道查单、退款查询和对账文件。

## 状态机增强

订单系统现在把“交易过程”和“最终流水”分开：

- `payment_order`：支付过程单，承载 `INIT/PAYING/SUCCESS/FAILED/CLOSED` 等状态。
- `payment_record`：支付成功后的财务流水，只记录已经确认成功的收款。
- `refund_order`：退款过程单，承载 `INIT/REFUNDING/SUCCESS/FAILED/CLOSED` 等状态。
- `refund_record`：退款成功后的财务流水，只记录已经确认成功的退款。

这样设计的原因是第三方支付并不总是同步返回结果，真实生产中会遇到超时、重复回调、回调乱序、人工补单、通道查询等情况。过程单负责追踪状态，流水负责财务入账，两者分开后数据更清晰，也更容易对账。

## 补偿任务

后端已启用 `PaymentCompensationJob`：

- 定时扫描待确认的支付过程单和退款过程单。
- 每次扫描增加 `check_count`，更新 `last_checked_at`。
- 未超过阈值时推迟下次检查。
- 超过 `max-check-count` 后自动关闭为 `CLOSED`。

当前配置项：

```yaml
edu:
  payment:
    compensation:
      enabled: true
      fixed-delay-ms: 60000
      initial-delay-ms: 30000
      batch-size: 100
      max-check-count: 12
```

后续接入微信、支付宝时，补偿任务里可以替换为真实的通道查询逻辑：先查通道订单状态，再决定成功、失败、继续等待或关闭。

## 对账查询接口

当前已经提供过程单查询和待对账查询：

```text
GET /api/orders/{id}/payment-orders
GET /api/orders/{id}/payment-orders?status=SUCCESS
GET /api/orders/{id}/refund-orders
GET /api/orders/{id}/refund-orders?status=SUCCESS
GET /api/orders/reconciliation/pending?limit=50
```

`reconciliation/pending` 会返回当前租户下仍处于待处理状态的支付单和退款单：

```text
payment_order.status in (INIT, PAYING)
refund_order.status in (INIT, REFUNDING)
```

这个接口后续可以直接做成财务/运维页面，用来发现卡住的支付、退款和通道异常。

## 通道适配层

当前已经抽出支付通道接口：

```text
PaymentChannel
PaymentChannelRegistry
OfflinePaymentChannel
PaymentCallbackVerifier
PaymentStateMachine
```

手工收款和退款目前走 `OFFLINE` 通道。后续接微信、支付宝时，建议新增独立通道实现，例如：

```text
WechatPayChannel
AlipayChannel
BankTransferChannel
```

每个通道只处理自己的验签、查单、退款查询和状态映射，订单中心只消费统一状态。
