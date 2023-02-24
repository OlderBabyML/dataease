<template>
  <div class="de-ds-form">
    <div
      class="de-ds-top"
    >
      <span class="name">
        <i
          class="el-icon-arrow-left"
          @click="logOutTips"
        />
        {{ tips }}
      </span>
      <div class="apply">
        <template>
          <deBtn
            secondary
            @click="logOutTips"
          >{{ $t('commons.cancel') }}
          </deBtn>
          <deBtn
            type="primary"
            @click="save"
          >{{ $t('commons.save') }}
          </deBtn>
        </template>
      </div>
    </div>
    <div style="height: calc(100% - 56px);background-color: #f6f8fa;display: flex;justify-content: center">
      <div style="width: 65%">
        <div style="height: 33%;background-color: #FFFFFF;padding: 5%;margin: 10px">
          <el-descriptions
            class="margin-top"
            title="基本信息"
            :column="1"
          >
            <el-descriptions-item label="图表名称">{{ alarm.title }}</el-descriptions-item>
            <el-descriptions-item label="指标名称">{{ alarm.name }}</el-descriptions-item>
          </el-descriptions>
        </div>
        <div style="height: 33%;background-color: #FFFFFF;padding: 5%;margin: 10px">
          <el-descriptions
            class="margin-top"
            title="设置预警规则"
            :column="1"
          >
            <el-descriptions-item label="cron表达式"><el-input
              v-model="alarm.cron"
              placeholder="请输入cron表达式"
              style="width: 240px"
            /></el-descriptions-item>
            <el-descriptions-item label="触发规则">18100000000</el-descriptions-item>
          </el-descriptions>
        </div>
        <div style="height: 33%;background-color: #FFFFFF;padding: 5%;margin: 10px">
          <el-descriptions
            class="margin-top"
            title="通知方式"
            :column="1"
          >
            <el-descriptions-item label="发送设置">飞书|邮箱|电话</el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import msgCfm from '@/components/msgCfm'

export default {
  name: 'AlarmForm',
  components: {
  },
  mixins: [msgCfm],
  props: {
    referencePosition: {
      type: String,
      default: 'datasource'
    },
    outerParams: {
      type: Object,
      request: false
    }
  },
  data() {
    return {
      typeMap: '',
      canEdit: false,
      formLoading: false,
      params: {},
      tips: '',
      alarm: {},
      formType: 'add'
    }
  },
  mounted() {
    this.tips = this.$t('commons.edit') + this.$route.query.name + '报警'
    this.alarm = JSON.parse(JSON.stringify(this.$route.query))
    console.log(this.alarm)
  },
  methods: {
    closeDraw() {
      this.$emit('closeDraw')
    },
    refreshType(form) {
      this.$emit('refresh-type', form)
    },
    setParams(params) {
      this.params = params
    },
    positionCheck(referencePosition) {
      return this.referencePosition === referencePosition
    },
    validaDatasource() {
      this.$refs.DsFormContent.validaDatasource()
    },
    save() {
      this.$refs.DsFormContent.save()
    },
    backToList() {
      this.$router.back(-1)
    },
    logOutTips() {
      const options = {
        title: 'role.tips',
        confirmButtonText: this.$t('commons.confirm'),
        content: 'system_parameter_setting.sure_to_exit',
        type: 'primary',
        cb: () => {
          this.backToList()
        }
      }
      this.handlerConfirm(options)
    }
  }
}
</script>
<style lang="scss" scoped>
.de-ds-form-app {
  width: 100%;
  height: 100%;

  .de-ds-cont {
    display: flex;
    width: 100%;
    height: 100%;
    overflow-y: auto;
    overflow-x: hidden;
    padding: 12px 24px 24px 24px;

    .de-ds-inner {
      width: 100%;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .w600 {
      width: 600px;
      height: 100%;
    }
  }

  .de-ds-bottom {
    display: flex;
    text-align: right;
    align-items: center;
    justify-content: space-between;
    height: 56px;
    padding: 12px 24px;
    box-shadow: 2px 2px 4px rgba(31, 35, 41, 0.08);

    .name {
      font-family: 'PingFang SC';
      font-style: normal;
      font-weight: 500;
      font-size: 16px;
      line-height: 24px;
      color: var(--deTextPrimary, #1f2329);
    }

    i {
      cursor: pointer;
    }
  }
}

.de-ds-form {
  width: 100vw;
  height: 100vh;

  .de-ds-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 56px;
    padding: 12px 24px;
    box-shadow: 0px 2px 4px rgba(31, 35, 41, 0.08);

    .name {
      font-family: 'PingFang SC';
      font-style: normal;
      font-weight: 500;
      font-size: 16px;
      line-height: 24px;
      color: var(--deTextPrimary, #1f2329);
    }

    i {
      cursor: pointer;
    }
  }
}
</style>
