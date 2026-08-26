# 设备运行状态与电能采集

应用每分钟直接请求 `meter.external.url`，暂不调用登录接口，也不添加鉴权请求头。应用解析附件格式的 `data.list[].properties[]`：

生产环境默认设备接口为：

```text
http://190.2.147.3:3000/openapi/v1.0/device/list
```

外部接口要求 `deviceId` 逐个传递，默认请求以下 10 个设备：

```text
d4,d14,d57,d68,d81,d88,d150,d157,d168,d172
```

多个设备也可以通过环境变量用英文逗号配置：

```bat
set METER_EXTERNAL_DEVICE_IDS=d14,d15,d16
```

系统会依次请求：

```text
.../device/list?deviceId=d14
.../device/list?deviceId=d15
.../device/list?deviceId=d16
```

如需更换接口地址，设置环境变量 `METER_EXTERNAL_URL` 覆盖默认值。

- `有功电能` 保存为累计电能（kWh）；
- `floorName` 保存为区域，`name` 保存为设备名称；
- `A相电流`、`B相电流`、`C相电流` 保存为三相电流；
- 任意一相电流 `> 0.5` 判定设备工作，否则判定设备停止；
- 工作状态从停止变为工作时创建区间，从工作变为停止时关闭区间。

首次部署请执行 [schema.sql](/Users/zrj/code/zhaoming/src/main/resources/db/schema.sql)。生产环境建议将其中 DDL 纳入正式数据库迁移流程。

查询接口：

```text
GET /api/devices/energy?deviceName=转运站&floorName=混匀区&startTime=2026-08-25T00:00:00&endTime=2026-08-26T00:00:00
GET /api/devices/work-periods?deviceName=转运站&floorName=混匀区&startTime=2026-08-25T00:00:00&endTime=2026-08-26T00:00:00
```

`deviceName` 为电表名称，支持模糊匹配；`floorName` 为区域，支持精确匹配。IP、设备状态和时间粒度不作为接口筛选条件，快捷时间由前端转换成 `startTime`、`endTime`。

能源列表接口每行返回 `deviceName`、`floorName`、`differenceKwh`（当前时间范围总能耗）、`totalOnlineSeconds` 和 `totalOnlineDuration`（格式 `HH:mm:ss`，当前时间范围总在线时长）。
```

电能接口返回 `differenceKwh = endEnergyKwh - startEnergyKwh`。边界优先使用边界时刻之前（含边界）的最近采样；若边界前没有数据，则使用边界之后的第一条采样，并返回实际使用的采样时刻。
