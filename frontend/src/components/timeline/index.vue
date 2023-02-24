<template>
  <div
    style="display: flex;font-size:14px;"
  >
    <div
      style="display: flex;align-items: center;justify-content: flex-start;width: 90px;height: 36px;padding-top: 12px;padding-right: 15px;
          color: #7789a6;;font-size: 12px;white-space: nowrap;"
    />
    <div style="width: calc(100% - 90px);padding-top: 5px;padding-bottom: 5px;padding-left: 10px;border-left: 1px solid #d9dfe6;">
      <div
        style="display: flex;align-items: center;justify-content: space-between;width: 100%;min-height: 36px;padding: 0 10px;
         background-color: #FFFFFF;border-bottom: 1px solid #d9dfe6;;box-shadow: 0 2px 4px rgb(0 0 0 / 5%);cursor: pointer;"
        @click="openPanel"
      >
        <div style="display: flex;align-items: center;width: 300px;">
          <span style="width: 0">
            <span class="time">{{ timestamp }}</span>
          </span>
          <slot />
        </div>
      </div>
      <div style="padding: 0 22px;background: #FFFFFF;border-radius: 2px;box-shadow: 0 2px 4px rgb(0 0 0 / 5%);">
        <div
          v-if="typeof dataList !=='undefined' && dataList !== null && isShow"
          style="display: flex;flex-wrap: wrap;margin-bottom: 0;padding: 16px 0;overflow: hidden;"
        >
          <div
            v-for="(item,index) in dataList"
            :key="index"
            style="width: 50%;height: 40px;padding: 0 32px;color: #42546d;font-weight: 400;word-break: break-all;"
          >
            <div style="border-bottom:1px solid #e6e6e6;display: flex;align-items: center;justify-content: space-between;width: 100%;height: 40px;padding: 0 24px;">
              <span
                style="padding-right: 10px;color: #67729d;font-weight: 400;font-size: 14px;min-width: 100px;max-width: 200px;overflow: hidden;
                white-space: nowrap;text-overflow: ellipsis;"
              >
                {{ item.name }}
              </span>
              <span style="    display: flex;flex: 1 1;align-items: center;justify-content: flex-end;min-width: calc(100% - 200px);max-width: calc(100% - 100px);text-align: right;">
                <el-tooltip
                  class="item"
                  effect="light"
                  :content="item.value"
                  placement="top-start"
                >
                  <span style="overflow: hidden;white-space: nowrap;text-overflow: ellipsis;">{{ item.value }}</span>
                </el-tooltip>
                <span style="position: relative;display: none;width: 0;" />
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Timeline',
  props: {
    indexArr: {
      type: Number,
      default: null
    },
    timestamp: {
      type: String,
      default: ''
    },
    isShow: {
      type: Boolean,
      default: false
    },
    dataList: {
      type: Array,
      required: false,
      default: function() {
        return []
      }
    }
  },
  data() {
    return {
      dialogVisible: false,
      id: ''
    }
  },
  mounted() {
  },
  methods: {
    openPanel() {
      this.$emit('openPanel', this.indexArr)
    }
  }
}
</script>

<style lang="scss" scoped>
.time{
  position: relative;left: -111px;color: #8d9eb9;;font-weight: 500;
}

.time:after {
  position: relative;
  left: 3px;
  color: #d9dfe6;;
  font-size: 9px;
  content: "●";
}
</style>
