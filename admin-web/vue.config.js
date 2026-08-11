'use strict'

const path = require('path')

function resolve(dir) {
  return path.join(__dirname, dir)
}

module.exports = {
  // 生产环境部署在 /admin/ 路径下(Nginx location /admin/)
  // 开发环境用相对路径
  publicPath: process.env.NODE_ENV === 'production' ? '/admin/' : './',
  outputDir: 'dist',
  assetsDir: 'static',
  lintOnSave: false,
  productionSourceMap: false,
  devServer: {
    port: 8081,
    open: true,
    client: {
      overlay: {
        warnings: false,
        errors: true,          // 保留编译错误遮罩
        runtimeErrors: false   // 关闭运行时 "Uncaught runtime errors" 遮罩,改由全局 errorHandler + Toast 友好提示
      }
    },
    proxy: {
      // 业务接口: /api/** 转到后端 8080
      // 注意: servlet context-path = /api,所以这里不要再 pathRewrite
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // 静态资源: /uploads/** 同样转到后端 8080
      // (后端 ResourceHandler 注册在 /uploads/** 和 /api/uploads/** 都能命中)
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  configureWebpack: {
    name: '中国人力资源专业技能人才评价中心管理后台',
    resolve: {
      alias: {
        '@': resolve('src')
      }
    },
    optimization: {
      splitChunks: {
        chunks: 'all',
        maxInitialRequests: 6,
        cacheGroups: {
          // Vue 全家桶
          vue: {
            name: 'chunk-vue',
            test: /[\\/]node_modules[\\/](vue|vue-router|vuex)[\\/]/,
            priority: 30
          },
          // Element UI (最大,单独拆分,浏览器缓存友好)
          element: {
            name: 'chunk-element',
            test: /[\\/]node_modules[\\/]element-ui[\\/]/,
            priority: 20
          },
          // 其他第三方库
          vendors: {
            name: 'chunk-vendors',
            test: /[\\/]node_modules[\\/]/,
            priority: 10
          }
        }
      }
    }
  }
}
