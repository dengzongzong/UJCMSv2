<template>
  <el-container class="layout-container">
    <el-aside :width="collapsed ? '64px' : '220px'" class="layout-aside">
      <div class="logo-container">
        <i class="el-icon-school logo-icon"></i>
        <span v-if="!collapsed" class="logo-text">中国人力资源专业技能人才评价中心</span>
      </div>
      <el-scrollbar class="menu-scroll">
        <el-menu
          :default-active="activeMenu"
          :collapse="collapsed"
          :collapse-transition="false"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
          router
          unique-opened
        >
          <template v-for="route in menuRoutes">
            <!-- 单项菜单 -->
            <el-menu-item
              v-if="route.children.length === 1 && !route.meta.alwaysShow"
              :key="route.path"
              :index="resolvePath(route, route.children[0])"
            >
              <i :class="route.meta.icon"></i>
              <span slot="title">{{ route.children[0].meta.title || route.meta.title }}</span>
            </el-menu-item>

            <!-- 子菜单(只有 1 个可见子项时折叠成单条,这里按"有多子项"才展示 submenu) -->
            <el-submenu
              v-else
              :key="route.path"
              :index="resolvePath(route, route.children[0])"
            >
              <template slot="title">
                <i :class="route.meta.icon"></i>
                <span>{{ route.meta.title }}</span>
              </template>
              <el-menu-item
                v-for="child in route.children"
                :key="child.path"
                :index="resolvePath(route, child)"
              >
                {{ child.meta.title }}
              </el-menu-item>
            </el-submenu>
          </template>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container>
      <el-header class="layout-header" height="56px">
        <div class="header-left">
          <i
            :class="collapsed ? 'el-icon-s-unfold' : 'el-icon-s-fold'"
            class="collapse-btn"
            @click="toggleSidebar"
          ></i>
          <el-breadcrumb separator="/" class="breadcrumb">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="admin-info">
              <el-avatar :size="32" icon="el-icon-user-solid" class="admin-avatar"></el-avatar>
              <span class="admin-name">{{ adminName }}</span>
              <i class="el-icon-arrow-down"></i>
            </span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="dashboard">
                <i class="el-icon-s-home"></i> 仪表盘
              </el-dropdown-item>
              <el-dropdown-item command="password">
                <i class="el-icon-key"></i> 修改密码
              </el-dropdown-item>
              <el-dropdown-item command="logout" divided>
                <i class="el-icon-switch-button"></i> 退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="layout-main">
        <router-view v-if="!$route.meta.keepAlive" :key="$route.fullPath" />
        <keep-alive>
          <router-view v-if="$route.meta.keepAlive" :key="$route.fullPath" />
        </keep-alive>
      </el-main>
    </el-container>

    

    <el-dialog title="修改密码" :visible.sync="pwdDialogVisible" width="420px" append-to-body>
      <el-form ref="pwdForm" :model="pwdForm" :rules="pwdRules" label-width="90px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="pwdDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="submitPwd">确 定</el-button>
      </div>
    </el-dialog>
  </el-container>
</template>

<script>
import { mapGetters } from 'vuex'
import { MessageBox } from 'element-ui'
import { constantRoutes } from '@/router'
import { changePassword } from '@/api/auth'
import TaskCenter from '@/components/TaskCenter'

// 把路由的相对子路径拼成绝对路径,用于 el-menu 的 index
function resolvePath(parent, child) {
  // 如果 child.path 是以 / 开头的,直接使用;否则拼接
  if (!child || !child.path) return parent.path || '/'
  return child.path.startsWith('/') ? child.path : `${parent.path}/${child.path}`.replace(/\/+/g, '/')
}

export default {
  name: 'LayoutIndex',
  components: { TaskCenter },
  data() {
    const validateConfirm = (rule, value, callback) => {
      if (value !== this.pwdForm.newPassword) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    return {
      pwdDialogVisible: false,
      pwdLoading: false,
      pwdForm: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      pwdRules: {
        oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 6, max: 20, message: '密码长度为6-20位', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请再次输入新密码', trigger: 'blur' },
          { validator: validateConfirm, trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    ...mapGetters(['sidebarCollapsed', 'adminInfo', 'certTypes']),
    collapsed() {
      return this.sidebarCollapsed
    },
    // 自动从路由表生成菜单:仅保留有 meta.title/icon 的父路由,排除 hidden 子项
    // 证书类型子菜单动态生成,依赖 certTypes 触发响应式更新
    menuRoutes() {
      // 引用 certTypes 使其成为响应式依赖
      const _ = this.certTypes
      const routes = this.$router.options.routes
      return routes
        .filter((r) => r.meta && r.meta.title && r.children && r.children.length)
        .map((r) => {
          if (r.path === '/certificate') {
            // 证书管理:合并静态子路由 + 动态添加的子路由
            const staticChildren = r.children.filter((c) => !c.hidden)
            const dynamicChildren = (this.$router.getRoutes ? this.$router.getRoutes() : [])
              .filter((rr) => rr.name && rr.name.startsWith('CertificateListDyn') && rr.meta)
              .map((rr) => ({ path: rr.path.replace('/certificate/', ''), name: rr.name, meta: rr.meta }))
            return { ...r, children: [...staticChildren, ...dynamicChildren] }
          }
          return { ...r, children: r.children.filter((c) => !c.hidden) }
        })
        .filter((r) => r.children.length)
    },
    activeMenu() {
      const route = this.$route
      const { meta, path } = route
      if (meta && meta.activeMenu) {
        return meta.activeMenu
      }
      return path
    },
    currentTitle() {
      return this.$route.meta && this.$route.meta.title ? this.$route.meta.title : ''
    },
    adminName() {
      if (this.adminInfo) {
        return this.adminInfo.username || this.adminInfo.account || '管理员'
      }
      return '管理员'
    }
  },
  methods: {
    resolvePath,
    toggleSidebar() {
      this.$store.dispatch('app/toggleSidebar')
    },
    handleCommand(command) {
      if (command === 'logout') {
        this.handleLogout()
      } else if (command === 'dashboard') {
        this.$router.push('/dashboard').catch(() => {})
      } else if (command === 'password') {
        this.openPwdDialog()
      }
    },
    handleLogout() {
      MessageBox.confirm('确定要退出登录吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(() => {
          this.$store.dispatch('admin/logout').then(() => {
            this.$router.push('/login').catch(() => {})
          })
        })
        .catch(() => {})
    },
    openPwdDialog() {
      this.pwdForm = {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      }
      this.pwdDialogVisible = true
      this.$nextTick(() => {
        this.$refs.pwdForm && this.$refs.pwdForm.clearValidate()
      })
    },
    submitPwd() {
      this.$refs.pwdForm.validate((valid) => {
        if (!valid) return
        this.pwdLoading = true
        changePassword({
          oldPassword: this.pwdForm.oldPassword,
          newPassword: this.pwdForm.newPassword
        })
          .then(() => {
            this.$message.success('密码修改成功，请重新登录')
            this.pwdDialogVisible = false
            this.pwdLoading = false
            this.$store.dispatch('admin/logout').then(() => {
              this.$router.push('/login').catch(() => {})
            })
          })
          .catch(() => {
            this.pwdLoading = false
          })
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.layout-container {
  height: 100%;
}

.layout-aside {
  background-color: #304156;
  transition: width 0.28s;
  overflow: hidden;

  .logo-container {
    height: 56px;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #2b3a4d;
    color: #fff;
    overflow: hidden;

    .logo-icon {
      font-size: 24px;
      color: #409eff;
    }

    .logo-text {
      margin-left: 10px;
      font-size: 16px;
      font-weight: 600;
      white-space: nowrap;
    }
  }

  .menu-scroll {
    height: calc(100% - 56px);

    ::v-deep .el-scrollbar__wrap {
      overflow-x: hidden;
    }
  }

  ::v-deep .el-menu {
    border-right: none;
  }
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #fff;
  border-bottom: 1px solid #f0f0f0;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  padding: 0 20px;

  .header-left {
    display: flex;
    align-items: center;

    .collapse-btn {
      font-size: 20px;
      cursor: pointer;
      margin-right: 16px;
      color: #5a5e66;

      &:hover {
        color: #409eff;
      }
    }

    .breadcrumb {
      line-height: 56px;
    }
  }

  .header-right {
    .admin-info {
      display: flex;
      align-items: center;
      cursor: pointer;
      color: #5a5e66;

      .admin-avatar {
        background-color: #409eff;
      }

      .admin-name {
        margin: 0 6px 0 8px;
        font-size: 14px;
      }
    }
  }
}

.layout-main {
  background-color: #f0f2f5;
  padding: 16px;
  overflow: auto;
}

.layout-footer {
  background-color: #fff;
  border-top: 1px solid #f0f0f0;
  padding: 12px 20px;
  text-align: center;
  font-size: 12px;
  color: #909399;

  .beian {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;

    a {
      color: #909399;
      text-decoration: none;
      display: inline-flex;
      align-items: center;
      gap: 4px;

      &:hover {
        color: #409eff;
      }
    }

    .beian-icon {
      width: 14px;
      height: 14px;
      vertical-align: middle;
    }
  }
}
</style>
