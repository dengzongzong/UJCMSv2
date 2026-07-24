<template>
  <div class="app-container cert-template-editor">
    <el-card>
      <div slot="header">
        <span>{{ isEdit ? '编辑证书模板' : '新增证书模板' }}</span>
        <el-button style="float:right" size="small" @click="$router.back()">返回</el-button>
      </div>

      <el-form :model="template" label-width="120px" size="small" style="max-width:720px">
        <el-form-item label="证书类型">
          <el-select v-model="template.name" placeholder="请选择证书类型" filterable allow-create style="width:100%">
            <el-option v-for="t in certTypes" :key="t.id" :label="t.name" :value="t.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="证书编号前缀">
          <el-input v-model="template.certNoPrefix" placeholder="如 ZGZH(留空则用全局配置)" maxlength="10" show-word-limit style="width:240px" />
          <span style="margin-left:8px;color:#999;font-size:12px">生成证书编号时优先使用此模板配置</span>
        </el-form-item>
        <el-form-item label="证书编号中段">
          <el-input v-model="template.certNoMiddle" placeholder="如 M(留空则用全局配置)" maxlength="10" show-word-limit style="width:240px" />
          <span style="margin-left:8px;color:#999;font-size:12px">生成证书编号时优先使用此模板配置</span>
        </el-form-item>
        <el-form-item label="背景图">
          <el-upload
            :show-file-list="false"
            :http-request="onUploadBg"
            :before-upload="beforeUpload"
            accept="image/*"
            action="#"
          >
            <el-button size="small" type="primary">{{ template.bgImageUrl ? '替换背景图' : '点击上传背景图' }}</el-button>
          </el-upload>
          <div v-if="template.bgImageUrl" style="margin-top:8px;color:#999">
            原始尺寸: {{ template.bgWidth }} × {{ template.bgHeight }} px
          </div>
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="template.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-divider content-position="left">钢印配置(可选)</el-divider>
        <el-form-item label="钢印图片">
          <el-upload
            :show-file-list="false"
            :http-request="onUploadStamp"
            :before-upload="beforeStampUpload"
            accept="image/png"
            action="#"
          >
            <el-button size="small" type="warning">{{ template.stampUrl ? '替换钢印图片' : '上传钢印图片(PNG)' }}</el-button>
          </el-upload>
          <div v-if="template.stampUrl" style="margin-top:8px;display:flex;align-items:center;gap:8px">
            <img :src="resolveUrl(template.stampUrl)" style="max-width:100px;max-height:100px;border:1px solid #ddd;border-radius:4px" />
            <el-button size="mini" type="text" style="color:#f56c6c" @click="clearStamp">移除钢印</el-button>
          </div>
          <div style="color:#e6a23c;font-size:12px;margin-top:4px;line-height:1.6">
            <i class="el-icon-warning" /> 钢印图片必须是<b>透明背景的PNG</b>才能正确叠加，白色或不透明背景会遮挡证书内容。<br />
            可使用Photoshop等工具将印章抠图后导出为透明PNG，再上传此处。
          </div>
        </el-form-item>
        <template v-if="template.stampUrl">
          <el-form-item label="X坐标">
            <el-input-number v-model="template.stampX" :min="0" :max="5000" controls-position="right" style="width:140px" />
            <span style="margin-left:8px;color:#999;font-size:12px">可在画布上直接拖拽钢印调整位置</span>
          </el-form-item>
          <el-form-item label="Y坐标">
            <el-input-number v-model="template.stampY" :min="0" :max="5000" controls-position="right" style="width:140px" />
          </el-form-item>
          <el-form-item label="宽度">
            <el-input-number v-model="template.stampWidth" :min="0" :max="3000" controls-position="right" style="width:140px" />
            <span style="margin-left:8px;color:#999;font-size:12px">0 = 原始尺寸</span>
          </el-form-item>
          <el-form-item label="旋转角度">
            <el-input-number v-model="template.stampRotation" :min="0" :max="360" :step="5" controls-position="right" style="width:140px" />
            <span style="margin-left:8px;color:#999;font-size:12px">度 (0-360)</span>
          </el-form-item>
          <el-form-item label="透明度">
            <el-slider v-model="template.stampOpacity" :min="0" :max="1" :step="0.05" style="width:220px" />
          </el-form-item>
        </template>
        <el-form-item label="备注">
          <el-input v-model="template.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>

      <el-divider content-position="left">字段位置(拖拽编辑)</el-divider>

      <!-- 缩放控制 -->
      <div v-if="template.bgImageUrl" class="zoom-control">
        <el-button size="mini" @click="zoomOut">
          <i class="el-icon-minus" />
        </el-button>
        <span class="zoom-value">{{ Math.round(zoom * 100) }}%</span>
        <el-button size="mini" @click="zoomIn">
          <i class="el-icon-plus" />
        </el-button>
        <el-button size="mini" @click="zoomReset">100%</el-button>
      </div>

      <div v-if="!template.bgImageUrl" class="editor-empty">
        <el-empty description="请先上传背景图" />
      </div>
      <div v-else class="editor-layout">
        <!-- 画布 -->
        <div class="canvas-wrap">
          <div
            ref="canvas"
            class="canvas"
            :style="canvasStyle"
            @click="onCanvasClick"
            @dragover.prevent
            @drop="onDropToCanvas"
          >
            <img v-if="template.bgImageUrl" :src="resolveUrl(template.bgImageUrl)" class="bg" draggable="false" />
            <div
              v-for="(f, idx) in template.fields"
              :key="idx"
              class="field-box"
              :class="{ active: selectedIdx === idx }"
              :style="fieldStyle(f)"
              @click.stop="onFieldClick(idx)"
              @mousedown="onFieldMouseDown($event, idx)"
            >
              <span class="field-label">{{ fieldLabel(f.fieldKey) }}</span>
              <span v-if="isImageField(f.fieldKey)" class="badge">图片</span>
              <div v-if="isImageField(f.fieldKey)" class="resize-handle" @mousedown.stop="onFieldResizeStart($event, idx)"></div>
            </div>
            <!-- 钢印预览(可拖拽定位和调整大小) -->
            <div
              v-if="template.stampUrl"
              class="stamp-box"
              :style="stampStyle"
              @mousedown.stop="onStampMouseDown($event)"
              @click.stop
            >
              <img :src="resolveUrl(template.stampUrl)" draggable="false" />
              <span class="stamp-badge">钢印</span>
              <div class="resize-handle" @mousedown.stop="onStampResizeStart($event)"></div>
            </div>
          </div>
        </div>

        <!-- 右侧字段面板 + 属性 -->
        <div class="side-panel">
          <el-tabs v-model="sideTab">
            <el-tab-pane label="字段库" name="palette">
              <el-input v-model="searchKey" size="small" placeholder="搜索字段" style="margin-bottom:8px" />
              <div class="palette">
                <div
                  v-for="f in filteredPalette"
                  :key="f.fieldKey"
                  class="palette-item"
                  draggable="true"
                  @dragstart="onPaletteDragStart($event, f)"
                >
                  <i class="el-icon-rank" />
                  <span>{{ f.fieldName }}</span>
                  <small>({{ f.fieldKey }})</small>
                </div>
              </div>
              <p style="color:#999;font-size:12px;margin-top:8px">拖拽到左侧背景图任意位置</p>
            </el-tab-pane>

            <el-tab-pane label="属性" name="props" :disabled="selectedIdx === null">
              <div v-if="selectedField">
                <el-form label-width="80px" size="small">
                  <el-form-item label="字段">
                    <el-select v-model="selectedField.fieldKey" filterable @change="onFieldKeyChange">
                      <el-option v-for="f in allFields" :key="f.fieldKey" :label="f.fieldName + ' (' + f.fieldKey + ')'" :value="f.fieldKey" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="X(像素)">
                    <el-input-number v-model="selectedField.x" :min="0" :max="5000" />
                  </el-form-item>
                  <el-form-item label="Y(像素)">
                    <el-input-number v-model="selectedField.y" :min="0" :max="5000" />
                  </el-form-item>
                  <el-form-item label="宽(像素)">
                    <el-input-number v-model="selectedField.width" :min="10" :max="3000" />
                  </el-form-item>
                  <el-form-item v-if="isImageField(selectedField.fieldKey)" label="高(像素)">
                    <el-input-number v-model="selectedField.height" :min="10" :max="3000" />
                  </el-form-item>
                  <el-form-item label="字号">
                    <el-input-number v-model="selectedField.fontSize" :min="8" :max="200" />
                  </el-form-item>
                  <el-form-item label="颜色">
                    <el-color-picker v-model="selectedField.color" />
                  </el-form-item>
                  <el-form-item label="粗体">
                    <el-switch v-model="selectedField.fontWeight" :active-value="2" :inactive-value="1" />
                  </el-form-item>
                  <el-form-item label="对齐">
                    <el-select v-model="selectedField.align">
                      <el-option :value="1" label="左对齐" />
                      <el-option :value="2" label="居中" />
                      <el-option :value="3" label="右对齐" />
                    </el-select>
                  </el-form-item>
                  <el-form-item>
                    <el-button type="danger" size="small" icon="el-icon-delete" @click="removeField">删除该字段</el-button>
                  </el-form-item>
                </el-form>
              </div>
              <el-empty v-else description="先选中背景上的字段" :image-size="80" />
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>

      <div v-if="template.bgImageUrl" style="margin-top:24px">
        <el-button type="primary" :loading="submitting" @click="onSave">保存</el-button>
        <el-button @click="$router.back()">取消</el-button>
      </div>
    </el-card>
  </div>
</template>

<script>
import { templateDetail, saveTemplate } from '@/api/certificateTemplate'
import { fieldList } from '@/api/certificateField'
import { uploadFile as uploadRequest } from '@/api/upload'
import { apiUrl } from '@/utils/apiBase'
import { publicCertificateTypes } from '@/api/certificateType'

export default {
  name: 'CertificateTemplateEdit',
  data() {
    return {
      submitting: false,
      isEdit: false,
      zoom: 1,
      certTypes: [],
      template: {
        id: null, name: '', bgImageUrl: '', bgWidth: 0, bgHeight: 0,
        isDefault: 0, remark: '',
        certNoPrefix: '', certNoMiddle: '',
        stampUrl: '', stampX: null, stampY: null, stampWidth: null, stampRotation: 0, stampOpacity: 0.8,
        fields: []
      },
      allFields: [],
      searchKey: '',
      sideTab: 'palette',
      selectedIdx: null,
      // 拖动瞬时状态(在画布上拖动已有字段用)
      drag: {
        active: false,
        idx: null,
        startX: 0, startY: 0,
        originX: 0, originY: 0
      },
      // 钢印拖动状态
      stampDrag: {
        active: false,
        startX: 0, startY: 0,
        originX: 0, originY: 0
      },
      // 调整大小状态
      resize: {
        active: false,
        type: '', // 'field' or 'stamp'
        idx: null,
        startX: 0, startY: 0,
        originWidth: 0, originHeight: 0
      }
    }
  },
  computed: {
    id() { return this.$route.params.id },
    filteredPalette() {
      const k = this.searchKey.trim().toLowerCase()
      if (!k) return this.allFields
      return this.allFields.filter(f => f.fieldName.toLowerCase().includes(k) || f.fieldKey.toLowerCase().includes(k))
    },
    selectedField() {
      return this.selectedIdx === null ? null : this.template.fields[this.selectedIdx]
    },
    canvasStyle() {
      const w = this.template.bgWidth || 800
      const h = this.template.bgHeight || 600
      return {
        width: w + 'px',
        height: h + 'px',
        backgroundImage: `url(${this.resolveUrl(this.template.bgImageUrl)})`,
        backgroundSize: '100% 100%',
        backgroundRepeat: 'no-repeat',
        transform: `scale(${this.zoom})`,
        transformOrigin: 'top left'
      }
    },
    stampStyle() {
      const x = this.template.stampX || 0
      const y = this.template.stampY || 0
      const w = this.template.stampWidth || 120
      const rotation = this.template.stampRotation || 0
      const opacity = this.template.stampOpacity != null ? this.template.stampOpacity : 0.8
      return {
        left: x + 'px',
        top: y + 'px',
        width: w + 'px',
        transform: `rotate(${rotation}deg)`,
        opacity: opacity
      }
    }
  },
  watch: {
    'template.fields': {
      handler() {
        // 列表变化时,自动 sort
        this.template.fields.forEach((f, i) => { if (f.sort !== i) f.sort = i })
      },
      deep: true
    }
  },
  async mounted() {
    const fields = await fieldList()
    this.allFields = fields.data || []
    // 加载证书类型列表
    try {
      const typeRes = await publicCertificateTypes()
      this.certTypes = (typeRes.data || typeRes || [])
    } catch (e) { /* ignore */ }
    if (this.id) {
      this.isEdit = true
      const res = await templateDetail(this.id)
      this.template = {
        id: res.data.id,
        name: res.data.name,
        bgImageUrl: res.data.bgImageUrl,
        bgWidth: res.data.bgWidth,
        bgHeight: res.data.bgHeight,
        isDefault: res.data.isDefault,
        remark: res.data.remark,
        certNoPrefix: res.data.certNoPrefix || '',
        certNoMiddle: res.data.certNoMiddle || '',
        stampUrl: res.data.stampUrl || '',
        stampX: res.data.stampX,
        stampY: res.data.stampY,
        stampWidth: res.data.stampWidth,
        stampRotation: res.data.stampRotation || 0,
        stampOpacity: res.data.stampOpacity != null ? res.data.stampOpacity : 0.8,
        fields: (res.data.fields || []).map(f => ({ ...f }))
      }
    }
    // 监听全局 mousemove / mouseup,处理拖动
    window.addEventListener('mousemove', this.onWindowMouseMove)
    window.addEventListener('mouseup', this.onWindowMouseUp)
  },
  beforeDestroy() {
    window.removeEventListener('mousemove', this.onWindowMouseMove)
    window.removeEventListener('mouseup', this.onWindowMouseUp)
  },
  methods: {
    zoomIn() {
      this.zoom = Math.min(2, this.zoom + 0.1)
    },
    zoomOut() {
      this.zoom = Math.max(0.25, this.zoom - 0.1)
    },
    zoomReset() {
      this.zoom = 1
    },
    resolveUrl(u) {
      if (!u) return ''
      if (u.startsWith('http')) return u
      // /uploads/** 走 ResourceHandler,后端已排除 JWT 鉴权,不需要 token
      return apiUrl(u)
    },
    fieldLabel(key) {
      const f = this.allFields.find(x => x.fieldKey === key)
      return f ? f.fieldName : key
    },
    isImageField(key) {
      if (!key) return false
      if (key === 'photo' || key === 'examQr') return true
      if (/^qr/i.test(key)) return true
      return false
    },
    fieldStyle(f) {
      const style = {
        left: f.x + 'px',
        top: f.y + 'px',
        fontSize: (f.fontSize || 24) + 'px',
        color: f.color || '#000000',
        fontWeight: f.fontWeight === 2 ? 700 : 400,
        textAlign: f.align === 2 ? 'center' : (f.align === 3 ? 'right' : 'left'),
        width: (f.width ? f.width + 'px' : 'auto')
      }
      // 图片字段: 设置高度(有 height 时用配置值,否则自适应)
      if (this.isImageField(f.fieldKey) && f.height) {
        style.height = f.height + 'px'
      }
      return style
    },
    // ============ 字段库(右侧)拖到画布 ============
    onPaletteDragStart(e, f) {
      // 用 dataTransfer 传递字段信息
      const newField = {
        fieldKey: f.fieldKey,
        x: 0, y: 0,
        width: this.isImageField(f.fieldKey) ? 120 : 240,
        height: this.isImageField(f.fieldKey) ? 160 : null,
        fontSize: 24,
        color: '#000000',
        fontWeight: 1,
        align: 1,
        sort: this.template.fields.length
      }
      e.dataTransfer.setData('application/x-cert-field', JSON.stringify(newField))
      e.dataTransfer.effectAllowed = 'copy'
    },
    onDropToCanvas(e) {
      const raw = e.dataTransfer.getData('application/x-cert-field')
      if (!raw) return
      let f
      try { f = JSON.parse(raw) } catch (err) { return }
      const rect = this.$refs.canvas.getBoundingClientRect()
      const scale = this.zoom || 1
      const dropX = (e.clientX - rect.left) / scale
      const dropY = (e.clientY - rect.top) / scale
      f.x = Math.max(0, Math.round(dropX - (f.width || 200) / 2))
      f.y = Math.max(0, Math.round(dropY - 16))
      if (!f.height) f.height = 32
      this.template.fields.push(f)
      this.selectedIdx = this.template.fields.length - 1
      this.sideTab = 'props'
    },
    // ============ 画布上已有字段拖动 ============
    onFieldMouseDown(e, idx) {
      this.drag.active = true
      this.drag.idx = idx
      this.drag.startX = e.clientX
      this.drag.startY = e.clientY
      this.drag.originX = this.template.fields[idx].x
      this.drag.originY = this.template.fields[idx].y
      this.selectedIdx = idx
      this.sideTab = 'props'
      e.preventDefault()
      e.stopPropagation()
    },
    onWindowMouseMove(e) {
      const scale = this.zoom || 1
      // 调整大小
      if (this.resize.active) {
        const dx = (e.clientX - this.resize.startX) / scale
        const dy = (e.clientY - this.resize.startY) / scale
        const newWidth = Math.max(20, Math.round(this.resize.originWidth + dx))
        const newHeight = Math.max(20, Math.round(this.resize.originHeight + dy))
        if (this.resize.type === 'field' && this.resize.idx !== null) {
          this.template.fields[this.resize.idx].width = newWidth
          // 图片字段同时调整高度(支持纵向缩放)
          if (this.isImageField(this.template.fields[this.resize.idx].fieldKey)) {
            this.template.fields[this.resize.idx].height = newHeight
          }
        } else if (this.resize.type === 'stamp') {
          this.template.stampWidth = newWidth
        }
        return
      }
      // 钢印拖动
      if (this.stampDrag.active) {
        const rect = this.$refs.canvas && this.$refs.canvas.getBoundingClientRect()
        if (!rect) return
        const dx = (e.clientX - this.stampDrag.startX) / scale
        const dy = (e.clientY - this.stampDrag.startY) / scale
        const maxX = (this.template.bgWidth || 800)
        const maxY = (this.template.bgHeight || 600)
        this.template.stampX = Math.max(0, Math.min(maxX, Math.round(this.stampDrag.originX + dx)))
        this.template.stampY = Math.max(0, Math.min(maxY, Math.round(this.stampDrag.originY + dy)))
        return
      }
      if (!this.drag.active || this.drag.idx === null) return
      const f = this.template.fields[this.drag.idx]
      if (!f) return
      const dx = (e.clientX - this.drag.startX) / scale
      const dy = (e.clientY - this.drag.startY) / scale
      const maxX = (this.template.bgWidth || 800)
      const maxY = (this.template.bgHeight || 600)
      const newX = Math.max(0, Math.min(maxX, Math.round(this.drag.originX + dx)))
      const newY = Math.max(0, Math.min(maxY, Math.round(this.drag.originY + dy)))
      f.x = newX
      f.y = newY
    },
    onWindowMouseUp() {
      this.drag.active = false
      this.drag.idx = null
      this.stampDrag.active = false
      this.resize.active = false
      this.resize.type = ''
      this.resize.idx = null
    },
    onFieldClick(idx) {
      this.selectedIdx = idx
      this.sideTab = 'props'
    },
    onCanvasClick(e) {
      // 点到画布空白(背景或 canvas 本身)时取消选中并切回字段库,方便继续拖新字段
      if (e.target === this.$refs.canvas || (e.target.classList && e.target.classList.contains('bg'))) {
        this.selectedIdx = null
        this.sideTab = 'palette'
      }
    },
    onFieldKeyChange() {
      const f = this.selectedField
      if (f && this.isImageField(f.fieldKey)) {
        if (!f.width) f.width = 120
        if (!f.height) f.height = 160
      }
    },
    removeField() {
      if (this.selectedIdx === null) return
      this.template.fields.splice(this.selectedIdx, 1)
      this.selectedIdx = null
    },
    beforeUpload(file) {
      if (!file.type.startsWith('image/')) { this.$message.error('只能上传图片'); return false }
      if (file.size / 1024 / 1024 > 10) { this.$message.error('图片不能超过 10MB'); return false }
      return true
    },
    async onUploadBg({ file }) {
      const fd = new FormData()
      fd.append('file', file)
      const res = await uploadRequest(fd)
      // 先清零旧尺寸,避免替换时短暂显示旧图的尺寸造成混淆
      this.template.bgImageUrl = res.data
      this.template.bgWidth = 0
      this.template.bgHeight = 0
      const img = new Image()
      img.onload = () => {
        this.template.bgWidth = img.naturalWidth
        this.template.bgHeight = img.naturalHeight
      }
      img.onerror = () => {
        this.$message.error('背景图加载失败,无法识别尺寸')
      }
      img.src = this.resolveUrl(res.data)
    },
    // ============ 钢印相关 ============
    beforeStampUpload(file) {
      if (file.type !== 'image/png') {
        this.$message.error('钢印图片必须是PNG格式(支持透明背景)')
        return false
      }
      if (file.size / 1024 / 1024 > 5) {
        this.$message.error('钢印图片不能超过 5MB')
        return false
      }
      return true
    },
    async onUploadStamp({ file }) {
      const fd = new FormData()
      fd.append('file', file)
      const res = await uploadRequest(fd)
      this.template.stampUrl = res.data
      // 默认放置在证书中心偏上
      const bgW = this.template.bgWidth || 800
      const bgH = this.template.bgHeight || 600
      if (this.template.stampX === null || this.template.stampX === undefined) {
        this.template.stampX = Math.round(bgW / 2 - 60)
      }
      if (this.template.stampY === null || this.template.stampY === undefined) {
        this.template.stampY = Math.round(bgH / 2 - 60)
      }
      if (this.template.stampWidth === null || this.template.stampWidth === undefined) {
        this.template.stampWidth = 120
      }
      this.$message.success('钢印图片上传成功，可在画布上拖拽调整位置')
    },
    clearStamp() {
      this.template.stampUrl = ''
      this.template.stampX = null
      this.template.stampY = null
      this.template.stampWidth = null
    },
    onStampMouseDown(e) {
      this.stampDrag.active = true
      this.stampDrag.startX = e.clientX
      this.stampDrag.startY = e.clientY
      this.stampDrag.originX = this.template.stampX || 0
      this.stampDrag.originY = this.template.stampY || 0
      e.preventDefault()
      e.stopPropagation()
    },
    onFieldResizeStart(e, idx) {
      this.resize.active = true
      this.resize.type = 'field'
      this.resize.idx = idx
      this.resize.startX = e.clientX
      this.resize.startY = e.clientY
      this.resize.originWidth = this.template.fields[idx].width || 120
      this.resize.originHeight = this.template.fields[idx].height || 0
      e.preventDefault()
      e.stopPropagation()
    },
    onStampResizeStart(e) {
      this.resize.active = true
      this.resize.type = 'stamp'
      this.resize.idx = null
      this.resize.startX = e.clientX
      this.resize.originWidth = this.template.stampWidth || 120
      e.preventDefault()
      e.stopPropagation()
    },
    async onSave() {
      if (!this.template.name) { this.$message.warning('请输入模板名'); return }
      this.submitting = true
      try {
        await saveTemplate(this.template)
        this.$message.success('保存成功')
        this.$router.push('/certificate/template')
      } finally { this.submitting = false }
    }
  }
}
</script>

<style scoped>
.editor-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.canvas-wrap {
  flex: 0 0 auto;
  background: repeating-conic-gradient(#f0f0f0 0% 25%, #fafafa 0% 50%) 0/20px 20px;
  border: 1px solid #e4e7ed;
  overflow: auto;
  max-width: 70%;
  max-height: 70vh;
  min-height: 200px;
}
.zoom-control {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  width: fit-content;

  .zoom-value {
    min-width: 50px;
    text-align: center;
    font-size: 13px;
    color: #606266;
    font-weight: 500;
  }
}
.canvas {
  position: relative;
  background-color: #fff;
}
.canvas .bg {
  display: block;
  width: 100%;
  height: 100%;
  pointer-events: none;
  user-select: none;
}
.field-box {
  position: absolute;
  display: inline-block;
  padding: 4px 8px;
  border: 1px dashed transparent;
  background: rgba(255, 255, 255, 0.4);
  cursor: move;
  user-select: none;
  white-space: nowrap;
  min-width: 32px;
  text-align: left;
  transition: border-color 0.15s;
}
.field-box.active {
  border: 1px dashed #409EFF;
  background: rgba(64, 158, 255, 0.1);
  z-index: 10;
}
.field-box .field-label {
  pointer-events: none;
}
.field-box .badge {
  display: inline-block;
  margin-left: 4px;
  padding: 0 4px;
  font-size: 10px;
  color: #fff;
  background: #67c23a;
  border-radius: 2px;
}
.stamp-box {
  position: absolute;
  cursor: move;
  user-select: none;
  z-index: 5;
  border: 1px dashed #e6a23c;
  background: rgba(230, 162, 60, 0.05);
  transition: border-color 0.15s;
}
.stamp-box:hover {
  border-color: #e6a23c;
  border-style: solid;
}
.stamp-box img {
  width: 100%;
  height: auto;
  display: block;
  pointer-events: none;
}
.stamp-box .stamp-badge {
  position: absolute;
  top: -18px;
  left: 0;
  display: inline-block;
  padding: 0 4px;
  font-size: 10px;
  color: #fff;
  background: #e6a23c;
  border-radius: 2px;
  white-space: nowrap;
}
.resize-handle {
  position: absolute;
  right: -6px;
  bottom: -6px;
  width: 12px;
  height: 12px;
  background: #409EFF;
  border: 2px solid #fff;
  border-radius: 50%;
  cursor: se-resize;
  z-index: 100;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}
.resize-handle:hover {
  background: #66b1ff;
  transform: scale(1.2);
}
.side-panel {
  width: 320px;
  flex: 0 0 320px;
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 8px;
  max-height: 70vh;
  overflow: auto;
}
.palette-item {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 8px 12px;
  margin-bottom: 6px;
  cursor: grab;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}
.palette-item:hover {
  border-color: #409EFF;
  color: #409EFF;
}
.palette-item small {
  color: #999;
  margin-left: auto;
}
.editor-empty {
  padding: 40px 0;
}
</style>
