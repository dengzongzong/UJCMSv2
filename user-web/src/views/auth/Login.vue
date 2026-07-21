<template>
  <div class="login-page">
    <div class="login-card">
      <div class="card-header">
        <div class="logo">
          <img src="/images/1756976082837900.png" alt="人力资源专业技能人才评价网" class="logo-img" />
        </div>
        <h1 class="title">人力资源专业技能人才评价网</h1>
        <p class="title-en">International Professional Competency Standard Talent Evaluation Network</p>
        <p class="subtitle">欢迎回来，请登录您的账号</p>
      </div>

      <van-form @submit="handleLogin" class="login-form">
        <!-- 登录方式切换: 手机号 / 身份证号 -->
        <div class="login-type-tabs">
          <div
            class="tab-item"
            :class="{ active: loginType === 'phone' }"
            @click="switchLoginType('phone')"
          >
            手机号登录
          </div>
          <div
            class="tab-item"
            :class="{ active: loginType === 'idCard' }"
            @click="switchLoginType('idCard')"
          >
            身份证号登录
          </div>
        </div>

        <van-field
          v-model="form.phone"
          name="login_phone_field"
          :label="loginType === 'phone' ? '手机号' : '身份证号'"
          :placeholder="loginType === 'phone' ? '请输入手机号' : '请输入身份证号'"
          :type="loginType === 'phone' ? 'tel' : 'text'"
          :maxlength="loginType === 'phone' ? 11 : 32"
          :left-icon="loginType === 'phone' ? 'phone-o' : 'credit-pay'"
          clearable
          autocomplete="new-username"
        />
        <div v-if="loginType === 'idCard'" class="field-hint">请输入身份证号</div>

        <van-field
          v-model="form.password"
          name="login_password_field"
          label="密码"
          placeholder="请输入密码(默认 123456)"
          :type="showPassword ? 'text' : 'password'"
          left-icon="lock"
          :right-icon="showPassword ? 'eye-o' : 'closed-eye'"
          @click-right-icon="showPassword = !showPassword"
          autocomplete="new-password"
        />
        <div class="field-hint">默认密码为 123456,如未修改请直接登录</div>

        <div class="form-options">
          <van-checkbox v-model="agreed" shape="square" icon-size="16">
            我已阅读并同意
            <span class="link" @click.stop="showAgreement = true">《用户协议》</span>
            和
            <span class="link" @click.stop="showAgreement = true">《隐私政策》</span>
          </van-checkbox>
        </div>

        <div class="submit-btn-wrapper">
          <van-button
            round
            block
            type="primary"
            native-type="submit"
            :loading="loading"
            loading-text="登录中..."
          >
            登录
          </van-button>
        </div>

        <div class="register-link">
          还没有账号？
          <span class="link" @click="$router.push('/register')">立即注册</span>
        </div>
      </van-form>
    </div>

    <!-- 用户协议弹窗 -->
    <van-dialog
      v-model="showAgreement"
      title="用户协议与隐私政策"
      confirm-button-text="我知道了"
      confirm-button-color="#1989fa"
    >
      <div class="agreement-content">
        <p>欢迎使用人力资源专业技能人才评价网。在使用本平台服务前，请您仔细阅读并同意以下协议：</p>
        <p>1. 本平台仅提供在线学习与考试服务，用户应遵守相关法律法规。</p>
        <p>2. 用户应保证注册信息真实有效，并对账号安全负责。</p>
        <p>3. 平台将保护用户隐私信息，不会向第三方泄露。</p>
        <p>4. 考试成绩仅供参考，最终以官方公布为准。</p>
      </div>
    </van-dialog>
  </div>
</template>

<script>
import { login } from '@/api/auth'
import { Toast } from 'vant'

export default {
  name: 'Login',
  // 路由进入前就清空原生 input.value,避开浏览器 autofill
  beforeRouteEnter(to, from, next) {
    next(vm => {
      vm.resetForm()
      vm.clearNativeInputs()
    })
  },
  beforeRouteUpdate(to, from, next) {
    this.resetForm()
    this.clearNativeInputs()
    next()
  },
  data() {
    return {
      form: {
        phone: '',
        password: '123456'
      },
      // 登录方式: 'phone'(手机号) 或 'idCard'(身份证号)
      loginType: 'phone',
      showPassword: false,
      agreed: false,
      loading: false,
      showAgreement: false
    }
  },
  methods: {
    // 强制重置 Vue 表单数据
    resetForm() {
      this.form.phone = ''
      // 密码默认值 123456
      this.form.password = '123456'
      this.loginType = 'phone'
      this.agreed = false
    },
    // 切换登录方式时清空已输入的账号,避免不同方式残留值
    switchLoginType(type) {
      if (this.loginType === type) return
      this.loginType = type
      this.form.phone = ''
    },
    // 强制清空原生 input.value(对抗浏览器密码管理器)
    clearNativeInputs() {
      this.$nextTick(() => {
        const root = this.$el
        if (!root) return
        const inputs = root.querySelectorAll('input')
        inputs.forEach((el) => {
          // 只清可见 input(不影响 Vant 内部隐藏控件)
          if (el.type === 'hidden') return
          // 密码字段保留默认值 123456,其余字段清空
          if (el.name === 'login_password_field') {
            el.value = '123456'
          } else {
            el.value = ''
          }
          // 触发 input 事件,让 v-model 同步
          try {
            el.dispatchEvent(new Event('input', { bubbles: true }))
            el.dispatchEvent(new Event('change', { bubbles: true }))
          } catch (e) { /* ignore */ }
        })
      })
    },
    redirectAfterLogin(userInfo) {
      const redirect = this.$route.query.redirect || '/home'
      const safeRedirect = redirect.startsWith('/login') ? '/home' : redirect
      const role = userInfo && userInfo.role
      if (role === 'student') {
        const profId = userInfo.professionId
        if (profId != null) {
          const currentSubject = {
            id: profId,
            professionId: profId,
            name: userInfo.professionName || '',
            professionName: userInfo.professionName || ''
          }
          this.$store.dispatch('setCurrentSubject', currentSubject)
          this.$router.replace(safeRedirect).catch(() => {})
        } else {
          this.$router.replace({ name: 'ChooseSubject', query: { redirect: safeRedirect } }).catch(() => {})
        }
        return
      }
      this.$router.replace(safeRedirect).catch(() => {})
    },
    async handleLogin() {
      if (!this.agreed) {
        Toast('请先阅读并同意用户协议和隐私政策')
        return
      }
      const username = (this.form.phone || '').trim()
      if (!username) {
        Toast(this.loginType === 'phone' ? '请输入手机号' : '请输入身份证号')
        return
      }
      this.loading = true
      try {
        const res = await login({
          username: username,
          password: this.form.password,
          role: 'student',
          agreement: this.agreed,
          loginType: this.loginType
        })
        const data = res.data || res
        const token = (data && (data.token || data.accessToken)) || (typeof data === 'string' ? data : '')
        if (!token) {
          throw new Error('登录失败：未获取到token')
        }
        const userInfo = data || {}
        this.$store.dispatch('login', { token, userInfo })
        Toast.success('登录成功')
        this.redirectAfterLogin(userInfo)
      } catch (error) {
        Toast.fail((error && error.message) || '登录失败，请检查账号和密码')
      } finally {
        this.loading = false
      }
    }
  },
  mounted() {
    // 进入登录页时主动清空,避免浏览器/密码管理器残留的 "sa" 等
    this.resetForm()
    // mounted 之后再清一次,覆盖那些在 mounted 之后才完成 autofill 的浏览器
    this.clearNativeInputs()
    this.$nextTick(() => this.clearNativeInputs())
  },
  activated() {
    // 路由 keep-alive 缓存场景下再次进入
    this.resetForm()
    this.clearNativeInputs()
  }
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-card {
  width: 360px;
  max-width: 100%;
  background: #fff;
  border-radius: 12px;
  padding: 28px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.card-header {
  text-align: center;
  margin-bottom: 20px;
  padding: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;

  .logo {
    width: 100%;
    max-width: 360px;
    height: auto;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 14px;
  }

  .logo-img {
    width: 100%;
    height: auto;
    object-fit: contain;
  }

  .title {
    font-size: 20px;
    font-weight: bold;
    color: #333;
    margin-bottom: 4px;
    line-height: 1.4;
  }

  .title-en {
    font-size: 11px;
    color: #999;
    font-weight: 300;
    letter-spacing: 1px;
    margin-bottom: 6px;
  }

  .subtitle {
    font-size: 13px;
    color: #999;
  }
}

.login-form {
  .van-field {
    margin-bottom: 16px;
    border-radius: 8px;
    background: #f7f8fa;
  }

  // 错误提示不换行，避免高度跳动
  ::v-deep .van-field__error-message {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.login-type-tabs {
  display: flex;
  margin-bottom: 16px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #dcdee0;

  .tab-item {
    flex: 1;
    text-align: center;
    padding: 10px 0;
    font-size: 14px;
    color: #646566;
    background: #f7f8fa;
    cursor: pointer;
    transition: all 0.2s;

    &.active {
      color: #fff;
      background: #1989fa;
      font-weight: 500;
    }
  }
}

.field-hint {
  margin: -8px 0 16px 12px;
  font-size: 12px;
  color: #909399;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 8px 0 24px;
  flex-wrap: wrap;
  gap: 8px;

  .van-checkbox {
    font-size: 13px;
    color: #666;
    flex: 1 1 100%;
    min-width: 0;

    ::v-deep .van-checkbox__label {
      white-space: nowrap;
      margin-left: 8px;
    }
  }

  .link {
    color: #1989fa;
    cursor: pointer;
  }
}

.submit-btn-wrapper {
  margin-bottom: 20px;

  .van-button {
    height: 46px;
    font-size: 16px;
    font-weight: 500;
  }
}

.register-link {
  text-align: center;
  font-size: 14px;
  color: #666;

  .link {
    color: #1989fa;
    cursor: pointer;
    font-weight: 500;

    &:hover {
      opacity: 0.8;
    }
  }
}

.agreement-content {
  padding: 16px 20px;
  max-height: 300px;
  overflow-y: auto;

  p {
    font-size: 13px;
    color: #666;
    line-height: 1.8;
    margin-bottom: 8px;
  }
}

@media (max-width: 480px) {
  .login-card {
    padding: 22px 18px;
  }
}
</style>
