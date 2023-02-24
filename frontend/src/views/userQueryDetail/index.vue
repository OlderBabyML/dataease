<template>
  <div
    style="position: relative;width: 100%;height: 100%;overflow: hidden;"
  >
    <div
      style="position: relative;height: 100%;overflow: hidden;background: #f0f2f5;"
    >
      <div
        style="display: flex;flex-direction: column;height: 100%;"
      >
        <div>
          <div style="width: 100%;font-size: 14px">
            <div style="z-index: 1000;display: flex;justify-content: space-between;height: 51px;padding: 0 50px 0 24px;line-height: 50px;background: #fff;border-right: 1px solid #e4e2ea;border-bottom: 1px solid #edf0f2;">
              <span>用户细查</span>
              <span style="cursor: pointer;"><i class="el-icon-refresh-right" />刷新数据</span>
            </div>
          </div>
        </div>
        <div
          style="display: flex;flex: 1 1;width: 100%;height: 0;"
        >
          <div
            style="z-index: 99;width: 328px;height: calc(100vh - 64px);background-color: #edf0f2;"
          >
            <div style="height: calc(100vh - 100px);overflow-y: auto;background-color: #FFFFFF;">
              <div
                style="    position: sticky;top: 0;z-index: 10;display: flex;align-items: center;justify-content: space-between;height: 48px;padding: 0 24px;color: #42546d;font-size: 16px;
                background: #f6f8fa;cursor: pointer;"
              >
                <div style="    width: 100%;overflow: hidden;font-weight: 500;white-space: nowrap;text-overflow: ellipsis;">
                  <i class="el-icon-tickets" />
                  用户属性
                </div>
              </div>
              <div style="height:100%;z-index: 9;padding-bottom: 8px;background-color: #fff;">
                <div
                  v-for="(item,index) in attrList"
                  :key="index"
                  style="display: flex;align-items: flex-start;justify-content: space-between;padding: 8px 24px;font-size: 14px;"
                >
                  <div style="width: 38.2%;color: #607595;font-size: 14px;word-break: break-all;">{{ item.name }}</div>
                  <div style="width: 61.8%;color: #42546d;font-size: 14px;text-align: right;word-break: break-all;">{{ item.value }}</div>
                </div>
              </div>
            </div>
          </div>
          <div
            style="z-index: 9;flex: 1 1;height: 100%;overflow: auto;"
          >
            <div style="position: relative;padding: 0 24px 50px;">
              <div style="font-size: 14px;display: flex;align-items: center;justify-content: flex-start;padding: 16px 0;">
                <span style="margin-right: 10px;">
                  <el-date-picker
                    v-model="dateRange"
                    type="datetimerange"
                    :picker-options="pickerOptions"
                    range-separator="至"
                    start-placeholder="开始日期"
                    end-placeholder="结束日期"
                    align="right"
                    :clearable="false"
                    :editable="false"
                  />
                </span>
                <div style="height: 36px;background: #FFFFFF;">
                  <div
                    class="event-chose"
                  >
                    <el-popover
                      v-model="totalFlag"
                      placement="bottom"
                      trigger="click"
                    >
                      <el-tree
                        :data="totalOption"
                        show-checkbox
                        node-key="id"
                        :default-checked-keys="[5,6]"
                        :props="defaultProps"
                      />
                      <el-button
                        size="mini"
                        @click="totalFlag = false"
                      >取消</el-button>
                      <el-button
                        type="primary"
                        size="mini"
                        @click="totalFlag = false"
                      >确认</el-button>
                      <template slot="reference">
                        <span style="margin-right: 10px;cursor: pointer;"><i class="el-icon-plus" />事件</span>
                      </template>
                    </el-popover>
                  </div>
                </div>
              </div>
              <div style="width: 100%;border-radius: 2px;">
                <div style="width: 100%;height: 440px;margin-bottom: 16px;background-color: #FFFFFF;">
                  <div style="display: flex;align-items: center;justify-content: space-between;height: 44px;padding: 0 20px 0 15px;border-bottom: 1px solid #f0f2f5;">
                    <div style="display: flex;align-items: center;justify-content: flex-start;color: #42546d;font-weight: 500;font-size: 14px;">行为事件总量</div>
                    <div>
                      <span style="font-size: 14px;margin-right: 8px">展示事件分布</span>
                      <el-switch
                        v-model="switchPieValue"
                        style="margin-right: 8px"
                        @change="showPie"
                      />
                      <span style="font-size: 14px;margin-right: 8px">显示数值</span>
                      <el-switch
                        v-model="switchValue"
                        @change="showNumber"
                      />
                    </div>
                  </div>
                  <div style="display: flex;justify-content: space-between;height: 350px;margin-top: 10px;margin-bottom: 10px;padding: 18px 15px 0;">
                    <div
                      :style="boardDivColor"
                    >
                      <div
                        :id="id"
                        style="width: 100%;height: 100%;overflow: hidden;"
                      />
                    </div>
                    <div
                      v-show="switchPieValue"
                      style="width: 400px;height: 350px;padding: 20px 10px 10px;border-left: 1px solid #f0f2f5;"
                    >
                      <div
                        :id="pieId"
                        style="width: 100%;height: 100%;overflow: hidden;"
                      />
                    </div>
                  </div>
                </div>
                <div style="margin-top: 16px;">
                  <div style="display: flex;align-items: center;justify-content: space-between;height: 56px;padding: 0 24px;font-size: 14px;background-color: #FFFFFF;">
                    <div style="display: flex;align-items: center;justify-content: flex-start;height: 56px;">
                      <span
                        style="display: flex;align-items: center;margin-right: 24px;padding: 0 10px; border: 1px solid #f6f6f6;;cursor: pointer;"
                        @click="sortOption"
                      >
                        <i class="el-icon-sort" />
                        {{ sortOperator === 'desc' ? '时间降序':'时间升序' }}
                      </span>
                      <el-popover
                        v-model="eventFlag"
                        placement="bottom"
                        trigger="click"
                      >
                        <el-tree
                          :data="option"
                          show-checkbox
                          node-key="id"
                          :default-checked-keys="[5,6]"
                          :props="defaultProps"
                        />
                        <el-button
                          size="mini"
                          @click="eventFlag = false"
                        >取消</el-button>
                        <el-button
                          type="primary"
                          size="mini"
                          @click="eventFlag = false"
                        >确认</el-button>
                        <template slot="reference">
                          <span style="display: flex;align-items: center;height: 32px;cursor: pointer"><i class="el-icon-location-outline" />定位事件</span>
                        </template>
                      </el-popover>
                    </div>
                  </div>
                  <div style="display: flex;background-color: #f6f8fa;">
                    <div style="width: 100%;height: auto;padding: 12px 24px;overflow: hidden;transition: .2s;">
                      <div style="display: flex;margin: 5px 0;">
                        <span style="font-weight: bold;font-size:14px;display: inline-block;height: 20px;margin-right: 5px;color: #42546d;white-space: nowrap;">2023-02-22</span></div>
                      <timeline
                        v-for="(item,index) in eventList"
                        :key="index"
                        :index-arr="index"
                        :is-show="item.isShow"
                        :data-list="item.dataList"
                        :timestamp="item.timestamp"
                        @openPanel="openPanel"
                      >
                        <span class="content">
                          <i
                            v-if="!item.isShow"
                            class="el-icon-arrow-right"
                          />
                          <i
                            v-else
                            class="el-icon-arrow-down"
                          />
                          {{ item.name }}
                        </span>
                      </timeline>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import Timeline from '@/components/timeline/index'

export default {
  components: { Timeline },
  data() {
    return {
      totalFlag: false,
      eventFlag: false,
      sortOperator: 'desc',
      totalOption: [{
        id: 1,
        label: '服务端埋点',
        children: [{
          id: 4,
          label: 'game_ludo_game_start_skin'
        }]
      }, {
        id: 2,
        label: '客户端埋点',
        children: [{
          id: 5,
          label: 'game_ludo_PlayMode_click'
        }, {
          id: 6,
          label: 'guessing_select_gift_confirm'
        }]
      }, {
        id: 3,
        label: 'H5埋点',
        children: [{
          id: 7,
          label: 'open_notify_pk_invite'
        }, {
          id: 8,
          label: 'live_room_room_exit'
        }]
      }, {
        id: 4,
        label: '技术埋点',
        children: [{
          id: 9,
          label: 'open_notify_pk_invite'
        }, {
          id: 10,
          label: 'live_room_room_exit'
        }]
      }],
      option: [{
        id: 2,
        label: '客户端埋点',
        children: [{
          id: 5,
          label: 'game_ludo_PlayMode_click'
        }, {
          id: 6,
          label: 'guessing_select_gift_confirm'
        }]
      }],
      defaultProps: {
        children: 'children',
        label: 'label'
      },
      dialogVisible: false,
      id: 'test123124',
      pieId: 'test1233131222124',
      switchValue: false,
      switchPieValue: false,
      chartWidth: '100%',
      myChart: null,
      myPieChart: null,
      pieOptions: {
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b}: {c} ({d}%)'
        },
        series: [
          {
            name: '事件分布',
            type: 'pie',
            radius: ['40%', '70%'],
            avoidLabelOverlap: false,
            itemStyle: {
              borderRadius: 10,
              borderColor: '#fff',
              borderWidth: 2
            },
            label: {
              formatter: '{b}:{d}%',
              show: true
            },
            data: [
              { value: 1048, name: 'Search Engine' },
              { value: 735, name: 'Direct' },
              { value: 580, name: 'Email' },
              { value: 484, name: 'Union Ads' },
              { value: 300, name: 'Video Ads' }
            ]
          }
        ]
      },
      chart: {
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: [
          {
            type: 'category',
            data: ['02/16', '02/17', '02/18', '02/19', '02/20', '02/21', '02/22'],
            axisTick: {
              alignWithLabel: true
            }
          }
        ],
        yAxis: [
          {
            type: 'value'
          }
        ],
        series: [
          {
            name: '总量',
            type: 'bar',
            barWidth: '25%',
            label: { show: false, position: 'top' },
            itemStyle: { color: '#3daae3' },
            data: [10, 52, 200, 334, 390, 330, 220]
          }
        ]
      },
      attrList: [
        { name: 'uid', value: '10004' },
        { name: 'user_id', value: '10004' },
        { name: 'platform', value: '1' },
        { name: 'avatar', value: '717816715008122888:2' },
        { name: 'photowall', value: '692683632678461448' },
        { name: 'status', value: 'normal' },
        { name: 'gender', value: 'female' },
        { name: 'user_level', value: '47' },
        { name: 'nike_name', value: 'Malaysia CS' },
        { name: 'birthday', value: '1992-12-28' },
        { name: 'regist_time', value: '2014-08-13 00:16:48' },
        { name: 'country', value: 'MY' },
        { name: 'region', value: 'MY' },
        { name: 'agent_type', value: 'non_agent' },
        { name: 'gifter_type', value: 'non_gifter' },
        { name: 'locale', value: 'en_US' },
        { name: 'silver_balance', value: '300' },
        { name: 'login_days', value: '0' },
        { name: '30_login_days', value: '0' },
        { name: 'recharge_times', value: '0' },
        { name: 'recharge_amount', value: '0' },
        { name: 'is_robot', value: '0' },
        { name: 'first_login_account_type', value: 'email' },
        { name: 'is_new', value: '0' }],
      pickerOptions: {
        shortcuts: [{
          text: '最近一周',
          onClick(picker) {
            const end = new Date()
            const start = new Date()
            start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
            picker.$emit('pick', [start, end])
          }
        }, {
          text: '最近一个月',
          onClick(picker) {
            const end = new Date()
            const start = new Date()
            start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
            picker.$emit('pick', [start, end])
          }
        }, {
          text: '最近三个月',
          onClick(picker) {
            const end = new Date()
            const start = new Date()
            start.setTime(start.getTime() - 3600 * 1000 * 24 * 90)
            picker.$emit('pick', [start, end])
          }
        }]
      },
      dateRange: [
        new Date(Date.now() - 60 * 1000 * 60 * 24 * 7),
        new Date(Date.now())
      ],
      eventList: [{
        isShow: false,
        timestamp: '21:36:34.886',
        name: 'game_ludo_game_start_skin',
        dataList: [
          { name: 'device_id', value: '85abfa7acc6a148a' },
          { name: 'uid', value: '10004' },
          { name: 'user_id', value: '10004' },
          { name: 'afid', value: '1672168744524-5752931834308158480' },
          { name: 'sys_version', value: 'android-8.1.0' },
          { name: 'os', value: 'android' },
          { name: 'vc', value: '1110900' },
          { name: 'vn', value: '8.0.0.0' },
          { name: 'ip', value: '197.59.135.38' },
          { name: 'timezone', value: '2' },
          { name: 'mcc', value: '602' },
          { name: 'pkg_name', value: 'com.mico' },
          { name: 'extra', value: '{"CheckerboardSkin_id":"100001","ChessSkin_id":"100001","uid":"1435579224160337920","DiceSkin_id":"200005"}' },
          { name: 'bus', value: 'game' },
          { name: 'client_timestamp', value: '1675243102338' },
          { name: 'locale', value: 'en_US' },
          { name: 'model', value: 'EVR-L29' }
        ]
      }, {
        isShow: false,
        timestamp: '20:21:34.866',
        name: 'game_ludo_PlayMode_click',
        dataList: [
          { name: 'device_id', value: '85abfa7acc6a148a' },
          { name: 'uid', value: '10004' },
          { name: 'user_id', value: '10004' },
          { name: 'afid', value: '1672168744524-5752931834308158480' },
          { name: 'sys_version', value: 'android-8.1.0' },
          { name: 'os', value: 'android' },
          { name: 'vc', value: '1110900' },
          { name: 'vn', value: '8.0.0.0' },
          { name: 'ip', value: '197.59.135.38' },
          { name: 'timezone', value: '2' },
          { name: 'mcc', value: '602' },
          { name: 'pkg_name', value: 'com.mico' },
          { name: 'extra', value: '{"CheckerboardSkin_id":"100001","ChessSkin_id":"100001","uid":"1435579224160337920","DiceSkin_id":"200005"}' },
          { name: 'bus', value: 'game' },
          { name: 'client_timestamp', value: '1675243102338' },
          { name: 'locale', value: 'en_US' },
          { name: 'model', value: 'EVR-L29' }
        ]
      }, {
        isShow: false,
        timestamp: '18:30:34.060',
        name: 'guessing_select_gift_confirm',
        dataList: [
          { name: 'device_id', value: '85abfa7acc6a148a' },
          { name: 'uid', value: '10004' },
          { name: 'user_id', value: '10004' },
          { name: 'afid', value: '1672168744524-5752931834308158480' },
          { name: 'sys_version', value: 'android-8.1.0' },
          { name: 'os', value: 'android' },
          { name: 'vc', value: '1110900' },
          { name: 'vn', value: '8.0.0.0' },
          { name: 'ip', value: '197.59.135.38' },
          { name: 'timezone', value: '2' },
          { name: 'mcc', value: '602' },
          { name: 'pkg_name', value: 'com.mico' },
          { name: 'extra', value: '{"CheckerboardSkin_id":"100001","ChessSkin_id":"100001","uid":"1435579224160337920","DiceSkin_id":"200005"}' },
          { name: 'bus', value: 'game' },
          { name: 'client_timestamp', value: '1675243102338' },
          { name: 'locale', value: 'en_US' },
          { name: 'model', value: 'EVR-L29' }
        ]
      }, {
        isShow: false,
        timestamp: '18:26:31.786',
        name: 'open_notify_pk_invite',
        dataList: [
          { name: 'device_id', value: '85abfa7acc6a148a' },
          { name: 'uid', value: '10004' },
          { name: 'user_id', value: '10004' },
          { name: 'afid', value: '1672168744524-5752931834308158480' },
          { name: 'sys_version', value: 'android-8.1.0' },
          { name: 'os', value: 'android' },
          { name: 'vc', value: '1110900' },
          { name: 'vn', value: '8.0.0.0' },
          { name: 'ip', value: '197.59.135.38' },
          { name: 'timezone', value: '2' },
          { name: 'mcc', value: '602' },
          { name: 'pkg_name', value: 'com.mico' },
          { name: 'extra', value: '{"CheckerboardSkin_id":"100001","ChessSkin_id":"100001","uid":"1435579224160337920","DiceSkin_id":"200005"}' },
          { name: 'bus', value: 'game' },
          { name: 'client_timestamp', value: '1675243102338' },
          { name: 'locale', value: 'en_US' },
          { name: 'model', value: 'EVR-L29' }
        ]
      }]
    }
  },
  computed: {
    boardDivColor() {
      const style = {
        height: '350px',
        width: this.chartWidth
      }
      return style
    }
  },
  mounted() {
    this.myChart = this.$echarts.getInstanceByDom(document.getElementById(this.id))
    if (!this.myChart) {
      this.myChart = this.$echarts.init(document.getElementById(this.id))
    }
    setTimeout(this.myChart.setOption(this.chart, true), 500)
  },
  methods: {
    openPanel(index) {
      this.eventList[index].isShow = !this.eventList[index].isShow
    },
    showNumber() {
      this.chart.series[0].label.show = this.switchValue
      setTimeout(this.myChart.setOption(this.chart, true), 200)
    },
    sortOption() {
      if (this.sortOperator === 'desc') {
        this.sortOperator = 'asc'
      } else {
        this.sortOperator = 'desc'
      }
    },
    showPie() {
      if (this.switchPieValue) {
        this.chartWidth = 'calc(100% - 450px)'
        setTimeout(() => {
          this.myPieChart = this.$echarts.getInstanceByDom(document.getElementById(this.pieId))
          if (!this.myPieChart) {
            this.myPieChart = this.$echarts.init(document.getElementById(this.pieId))
            this.myPieChart.setOption(this.pieOptions, true)
          }
        }, 200)
      } else {
        this.chartWidth = '100%'
      }
      setTimeout(() => {
        this.myChart.setOption(this.chart, true)
        this.myChart.resize()
      }, 300)
    }
  }
}
</script>

<style lang="scss" scoped>
.content{
  position: relative;
  z-index: 1000;
  color: #42546d;
  font-weight: 600;
  white-space: nowrap;
}
.event-chose{
  display: inline-flex;align-items: center;min-width: 36px;height: 36px;padding: 0 6px;font-size: 14px;line-height: 36px;white-space: nowrap;vertical-align: top;
  border: 1px solid #f0f2f5;border-radius: 2px;cursor: pointer;transition: all .3s;margin: 0;background-color: #FFFFFF;
}
.event-chose:hover{
  border-color: #0aafe6;
}
</style>
