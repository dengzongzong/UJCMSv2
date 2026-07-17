<template>
  <div class="app-container">
    <el-card>
      <div slot="header">
        <span>证书字段(自定义字段)</span>
        <el-button style="float:right" type="primary" size="small" icon="el-icon-plus" @click="onAdd">新增字段</el-button>
      </div>
      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="fieldKey" label="字段键" width="140" />
        <el-table-column prop="fieldName" label="显示名" width="160" />
        <el-table-column label="类型" width="100">
          <template slot-scope="s">
            <el-tag size="mini" :type="typeColor(s.row.fieldType)">{{ typeName(s.row.fieldType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="必填" width="60">
          <template slot-scope="s">
            <el-tag size="mini" :type="s.row.required === 1 ? 'danger' : 'info'">{{ s.row.required === 1 ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column prop="defaultValue" label="默认值" />
        <el-table-column prop="options" label="选项" show-overflow-tooltip />
        <el-table-column label="系统" width="80">
          <template slot-scope="s">
            <el-tag size="mini" :type="s.row.isSystem === 1 ? 'warning' : 'success'">{{ s.row.isSystem === 1 ? '系统' : '自定义' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template slot-scope="s">
            <el-button size="mini" :disabled="s.row.isSystem === 1" @click="onEdit(s.row)">编辑</el-button>
            <el-button size="mini" type="danger" :disabled="s.row.isSystem === 1" @click="onDelete(s.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog :title="form.id ? '编辑字段' : '新增字段'" :visible.sync="dialogVisible" width="500px" @closed="onDialogClosed">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" size="small">
        <el-form-item label="字段键" prop="fieldKey">
          <el-input v-model="form.fieldKey" :disabled="!!form.id && form.isSystem === 1" placeholder="英文,如 custom1" />
        </el-form-item>
        <el-form-item label="显示名" prop="fieldName">
          <el-input v-model="form.fieldName" />
        </el-form-item>
        <el-form-item label="类型" prop="fieldType">
          <el-select v-model="form.fieldType" :disabled="!!form.id && form.isSystem === 1">
            <el-option :value="1" label="文本" />
            <el-option :value="2" label="数字" />
            <el-option :value="3" label="日期" />
            <el-option :value="4" label="选择项" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否必填">
          <el-switch v-model="form.required" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="默认值">
          <el-input v-model="form.defaultValue" />
        </el-form-item>
        <el-form-item v-if="form.fieldType === 4" label="选择项">
          <el-input v-model="form.options" placeholder="逗号分隔,如 A,B,C" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { fieldList, addField, updateField, deleteField } from '@/api/certificateField'

export default {
  name: 'CertificateFieldList',
  data() {
    return {
      loading: false,
      list: [],
      dialogVisible: false,
      submitting: false,
      form: { id: null, fieldKey: '', fieldName: '', fieldType: 1, required: 0, sort: 99, defaultValue: '', options: '', isSystem: 0 },
      rules: {
        fieldKey: [{ required: true, message: '请输入字段键', trigger: 'blur' }, { pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/, message: '英文/数字/下划线,以字母开头' }],
        fieldName: [{ required: true, message: '请输入显示名', trigger: 'blur' }],
        fieldType: [{ required: true, message: '请选择类型', trigger: 'change' }]
      }
    }
  },
  mounted() { this.load() },
  methods: {
    typeName(t) { return { 1: '文本', 2: '数字', 3: '日期', 4: '选择项', 5: '图片' }[t] || '-' },
    typeColor(t) { return { 1: '', 2: 'success', 3: 'warning', 4: 'info', 5: 'danger' }[t] || '' },
    async load() {
      this.loading = true
      try {
        const res = await fieldList()
        this.list = res.data || []
      } finally { this.loading = false }
    },
    onAdd() {
      this.form = { id: null, fieldKey: '', fieldName: '', fieldType: 1, required: 0, sort: 99, defaultValue: '', options: '', isSystem: 0 }
      this.dialogVisible = true
    },
    onEdit(row) {
      this.form = { ...row }
      this.dialogVisible = true
    },
    onDelete(row) {
      this.$confirm(
        '确定删除字段 "' + row.fieldName + '" 吗?\n删除后历史证书中的该字段值将无法读取,建议先备份数据。',
        '删除确认',
        { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消', dangerouslyUseHTMLString: false }
      ).then(() => {
        return deleteField(row.id)
      }).then(() => {
        this.$message.success('已删除')
        this.load()
      }).catch(err => {
        // 取消时 err 是字符串 'cancel' / 'close'
        if (err && err !== 'cancel' && err !== 'close') {
          this.$message.error('删除失败')
        }
      })
    },
    onSubmit() {
      this.$refs.formRef.validate(valid => {
        if (!valid) return
        this.submitting = true
        const api = this.form.id ? updateField : addField
        api(this.form).then(() => {
          this.$message.success('保存成功')
          this.dialogVisible = false
          this.load()
        }).finally(() => { this.submitting = false })
      })
    },
    onDialogClosed() {
      this.$refs.formRef && this.$refs.formRef.resetFields()
    }
  }
}
</script>
