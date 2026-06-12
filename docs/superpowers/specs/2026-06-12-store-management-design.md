# 门店管理模块设计文档

## 概述

为 HQ_OPERATOR / ADMIN 角色提供门店的完整 CRUD 管理功能，包括创建、编辑、删除、列表查看，以及门店封面图片上传。

## 需求

- **功能范围**：完整 CRUD（创建、编辑、删除、列表管理）
- **权限**：仅限 `HQ_OPERATOR` 和 `ADMIN` 角色
- **页面位置**：Admin 侧边栏新增「门店管理」菜单项
- **封面图**：支持 URL 输入 + 文件本地上传

## 现有基础

- `store` 表已包含所有必要字段（V001 + V008）：id, name, city, address, phone, opening_time, closing_time, status, description, business_area, latitude, longitude, cover_url, area_square_meter, created_at, updated_at, deleted
- 后端已有 `Store` 实体、`StoreMapper`、`StoreService`（仅读操作）、`StoreController`（仅 GET 端点）
- 前端已有客户面向的 `StoreListView.vue` 和 `StoreDetailView.vue`
- 项目已有图片上传机制（`CatPhotoStorageService`），可作为参考模式
- Admin 模块已有 CRUD 管理页面模式（`ActivityManageView.vue`）

## 后端设计

### StoreService 新增方法

```java
// 创建门店
public StoreDetailResponse create(CreateStoreRequest request)

// 更新门店
public StoreDetailResponse update(Long id, CreateStoreRequest request)

// 删除门店（软删除）
public void delete(Long id)

// 管理端门店列表（含所有状态，非仅 OPEN）
public List<StoreSummaryResponse> adminList()
```

### DTO

```java
public record CreateStoreRequest(
    String name,
    String city,
    String address,
    String phone,
    LocalTime openingTime,
    LocalTime closingTime,
    String status,          // OPEN / PREPARING / CLOSED
    String description,
    String businessArea,
    BigDecimal latitude,
    BigDecimal longitude,
    String coverUrl,
    BigDecimal areaSquareMeter
) {}
```

### StoreController 新增端点

所有管理端点使用 `/api/admin/stores` 路径，控制器级别 `@PreAuthorize("hasAnyRole('HQ_OPERATOR', 'ADMIN')")`：

```
POST   /api/admin/stores          创建门店
PUT    /api/admin/stores/{id}     编辑门店
DELETE /api/admin/stores/{id}     删除门店（软删除）
GET    /api/admin/stores          管理端门店列表
POST   /api/admin/stores/upload   上传门店封面图 → 返回 URL
```

注意：现有的公开 GET `/api/store` 和 `/api/store/{id}` 不变。

### StorePhotoStorageService

新建 `com.nekocafe.store.service.StorePhotoStorageService`，参考 `CatPhotoStorageService`：

- 上传目录：`uploads/stores`（可配置）
- URL 前缀：`/uploads/stores`
- 文件大小限制：5MB
- 支持格式：jpg, png, webp, gif
- 文件名：UUID + 扩展名

### 配置变更

`application.yml` 新增：
```yaml
nekocafe:
  upload:
    store-photo-dir: ${STORE_PHOTO_UPLOAD_DIR:uploads/stores}
    store-photo-url-prefix: /uploads/stores
```

### SecurityConfig 变更

公开访问路径新增 `/uploads/stores/**`。

### 文件变更清单

| 文件 | 操作 |
|---|---|
| `store/controller/StoreController.java` | 修改 - 新增管理端点 |
| `store/service/StoreService.java` | 修改 - 新增 CRUD 方法 |
| `store/service/StorePhotoStorageService.java` | 新建 - 图片上传 |
| `application.yml` | 修改 - 新增配置 |
| `security/SecurityConfig.java` | 修改 - 新增公开路径 |

## 前端设计

### 路由

新增路由 `admin/stores`，指向 `StoreManageView.vue`：
```ts
{ path: 'admin/stores', name: 'admin-stores',
  component: () => import('@/views/admin/StoreManageView.vue'),
  meta: { requiresAuth: true, roles: ['HQ_OPERATOR', 'ADMIN'] } }
```

### 菜单

`permissions.ts` 中 `APP_MENU_ITEMS` 新增：
```ts
{ path: '/admin/stores', label: '门店管理', icon: '🏪',
  hint: '创建、编辑与管理门店', roles: ['HQ_OPERATOR', 'ADMIN'] }
```

### StoreManageView 页面布局

参考 `ActivityManageView.vue` 模式：

- **头部**：标题「门店管理」+ 描述 + 刷新按钮
- **工具栏**：城市筛选下拉框 + 状态筛选下拉框 + 创建门店按钮
- **数据表格**：
  - 列：ID、名称、城市、地址、电话、营业时间、状态（Tag）、操作（编辑/删除）
  - 支持分页
- **创建/编辑对话框**（`el-dialog`）：
  - 表单字段：名称（必填）、城市（必填）、地址（必填）、电话、开始营业时间、结束营业时间、状态（OPEN/PREPARING/CLOSED）、描述、商圈、面积、纬度、经度、封面图URL + 上传按钮
  - 上传使用 `el-upload` 组件，调用 `/api/admin/stores/upload`

### API 函数

`api/admin.ts` 新增：
```ts
// 管理端门店列表
fetchAdminStoreList(): Promise<AdminStoreDetailRow[]>

// 创建门店
createStore(req: CreateStoreRequest): Promise<AdminStoreDetailRow>

// 更新门店
updateStore(id: number, req: CreateStoreRequest): Promise<AdminStoreDetailRow>

// 删除门店
deleteStore(id: number): Promise<void>

// 上传门店封面图
uploadStorePhoto(file: File): Promise<{ url: string }>
```

### 文件变更清单

| 文件 | 操作 |
|---|---|
| `views/admin/StoreManageView.vue` | 新建 |
| `api/admin.ts` | 修改 - 新增 API 函数及类型 |
| `router/index.ts` | 修改 - 新增路由 |
| `router/permissions.ts` | 修改 - 新增菜单项 |

## 错误处理

- 创建/更新时校验必填字段（name、city、address）
- 删除时使用 `ElMessageBox.confirm` 确认
- 上传图片时校验文件类型和大小
- 后端返回 BizException 统一处理

## 约束与边界

- 不在本次范围：桌位管理、员工分配（这些已有独立模块）
- 删除为软删除（设置 `deleted = 1`），不物理删除
- 不新建数据库迁移，复用现有 `store` 表结构
- 保持现有公开 `/api/store` 端点不变，管理端点使用 `/api/admin/stores` 路径
