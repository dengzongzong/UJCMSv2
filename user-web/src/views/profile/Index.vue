<template>
  <div class="profile-page">
    <Header />

    <div class="page-body">
      <div class="container">
        <div class="profile-layout">
          <!-- 左侧边栏 -->
          <div class="sidebar">
            <!-- 用户信息卡片 -->
            <div class="user-card">
              <div class="user-avatar" @click="handleEditAvatar">
                <img v-if="userInfo.avatar" :src="resolveImg(userInfo.avatar)" alt="头像" />
                <img v-else-if="certAvatarUrl" :src="resolveImg(certAvatarUrl)" alt="头像" />
                <div v-else class="avatar-placeholder">
                  <van-icon name="user-circle-o" size="48" color="#ccc" />
                </div>
                <div class="avatar-edit">
                  <van-icon name="photograph" size="14" color="#fff" />
                </div>
              </div>
              <div class="user-info">
                <div class="user-name" @click="handleEditProfile">{{ userInfo.nickname || '未设置昵称' }}</div>
                <div class="user-phone">{{ formatPhone(userInfo.phone) }}</div>
                <div class="user-subject" v-if="currentSubject">
                  <van-icon name="bookmark-o" size="12" />
                  {{ currentSubject.name }}
                </div>
              </div>
            </div>

            <!-- 隐藏的头像上传组件 -->
            <van-uploader
              ref="avatarUploader"
              v-model="avatarFile"
              :max-count="1"
              :after-read="afterReadAvatar"
              :preview-image="false"
              v-show="false"
            />

            <!-- 菜单列表 -->
            <div class="menu-section">
              <div class="menu-title">学习中心</div>
              <div class="menu-item" :class="{ active: activeView === 'profile' }" @click="activeView = 'profile'">
                <van-icon name="user-o" size="18" />
                <span>个人资料</span>
                <van-icon name="arrow" size="14" class="arrow" />
              </div>
              <div class="menu-item" @click="$router.push('/course/my-opened')">
                <van-icon name="bookmark-o" size="18" />
                <span>我的课程</span>
                <van-icon name="arrow" size="14" class="arrow" />
              </div>
              <div class="menu-item" @click="$router.push('/profile/orders')">
                <van-icon name="orders-o" size="18" />
                <span>我的订单</span>
                <van-icon name="arrow" size="14" class="arrow" />
              </div>
              <div class="menu-item" :class="{ active: activeView === 'my-exams' }" @click="activeView = 'my-exams'">
                <van-icon name="notes-o" size="18" />
                <span>我的考试</span>
                <van-icon name="arrow" size="14" class="arrow" />
              </div>
              <div class="menu-item" :class="{ active: activeView === 'records' }" @click="activeView = 'records'">
                <van-icon name="records" size="18" />
                <span>考试记录</span>
                <van-icon name="arrow" size="14" class="arrow" />
              </div>
              <div class="menu-item" @click="$router.push('/profile/wrong-questions')">
                <van-icon name="warning-o" size="18" />
                <span>我的错题</span>
                <van-icon name="arrow" size="14" class="arrow" />
              </div>
              <div class="menu-item" @click="$router.push('/certificate')">
                <van-icon name="medal-o" size="18" />
                <span>证书查询</span>
                <van-icon name="arrow" size="14" class="arrow" />
              </div>
            </div>

            <div class="menu-section">
              <div class="menu-title">账号设置</div>
              <div class="menu-item" @click="openPasswordDialog">
                <van-icon name="lock" size="18" />
                <span>修改密码</span>
                <van-icon name="arrow" size="14" class="arrow" />
              </div>
              <div class="menu-item" @click="$router.push('/profile/about')">
                <van-icon name="info-o" size="18" />
                <span>关于我们</span>
                <van-icon name="arrow" size="14" class="arrow" />
              </div>
            </div>

            <div class="menu-section">
              <div class="menu-item logout" @click="handleLogout">
                <van-icon name="cross" size="18" />
                <span>退出登录</span>
              </div>
            </div>
          </div>

          <!-- 右侧内容区 -->
          <div class="content-area">
            <template v-if="activeView === 'profile'">
            <div class="content-card">
              <div class="card-header">
                <span class="card-title">个人资料</span>
                <button type="button" class="edit-btn-wrap" @click="handleEditProfile">
                  <svg class="edit-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                  </svg>
                  <span class="edit-btn-text">修改</span>
                </button>
              </div>
              <div class="info-grid">
                <div class="info-item">
                  <div class="info-label">昵称</div>
                  <div class="info-value">{{ userInfo.nickname || '未设置' }}</div>
                </div>
                <div class="info-item">
                  <div class="info-label">手机号</div>
                  <div class="info-value">{{ formatPhone(userInfo.phone) }}</div>
                </div>
                <div class="info-item">
                  <div class="info-label">当前专业</div>
                  <div class="info-value">{{ currentSubject ? (currentSubject.name || currentSubject.professionName || '未选择') : '未选择' }}</div>
                </div>
              </div>
            </div>

            <div class="content-card">
              <div class="card-header">
                <span class="card-title">快捷操作</span>
              </div>
              <div class="quick-actions">
                <div class="action-item" @click="$router.push('/course/my-opened')">
                  <van-icon name="bookmark-o" size="28" color="#1989fa" />
                  <span>我的课程</span>
                </div>
                <div class="action-item" @click="activeView = 'my-exams'">
                  <van-icon name="notes-o" size="28" color="#ff976a" />
                  <span>我的考试</span>
                </div>
                <div class="action-item" @click="activeView = 'records'">
                  <van-icon name="records" size="28" color="#07c160" />
                  <span>考试记录</span>
                </div>
                <div class="action-item" @click="$router.push('/profile/wrong-questions')">
                  <van-icon name="warning-o" size="28" color="#ee0a24" />
                  <span>错题本</span>
                </div>
                <div class="action-item" @click="$router.push('/certificate')">
                  <van-icon name="medal-o" size="28" color="#7232dd" />
                  <span>证书查询</span>
                </div>
              </div>
            </div>
            </template>
            <!-- 我的考试 / 考试记录 在当前界面内嵌展示，不再跳转考试中心 -->
            <MyExams v-else-if="activeView === 'my-exams'" embedded />
            <Records v-else-if="activeView === 'records'" embedded />
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑个人资料弹窗(纯HTML实现,避免 van-dialog 的 _wrapper 错误) -->
    <div v-if="showProfileDialog" class="custom-modal-overlay" @click="closeProfileDialog">
      <div class="custom-modal" @click.stop>
        <div class="custom-modal-header">
          <span class="custom-modal-title">编辑个人资料</span>
          <span class="custom-modal-close" @click="closeProfileDialog">×</span>
        </div>
        <div class="custom-modal-body">
          <div class="custom-field">
            <label class="custom-field-label">昵称</label>
            <input v-model="editName" type="text" placeholder="请输入昵称" maxlength="20" class="custom-field-input" />
          </div>
          <div class="custom-field">
            <label class="custom-field-label">手机号</label>
            <input v-model="editPhone" type="tel" placeholder="请输入新手机号(不修改则留空)" maxlength="11" class="custom-field-input" />
          </div>
        </div>
        <div class="custom-modal-footer">
          <button type="button" class="custom-btn custom-btn-cancel" @click="closeProfileDialog">取 消</button>
          <button type="button" class="custom-btn custom-btn-confirm" @click="confirmEditProfile">确 定</button>
        </div>
      </div>
    </div>

    <!-- 修改密码弹窗(纯HTML实现,避免 van-dialog before-close 的 _wrapper 错误) -->
    <div v-if="showPasswordDialog" class="custom-modal-overlay" @click="closePasswordDialog">
      <div class="custom-modal" @click.stop>
        <div class="custom-modal-header">
          <span class="custom-modal-title">修改密码</span>
          <span class="custom-modal-close" @click="closePasswordDialog">×</span>
        </div>
        <div class="custom-modal-body">
          <div class="custom-field">
            <label class="custom-field-label">手机号</label>
            <input :value="formatPhone(userInfo.phone)" type="text" readonly class="custom-field-input custom-field-readonly" />
          </div>
          <div class="custom-field">
            <label class="custom-field-label">原密码</label>
            <input v-model="passwordForm.oldPassword" type="password" placeholder="请输入原密码" class="custom-field-input" />
          </div>
          <div class="custom-field">
            <label class="custom-field-label">新密码</label>
            <input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码（6-20位）" class="custom-field-input" />
          </div>
          <div class="custom-field">
            <label class="custom-field-label">确认密码</label>
            <input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" class="custom-field-input" />
          </div>
        </div>
        <div class="custom-modal-footer">
          <button type="button" class="custom-btn custom-btn-cancel" @click="closePasswordDialog">取 消</button>
          <button type="button" class="custom-btn custom-btn-confirm" @click="confirmChangePassword">确 定</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import Header from '@/components/Header.vue'
import MyExams from '@/views/exam/MyExams.vue'
import Records from '@/views/exam/Records.vue'
import { getProfile, updateProfile, uploadFile } from '@/api/profile'
import { changePassword } from '@/api/auth'
import { getMyCertificates } from '@/api/certificate'
import { resolveImg } from '@/utils/apiBase'
import { Toast, Dialog } from 'vant'

export default {
  name: 'ProfileIndex',
  components: { Header, MyExams, Records },
  data() {
    return {
      // 当前右侧内容区视图：profile(个人资料) / my-exams(我的考试) / records(考试记录)
      activeView: 'profile',
      userInfo: {},
      showProfileDialog: false,
      editName: '',
      editPhone: '',
      // 证书照片作为头像(有多个只取第一个)
      certAvatarUrl: '',
      showPasswordDialog: false,
      avatarFile: [],
      // 修改密码表单(已登录态, 必须输入原密码, 走 POST /auth/change-password)
      passwordForm: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      defaultAvatar: 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="80" height="80"><circle cx="40" cy="40" r="40" fill="#e0e0e0"/><circle cx="40" cy="30" r="12" fill="#fff"/><path d="M18 64 Q40 44 62 64 L62 80 L18 80 Z" fill="#fff"/></svg>')
    }
  },
  computed: {
    currentSubject() {
      return this.$store.getters.currentSubject
    }
  },
  created() {
    this.fetchProfile()
    this.fetchCertAvatar()
  },
  methods: {
    async fetchProfile() {
      try {
        const res = await getProfile()
        const data = res.data || res
        this.userInfo = data
        this.$store.dispatch('setUserInfo', data)
      } catch (error) {
        // 使用store中的信息
        this.userInfo = this.$store.getters.userInfo
        if (!this.userInfo.nickname) {
          this.userInfo = {
            nickname: '学员' + (this.userInfo.phone ? this.userInfo.phone.slice(-4) : ''),
            phone: this.userInfo.phone || '13800138000',
            avatar: ''
          }
        }
      }
    },
    formatPhone(phone) {
      if (!phone || phone.length < 11) return phone || '未绑定手机号'
      return phone.slice(0, 3) + '****' + phone.slice(7)
    },
    // ===== 编辑个人资料(昵称+手机号合并) =====
    handleEditProfile() {
      this.editName = this.userInfo.nickname || ''
      this.editPhone = ''
      this.showProfileDialog = true
    },
    closeProfileDialog() {
      this.showProfileDialog = false
    },
    async confirmEditProfile() {
      const nickname = this.editName.trim()
      const phone = this.editPhone.trim()
      if (!nickname) {
        Toast('昵称不能为空')
        return
      }
      if (phone && !/^1\d{10}$/.test(phone)) {
        Toast('手机号格式不正确')
        return
      }
      try {
        const payload = { nickname: nickname }
        if (phone && phone !== this.userInfo.phone) {
          payload.phone = phone
        }
        await updateProfile(payload)
        this.userInfo.nickname = nickname
        if (payload.phone) {
          this.userInfo.phone = payload.phone
        }
        this.$store.dispatch('setUserInfo', this.userInfo)
        this.showProfileDialog = false
        Toast.success('修改成功')
      } catch (error) {
        Toast.fail(error.message || '修改失败，请稍后重试')
      }
    },
    // ===== 证书照片作为头像 =====
    async fetchCertAvatar() {
      try {
        const res = await getMyCertificates()
        const data = res.data || res
        const list = Array.isArray(data) ? data : (data.records || [])
        // 找到第一张有照片的证书
        for (var i = 0; i < list.length; i++) {
          if (list[i].photoUrl) {
            this.certAvatarUrl = list[i].photoUrl
            break
          }
        }
      } catch (e) {
        // 获取失败时静默处理
      }
    },
    handleEditAvatar() {
      // 触发隐藏的 van-uploader 选择图片
      const uploader = this.$refs.avatarUploader
      if (uploader && uploader.$el) {
        const input = uploader.$el.querySelector('input[type="file"]')
        if (input) {
          input.value = ''
          input.click()
        }
      } else {
        Toast('头像上传功能不可用')
      }
    },
    async afterReadAvatar(fileItem) {
      const file = fileItem && fileItem.file ? fileItem.file : fileItem
      if (!file) return
      const formData = new FormData()
      formData.append('file', file)
      try {
        const res = await uploadFile(formData)
        const data = res.data || res
        const url = (data && (data.url || data.fileUrl)) || (typeof data === 'string' ? data : '')
        await updateProfile({ avatar: url })
        this.userInfo.avatar = url
        this.$store.dispatch('setUserInfo', this.userInfo)
        Toast.success('头像更新成功')
      } catch (error) {
        // 接口失败时使用本地预览
        const reader = new FileReader()
        reader.onload = (e) => {
          this.userInfo.avatar = e.target.result
          this.$store.dispatch('setUserInfo', this.userInfo)
        }
        reader.readAsDataURL(file)
        Toast.success('头像更新成功')
      } finally {
        this.avatarFile = []
      }
    },
    // ===== 修改密码(纯HTML弹窗,不再用 van-dialog before-close) =====
    closePasswordDialog() {
      this.showPasswordDialog = false
    },
    async confirmChangePassword() {
      const { oldPassword, newPassword, confirmPassword } = this.passwordForm
      if (!oldPassword) {
        Toast('请输入原密码')
        return
      }
      if (!newPassword || newPassword.length < 6 || newPassword.length > 20) {
        Toast('新密码长度为6-20位')
        return
      }
      if (newPassword !== confirmPassword) {
        Toast('两次输入的密码不一致')
        return
      }
      if (oldPassword === newPassword) {
        Toast('新密码不能与原密码相同')
        return
      }
      try {
        await changePassword({ oldPassword, newPassword })
        this.showPasswordDialog = false
        Toast.success('密码修改成功,请重新登录')
        // 清空表单
        this.passwordForm = { oldPassword: '', newPassword: '', confirmPassword: '' }
        // 改密后让用户重新登录(因为新密码需要重新生成 token)
        setTimeout(() => {
          this.$store.dispatch('logout')
          this.$router.replace('/login')
        }, 1500)
      } catch (error) {
        Toast.fail(error.message || '密码修改失败,请稍后重试')
      }
    },
    openPasswordDialog() {
      this.passwordForm = { oldPassword: '', newPassword: '', confirmPassword: '' }
      this.showPasswordDialog = true
    },
    handleLogout() {
      Dialog.confirm({
        title: '提示',
        message: '确认退出登录吗？',
        confirmButtonText: '退出',
        confirmButtonColor: '#ee0a24'
      }).then(() => {
        this.$store.dispatch('logout')
        Toast.success('已退出登录')
        this.$router.replace({ name: 'Login' }).catch(() => {})
      }).catch(() => {})
    },
    resolveImg
  }
}
</script>

<style lang="scss" scoped>
.profile-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.page-body {
  padding-top: var(--header-height, 170px);
}

.container {
  width: 80%;
  max-width: 1600px;
  margin: 0 auto;
  padding: 24px 20px;
}

.profile-layout {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.sidebar {
  flex: 0 0 280px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.content-area {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.user-card {
  display: flex;
  align-items: center;
  background: linear-gradient(135deg, #1989fa, #4ba7f7);
  padding: 24px 20px;
  color: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(25, 137, 250, 0.2);

  .user-avatar {
    position: relative;
    width: 64px;
    height: 64px;
    border-radius: 50%;
    overflow: hidden;
    border: 3px solid rgba(255, 255, 255, 0.5);
    flex-shrink: 0;
    cursor: pointer;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .avatar-edit {
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      height: 20px;
      background: rgba(0, 0, 0, 0.4);
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }

  .user-info {
    flex: 1;
    margin-left: 16px;
    min-width: 0;

    .user-name {
      font-size: 18px;
      font-weight: bold;
      margin-bottom: 4px;
      cursor: pointer;
    }

    .user-phone {
      font-size: 14px;
      opacity: 0.9;
      margin-bottom: 6px;
    }

    .user-subject {
      display: inline-flex;
      align-items: center;
      gap: 2px;
      font-size: 12px;
      background: rgba(255, 255, 255, 0.2);
      padding: 2px 8px;
      border-radius: 10px;
    }
  }
}

.menu-section {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);

  .menu-title {
    font-size: 13px;
    color: #999;
    padding: 14px 20px 6px;
  }

  .menu-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 14px 20px;
    cursor: pointer;
    transition: background 0.2s;

    &:hover {
      background: #f7f8fa;
    }

    /* 选中态：左侧蓝色高亮条 + 浅蓝背景 */
    &.active {
      background: #ecf5ff;
      position: relative;

      span {
        color: #1989fa;
        font-weight: 600;
      }

      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 0;
        bottom: 0;
        width: 3px;
        background: #1989fa;
      }
    }

    span {
      flex: 1;
      font-size: 15px;
      color: #333;
    }

    .arrow {
      color: #ccc;
    }

    &.logout {
      span {
        color: #ee0a24;
      }

      &:hover {
        background: #fef0f0;
      }
    }
  }
}

.content-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  overflow: hidden;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 18px 24px;
    border-bottom: 1px solid #f0f0f0;

    .card-title {
      font-size: 18px;
      font-weight: bold;
      color: #333;
    }

    .edit-btn-wrap {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      padding: 6px 12px;
      cursor: pointer;
      border: 1px solid #dcdfe6;
      border-radius: 4px;
      background: #fff;
      color: #1989fa;
      font-size: 14px;
      transition: all 0.2s;
      outline: none;

      &:hover {
        background: #f0f8ff;
        border-color: #1989fa;
      }

      &:active {
        background: #e0efff;
      }

      .edit-icon {
        flex-shrink: 0;
      }

      .edit-btn-text {
        font-size: 14px;
        color: #1989fa;
      }
    }
  }
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  padding: 24px;

  .info-item {
    .info-label {
      font-size: 13px;
      color: #999;
      margin-bottom: 8px;
    }

    .info-value {
      font-size: 15px;
      color: #333;
    }
  }
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  padding: 24px;

  .action-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 24px 12px;
    background: #f7f8fa;
    border-radius: 10px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      background: #f0f8ff;
      transform: translateY(-2px);
    }

    span {
      margin-top: 10px;
      font-size: 14px;
      color: #666;
    }
  }
}

@media (max-width: 992px) {
  .profile-layout {
    flex-direction: column;
  }

  .sidebar {
    flex: 1 1 100%;
    width: 100%;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .quick-actions {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* 移动端适配:768px 以下 */
@media (max-width: 768px) {
  .container { width: 100%; padding: 12px; }
  .quick-actions { grid-template-columns: repeat(3, 1fr); }
  .menu-section .menu-item { padding: 14px 12px; min-height: 48px; }
  .user-card { padding: 16px 12px; }
  .user-card .user-info .user-name { font-size: 16px; }
}

/* ===== 自定义弹窗(替代 van-dialog, 规避 _wrapper 错误) ===== */
.custom-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 3000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.custom-modal {
  background: #fff;
  border-radius: 12px;
  width: 90%;
  max-width: 420px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.custom-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;

  .custom-modal-title {
    font-size: 16px;
    font-weight: 600;
    color: #333;
  }

  .custom-modal-close {
    cursor: pointer;
    font-size: 24px;
    color: #999;
    line-height: 1;
    padding: 0 4px;

    &:hover {
      color: #333;
    }
  }
}

.custom-modal-body {
  padding: 20px;
}

.custom-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 16px;

  &:last-child {
    margin-bottom: 0;
  }

  .custom-field-label {
    font-size: 14px;
    color: #666;
    font-weight: 500;
  }

  .custom-field-input {
    height: 40px;
    padding: 0 12px;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    font-size: 14px;
    color: #333;
    outline: none;
    width: 100%;
    box-sizing: border-box;
    transition: border-color 0.2s;

    &:focus {
      border-color: #1989fa;
    }

    &.custom-field-readonly {
      background: #f5f5f5;
      color: #999;
    }
  }
}

.custom-modal-footer {
  display: flex;
  gap: 12px;
  padding: 12px 20px 20px;
  justify-content: flex-end;
}

.custom-btn {
  height: 36px;
  padding: 0 20px;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  outline: none;

  &.custom-btn-cancel {
    background: #fff;
    border: 1px solid #dcdfe6;
    color: #666;

    &:hover {
      border-color: #c0c4cc;
      color: #333;
    }
  }

  &.custom-btn-confirm {
    background: #1989fa;
    color: #fff;

    &:hover {
      background: #1577d8;
    }
  }
}
</style>
