<template>
  <div class="forgot-page">
    <div class="forgot-card">
      <div class="card-header">
        <div class="logo">
          <van-icon name="lock" size="36" color="#1989fa" />
        </div>
        <h1 class="title">找回密码</h1>
        <p class="subtitle">通过手机验证码重置您的密码</p>
      </div>

      <van-form @submit="handleReset" class="forgot-form">
        <van-field
          v-model="form.phone"
          name="phone"
          label="手机号"
          placeholder="请输入手机号"
          type="tel"
          maxlength="11"
          :rules="[
            { required: true, message: '请输入手机号' },
            { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }
          ]"
          left-icon="phone-o"
          clearable
        />

        <van-field
          v-model="form.captchaCode"
          name="captchaCode"
          label="验证码"
          placeholder="请输入右侧图形验证码"
          type="text"
          maxlength="6"
          :rules="[
            { required: true, message: '请输入图形验证码' }
          ]"
          left-icon="shield-o"
        >
          <template #button>
            <div class="captcha-box" @click="refreshCaptcha">
              <img v-if="captchaImage" :src="captchaImage" class="captcha-img" alt="点击刷新" />
              <span v-else class="captcha-loading">加载中...</span>
            </div>
          </template>
        </van-field>

        <van-field
          v-model="form.newPassword"
          name="newPassword"
          label="新密码"
          placeholder="请输入新密码（6-20位）"
          :type="showPassword ? 'text' : 'password'"
          :rules="[
            { required: true, message: '请输入新密码' },
            { pattern: /^.{6,20}$/, message: '密码长度为6-20位' }
          ]"
          left-icon="lock"
          :right-icon="showPassword ? 'eye-o' : 'closed-eye'"
          @click-right-icon="showPassword = !showPassword"
        />

        <van-field
          v-model="form.confirmPassword"
          name="confirmPassword"
          label="确认密码"
          placeholder="请再次输入新密码"
          :type="showPassword ? 'text' : 'password'"
          :rules="[
            { required: true, message: '请确认密码' },
            { validator: validateConfirm, message: '两次输入的密码不一致' }
          ]"
          left-icon="lock"
        />

        <div class="submit-btn-wrapper">
          <van-button
            round
            block
            type="primary"
            native-type="submit"
            :loading="loading"
            loading-text="提交中..."
          >
            重置密码
          </van-button>
        </div>

        <div class="login-link">
          想起密码了？
          <span class="link" @click="$router.push('/login')">返回登录</span>
        </div>
      </van-form>
    </div>
  </div>
</template>

<script>
import { getCaptcha, resetPassword } from '@/api/auth'
import { Toast } from 'vant'

export default {
  name: 'ForgotPassword',
  data() {
    return {
      form: {
        phone: '',
        captchaKey: '',
        captchaCode: '',
        newPassword: '',
        confirmPassword: ''
      },
      showPassword: false,
      loading: false,
      captchaImage: ''
    }
  },
  mounted() {
    this.refreshCaptcha()
  },
  methods: {
    validateConfirm() {
      return this.form.newPassword === this.form.confirmPassword
    },
    async refreshCaptcha() {
      try {
        const res = await getCaptcha()
        const data = res.data || res
        // 后端 captcha/generate 返回的字段名已改为 captchaKey
        this.form.captchaKey = data.captchaKey || data.captchaId
        this.captchaImage = 'data:image/png;base64,' + data.imageBase64
      } catch (e) {
        Toast('图形验证码加载失败,请重试')
      }
    },
    async handleReset() {
      if (!this.form.captchaKey || !this.form.captchaCode) {
        Toast('请输入图形验证码')
        return
      }
      this.loading = true
      try {
        await resetPassword({
          phone: this.form.phone,
          captchaKey: this.form.captchaKey,
          captchaCode: this.form.captchaCode,
          newPassword: this.form.newPassword
        })
        Toast.success('重置成功,请用新密码登录')
        this.$router.replace('/login').catch(() => {})
      } catch (error) {
        Toast.fail(error.message || '重置失败,请稍后重试')
        this.refreshCaptcha()
        this.form.captchaCode = ''
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.forgot-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.forgot-card {
  width: 400px;
  max-width: 100%;
  background: #fff;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.card-header {
  text-align: center;
  margin-bottom: 28px;

  .logo {
    width: 72px;
    height: 72px;
    border-radius: 18px;
    background: linear-gradient(135deg, #f0f8ff, #e6f4ff);
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 16px;
  }

  .title {
    font-size: 24px;
    font-weight: bold;
    color: #333;
    margin-bottom: 8px;
  }

  .subtitle {
    font-size: 14px;
    color: #999;
  }
}

.forgot-form {
  .van-field {
    margin-bottom: 14px;
    border-radius: 8px;
    background: #f7f8fa;
  }
}

.captcha-box {
  width: 110px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  overflow: hidden;
  background: #fff;
  margin: -8px 0;
}
.captcha-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.captcha-loading {
  font-size: 12px;
  color: #999;
}

.submit-btn-wrapper {
  margin: 24px 0 20px;

  .van-button {
    height: 46px;
    font-size: 16px;
    font-weight: 500;
  }
}

.login-link {
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

@media (max-width: 480px) {
  .forgot-card {
    padding: 28px 24px;
  }

  .card-header .title {
    font-size: 20px;
  }
}
</style>
