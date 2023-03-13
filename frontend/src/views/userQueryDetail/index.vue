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
              <span
                style="cursor: pointer;"
                @click="openDialog"
              ><i class="el-icon-search" />重新选择用户</span>
              <span
                style="cursor: pointer;"
                @click="closeUserQuery"
              ><i class="el-icon-refresh-right" />刷新数据</span>
            </div>
          </div>
        </div>
        <div
          v-if="userDialogVisible"
          style="display: flex;flex: 1 1;width: 100%;height: 0;"
        >
          <div
            v-loading="userInfoLoading"
            element-loading-text="加载中"
            element-loading-spinner="el-icon-loading"
            element-loading-background="rgba(0, 0, 0, 0.8)"
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
                    @change="reloadAll"
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
                      <div style="height: 180px; overflow-y: auto">
                        <el-input
                          v-model="filterText"
                          placeholder="输入埋点名进行过滤"
                        />
                        <el-tree
                          ref="tree"
                          :data="totalOption"
                          show-checkbox
                          node-key="id"
                          :filter-node-method="filterNode"
                          :default-checked-keys="defaultTree"
                          :props="defaultProps"
                        />
                      </div>
                      <el-button
                        size="mini"
                        @click="totalFlag = false"
                      >取消</el-button>
                      <el-button
                        type="primary"
                        size="mini"
                        @click="reloadAll"
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
                  <div
                    v-show="showChart"
                    style="display: flex;justify-content: center;align-items: center;height: 100%"
                  >
                    <el-progress
                      type="circle"
                      :percentage="percentage"
                    />
                  </div>
                  <div
                    v-show="!showChart"
                    style="display: flex;justify-content: space-between;height: 350px;margin-top: 10px;margin-bottom: 10px;padding: 18px 15px 0;"
                  >
                    <div
                      :style="{
                        'height': '350px',
                        'width': chartWidth
                      }"
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
                        <div style="height: 180px; overflow-y: auto">
                          <el-input
                            v-model="filterDetailText"
                            placeholder="输入埋点名进行过滤"
                          />
                          <el-tree
                            ref="treeDetail"
                            :data="option"
                            show-checkbox
                            node-key="id"
                            :filter-node-method="filterDetailNode"
                            :default-checked-keys="defaultTreeDetail"
                            :props="defaultProps"
                          />
                        </div>
                        <el-button
                          size="mini"
                          @click="eventFlag = false"
                        >取消</el-button>
                        <el-button
                          type="primary"
                          size="mini"
                          @click="reloadTimeLine(true)"
                        >确认</el-button>
                        <template slot="reference">
                          <span style="display: flex;align-items: center;height: 32px;cursor: pointer"><i class="el-icon-location-outline" />定位事件</span>
                        </template>
                      </el-popover>
                    </div>
                  </div>
                  <div
                    v-show="showTimeline"
                    style="display: flex;justify-content: center;align-items: center;height: 300px"
                  >
                    <el-progress
                      type="circle"
                      :percentage="percentageDetail"
                    />
                  </div>
                  <div
                    v-for="(item,index) in timeList"
                    v-show="!showTimeline"
                    :key="index"
                    style="display: flex;background-color: #f6f8fa;"
                  >
                    <div
                      style="width: 100%;height: auto;padding: 12px 24px;overflow: hidden;transition: .2s;"
                    >
                      <div
                        style="display: flex;margin: 5px 0;"
                      >
                        <span style="font-weight: bold;font-size:14px;display: inline-block;height: 20px;margin-right: 5px;color: #42546d;white-space: nowrap;">{{ item.date }}</span></div>
                      <timeline
                        v-for="(event,i) in item.eventList"
                        :key="i"
                        :index-arr="i"
                        :is-show="event.isShow"
                        :data-list="event.dataList"
                        :timestamp="event.timestamp"
                        @openPanel="openPanel(index,i)"
                      >
                        <span class="content">
                          <i
                            v-if="!event.isShow"
                            class="el-icon-arrow-right"
                          />
                          <i
                            v-else
                            class="el-icon-arrow-down"
                          />
                          {{ event.name }}
                        </span>
                      </timeline>
                    </div>
                  </div>
                  <div
                    v-show="!isTotal && !showTimeline"
                    style="display: flex;align-items: center;justify-content: center;height: 40px;cursor: pointer;"
                    @click="pageTimeLine"
                  >
                    加载更多
                  </div>
                  <div
                    v-show="isTotal && !showTimeline"
                    style="display: flex;align-items: center;justify-content: center;height: 40px;"
                  >
                    已加载全部
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <el-dialog
      :visible.sync="dialogVisible"
      :destroy-on-close="true"
      :append-to-body="true"
      :show-close="true"
      :close-on-click-modal="false"
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
          @change="changeSelectList"
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
            v-for="(item,index) in userAttrList"
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
          v-model="attrValue"
          style="width: 200px"
        />
        <el-button
          class="right-menu-item"
          type="primary"
          @click="searchUserList"
        >搜索</el-button>
      </div>
      <div
        v-loading="userListLoading"
        element-loading-text="加载中"
        element-loading-spinner="el-icon-loading"
        element-loading-background="rgba(0, 0, 0, 0.8)"
      >
        <el-table
          height="400"
          :data="userList"
          style="width: 100%"
        >
          <el-table-column
            prop="id"
            :label="attr"
          />
          <el-table-column
            label="userId"
          >
            <template slot-scope="{row}">
              <el-link
                type="primary"
                @click="enterQuery(row)"
              >{{ row.userId }}</el-link>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  getOptions,
  getUserEventDetail,
  getUserEventDetailForDate,
  getUserEventNum,
  getUserInfo,
  getUserList
} from '@/api/userQuery/index'
import Timeline from '@/components/timeline/index'

export default {
  components: { Timeline },
  data() {
    return {
      filterText: '',
      filterDetailText: '',
      userId: null,
      userListLoading: false,
      isTotal: false,
      showChart: true,
      showTimeline: true,
      pageNo: 1,
      percentage: 15,
      percentageDetail: 15,
      userInfoLoading: false,
      attrValue: '',
      attr: 'userId',
      userAttrList: [{ label: 'user_id', value: 'userId' }, { label: 'uid', value: 'uid' }, { label: 'country', value: 'country' }],
      userAttrYohoList: [{ label: 'uid', value: 'uid' }, { label: 'country', value: 'country' }],
      productList: ['Mico', 'Yoho'],
      operatorList: [{ label: '等于', value: '=' }],
      operator: '=',
      product: 'Mico',
      userList: [],
      totalFlag: false,
      eventFlag: false,
      sortOperator: 'desc',
      totalOption: [{
        id: 'server',
        label: '服务端埋点',
        children: [{
          type: 'server',
          id: 'game_ludo_game_start_skin',
          label: 'game_ludo_game_start_skin'
        }]
      }, {
        id: 'client',
        label: '客户端埋点',
        children: [{
          type: 'client',
          id: 'game_ludo_PlayMode_click',
          label: 'game_ludo_PlayMode_click'
        }, {
          type: 'client',
          id: 'game_ludo_box_rewardshow',
          label: 'game_ludo_box_rewardshow'
        }]
      }, {
        id: 'h5',
        label: 'H5埋点',
        children: [{
          type: 'h5',
          id: 'activity_worldcup_homepage_show',
          label: 'activity_worldcup_homepage_show'
        }, {
          type: 'h5',
          id: 'game_gold_TeenPatt_show',
          label: 'game_gold_TeenPatt_show'
        }]
      }, {
        id: 'technology',
        label: '技术埋点',
        children: [{
          type: 'technology',
          id: 'apm_app_start',
          label: 'apm_app_start'
        }, {
          type: 'technology',
          id: 'apm_app_end',
          label: 'apm_app_end'
        }, {
          type: 'technology',
          id: 'dynamic_distribution',
          label: 'dynamic_distribution'
        }, {
          type: 'technology',
          id: 'appinit',
          label: 'appinit'
        }, {
          type: 'technology',
          id: 'enter_room_api_optimization',
          label: 'enter_room_api_optimization'
        }]
      }],
      option: [],
      defaultProps: {
        children: 'children',
        label: 'label'
      },
      dialogVisible: true,
      userDialogVisible: false,
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
            radius: ['30%', '60%'],
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
            data: [{ value: 10, itemStyle: { color: '#3daae3' }},
              { value: 52, itemStyle: { color: '#3daae3' }},
              { value: 200, itemStyle: { color: '#3daae3' }},
              { value: 334, itemStyle: { color: '#3daae3' }},
              { value: 390, itemStyle: { color: '#3daae3' }},
              { value: 330, itemStyle: { color: '#3daae3' }},
              { value: 220, itemStyle: { color: '#3daae3' }}]
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
      timeList: [
        {
          date: '2023-03-01',
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
      ],
      tree: null,
      treeDetail: null,
      currentDate: '',
      defaultTree: ['technology'],
      defaultTreeDetail: []
    }
  },
  watch: {
    filterText(val) {
      this.$refs.tree.filter(val)
    },
    filterDetailText(val) {
      this.$refs.treeDetail.filter(val)
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.$store.dispatch('app/toggleSideBarHide', true)
    })
    this.getOptions()
  },
  methods: {
    changeSelectList() {
      if (this.product === 'Mico') {
        this.attr = 'userId'
        this.userAttrList = [{ label: 'user_id', value: 'userId' }, { label: 'uid', value: 'uid' }, { label: 'country', value: 'country' }]
      } else {
        this.attr = 'uid'
        this.userAttrList = [{ label: 'uid', value: 'uid' }, { label: 'country', value: 'country' }]
      }
    },
    filterNode(value, data) {
      if (!value) return true
      return data.label.indexOf(value) !== -1
    },
    filterDetailNode(value, data) {
      if (!value) return true
      return data.label.indexOf(value) !== -1
    },
    barProcess() {
      setTimeout(() => {
        if (this.percentage <= 80) {
          this.percentage = this.percentage + 15
        }
      }, 5000)
      setTimeout(() => {
        if (this.percentage <= 80) {
          this.percentage = this.percentage + 15
        }
      }, 10000)
      setTimeout(() => {
        if (this.percentage <= 80) {
          this.percentage = this.percentage + 15
        }
      }, 15000)
      setTimeout(() => {
        if (this.percentage <= 80) {
          this.percentage = this.percentage + 15
        }
      }, 25000)
      setTimeout(() => {
        if (this.percentage <= 80) {
          this.percentage = this.percentage + 15
        }
      }, 35000)
    },
    barDetailProcess() {
      setTimeout(() => {
        if (this.percentageDetail <= 80) {
          this.percentageDetail = this.percentageDetail + 15
        }
      }, 5000)
      setTimeout(() => {
        if (this.percentageDetail <= 80) {
          this.percentageDetail = this.percentageDetail + 15
        }
      }, 10000)
      setTimeout(() => {
        if (this.percentageDetail <= 80) {
          this.percentageDetail = this.percentageDetail + 15
        }
      }, 15000)
      setTimeout(() => {
        if (this.percentageDetail <= 80) {
          this.percentageDetail = this.percentageDetail + 15
        }
      }, 25000)
      setTimeout(() => {
        if (this.percentageDetail <= 80) {
          this.percentageDetail = this.percentageDetail + 15
        }
      }, 35000)
    },
    getTreeCheckedNodes() {
      this.option = this.$refs.tree.getCheckedNodes().filter(x => {
        if (typeof x.children === 'undefined') { return true }
      })
      this.defaultTreeDetail = this.$refs.tree.getCheckedKeys()
    },
    getTreeDetailCheckedNodes() {
      console.log(this.$refs.treeDetail.getCheckedNodes())
    },
    openPanel(index, i) {
      this.timeList[index].eventList[i].isShow = !this.timeList[index].eventList[i].isShow
    },
    searchUserList() {
      this.userListLoading = true
      if (this.attrValue !== '') {
        getUserList({ [this.attr]: this.attrValue, product: this.product }).then(response => {
          this.userList = response.data
          this.userListLoading = false
        })
      }
    },
    getOptions() {
      getOptions({ product: this.product }).then(response => {
        this.totalOption = response.data
      })
    },
    closeUserQuery() {
      this.isTotal = false
      this.showChart = true
      this.showTimeline = true
      this.pageNo = 1
      this.percentag = 15
      this.percentageDetail = 15
      this.userDialogVisible = true
      this.dialogVisible = false
      this.chartWidth = '100%'
      this.pageNo = 1
      setTimeout(() => {
        this.showChart = true
        this.getTreeCheckedNodes()
        if (this.attrValue !== '') {
          // getUserInfo({ userId: this.userId, product: this.product }).then(response => {
          //   this.attrList = response.data
          //   this.userInfoLoading = false
          // })
          const op = this.$refs.tree.getCheckedNodes().filter(x => {
            if (typeof x.children === 'undefined') { return true }
          })
          getUserEventNum({ userId: this.userId, product: this.product, startTime: this.dateRange[0].getTime(),
            endTime: this.dateRange[1].getTime(), eventList: op }).then(response => {
            this.showChart = false
            this.myChart = this.$echarts.getInstanceByDom(document.getElementById(this.id))
            if (!this.myChart) {
              this.myChart = this.$echarts.init(document.getElementById(this.id))
              this.myChart.on('click', (params) => {
                this.chart.series[0].data[params.dataIndex].itemStyle.color = '#3daae3'
                this.chart.series[0].data.forEach((x, index) => {
                  if (index !== params.dataIndex) {
                    x.itemStyle.color = '#b1ddf4'
                  }
                })
                this.myChart.setOption(this.chart, true)
                this.myChart.resize()
                this.pageNo = 1
                this.eventFlag = false
                this.showTimeline = true
                this.percentageDetail = 15
                this.barDetailProcess()
                const op = this.$refs.treeDetail.getCheckedNodes()
                this.currentDate = params.name
                getUserEventDetailForDate({ userId: this.userId, product: this.product,
                  date: params.name, eventList: op, sort: this.sortOperator, pageNo: this.pageNo }).then(response => {
                  this.showTimeline = false
                  this.timeList = response.data
                })
              })
            }
            this.chart.xAxis[0].data = response.data.x
            this.chart.series[0].data = response.data.y.map((x) => {
              return { value: x, itemStyle: { color: '#3daae3' }}
            })
            this.pieOptions.series[0].data = response.data.pip
            this.myChart.setOption(this.chart, true)
            this.myChart.resize({ height: 350, width: window.innerWidth - 328 })
          })
          getUserEventDetail({ userId: this.userId, product: this.product, startTime: this.dateRange[0].getTime(),
            endTime: this.dateRange[1].getTime(), eventList: op, sort: this.sortOperator, pageNo: this.pageNo }).then(response => {
            this.showTimeline = false
            this.timeList = response.data
          })
        } else {
          this.userInfoLoading = false
        }
        this.barProcess()
        this.barDetailProcess()
      }, 300)
    },
    enterQuery(row) {
      this.userId = row.userId
      this.attrList = row.userInfo
      this.getOptions()
      this.closeUserQuery()
    },
    reloadChart() {
      this.getTreeCheckedNodes()
      this.showChart = true
      this.switchPieValue = false
      this.percentage = 15
      this.barProcess()
      const op = this.$refs.tree.getCheckedNodes().filter(x => {
        if (typeof x.children === 'undefined') { return true }
      })
      getUserEventNum({ userId: this.userId, product: this.product, startTime: this.dateRange[0].getTime(),
        endTime: this.dateRange[1].getTime(), eventList: op }).then(response => {
        this.showChart = false
        this.chart.xAxis[0].data = response.data.x
        this.chart.series[0].data = response.data.y.map((x) => {
          return { value: x, itemStyle: { color: '#3daae3' }}
        })
        this.pieOptions.series[0].data = response.data.pip
        this.myChart.setOption(this.chart, true)
        this.myChart.resize({ height: 350, width: window.innerWidth - 328 })
      })
    },
    reloadTimeLine(flag) {
      this.eventFlag = false
      this.showTimeline = true
      this.percentageDetail = 15
      this.barDetailProcess()
      this.pageNo = 1
      const op = this.$refs.treeDetail.getCheckedNodes()
      if (this.currentDate !== '' && flag) {
        getUserEventDetailForDate({ userId: this.userId, product: this.product,
          date: this.currentDate, eventList: op, sort: this.sortOperator, pageNo: this.pageNo }).then(response => {
          this.showTimeline = false
          this.timeList = response.data
        })
      } else {
        getUserEventDetail({ userId: this.userId, product: this.product, startTime: this.dateRange[0].getTime(),
          endTime: this.dateRange[1].getTime(), eventList: op, sort: this.sortOperator, pageNo: this.pageNo }).then(response => {
          this.showTimeline = false
          this.timeList = response.data
        })
      }
    },
    pageTimeLine() {
      this.eventFlag = false
      this.showTimeline = true
      this.percentageDetail = 15
      this.barDetailProcess()
      const op = this.$refs.treeDetail.getCheckedNodes()
      this.pageNo = this.pageNo + 1
      if (this.currentDate !== '') {
        getUserEventDetailForDate({ userId: this.userId, product: this.product,
          date: this.currentDate, eventList: op, sort: this.sortOperator, pageNo: this.pageNo }).then(response => {
          this.showTimeline = false
          if (response.data.length === 0) {
            this.isTotal = true
          } else {
            response.data.forEach(x => {
              this.timeList.push(x)
            })
          }
        })
      } else {
        getUserEventDetail({ userId: this.userId, product: this.product, startTime: this.dateRange[0].getTime(),
          endTime: this.dateRange[1].getTime(), eventList: op, sort: this.sortOperator, pageNo: this.pageNo }).then(response => {
          this.showTimeline = false
          if (response.data.length === 0) {
            this.isTotal = true
          } else {
            response.data.forEach(x => {
              this.timeList.push(x)
            })
          }
        })
      }
    },
    reloadAll() {
      this.totalFlag = false
      this.reloadChart()
      setTimeout(() => {
        this.reloadTimeLine(false)
      }, 100)
    },
    showNumber() {
      this.chart.series[0].label.show = this.switchValue
      setTimeout(() => {
        this.myChart.setOption(this.chart, true)
        this.myChart.resize()
      }, 200)
    },
    sortOption() {
      if (this.sortOperator === 'desc') {
        this.sortOperator = 'asc'
      } else {
        this.sortOperator = 'desc'
      }
      this.reloadTimeLine(true)
    },
    showPie() {
      if (this.switchPieValue) {
        this.chartWidth = 'calc(100% - 450px)'
        setTimeout(() => {
          this.myPieChart = this.$echarts.getInstanceByDom(document.getElementById(this.pieId))
          if (!this.myPieChart) {
            this.myPieChart = this.$echarts.init(document.getElementById(this.pieId))
            this.myPieChart.setOption(this.pieOptions, true)
            this.myPieChart.resize()
          }
        }, 200)
        setTimeout(() => {
          this.myChart.setOption(this.chart, true)
          this.myChart.resize({ height: 350, width: window.innerWidth - 328 - 450 })
        }, 300)
      } else {
        this.chartWidth = '100%'
        setTimeout(() => {
          this.myChart.setOption(this.chart, true)
          this.myChart.resize({ height: 350, width: window.innerWidth - 328 })
        }, 300)
      }
    },
    openDialog() {
      this.dialogVisible = true
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
