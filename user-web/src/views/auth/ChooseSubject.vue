<template>
  <div class="choose-subject-page">
    <Header />

    <div class="page-body">
      <div class="container">
        <div class="choose-card">
          <div class="card-header">
            <h1 class="title">选择专业</h1>
            <p class="subtitle">请选择您要学习的专业，选择后可在首页随时切换</p>
          </div>

          <div class="header-tip">
            <van-icon name="info-o" size="20" color="#1989fa" />
            <p>请选择您要学习的专业，选择后可在首页随时切换</p>
          </div>

          <div class="search-box">
            <van-search
              v-model="searchKeyword"
              placeholder="搜索专业名称"
              shape="round"
              show-action
            >
              <template #action>
                <span @click="searchKeyword = ''" v-if="searchKeyword">清除</span>
              </template>
            </van-search>
          </div>

          <div class="profession-grid">
            <div
              v-for="prof in displayedProfessions"
              :key="prof.id"
              class="profession-item"
              :class="{ active: selectedProfessionId === prof.id }"
              @click="selectProfession(prof)"
            >
              <van-icon name="bookmark-o" size="28" :color="selectedProfessionId === prof.id ? '#1989fa' : '#999'" />
              <div class="profession-info">
                <div class="profession-name">{{ prof.name }}</div>
                <div class="profession-desc" v-if="prof.description">{{ prof.description }}</div>
              </div>
              <van-icon v-if="selectedProfessionId === prof.id" name="success" color="#1989fa" size="20" />
            </div>
          </div>

          <!-- 展开/收缩按钮:专业超过默认显示数量时显示 -->
          <div v-if="filteredProfessions.length > defaultShowCount && !searchKeyword" class="expand-btn-wrap">
            <van-button plain type="info" size="small" @click="showAll = !showAll">
              {{ showAll ? '收起专业 ▲' : '展开全部专业 (' + filteredProfessions.length + ') ▼' }}
            </van-button>
          </div>

          <div v-if="filteredProfessions.length === 0 && !loading" class="empty-tip">
            <van-empty :description="searchKeyword ? '未找到匹配的专业' : '暂无专业数据'" />
          </div>

          <div class="submit-btn">
            <van-button
              round
              block
              type="primary"
              :loading="loading"
              :disabled="!selectedProfessionId"
              @click="handleConfirm"
            >
              确认选择
            </van-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import Header from '@/components/Header.vue'
import { getSubjectList, chooseSubject } from '@/api/auth'
import { Toast } from 'vant'

export default {
  name: 'ChooseSubject',
  components: { Header },
  data() {
    return {
      professionList: [],
      selectedProfessionId: null,
      selectedProfession: null,
      loading: false,
      searchKeyword: '',
      showAll: false,
      defaultShowCount: 8
    }
  },
  computed: {
    filteredProfessions() {
      if (!this.searchKeyword) return this.professionList
      const kw = this.searchKeyword.toLowerCase()
      return this.professionList.filter(p => p.name && p.name.toLowerCase().includes(kw))
    },
    displayedProfessions() {
      // 搜索时不限制显示数量;未搜索时默认只显示4行(双列=8项),展开后显示全部
      if (this.searchKeyword || this.showAll) return this.filteredProfessions
      return this.filteredProfessions.slice(0, this.defaultShowCount)
    }
  },
  created() {
    this.fetchProfessions()
  },
  methods: {
    async fetchProfessions() {
      this.loading = true
      try {
        const res = await getSubjectList()
        const list = res.data || res || []
        // 当前业务模型:只选专业,不再选科目;忽略后端返回的 subjects 字段
        this.professionList = Array.isArray(list) ? list : []
        // 后端无数据:留空,提示用户去联系管理员
      } catch (error) {
        this.professionList = []
        Toast.fail('专业列表加载失败,请检查网络')
      } finally {
        this.loading = false
      }
    },
    selectProfession(prof) {
      this.selectedProfessionId = prof.id
      this.selectedProfession = {
        id: prof.id,
        professionId: prof.id,
        name: prof.name,
        professionName: prof.name
      }
    },
    async handleConfirm() {
      if (!this.selectedProfessionId) {
        Toast('请选择专业')
        return
      }

      this.loading = true
      try {
        // 后端 /auth/choose-subject 只要求 professionId
        await chooseSubject({
          professionId: this.selectedProfessionId
        })
        // 后端成功后再写 store,保证 localStorage 和后端一致
        await this.$store.dispatch('setCurrentSubject', this.selectedProfession)
        Toast.success('选择成功')
        const redirect = this.$route.query.redirect || '/home'
        const safeRedirect = redirect.startsWith('/login') ? '/home' : redirect
        this.$router.replace(safeRedirect).catch(() => {})
      } catch (error) {
        // 后端报错时不让 store 写入,避免下次再跳进来
        Toast.fail((error && error.message) || '选择失败，请重试')
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.choose-subject-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.page-body {
  padding-top: var(--header-height, 170px);
}

.container {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px 20px;
}

.choose-card {
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.card-header {
  text-align: center;
  margin-bottom: 24px;

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

.header-tip {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 14px 16px;
  background: #ebf5ff;
  border-radius: 8px;
  margin-bottom: 20px;

  p {
    font-size: 13px;
    color: #646566;
    margin: 0;
    line-height: 1.5;
  }
}

.search-box {
  margin-bottom: 16px;
  .van-search {
    padding: 0;
    background: transparent;
  }
}

.profession-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.profession-item {
  display: flex;
  align-items: center;
  padding: 16px 18px;
  background: #f7f8fa;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;

  &:hover {
    background: #f0f8ff;
  }

  &.active {
    background: #f0f8ff;
    border-color: #1989fa;
  }

  .profession-info {
    flex: 1;
    margin-left: 12px;
    min-width: 0;

    .profession-name {
      font-size: 15px;
      color: #323233;
      font-weight: 500;
    }

    .profession-desc {
      font-size: 12px;
      color: #999;
      margin-top: 4px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}

.submit-btn {
  margin-top: 28px;

  .van-button {
    height: 46px;
    font-size: 16px;
    font-weight: 500;
  }
}

.expand-btn-wrap {
  text-align: center;
  margin-top: 16px;
  padding-bottom: 4px;
}

.empty-tip {
  padding: 40px 0;
}

@media (max-width: 600px) {
  .profession-grid {
    grid-template-columns: 1fr;
  }
}
</style>
