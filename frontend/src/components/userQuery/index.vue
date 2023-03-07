<template>
  <div>
    <div
      class="user-query"
      @click="openSearch"
    >
      <i
        class="el-icon-tickets"
        style="font-size: 14px;vertical-align: text-top;"
      />
      <span>用户细查</span>
    </div>
    <el-dialog
      :visible.sync="dialogVisible"
      :destroy-on-close="true"
      :append-to-body="true"
      :show-close="false"
      :close-on-click-modal="true"
      width="40%"
    >
      <template #title>
        <i class="el-icon-user" />
        用户细查
      </template>
      <div>
        <el-select
          v-model="product"
          style="width: 120px"
        >
          <el-option
            v-for="(item,index) in productList"
            :key="index"
            :label="item"
            :value="item"
          >
            <i class="el-icon-goblet-square" />
            <span>{{ item }}</span>
          </el-option>
        </el-select>
        <el-select
          v-model="attr"
          style="width: 140px"
        >
          <el-option
            v-for="(item,index) in attrList"
            :key="index"
            :label="item.label"
            :value="item.value"
          >
            <i class="el-icon-user" />
            <span>{{ item.label }}</span>
          </el-option>
        </el-select>
        <el-select
          v-model="operator"
          style="width: 100px"
        >
          <el-option
            v-for="(item,index) in operatorList"
            :key="index"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-input
          v-model="id"
          style="width: 200px"
        />
        <el-button
          class="right-menu-item"
          type="primary"
          @click="openUserQuery"
        >搜索</el-button>
      </div>
      <div v-show="userList.length > 0">
        <el-table
          :data="userList"
          style="width: 100%"
        >
          <el-table-column
            :prop="attr"
            :label="attr"
          />
          <el-table-column
            prop="user_id"
            label="user_id"
          />
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  data() {
    return {
      attr: 'user_id',
      attrList: [{ label: 'user_id', value: 'user_id' }, { label: 'uid', value: 'uid' }, { label: 'country', value: 'country' }],
      productList: ['Mico', 'Yoho'],
      operatorList: [{ label: '等于', value: '=' }],
      operator: '=',
      product: 'Mico',
      userList: [],
      dialogVisible: false,
      id: ''
    }
  },
  mounted() {
  },
  methods: {
    openSearch() {
      this.$router.push({ path: '/user-query-detail/index' })
      // this.dialogVisible = true
    },
    openUserQuery() {
      this.$router.push({ path: '/user-query-detail/index' })
      this.dialogVisible = false
    }
  }
}
</script>

<style lang="scss" scoped>
.user-query{
  height: 100%;
  color: var(--nav-color);
  font-size: 14px;
  text-align: center;
  border-radius: 2px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
}
</style>
