module.exports = {
  // 在 jsdom(浏览器)环境跑,组件/路由都依赖 DOM
  testEnvironment: 'jsdom',

  // 测试文件匹配
  testMatch: [
    '<rootDir>/tests/unit/**/*.spec.js'
  ],

  // 转换器:Vue SFC 用 @vue/vue2-jest (支持 babel 7 + 完整 ESNext)
  transform: {
    '^.+\\.vue$': '@vue/vue2-jest',
    '^.+\\.js$': 'babel-jest'
  },

  // 不想被 jest 报告的文件
  modulePathIgnorePatterns: [
    '<rootDir>/node_modules',
    '<rootDir>/dist'
  ],

  // CSS/SCSS 假装导入(测试不需要)
  moduleNameMapper: {
    '\\.(css|less|scss|sass)$': 'identity-obj-proxy',
    '\\.(png|jpg|jpeg|gif|svg|webp)$': 'jest-transform-stub',
    // @ 别名(对应 jsconfig.json paths)
    '^@/(.*)$': '<rootDir>/src/$1',
    // 这些是项目里 webpack alias / context,测试中不需要
    '^@/icons(.*)$': '<rootDir>/tests/__mocks__/iconsStub.js',
    '^vuex-persistedstate$': '<rootDir>/tests/__mocks__/noop.js',
    '^@/styles/(.*)\\.(css|scss|sass)$': '<rootDir>/tests/__mocks__/styleStub.js'
  },

  // 给 store/router 等需要模块解析的,使用 node_modules 别名
  moduleDirectories: ['node_modules', '<rootDir>'],

  // Element UI 完整 mock
  setupFiles: ['<rootDir>/tests/setup.js'],

  // 每个测试文件结束清空 mock
  clearMocks: true,
  restoreMocks: true,

  // 详细输出
  verbose: true
}
