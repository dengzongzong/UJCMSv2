<template>
  <div class="register-page">
    <div class="register-card">
      <div class="card-header">
        <div class="logo">
          <img src="/images/1756976082837900.png" alt="中国人力资源专业技能人才评价中心" class="logo-img" />
        </div>
        <p class="subtitle">创建您的中国人力资源专业技能人才评价中心账号</p>
      </div>

      <van-form @submit="handleRegister" class="register-form">
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
          v-model="form.name"
          name="name"
          label="姓名"
          placeholder="请输入真实姓名"
          maxlength="20"
          :rules="[{ required: true, message: '请输入姓名' }]"
          left-icon="manager-o"
          clearable
        />

        <van-field
          v-model="form.idCard"
          name="idCard"
          label="身份证号"
          placeholder="请输入身份证号"
          :rules="[
            { required: true, message: '请输入身份证号' }
          ]"
          left-icon="credit-pay"
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
          v-model="form.nickname"
          name="nickname"
          label="昵称"
          placeholder="请输入昵称"
          maxlength="20"
          :rules="[{ required: true, message: '请输入昵称' }]"
          left-icon="manager-o"
          clearable
        />

        <van-field
          v-model="form.password"
          name="password"
          label="密码"
          placeholder="请输入密码（6-20位）"
          :type="showPassword ? 'text' : 'password'"
          :rules="[
            { required: true, message: '请输入密码' },
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
          placeholder="请再次输入密码"
          :type="showPassword ? 'text' : 'password'"
          :rules="[
            { required: true, message: '请确认密码' },
            { validator: validateConfirm, message: '两次输入的密码不一致' }
          ]"
          left-icon="lock"
        />

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
            loading-text="注册中..."
          >
            注册
          </van-button>
        </div>

        <div class="login-link">
          已有账号？
          <span class="link" @click="$router.push('/login')">立即登录</span>
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
        <p>欢迎使用中国人力资源专业技能人才评价中心。在使用本平台服务前，请您仔细阅读并同意以下协议：</p>
        <p>1. 本平台仅提供在线学习与考试服务，用户应遵守相关法律法规。</p>
        <p>2. 用户应保证注册信息真实有效，并对账号安全负责。</p>
        <p>3. 平台将保护用户隐私信息，不会向第三方泄露。</p>
        <p>4. 考试成绩仅供参考，最终以官方公布为准。</p>
      </div>
    </van-dialog>
  </div>
</template>

<script>
import { register, getCaptcha } from '@/api/auth'
import { Toast } from 'vant'

export default {
  name: 'Register',
  data() {
    return {
      form: {
        phone: '',
        name: '',
        idCard: '',
        captchaKey: '',
        captchaCode: '',
        nickname: '',
        password: '',
        confirmPassword: ''
      },
      showPassword: false,
      agreed: false,
      loading: false,
      showAgreement: false,
      captchaImage: ''
    }
  },
  mounted() {
    this.refreshCaptcha()
  },
  beforeDestroy() {},
  methods: {
    validateConfirm() {
      return this.form.password === this.form.confirmPassword
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
    async handleRegister() {
      if (!this.agreed) {
        Toast('请先阅读并同意用户协议和隐私政策')
        return
      }
      if (!this.form.captchaKey || !this.form.captchaCode) {
        Toast('请输入图形验证码')
        return
      }
      this.loading = true
      try {
        await register({
          phone: this.form.phone,
          name: this.form.name,
          idCard: this.form.idCard,
          captchaKey: this.form.captchaKey,
          captchaCode: this.form.captchaCode,
          nickname: this.form.nickname,
          password: this.form.password,
          confirmPassword: this.form.confirmPassword,
          agreement: this.agreed
        })
        Toast.success('注册成功,请登录')
        this.$router.replace('/login').catch(() => {})
      } catch (error) {
        Toast.fail(error.message || '注册失败,请稍后重试')
        // 失败后刷新验证码
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
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.register-card {
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

  .subtitle {
    font-size: 13px;
    color: #999;
  }
}

.register-form {
  .van-field {
    margin-bottom: 14px;
    border-radius: 8px;
    background: #f7f8fa;
  }
}

.form-options {
  display: flex;
  align-items: center;
  margin: 8px 0 24px;

  .van-checkbox {
    font-size: 13px;
    color: #666;
  }

  .link {
    color: #1989fa;
    cursor: pointer;
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
  margin-bottom: 20px;

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
  .register-card {
    padding: 28px 24px;
  }

  .card-header .title {
    font-size: 20px;
  }
}
</style>
