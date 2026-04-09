## 汇率api
### 技术栈(该项目技术栈仅为炫技😎，实际不需要这么多重服务)
- 语言 & 框架
  - Java 17 
    - Spring Boot：构建微服务
    - Spring Cloud Gateway：统一网关，路由、限流、鉴权
- 持久化 & 缓存
  - PostgreSQL：持久化存储，事务支持
  - Redis：分布式缓存
  - Caffeine：JVM 本地缓存
- 消息队列
  - Kafka：异步事件驱动，Producer / Consumer 模型
  - Spring Kafka（spring-kafka + kafka-clients）
- 任务调度
  - 开发阶段：@Scheduled 定时任务
  - Kubernetes 部署：CronJob
- 监控 & 可观测性
  - Micrometer → Prometheus
  - Grafana → 可视化展示
  - Spring Boot Actuator 输出指标
- 构建 & 容器化
  - Maven / Gradle（多模块项目）
  - Docker 多阶段构建
  - Kubernetes：Deployment、Service、CronJob
## 运行顺序
1. common（只需编译即可）
2. rate-fetcher（产生数据）
3. rate-consumer（消费数据写数据库）
4. rate-api（提供 API 查询数据）
5. gateway（路由请求到 API）
