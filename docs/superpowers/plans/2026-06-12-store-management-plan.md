# Store Management Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a full CRUD store management module for HQ_OPERATOR/ADMIN roles, including backend admin endpoints, file upload for store photos, and a frontend management page.

**Architecture:** Backend extends the existing `store` module with new service methods, a dedicated admin controller (or extended controller), and a photo storage service following the `CatPhotoStorageService` pattern. Frontend adds a new `StoreManageView.vue` under admin views with dialog-based create/edit, following the `ActivityManageView.vue` pattern. No database migration needed — the `store` table already has all required fields.

**Tech Stack:** Java 17 / Spring Boot 3.3.5 / MyBatis-Plus / Vue 3 / TypeScript / Element Plus / Axios

---

### Task 1: Create StorePhotoStorageService (backend — file upload)

**Files:**
- Create: `NekoCafe/backend/src/main/java/com/nekocafe/store/service/StorePhotoStorageService.java`

- [ ] **Step 1: Write the service class**

Create `NekoCafe/backend/src/main/java/com/nekocafe/store/service/StorePhotoStorageService.java`:

```java
package com.nekocafe.store.service;

import com.nekocafe.common.exception.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class StorePhotoStorageService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of(
        "image/jpeg", ".jpg",
        "image/png", ".png",
        "image/webp", ".webp",
        "image/gif", ".gif"
    );

    private final Path uploadDir;
    private final String urlPrefix;

    public StorePhotoStorageService(
            @Value("${nekocafe.upload.store-photo-dir:uploads/stores}") String uploadDir,
            @Value("${nekocafe.upload.store-photo-url-prefix:/uploads/stores}") String urlPrefix) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.urlPrefix = normalizePrefix(urlPrefix);
    }

    public record UploadResult(String url) {}

    public UploadResult store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(4301, "请选择要上传的门店封面图");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException(4303, "门店封面图不能超过 5MB");
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String extension = EXTENSIONS.get(contentType);
        if (extension == null) {
            throw new BizException(4302, "仅支持 jpg、png、webp、gif 格式的门店封面图");
        }

        String filename = UUID.randomUUID() + extension;
        Path target = uploadDir.resolve(filename).normalize();
        if (!target.startsWith(uploadDir)) {
            throw new BizException(4304, "封面图保存失败");
        }

        try {
            Files.createDirectories(uploadDir);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new BizException(4304, "封面图保存失败");
        }

        return new UploadResult(urlPrefix + "/" + filename);
    }

    private String normalizePrefix(String prefix) {
        String normalized = prefix == null || prefix.isBlank() ? "/uploads/stores" : prefix.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add NekoCafe/backend/src/main/java/com/nekocafe/store/service/StorePhotoStorageService.java
git commit -m "feat: add StorePhotoStorageService for store cover image upload"
```

---

### Task 2: Add store photo upload config to application.yml

**Files:**
- Modify: `NekoCafe/backend/src/main/resources/application.yml:29-30`

- [ ] **Step 1: Add store photo config**

In `NekoCafe/backend/src/main/resources/application.yml`, add two lines after the existing `cat-photo-url-prefix` line. The `nekocafe.upload` section should become:

```yaml
  upload:
    cat-photo-dir: ${CAT_PHOTO_UPLOAD_DIR:uploads/cats}
    cat-photo-url-prefix: /uploads/cats
    store-photo-dir: ${STORE_PHOTO_UPLOAD_DIR:uploads/stores}
    store-photo-url-prefix: /uploads/stores
```

- [ ] **Step 2: Commit**

```bash
git add NekoCafe/backend/src/main/resources/application.yml
git commit -m "feat: add store photo upload directory config"
```

---

### Task 3: Add /uploads/stores/** to SecurityConfig permitAll

**Files:**
- Modify: `NekoCafe/backend/src/main/java/com/nekocafe/security/SecurityConfig.java:51`

- [ ] **Step 1: Add the public path**

In `NekoCafe/backend/src/main/java/com/nekocafe/security/SecurityConfig.java`, add `"/uploads/stores/**"` to the `.requestMatchers(...)` list inside `.permitAll()`, right after the existing `"/uploads/cats/**"` line.

The `.requestMatchers(...)` block should become:

```java
                .requestMatchers(
                    "/api/health",
                    "/actuator/health",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/api-docs/**",
                    "/v3/api-docs/**",
                    "/api/auth/status",
                    "/api/auth/login",
                    "/api/auth/register",
                    "/uploads/cats/**",
                    "/uploads/stores/**",
                    "/api/store",
                    "/api/store/**",
                    "/api/menu",
                    "/api/menu/**"
                ).permitAll()
```

- [ ] **Step 2: Commit**

```bash
git add NekoCafe/backend/src/main/java/com/nekocafe/security/SecurityConfig.java
git commit -m "feat: allow public access to /uploads/stores/**"
```

---

### Task 4: Add CRUD methods and DTOs to StoreService

**Files:**
- Modify: `NekoCafe/backend/src/main/java/com/nekocafe/store/service/StoreService.java`

- [ ] **Step 1: Add CreateStoreRequest DTO, adminList, create, update, and delete methods**

Add the following code to `StoreService.java`. Place the new `CreateStoreRequest` record alongside the existing DTO records at the bottom of the file. Place the new methods (`adminList`, `create`, `update`, `delete`) after the existing `detail` method.

**New imports to add at the top:**
```java
import java.math.BigDecimal;
```

**New methods — add after the `detail()` method, before the `private long countAvailableTables` method:**

```java
    public List<StoreSummaryResponse> adminList() {
        return storeMapper.selectList(new LambdaQueryWrapper<Store>()
                .eq(Store::getDeleted, 0)
                .orderByAsc(Store::getCity)
                .orderByAsc(Store::getId))
            .stream()
            .map(store -> toSummary(store, countAvailableTables(store.getId())))
            .toList();
    }

    public StoreDetailResponse create(CreateStoreRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new BizException(400, "门店名称不能为空");
        }
        if (request.city() == null || request.city().isBlank()) {
            throw new BizException(400, "城市不能为空");
        }
        if (request.address() == null || request.address().isBlank()) {
            throw new BizException(400, "地址不能为空");
        }

        Store store = new Store();
        applyRequestToStore(request, store);
        store.setStatus(request.status() != null ? request.status() : "PREPARING");
        storeMapper.insert(store);
        return toDetail(store, List.of());
    }

    public StoreDetailResponse update(Long id, CreateStoreRequest request) {
        Store store = storeMapper.selectOne(new LambdaQueryWrapper<Store>()
            .eq(Store::getId, id)
            .eq(Store::getDeleted, 0)
            .last("LIMIT 1"));
        if (store == null) {
            throw new BizException(2001, "门店不存在");
        }
        applyRequestToStore(request, store);
        storeMapper.updateById(store);
        return toDetail(store, List.of());
    }

    public void delete(Long id) {
        Store store = storeMapper.selectOne(new LambdaQueryWrapper<Store>()
            .eq(Store::getId, id)
            .eq(Store::getDeleted, 0)
            .last("LIMIT 1"));
        if (store == null) {
            throw new BizException(2001, "门店不存在");
        }
        store.setDeleted(1);
        storeMapper.updateById(store);
    }

    private void applyRequestToStore(CreateStoreRequest request, Store store) {
        store.setName(request.name());
        store.setCity(request.city());
        store.setAddress(request.address());
        store.setPhone(request.phone());
        store.setOpeningTime(request.openingTime());
        store.setClosingTime(request.closingTime());
        store.setDescription(request.description());
        store.setBusinessArea(request.businessArea());
        store.setLatitude(request.latitude());
        store.setLongitude(request.longitude());
        store.setCoverUrl(request.coverUrl());
        store.setAreaSquareMeter(request.areaSquareMeter());
    }

    private StoreDetailResponse toDetail(Store store, List<TableSummaryResponse> tables) {
        return new StoreDetailResponse(
            store.getId(),
            store.getName(),
            store.getCity(),
            store.getAddress(),
            store.getPhone(),
            store.getOpeningTime(),
            store.getClosingTime(),
            store.getStatus(),
            store.getDescription(),
            tables
        );
    }
```

**New DTO record — add at the bottom of the file alongside existing records:**

```java
    public record CreateStoreRequest(
        String name,
        String city,
        String address,
        String phone,
        LocalTime openingTime,
        LocalTime closingTime,
        String status,
        String description,
        String businessArea,
        BigDecimal latitude,
        BigDecimal longitude,
        String coverUrl,
        BigDecimal areaSquareMeter
    ) {}
```

- [ ] **Step 2: Commit**

```bash
git add NekoCafe/backend/src/main/java/com/nekocafe/store/service/StoreService.java
git commit -m "feat: add CRUD methods and CreateStoreRequest DTO to StoreService"
```

---

### Task 5: Add admin CRUD and upload endpoints to StoreController

**Files:**
- Modify: `NekoCafe/backend/src/main/java/com/nekocafe/store/controller/StoreController.java`

- [ ] **Step 1: Rewrite StoreController with admin endpoints**

Replace the entire content of `StoreController.java` with:

```java
package com.nekocafe.store.controller;

import com.nekocafe.common.result.ApiResult;
import com.nekocafe.store.service.StorePhotoStorageService;
import com.nekocafe.store.service.StorePhotoStorageService.UploadResult;
import com.nekocafe.store.service.StoreService;
import com.nekocafe.store.service.StoreService.CreateStoreRequest;
import com.nekocafe.store.service.StoreService.StoreDetailResponse;
import com.nekocafe.store.service.StoreService.StoreSummaryResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
public class StoreController {

    private final StoreService storeService;
    private final StorePhotoStorageService storePhotoStorageService;

    public StoreController(StoreService storeService,
                           StorePhotoStorageService storePhotoStorageService) {
        this.storeService = storeService;
        this.storePhotoStorageService = storePhotoStorageService;
    }

    // ---- public endpoints ----

    @GetMapping("/api/store/status")
    public ApiResult<Map<String, String>> status() {
        return ApiResult.ok(Map.of("module", "store", "status", "ready"));
    }

    @GetMapping("/api/store")
    public ApiResult<List<StoreSummaryResponse>> list() {
        return ApiResult.ok(storeService.listStores());
    }

    @GetMapping("/api/store/{id}")
    public ApiResult<StoreDetailResponse> detail(@PathVariable Long id) {
        return ApiResult.ok(storeService.detail(id));
    }

    // ---- admin endpoints ----

    @GetMapping("/api/admin/stores")
    @PreAuthorize("hasAnyRole('HQ_OPERATOR', 'ADMIN')")
    public ApiResult<List<StoreSummaryResponse>> adminList() {
        return ApiResult.ok(storeService.adminList());
    }

    @PostMapping("/api/admin/stores")
    @PreAuthorize("hasAnyRole('HQ_OPERATOR', 'ADMIN')")
    public ApiResult<StoreDetailResponse> create(@RequestBody CreateStoreRequest request) {
        return ApiResult.ok(storeService.create(request));
    }

    @PutMapping("/api/admin/stores/{id}")
    @PreAuthorize("hasAnyRole('HQ_OPERATOR', 'ADMIN')")
    public ApiResult<StoreDetailResponse> update(@PathVariable Long id,
                                                  @RequestBody CreateStoreRequest request) {
        return ApiResult.ok(storeService.update(id, request));
    }

    @DeleteMapping("/api/admin/stores/{id}")
    @PreAuthorize("hasAnyRole('HQ_OPERATOR', 'ADMIN')")
    public ApiResult<Void> delete(@PathVariable Long id) {
        storeService.delete(id);
        return ApiResult.ok(null);
    }

    @PostMapping("/api/admin/stores/upload")
    @PreAuthorize("hasAnyRole('HQ_OPERATOR', 'ADMIN')")
    public ApiResult<UploadResult> uploadPhoto(@RequestParam("file") MultipartFile file) {
        return ApiResult.ok(storePhotoStorageService.store(file));
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add NekoCafe/backend/src/main/java/com/nekocafe/store/controller/StoreController.java
git commit -m "feat: add admin CRUD and photo upload endpoints to StoreController"
```

---

### Task 6: Add store management API functions and types to frontend admin.ts

**Files:**
- Modify: `NekoCafe/frontend/src/api/admin.ts`

- [ ] **Step 1: Add new types and API functions**

Add the following types after the existing `AdminStoreRow` interface (after line 27), and add the API functions at the end of the file.

**New types — add after `AdminStoreRow`:**

```ts
export interface AdminStoreDetailRow {
  id: number
  name: string
  city: string
  address: string
  phone?: string
  openingTime: string
  closingTime: string
  status: string
  description?: string
  businessArea?: string
  latitude?: number
  longitude?: number
  coverUrl?: string
  areaSquareMeter?: number
  availableTableCount: number
}

export interface CreateStoreRequest {
  name: string
  city: string
  address: string
  phone?: string
  openingTime?: string
  closingTime?: string
  status?: string
  description?: string
  businessArea?: string
  latitude?: number
  longitude?: number
  coverUrl?: string
  areaSquareMeter?: number
}
```

**New API functions — add at the end of the file:**

```ts
export async function fetchAdminStoreList() {
  const { data } = await http.get<ApiResult<AdminStoreDetailRow[]>>('/admin/stores')
  return data.data
}

export async function createStore(req: CreateStoreRequest) {
  const { data } = await http.post<ApiResult<AdminStoreDetailRow>>('/admin/stores', req)
  return data.data
}

export async function updateStore(id: number, req: CreateStoreRequest) {
  const { data } = await http.put<ApiResult<AdminStoreDetailRow>>(`/admin/stores/${id}`, req)
  return data.data
}

export async function deleteStore(id: number) {
  const { data } = await http.delete<ApiResult<null>>(`/admin/stores/${id}`)
  return data.data
}

export async function uploadStorePhoto(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await http.post<ApiResult<{ url: string }>>('/admin/stores/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data.data
}
```

- [ ] **Step 2: Commit**

```bash
git add NekoCafe/frontend/src/api/admin.ts
git commit -m "feat: add store management API functions and types"
```

---

### Task 7: Create StoreManageView.vue

**Files:**
- Create: `NekoCafe/frontend/src/views/admin/StoreManageView.vue`

- [ ] **Step 1: Write the full view component**

Create `NekoCafe/frontend/src/views/admin/StoreManageView.vue`:

```vue
<template>
  <section class="page-card role-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">HQ_OPERATOR / 门店管理</p>
        <h1>门店管理</h1>
        <p>创建、编辑与管理各城市门店信息。</p>
      </div>
    </header>

    <!-- Toolbar -->
    <div class="toolbar">
      <el-select v-model="filterCity" placeholder="城市筛选" clearable style="width:160px">
        <el-option v-for="c in cityOptions" :key="c" :label="c" :value="c" />
      </el-select>
      <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width:140px; margin-left:8px">
        <el-option label="营业中" value="OPEN" />
        <el-option label="筹备中" value="PREPARING" />
        <el-option label="已关闭" value="CLOSED" />
      </el-select>
      <el-button type="primary" style="margin-left:auto" @click="openCreate">创建门店</el-button>
    </div>

    <!-- Store table -->
    <el-table :data="filteredStores" border v-loading="loading" empty-text="暂无门店数据">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column prop="city" label="城市" width="100" />
      <el-table-column prop="address" label="地址" min-width="180" />
      <el-table-column prop="phone" label="电话" width="130" />
      <el-table-column label="营业时间" width="160">
        <template #default="{ row }">
          {{ fmtTime(row.openingTime) }} - {{ fmtTime(row.closingTime) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Create/Edit dialog -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑门店' : '创建门店'" width="640px" :close-on-click-modal="false">
      <el-form :model="form" label-position="top">
        <el-row :gutter="12">
          <el-col :span="14">
            <el-form-item label="门店名称" required>
              <el-input v-model="form.name" placeholder="如：NekoCafé 朝阳大悦城店" />
            </el-form-item>
          </el-col>
          <el-col :span="10">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width:100%">
                <el-option label="营业中" value="OPEN" />
                <el-option label="筹备中" value="PREPARING" />
                <el-option label="已关闭" value="CLOSED" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="城市" required>
              <el-input v-model="form.city" placeholder="如：北京" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="电话">
              <el-input v-model="form.phone" placeholder="如：010-88886666" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="地址" required>
          <el-input v-model="form.address" placeholder="详细地址" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="开始营业时间">
              <el-time-picker v-model="form.openingTime" placeholder="选择时间" format="HH:mm" value-format="HH:mm:ss" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束营业时间">
              <el-time-picker v-model="form.closingTime" placeholder="选择时间" format="HH:mm" value-format="HH:mm:ss" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="商圈">
              <el-input v-model="form.businessArea" placeholder="如：核心商圈" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="面积（㎡）">
              <el-input-number v-model="form.areaSquareMeter" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="纬度">
              <el-input-number v-model="form.latitude" :precision="7" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="经度">
              <el-input-number v-model="form.longitude" :precision="7" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="门店描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="门店介绍" />
        </el-form-item>
        <el-form-item label="封面图">
          <div style="display:flex; gap:12px; align-items:flex-start">
            <el-input v-model="form.coverUrl" placeholder="输入封面图 URL 或上传图片" style="flex:1" />
            <el-upload
              :http-request="handleUpload"
              :show-file-list="false"
              accept="image/jpeg,image/png,image/webp,image/gif"
            >
              <el-button type="primary" :loading="uploading">上传图片</el-button>
            </el-upload>
          </div>
          <img v-if="form.coverUrl" :src="form.coverUrl" style="margin-top:8px; max-width:200px; max-height:120px; border-radius:8px; object-fit:cover" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  fetchAdminStoreList, createStore, updateStore, deleteStore, uploadStorePhoto,
  type AdminStoreDetailRow, type CreateStoreRequest,
} from '@/api/admin'

const stores = ref<AdminStoreDetailRow[]>([])
const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const filterCity = ref('')
const filterStatus = ref('')

// Create/Edit
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive<CreateStoreRequest>({
  name: '', city: '', address: '', phone: '',
  openingTime: undefined, closingTime: undefined, status: 'OPEN',
  description: '', businessArea: '', areaSquareMeter: undefined,
  latitude: undefined, longitude: undefined, coverUrl: '',
})

const cityOptions = computed(() => [...new Set(stores.value.map(s => s.city))].sort())

const filteredStores = computed(() => {
  return stores.value.filter(s => {
    if (filterCity.value && s.city !== filterCity.value) return false
    if (filterStatus.value && s.status !== filterStatus.value) return false
    return true
  })
})

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    stores.value = await fetchAdminStoreList()
  } catch (e) {
    ElMessage.warning(e instanceof Error ? e.message : '加载门店列表失败')
  } finally { loading.value = false }
}

function resetForm() {
  editingId.value = null
  Object.assign(form, {
    name: '', city: '', address: '', phone: '',
    openingTime: undefined, closingTime: undefined, status: 'OPEN',
    description: '', businessArea: '', areaSquareMeter: undefined,
    latitude: undefined, longitude: undefined, coverUrl: '',
  })
}

function openCreate() { resetForm(); dialogVisible.value = true }

function openEdit(row: AdminStoreDetailRow) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name, city: row.city, address: row.address,
    phone: row.phone || '', openingTime: row.openingTime,
    closingTime: row.closingTime, status: row.status,
    description: row.description || '', businessArea: row.businessArea || '',
    areaSquareMeter: row.areaSquareMeter, latitude: row.latitude,
    longitude: row.longitude, coverUrl: row.coverUrl || '',
  })
  dialogVisible.value = true
}

async function handleUpload(options: { file: File }) {
  uploading.value = true
  try {
    const result = await uploadStorePhoto(options.file)
    form.coverUrl = result.url
    ElMessage.success('封面图上传成功')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '上传失败')
  } finally { uploading.value = false }
}

async function handleSave() {
  if (!form.name.trim()) { ElMessage.warning('请输入门店名称'); return }
  if (!form.city.trim()) { ElMessage.warning('请输入城市'); return }
  if (!form.address.trim()) { ElMessage.warning('请输入地址'); return }

  saving.value = true
  try {
    if (editingId.value) {
      await updateStore(editingId.value, form)
      ElMessage.success('门店已更新')
    } else {
      await createStore(form)
      ElMessage.success('门店已创建')
    }
    dialogVisible.value = false
    await loadData()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  } finally { saving.value = false }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该门店？删除后门店将不再显示。', '删除确认', { type: 'warning' })
    await deleteStore(id)
    ElMessage.success('已删除')
    await loadData()
  } catch { /* cancelled */ }
}

// --- helpers ---
function fmtTime(v?: string) { return v ? v.slice(0, 5) : '--:--' }
function statusLabel(s: string) { const m: Record<string, string> = { OPEN: '营业中', PREPARING: '筹备中', CLOSED: '已关闭' }; return m[s] || s }
function statusTagType(s: string) { if (s === 'OPEN') return 'success'; if (s === 'PREPARING') return 'warning'; return 'info' }
</script>

<style scoped>
.role-page { display: grid; gap: 16px; }
.page-header { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.eyebrow { margin: 0 0 8px; color: #d97706; font-weight: 800; letter-spacing: 0.08em; }
.toolbar { display: flex; align-items: center; gap: 8px; }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add NekoCafe/frontend/src/views/admin/StoreManageView.vue
git commit -m "feat: add StoreManageView with full CRUD and photo upload"
```

---

### Task 8: Add route and sidebar menu item

**Files:**
- Modify: `NekoCafe/frontend/src/router/index.ts`
- Modify: `NekoCafe/frontend/src/router/permissions.ts`

- [ ] **Step 1: Add route in index.ts**

In `NekoCafe/frontend/src/router/index.ts`, add the new route after the existing admin routes (after line 53, before the `]` closing the children array):

```ts
        { path: 'admin/stores', name: 'admin-stores', component: () => import('@/views/admin/StoreManageView.vue'), meta: { requiresAuth: true, roles: ['HQ_OPERATOR', 'ADMIN'] } },
```

- [ ] **Step 2: Add menu item in permissions.ts**

In `NekoCafe/frontend/src/router/permissions.ts`, add the new menu item after the existing "活动管理" entry (after line 53):

```ts
  { path: '/admin/stores', label: '门店管理', icon: '🏪', hint: '创建、编辑与管理门店', roles: ['HQ_OPERATOR', 'ADMIN'] },
```

- [ ] **Step 3: Commit**

```bash
git add NekoCafe/frontend/src/router/index.ts NekoCafe/frontend/src/router/permissions.ts
git commit -m "feat: add store management route and sidebar menu item"
```

---

### Task 9: Build and verify

**Files:** None (verification only)

- [ ] **Step 1: Build backend**

```bash
cd NekoCafe/backend && mvn -B -DskipTests compile
```

Expected: BUILD SUCCESS

- [ ] **Step 2: Build frontend**

```bash
cd NekoCafe/frontend && npm run build
```

Expected: Build completes without errors

- [ ] **Step 3: Final commit if any fixups were needed**

Only if the builds revealed issues that required code changes:

```bash
git add -A && git commit -m "chore: fix build issues from store management module"
```
