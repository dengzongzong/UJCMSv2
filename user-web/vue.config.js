const path = require('path')

module.exports = {
  publicPath: '/',
  productionSourceMap: false,
  devServer: {
    host: '0.0.0.0',
    port: 8082,
    open: true,
    client: {
      overlay: {
        warnings: false,
        errors: true,          // 保留编译错误遮罩
        runtimeErrors: false   // 关闭运行时 "Uncaught runtime errors" 遮罩
      }
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/public': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // 静态资源: 兼容 baseURL 被错误配置成空时的 fallback
      // (前端已用 resolveImg 拼 /api/uploads/xxx,这里只是双保险,后端
      //  WebMvcConfig 同时注册了 /uploads/** 和 /api/uploads/**)
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  configureWebpack: {
    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src')
      }
    }
  },
  chainWebpack: config => {
    config.plugin('html').tap(args => {
      args[0].title = '人力资源专业技能人才评价网'
      return args
    })
  }
}
