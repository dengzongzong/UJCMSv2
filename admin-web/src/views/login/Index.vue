<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-banner">
        <div class="banner-content">
          <i class="el-icon-school banner-icon"></i>
          <h1 class="banner-title">人力资源专业技能人才评价网</h1>
          <p class="banner-subtitle">管理后台</p>
          <p class="banner-desc">一站式职业能力评测管理解决方案</p>
        </div>
      </div>
      <div class="login-form-wrapper">
        <div class="login-header">
          <h2>管理员登录</h2>
          <p>欢迎回来，请输入您的账号信息</p>
        </div>
        <el-form
          ref="loginForm"
          :model="loginForm"
          class="login-form"
          auto-complete="off"
          label-position="left"
          @submit.native.prevent="handleLogin"
        >
          <el-form-item>
            <el-input
              ref="usernameInput"
              v-model="loginForm.username"
              placeholder="请输入用户名"
              prefix-icon="el-icon-user"
              name="login-username"
              type="text"
              autocomplete="new-username"
              readonly
              onfocus="this.removeAttribute('readonly')"
              clearable
            />
          </el-form-item>
          <el-form-item>
            <el-input
              ref="passwordInput"
              v-model="loginForm.password"
              :type="passwordVisible ? 'text' : 'password'"
              placeholder="请输入密码(默认 123456)"
              prefix-icon="el-icon-lock"
              name="login-password"
              autocomplete="new-password"
              readonly
              onfocus="this.removeAttribute('readonly')"
              clearable
              @keyup.enter.native="handleLogin"
            >
              <i
                slot="suffix"
                :class="passwordVisible ? 'el-icon-view' : 'el-icon-minus'"
                class="password-toggle"
                @click="passwordVisible = !passwordVisible"
              ></i>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              :loading="loading"
              class="login-btn"
              @click.native.prevent="handleLogin"
            >
              登 录
            </el-button>
          </el-form-item>
          <div class="login-tip">
            默认密码：<b>123456</b>
          </div>
        </el-form>
      </div>
    </div>
    <div class="login-footer">
      <div>Copyright &copy; {{ year }} 人力资源专业技能人才评价网. All Rights Reserved.</div>
      <div class="beian">
        <a href="https://beian.mps.gov.cn/#/query/webSearch" target="_blank" rel="noopener">
          <img src="https://beian.mps.gov.cn/img/logo01.png" alt="公安备案" class="beian-icon" />
          冀公网安备 13068402000386 号
        </a>
        <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener">
          冀ICP备2025108945号-4
        </a>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'LoginIndex',
  data() {
    return {
      loginForm: {
        username: '',
        password: '123456'
      },
      passwordVisible: false,
      loading: false,
      year: new Date().getFullYear()
    }
  },
  // 进入登录页时清空表单, 防止:
  //   1) 浏览器密码管理器/自动填充残留
  //   2) 上次登录失败后的值仍在
  //   3) 路由切换时 v-model 状态污染
  beforeRouteEnter(to, from, next) {
    next((vm) => {
      vm.loginForm.username = ''
      // 密码默认值 123456(进入登录页时恢复默认值)
      vm.loginForm.password = '123456'
      vm.passwordVisible = false
      // 关键: 主动重置原生 input 的值
      //   避免浏览器把"自动填充"的内容填到 DOM 上但 Vue 不知道
      //   (尤其是 Safari/Edge 的密码管理器)
      if (vm.$refs.usernameInput) {
        const el = vm.$refs.usernameInput.$el && vm.$refs.usernameInput.$el.querySelector('input')
        if (el) el.value = ''
      }
      if (vm.$refs.passwordInput) {
        const el = vm.$refs.passwordInput.$el && vm.$refs.passwordInput.$el.querySelector('input')
        if (el) el.value = '123456'
      }
    })
  },
  // 离开登录页时也清空(防止用户从其他页面跳回登录页时数据残留)
  beforeRouteLeave(to, from, next) {
    this.loginForm.username = ''
    this.loginForm.password = '123456'
    this.passwordVisible = false
    next()
  },
  mounted() {
    // 防御: mounted 时再次设置默认密码, 防止浏览器在 mount 之后才完成自动填充
    this.$nextTick(() => {
      this.loginForm.username = ''
      this.loginForm.password = '123456'
    })
  },
  methods: {
    handleLogin() {
      if (!this.loginForm.username || !this.loginForm.password) {
        this.$message.error('请输入用户名和密码')
        return
      }
      this.loading = true
      this.$store
        .dispatch('admin/login', {
          username: this.loginForm.username,
          password: this.loginForm.password
        })
        .then(() => {
          this.$message.success('登录成功')
          // 登录成功后立刻清空表单(防止用户 F5/后退到登录页时数据残留)
          this.loginForm.username = ''
          this.loginForm.password = '123456'
          const redirect = this.$route.query.redirect || '/'
          this.$router.push(redirect).catch(() => {})
        })
        .catch(() => {
          // 登录失败保留输入, 方便用户修改; 但点叉时应能清空
        })
        .finally(() => {
          this.loading = false
        })
    }
  }
}
</script>

<style lang="scss" scoped>
.login-container {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #1f2d3d 0%, #324157 50%, #1f2d3d 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
}

.login-box {
  width: 880px;
  max-width: 92%;
  height: 600px;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.25);
  display: flex;
}

.login-banner {
  flex: 1;
  background: linear-gradient(135deg, #409eff 0%, #1d6fd0 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 40px;

  .banner-content {
    text-align: center;

    .banner-icon {
      font-size: 80px;
      margin-bottom: 24px;
    }

    .banner-title {
      font-size: 42px;
      font-weight: 600;
      margin-bottom: 16px;
      line-height: 1.4;
    }

    .banner-subtitle {
      font-size: 24px;
      opacity: 0.9;
      margin-bottom: 32px;
    }

    .banner-desc {
      font-size: 18px;
      opacity: 0.8;
      line-height: 1.8;
    }
  }
}

.login-form-wrapper {
  width: 380px;
  padding: 48px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;

  .login-header {
    margin-bottom: 28px;

    h2 {
      font-size: 22px;
      color: #303133;
      margin-bottom: 8px;
    }

    p {
      font-size: 13px;
      color: #909399;
    }
  }
}

.login-form {
  .login-btn {
    width: 100%;
    height: 40px;
    font-size: 15px;
  }

  .password-toggle {
    cursor: pointer;
    color: #909399;
    margin-right: 6px;
  }

  .login-tip {
    margin-top: 8px;
    font-size: 12px;
    color: #909399;
    text-align: center;
    line-height: 1.8;

    b {
      color: #409eff;
    }
  }
}

.login-footer {
  position: absolute;
  bottom: 16px;
  color: rgba(255, 255, 255, 0.6);
  font-size: 12px;
  text-align: center;

  .beian {
    margin-top: 6px;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;

    a {
      color: rgba(255, 255, 255, 0.6);
      text-decoration: none;
      display: inline-flex;
      align-items: center;
      gap: 4px;

      &:hover {
        color: rgba(255, 255, 255, 0.9);
      }
    }

    .beian-icon {
      width: 14px;
      height: 14px;
      vertical-align: middle;
    }
  }
}

@media (max-width: 768px) {
  .login-box {
    flex-direction: column;
    height: auto;

    .login-banner {
      display: none;
    }

    .login-form-wrapper {
      width: 100%;
      padding: 32px 24px;
    }
  }
}
</style>
