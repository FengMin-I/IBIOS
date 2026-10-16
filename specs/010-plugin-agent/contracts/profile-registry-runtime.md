# ProfileRegistry 运行时注册契约

## 既有方法（不变）

- `void register(Profile profile)`：注册 Profile，key = profile.name()
- `Profile get(String name)`：按名字查找，不存在返回 null
- `Collection<Profile> getAll()`：返回所有已注册 Profile 的不可变视图

## 新增方法

### `void remove(String name)`

**行为**：从内部 Map 移除指定名字的 Profile。若不存在，无操作（不报错）。

### `boolean exists(String name)`

**行为**：返回内部 Map 是否包含指定名字的 Profile。

## 不变量

- `register()` 后立即 `get()` 必须返回同一 Profile
- `remove()` 后 `get()` 必须返回 null、`exists()` 必须返回 false
- 所有操作线程安全（ConcurrentHashMap）
- 非法 Profile（如 provider.name 为空）在 register 时不额外校验——校验由调用方（AgentLoader.scanAndRegister 或 ProfileLoader）在 register 之前完成

## 测试锚点

- `ProfileRegistryRuntimeTest`：register/get/remove/exists 基本流程、线程安全、remove 不存在的 key 不报错
