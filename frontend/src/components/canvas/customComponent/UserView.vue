<template>
  <div
    v-loading="loadingFlag"
    :class="[
      {
        ['active']: active
      },
      'rect-shape'
    ]"
  >
    <EditBarView
      v-if="editBarViewShowFlag"
      :element="element"
      :show-position="showPosition"
      :panel-id="panelInfo.id"
      :chart-title="chart.title || chart.name"
      :is-edit="isEdit"
      :view-id="element.propValue.viewId"
      @showViewDetails="openChartDetailsDialog"
    />
    <div
      v-if="requestStatus==='error'"
      class="chart-error-class"
    >
      <div class="chart-error-message-class">
        {{ message }},{{ $t('chart.chart_show_error') }}
        <br>
        {{ $t('chart.chart_error_tips') }}
      </div>
    </div>
    <plugin-com
      v-if="chart.isPlugin"
      :ref="element.propValue.id"
      :component-name="chart.type + '-view'"
      :obj="{active, chart, trackMenu, searchCount, terminalType: scaleCoefficientType}"
      :chart="chart"
      :track-menu="trackMenu"
      :search-count="searchCount"
      :terminal-type="scaleCoefficientType"
      :scale="scale"
      :theme-style="element.commonBackground"
      :canvas-style-data="canvasStyleData"
      class="chart-class"
      @onChartClick="chartClick"
      @onJumpClick="jumpClick"
      @trigger-edit-click="pluginEditHandler"
    />
    <de-rich-text-view
      v-else-if="richTextViewShowFlag"
      :ref="element.propValue.id"
      :scale="scale"
      :element="element"
      :prop-value="element.propValue.textValue"
      :active="active"
      :edit-mode="editMode"
      :data-row-select="dataRowSelect"
      :data-row-name-select="dataRowNameSelect"
    />
    <chart-component
      v-else-if="charViewShowFlag"
      :ref="element.propValue.id"
      class="chart-class"
      :chart="chart"
      :track-menu="trackMenu"
      :search-count="searchCount"
      :terminal-type="scaleCoefficientType"
      :scale="scale"
      :theme-style="element.commonBackground"
      :active="active"
      @onChartClick="chartClick"
      @onJumpClick="jumpClick"
    />
    <chart-component-g2
      v-else-if="charViewG2ShowFlag"
      :ref="element.propValue.id"
      class="chart-class"
      :chart="chart"
      :track-menu="trackMenu"
      :search-count="searchCount"
      :scale="scale"
      @onChartClick="chartClick"
      @onJumpClick="jumpClick"
    />
    <chart-component-s2
      v-else-if="charViewS2ShowFlag"
      :ref="element.propValue.id"
      class="chart-class"
      :chart="chart"
      :terminal-type="scaleCoefficientType"
      :track-menu="trackMenu"
      :search-count="searchCount"
      @onChartClick="chartClick"
      @onJumpClick="jumpClick"
      @onPageChange="pageClick"
    />
    <table-normal
      v-else-if="tableShowFlag"
      :ref="element.propValue.id"
      :show-summary="chart.type === 'table-normal'"
      :chart="chart"
      class="table-class"
      @onPageChange="pageClick"
    />
    <label-normal
      v-else-if="labelShowFlag"
      :ref="element.propValue.id"
      :chart="chart"
      class="table-class"
    />
    <label-normal-text
      v-else-if="labelTextShowFlag"
      :ref="element.propValue.id"
      :chart="chart"
      class="table-class"
      :track-menu="trackMenu"
      :search-count="searchCount"
      @onChartClick="chartClick"
      @onJumpClick="jumpClick"
    />
    <div style="position: absolute;left: 8px;bottom:8px;">
      <drill-path
        :drill-filters="drillFilters"
        :theme-style="element.commonBackground"
        @onDrillJump="drillJump"
      />
    </div>

    <!--dialog-->
    <!--视图详情-->
    <el-dialog
      :visible.sync="chartDetailsVisible"
      width="80%"
      class="dialog-css"
      :destroy-on-close="true"
      :show-close="true"
      :append-to-body="true"
      top="5vh"
    >
      <span
        v-if="chartDetailsVisible"
        style="position: absolute;right: 70px;top:15px"
      >
        <el-button
          v-if="showChartInfoType==='enlarge' && hasDataPermission('export',panelInfo.privileges)&& showChartInfo && showChartInfo.type !== 'symbol-map'"
          class="el-icon-picture-outline"
          size="mini"
          :disabled="imageDownloading"
          @click="exportViewImg"
        >
          {{ $t('chart.export_img') }}
        </el-button>
        <el-button
          v-if="showChartInfoType==='details'"
          size="mini"
          @click="exportJSON"
        >
          导出JSON
        </el-button>
        <el-button
          v-if="showChartInfoType==='details' && hasDataPermission('export',panelInfo.privileges)"
          size="mini"
          :disabled="$store.getters.loadingMap[$store.getters.currentPath]"
          @click="exportExcel"
        >
          <svg-icon
            icon-class="ds-excel"
            class="ds-icon-excel"
          />{{ $t('chart.export') }}Excel
        </el-button>
      </span>
      <user-view-dialog
        v-if="chartDetailsVisible && showChartInfoType!=='clock'"
        ref="userViewDialog"
        :chart="showChartInfo"
        :chart-table="showChartTableInfo"
        :canvas-style-data="canvasStyleData"
        :open-type="showChartInfoType"
      />
      <el-table
        v-if="showChartInfoType==='clock'"
        :data="indexList"
        border
        style="width: 100%"
      >
        <el-table-column
          prop="name"
          label="指标"
        />
        <el-table-column
          prop="success"
          label="累计成功报警次数"
        />
        <el-table-column
          prop="failed"
          label="累计失败报警次数"
        />
        <el-table-column
          prop="lastTime"
          label="上次报警时间"
        />
        <el-table-column
          label="状态"
        >
          <template slot-scope="{row}">
            <el-switch
              v-model="row.status"
              active-color="#13ce66"
              inactive-color="#ff4949"
              @change="updateStatus(row)"
            />
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="200"
        >
          <template slot-scope="{row}">
            <el-button
              type="text"
              @click="editAlarm(row)"
            >编辑</el-button>
            <el-button
              type="text"
              @click="openLog(row)"
            >调度日志</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
    <!--报警详情-->
    <el-dialog
      :visible.sync="chartAlarmVisible"
      width="100%"
      class="alarm-css"
      :destroy-on-close="true"
      :append-to-body="true"
      :fullscreen="true"
      :show-close="false"
    >
      <template slot="title">
        <div class="de-ds-form">
          <div
            class="de-ds-top"
          >
            <span class="name">
              <i
                class="el-icon-arrow-left"
                @click="logOut"
              />
              {{ $t('commons.edit') + alarm.name + '报警' }}
            </span>
            <div class="apply">
              <deBtn
                secondary
                @click="logOut"
              >{{ $t('commons.cancel') }}
              </deBtn>
              <deBtn
                type="primary"
                @click="save"
              >{{ $t('commons.save') }}
              </deBtn>
            </div>
          </div>
        </div>
      </template>
      <div style="height: 100%;background-color: #f6f8fa;display: flex;justify-content: center">
        <div style="width: 65%">
          <div style="height: 33%;background-color: #FFFFFF;padding: 5%;margin: 10px">
            <div style="font-weight: bold;margin-bottom: 15px;font-size: 16px">{{ '基本信息' }}</div>
            <el-divider />
            <el-form
              label-position="left"
              label-width="100px"
              :v-model="alarm"
            >
              <el-form-item label="图表名称">
                {{ chart.title }}
              </el-form-item>
              <el-form-item label="指标名称">
                {{ alarm.name }}
              </el-form-item>
              <el-form-item
                v-if="typeof chart.data !== 'undefined' && typeof chart.data.sourceFields !== 'undefined'"
                label="时间字段"
              >
                <el-select
                  v-model="alarm.timeField"
                  style="width: 140px"
                  value-key="id"
                  filterable
                  placeholder="请选择"
                >
                  <el-option
                    v-for="(item,index) in chart.data.sourceFields"
                    :key="index"
                    :label="item.name"
                    :value="item"
                  />
                </el-select>
                <el-select
                  v-model="alarm.format"
                  style="width: 220px"
                  placeholder="请选择格式"
                >
                  <el-option
                    label="yyyy"
                    value="yyyy"
                  />
                  <el-option
                    label="yyyy-MM"
                    value="yyyy-MM"
                  />
                  <el-option
                    label="yyyy-MM-dd"
                    value="yyyy-MM-dd"
                  />
                  <el-option
                    label="yyyy-MM-dd HH"
                    value="yyyy-MM-dd HH"
                  />
                  <el-option
                    label="yyyy-MM-dd HH:mm"
                    value="yyyy-MM-dd HH:mm"
                  />
                  <el-option
                    label="yyyy-MM-dd HH:mm:ss"
                    value="yyyy-MM-dd HH:mm:ss"
                  />
                </el-select>
              </el-form-item>
              <el-form-item
                label="时间范围"
              >
                <el-input
                  v-model="alarm.timeNumber"
                  type="number"
                  style="width: 160px"
                />
                <el-select
                  v-model="alarm.timeType"
                  style="width: 100px"
                  placeholder="请选择时间单位"
                >
                  <el-option
                    label="秒"
                    value="秒"
                  />
                  <el-option
                    label="分钟"
                    value="分钟"
                  />
                  <el-option
                    label="小时"
                    value="小时"
                  />
                  <el-option
                    label="天"
                    value="天"
                  />
                </el-select>
              </el-form-item>
            </el-form>
          </div>
          <div style="background-color: #FFFFFF;padding: 5%;margin: 10px">
            <div style="font-weight: bold;margin-bottom: 15px;font-size: 16px">{{ '设置预警规则' }}</div>
            <el-divider />
            <el-form
              label-position="left"
              label-width="100px"
              :v-model="alarm"
              :rules="alarmFormRules"
            >
              <el-form-item
                label="cron表达式"
              >
                <el-input
                  v-model="alarm.cron"
                  style="width: 240px"
                />
                <el-button
                  type="primary"
                  @click="dialogFormVisibleCron = true"
                >
                  生成表达式
                  <i class="el-icon-time el-icon--right" />
                </el-button>
              </el-form-item>
              <el-form-item
                v-for="(item,index) in alarm.rules"
                :key="index"
                :label="'规则'+ (index + 1)"
              >
                <div style="display: flex;justify-content: left">
                  <el-select
                    v-model="item.operate"
                    style="width: 100px"
                    placeholder="请选择"
                  >
                    <el-option
                      label="高于"
                      value="高于"
                    />
                    <el-option
                      label="低于"
                      value="低于"
                    />
                  </el-select>
                  <el-select
                    v-model="item.type"
                    style="width: 170px"
                    placeholder="请选择"
                  >
                    <el-option
                      label="固定值"
                      value="固定值"
                    />
                    <el-option
                      label="环比过去N天平均值"
                      value="环比过去N天平均值"
                    />
                    <el-option
                      label="同比上月同期"
                      value="同比上月同期"
                    />
                  </el-select>
                  <span v-show="item.type === '环比过去N天平均值'">
                    <el-input
                      v-model="item.numDay"
                      type="number"
                      style="width: 70px"
                    />天
                  </span>
                  <el-input
                    v-model="item.value"
                    style="width: 160px"
                  />
                </div>
                <div style="display: flex;justify-content: left">
                  报警条件：
                  <el-select
                    v-model="item.condition"
                    style="width: 140px"
                    value-key="id"
                    filterable
                    clearable
                    placeholder="请选择"
                  >
                    <el-option
                      v-for="(item1,index1) in chart.data.sourceFields"
                      :key="index1"
                      :label="item1.name"
                      :value="item1.id"
                    />
                  </el-select>
                  =
                  <el-input
                    v-model="item.conditionValue"
                    style="width: 160px"
                  />
                </div>
                <div style="display: flex;justify-content: left">
                  发送设置:
                  <el-select
                    v-model="item.send.type"
                    style="width: 140px"
                    placeholder="请选择"
                  >
                    <el-option
                      v-for="(type,i) in options.sendType"
                      :key="i"
                      :label="type"
                      :value="type"
                    />
                  </el-select>
                  <el-select
                    v-if="item.send.type === '飞书个人' || item.send.type === '邮箱' || item.send.type === '电话'"
                    v-model="item.send.users"
                    value-key="userId"
                    multiple
                    filterable
                    placeholder="请选择"
                  >
                    <el-option
                      v-for="item3 in options.userList"
                      :key="item3.userId"
                      :label="item3.nickName"
                      :value="item3"
                    />
                  </el-select>
                  <el-input
                    v-else
                    v-model="item.send.link"
                    style="width: 400px"
                  />
                </div>
                <div style="display: flex;justify-content: left;align-items: center">
                  报警时图表背景颜色：
                  <el-color-picker
                    v-model="item.color"
                    class="color-picker-style"
                    :predefine="predefineColors"
                  />
                  <i
                    v-if="index !== 0"
                    class="el-icon-minus"
                    style="color: #cc0000;cursor: pointer;margin-left: 5px"
                    @click="subAlarmRuleType(index)"
                  />
                </div>
              </el-form-item>
              <el-form-item>
                <span
                  class="add-item"
                  @click="addAlarmRuleType"
                ><i
                  class="el-icon-plus"
                />添加规则</span>
              </el-form-item>
            </el-form>
          </div>
          <div style="background-color: #FFFFFF;padding: 5%;margin: 10px">
            <div style="font-weight: bold;margin-bottom: 15px;font-size: 16px">{{ '通知方式' }}</div>
            <el-divider />
            <el-form
              label-position="left"
              label-width="100px"
              :v-model="alarm"
            >
              <el-form-item
                v-for="(item,index) in alarm.sends"
                :key="index"
                :label="index === 0 ?'发送设置':''"
              >
                <el-select
                  v-model="item.type"
                  style="width: 140px"
                  placeholder="请选择"
                >
                  <el-option
                    v-for="(type,i) in options.sendType"
                    :key="i"
                    :label="type"
                    :value="type"
                  />
                </el-select>
                <el-select
                  v-if="item.type === '飞书个人' || item.type === '邮箱' || item.type === '电话'"
                  v-model="item.users"
                  value-key="userId"
                  multiple
                  filterable
                  placeholder="请选择"
                >
                  <el-option
                    v-for="item in options.userList"
                    :key="item.userId"
                    :label="item.nickName"
                    :value="item"
                  />
                </el-select>
                <el-input
                  v-else
                  v-model="item.link"
                  style="width: 400px"
                />
                <i
                  v-if="index !== 0"
                  class="el-icon-minus"
                  style="color: #cc0000;cursor: pointer;margin-left: 5px"
                  @click="subAlarmSendType(index)"
                />
              </el-form-item>
              <el-form-item>
                <span
                  class="add-item"
                  @click="addAlarmSendType"
                ><i class="el-icon-plus" />添加发送设置</span>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </div>
      <template slot="footer">
        <div style="background-color: #f6f8fa;height: 100%" />
      </template>
    </el-dialog>
    <!--cron-->
    <el-dialog
      :visible.sync="dialogFormVisibleCron"
      title="cron表达式生成器"
      width="60%"
      @close="dialogFormVisibleCron = false"
    >
      <crontab
        :expression="alarm.cron"
        @fill="handleCron"
        @hide="dialogFormVisibleCron = false"
      />
    </el-dialog>
    <!--日志-->
    <el-dialog
      :visible.sync="dialogFormVisibleLog"
      title="调度日志"
      width="60%"
      @close="dialogFormVisibleLog = false"
    >
      <div style="height: 600px">
        <grid-table
          style="height: 100%"
          :table-data="jobLogList"
          current-row-key="id"
          :pagination="paginationConfig"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        >
          <el-table-column type="expand">
            <template slot-scope="{row}">
              <json-viewer
                :value="JSON.parse(row.remark)"
              />
            </template>
          </el-table-column>
          <el-table-column
            key="id"
            align="center"
            show-overflow-tooltip
            label="id"
            prop="id"
          />
          <el-table-column
            key="sourceName"
            align="center"
            show-overflow-tooltip
            label="标题"
            prop="sourceName"
          />
          <el-table-column
            key="remark"
            show-overflow-tooltip
            align="center"
            label="详细信息"
            prop="remark"
          />
          <el-table-column
            key="operateType"
            show-overflow-tooltip
            align="center"
            label="执行状态"
            prop="operateType"
          >
            <template #default="{ row }">
              <el-tag>{{ row.operateType === 30 ? '执行成功' : '执行失败' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column
            key="nickName"
            show-overflow-tooltip
            align="center"
            label="调度时间"
            prop="nickName"
          />
        </grid-table>
      </div>
    </el-dialog>

    <!--手机视图详情-->
    <el-dialog
      class="mobile-dialog-css"
      :visible.sync="mobileChartDetailsVisible"
      :fullscreen="true"
      :append-to-body="true"
      :destroy-on-close="true"
    >
      <user-view-mobile-dialog
        v-if="mobileChartDetailsVisible"
        :canvas-style-data="canvasStyleData"
        :chart="showChartInfo"
        :chart-table="showChartTableInfo"
      />
    </el-dialog>
  </div>
</template>

<script>
import { saveScheduler, listScheduler, statusScheduler, logScheduler } from '@/api/chart/chart'
import { allUsers } from '@/api/system/user'
import { viewData } from '@/api/panel/panel'
import { viewInfo } from '@/api/link'
import ChartComponent from '@/views/chart/components/ChartComponent.vue'
import TableNormal from '@/views/chart/components/table/TableNormal'
import LabelNormal from '../../../views/chart/components/normal/LabelNormal'
import { uuid } from 'vue-uuid'
import bus from '@/utils/bus'
import { mapState } from 'vuex'
import { isChange } from '@/utils/conditionUtil'
import { BASE_CHART_STRING, COLOR_PANEL } from '@/views/chart/chart/chart'
import { deepCopy } from '@/components/canvas/utils/utils'
import { getLinkToken, getToken } from '@/utils/auth'
import DrillPath from '@/views/chart/view/DrillPath'
import { areaMapping } from '@/api/map/map'
import ChartComponentG2 from '@/views/chart/components/ChartComponentG2'
import EditBarView from '@/components/canvas/components/editor/EditBarView'
import { adaptCurTheme, customAttrTrans, customStyleTrans, recursionTransObj } from '@/components/canvas/utils/style'
import ChartComponentS2 from '@/views/chart/components/ChartComponentS2'
import PluginCom from '@/views/system/plugin/PluginCom'
import LabelNormalText from '@/views/chart/components/normal/LabelNormalText'
import { viewEditSave, viewPropsSave } from '@/api/chart/chart'
import { checkAddHttp } from '@/utils/urlUtils'
import DeRichTextView from '@/components/canvas/customComponent/DeRichTextView'
import Vue from 'vue'
import { formatterItem, valueFormatter } from '@/views/chart/chart/formatter'
import UserViewDialog from '@/components/canvas/customComponent/UserViewDialog'
import UserViewMobileDialog from '@/components/canvas/customComponent/UserViewMobileDialog'
import Crontab from '@/components/dispatch/Crontab/index'
import msgCfm from '@/components/msgCfm/index'
import GridTable from '@/components/gridTable/index.vue'
import JsonViewer from 'vue-json-viewer'

export default {
  name: 'UserView',
  components: {
    JsonViewer,
    UserViewMobileDialog,
    UserViewDialog,
    DeRichTextView,
    LabelNormalText,
    PluginCom,
    GridTable,
    ChartComponentS2,
    EditBarView,
    ChartComponent,
    TableNormal,
    LabelNormal,
    DrillPath,
    ChartComponentG2,
    Crontab
  },
  mixins: [msgCfm],
  props: {
    inScreen: {
      type: Boolean,
      required: false,
      default: true
    },
    canvasId: {
      type: String,
      required: true
    },
    element: {
      type: Object,
      default: null
    },
    outStyle: {
      type: Object,
      required: false,
      default: function() {
        return {}
      }
    },
    searchCount: {
      type: Number,
      required: false,
      default: 0
    },
    active: {
      type: Boolean,
      required: false,
      default: false
    },
    componentIndex: {
      type: Number,
      required: false
    },
    inTab: {
      type: Boolean,
      required: false,
      default: false
    },
    isEdit: {
      type: Boolean,
      require: false,
      default: true
    },
    terminal: {
      type: String,
      default: 'pc'
    },
    filters: {
      type: Array,
      default: () => []
    },
    canvasStyleData: {
      type: Object,
      required: false,
      default: function() {
        return {}
      }
    },
    showPosition: {
      type: String,
      required: false,
      default: 'NotProvided'
    },
    editMode: {
      type: String,
      require: false,
      default: 'preview'
    }
  },
  data() {
    return {
      paginationConfig: {
        currentPage: 1,
        pageSize: 10,
        total: 0
      },
      layout: 'total, sizes, prev, pager, next, jumper',
      predefineColors: COLOR_PANEL,
      alarm: {},
      jobLogList: [],
      imageDownloading: false,
      innerRefreshTimer: null,
      dialogFormVisibleLog: false,
      mobileChartDetailsVisible: false,
      chartDetailsVisible: false,
      chartAlarmVisible: false,
      dialogFormVisibleCron: false,
      showChartInfo: {},
      showChartTableInfo: {},
      showChartInfoType: 'details',
      dataRowNameSelect: {},
      dataRowSelect: {},
      curFields: [],
      indexList: [],
      isFirstLoad: true, // 是否是第一次加载
      refId: null,
      chart: BASE_CHART_STRING,
      requestStatus: 'success',
      message: null,
      drillClickDimensionList: [],
      drillFilters: [],
      drillFields: [],
      places: [],
      httpRequest: {
        status: true,
        msg: ''
      },
      timeMachine: null,
      scaleTimeMachine: null,
      changeIndex: 0,
      changeScaleIndex: 0,
      pre: null,
      // string
      sourceCustomAttrStr: null,
      // obj
      sourceCustomAttr: null,
      // string
      sourceCustomStyleStr: null,
      // obj
      sourceCustomStyle: null,
      scale: 1,
      currentPage: {
        page: 1,
        pageSize: 20,
        show: 0
      },
      view: {},
      alarmFormRules: {
        cron: [
          {
            required: true,
            message: this.$t('commons.input_content'),
            trigger: 'blur'
          }
        ],
        rules: { operate: [{
          required: true,
          message: this.$t('commons.input_content'),
          trigger: 'blur'
        }], type: [{
          required: true,
          message: this.$t('commons.input_content'),
          trigger: 'blur'
        }], value: [{
          required: true,
          message: this.$t('commons.input_content'),
          trigger: 'blur'
        }] }
      },
      options: {
        sendType: ['飞书群组', '飞书个人', '邮箱', '电话'],
        userList: []
      }
    }
  },
  computed: {
    // 首次加载且非编辑状态新复制的视图，使用外部filter
    initLoad() {
      return !(this.isEdit && this.currentCanvasNewId.includes(this.element.id)) && this.isFirstLoad
    },
    scaleCoefficient() {
      if (this.terminal === 'pc' && !this.mobileLayoutStatus) {
        return 1.1
      } else {
        return 4.5
      }
    },
    scaleCoefficientType() {
      if (this.terminal === 'pc' && !this.mobileLayoutStatus) {
        return 'pc'
      } else {
        return 'mobile'
      }
    },
    editBarViewShowFlag() {
      return (this.active && this.inTab && !this.mobileLayoutStatus) && !this.showPosition.includes('multiplexing') || this.showPosition.includes('email-task')
    },
    richTextViewShowFlag() {
      return this.httpRequest.status && this.chart.type && this.chart.type === 'richTextView'
    },
    charViewShowFlag() {
      return this.httpRequest.status && this.chart.type && !this.chart.type.includes('table') && !this.chart.type.includes('text') && this.chart.type !== 'label' && this.renderComponent() === 'echarts'
    },
    charViewG2ShowFlag() {
      return this.httpRequest.status && this.chart.type && !this.chart.type.includes('table') && !this.chart.type.includes('text') && this.chart.type !== 'label' && this.renderComponent() === 'antv'
    },
    charViewS2ShowFlag() {
      return this.httpRequest.status && this.chart.type && this.chart.type.includes('table') && !this.chart.type.includes('text') && this.chart.type !== 'label' && this.renderComponent() === 'antv'
    },
    tableShowFlag() {
      return this.httpRequest.status && this.chart.type && this.chart.type.includes('table') && this.renderComponent() === 'echarts'
    },
    labelShowFlag() {
      return this.httpRequest.status && this.chart.type && this.chart.type.includes('text')
    },
    labelTextShowFlag() {
      return this.httpRequest.status && this.chart.type && this.chart.type === 'label'
    },
    loadingFlag() {
      return (this.canvasStyleData.refreshViewLoading || (!this.innerRefreshTimer && this.searchCount === 0)) && this.requestStatus === 'waiting'
    },
    panelInfo() {
      return this.$store.state.panel.panelInfo
    },
    filter() {
      const filter = {}
      filter.filter = this.initLoad ? this.filters : this.cfilters
      filter.linkageFilters = this.element.linkageFilters
      filter.outerParamsFilters = this.element.outerParamsFilters
      filter.drill = this.drillClickDimensionList
      filter.resultCount = this.resultCount
      filter.resultMode = this.resultMode
      filter.queryFrom = 'panel'
      return filter
    },
    cfilters() {
      // 必要 勿删勿该  watch数组，哪怕发生变化 oldValue等于newValue ，深拷贝解决
      if (!this.element.filters) return []
      return JSON.parse(JSON.stringify(this.element.filters))
    },
    linkageFilters() {
      // 必要 勿删勿该  watch数组，哪怕发生变化 oldValue等于newValue ，深拷贝解决
      if (!this.element.linkageFilters) return []
      return JSON.parse(JSON.stringify(this.element.linkageFilters))
    },
    trackMenu() {
      const trackMenuInfo = []
      let linkageCount = 0
      let jumpCount = 0
      this.chart.data && this.chart.data.fields && this.chart.data.fields.forEach(item => {
        const sourceInfo = this.chart.id + '#' + item.id
        if (this.nowPanelTrackInfo[sourceInfo]) {
          linkageCount++
        }
      })
      this.chart.data && this.chart.data.fields && this.chart.data.fields.forEach(item => {
        const sourceInfo = this.chart.id + '#' + item.id
        if (this.nowPanelJumpInfo[sourceInfo]) {
          jumpCount++
        }
      })
      jumpCount && trackMenuInfo.push('jump')
      linkageCount && trackMenuInfo.push('linkage')
      this.drillFields.length && trackMenuInfo.push('drill')
      return trackMenuInfo
    },
    chartType() {
      return this.chart.type
    },
    hw() {
      return this.outStyle.width * this.outStyle.height
    },
    resultMode() {
      return this.canvasStyleData.panel && this.canvasStyleData.panel.resultMode || null
    },
    resultCount() {
      return this.canvasStyleData.panel && this.canvasStyleData.panel.resultCount || null
    },
    gap() {
      return this.canvasStyleData.panel && this.canvasStyleData.panel.gap || null
    },
    innerPadding() {
      return this.element.commonBackground && this.element.commonBackground.innerPadding || 0
    },
    ...mapState([
      'currentCanvasNewId',
      'nowPanelTrackInfo',
      'nowPanelJumpInfo',
      'publicLinkStatus',
      'previewCanvasScale',
      'mobileLayoutStatus',
      'panelViewDetailsInfo',
      'componentViewsData',
      'curBatchOptComponents'
    ])
  },
  watch: {
    'innerPadding': {
      handler: function(val1, val2) {
        if (val1 !== val2) {
          this.resizeChart()
        }
      },
      deep: true
    },
    'cfilters': {
      handler: function(val1, val2) {
        if (isChange(val1, val2) && !this.isFirstLoad) {
          this.getData(this.element.propValue.viewId)
        }
      },
      deep: true
    },
    linkageFilters: {
      handler(newVal, oldVal) {
        if (isChange(newVal, oldVal)) {
          this.getData(this.element.propValue.viewId)
        }
      },
      deep: true
    },
    resultCount: function() {
      this.getData(this.element.propValue.viewId, false)
    },
    resultMode: function() {
      this.getData(this.element.propValue.viewId, false)
    },
    gap: function() {
      this.resizeChart()
    },
    // 监听外部的样式变化 （非实时性要求）
    'hw': {
      handler(newVal, oldVla) {
        if (newVal !== oldVla && this.$refs[this.element.propValue.id]) {
          this.resizeChart()
        }
      },
      deep: true
    },
    // 监听外部的样式变化 （非实时性要求）
    outStyle: {
      handler(newVal, oldVla) {
      },
      deep: true
    },
    // 监听外部计时器变化
    searchCount: function(val1) {
      // 内部计时器启动 忽略外部计时器
      if (val1 > 0 && this.requestStatus !== 'waiting' && !this.innerRefreshTimer) {
        this.getData(this.element.propValue.viewId)
      }
    },
    'chartType': function(newVal, oldVal) {
      if ((newVal === 'map' || newVal === 'buddle-map') && newVal !== oldVal) {
        this.initAreas()
      }
    },
    // 监控缩放比例
    previewCanvasScale: {
      handler(newVal, oldVal) {
        this.destroyScaleTimeMachine()
        this.changeScaleIndex++
        this.chartScale(this.changeScaleIndex)
      },
      deep: true
    },
    'chart.yaxis': function(newVal, oldVal) {
      this.$emit('fill-chart-2-parent', this.chart)
    }
  },
  mounted() {
    bus.$on('tab-canvas-change', this.tabSwitch)
    this.bindPluginEvent()
    allUsers().then((res) => {
      this.options.userList = res.data
    })
  },
  beforeDestroy() {
    this.innerRefreshTimer && clearInterval(this.innerRefreshTimer)
    bus.$off('plugin-chart-click', this.pluginChartClick)
    bus.$off('plugin-jump-click', this.pluginJumpClick)
    bus.$off('plugin-add-view-track-filter', this.pluginAddViewTrackFilter)
    bus.$off('view-in-cache', this.viewInCache)
    bus.$off('batch-opt-change', this.batchOptChange)
    bus.$off('onSubjectChange', this.optFromBatchThemeChange)
    bus.$off('onThemeColorChange', this.optFromBatchThemeChange)
    bus.$off('onThemeAttrChange', this.optFromBatchSingleProp)
    bus.$off('clear_panel_linkage', this.clearPanelLinkage)
  },
  created() {
    this.refId = uuid.v1
    if (this.element && this.element.propValue && this.element.propValue.viewId) {
      // 如果watch.filters 已经进行数据初始化时候，此处放弃数据初始化
      this.getData(this.element.propValue.viewId, false)
    }
  },
  methods: {
    openLog(row) {
      this.dialogFormVisibleLog = true
      this.jobLogList = []
      const { currentPage, pageSize } = this.paginationConfig
      logScheduler(currentPage, pageSize, { id: row.id }).then((res) => {
        this.jobLogList = res.data.listObject
        this.paginationConfig.total = res.data.itemCount
      })
    },
    handleSizeChange(pageSize) {
      this.paginationConfig.currentPage = 1
      this.paginationConfig.pageSize = pageSize
      this.openLog()
    },
    handleCurrentChange(currentPage) {
      this.paginationConfig.currentPage = currentPage
      this.openLog()
    },
    tabSwitch(tabCanvasId) {
      if (this.charViewS2ShowFlag && tabCanvasId === this.canvasId && this.$refs[this.element.propValue.id]) {
        this.$refs[this.element.propValue.id].chartResize()
      }
    },
    // 编辑状态下 不启动刷新
    buildInnerRefreshTimer(refreshViewEnable = false, refreshUnit = 'minute', refreshTime = 5) {
      if (this.editMode === 'preview' && !this.innerRefreshTimer && refreshViewEnable) {
        this.innerRefreshTimer && clearInterval(this.innerRefreshTimer)
        const timerRefreshTime = refreshUnit === 'second' ? refreshTime * 1000 : refreshTime * 60000
        this.innerRefreshTimer = setInterval(() => {
          this.clearViewLinkage()
          this.getData(this.element.propValue.viewId)
        }, timerRefreshTime)
      }
    },
    clearViewLinkage() {
      this.$store.commit('clearViewLinkage', this.element.propValue.viewId)
    },
    responseResetButton() {
      if (!this.cfilters?.length) {
        this.getData(this.element.propValue.viewId, false)
      }
    },
    exportExcel() {
      this.$refs['userViewDialog'].exportExcel()
    },
    exportJSON() {
      this.$refs['userViewDialog'].exportJsonDownload()
    },
    exportViewImg() {
      this.imageDownloading = true
      this.$refs['userViewDialog'].exportViewImg(() => {
        this.imageDownloading = false
      })
    },
    pluginEditHandler(e) {
      this.$emit('trigger-plugin-edit', { e, id: this.element.id })
    },
    batchOptChange(param) {
      if (this.curBatchOptComponents.includes(this.element.propValue.viewId)) {
        this.optFromBatchSingleProp(param)
      }
    },
    optFromBatchSingleProp(param) {
      this.$store.commit('canvasChange')
      const updateParams = { 'id': this.chart.id }
      if (param.custom === 'customAttr') {
        const sourceCustomAttr = JSON.parse(this.sourceCustomAttrStr)
        if (!sourceCustomAttr[param.property]) {
          this.$set(sourceCustomAttr, param.property, {})
        }
        sourceCustomAttr[param.property][param.value.modifyName] = param.value[param.value.modifyName]
        this.sourceCustomAttrStr = JSON.stringify(sourceCustomAttr)
        this.chart.customAttr = this.sourceCustomAttrStr
        this.$store.commit('updateComponentViewsData', {
          viewId: this.chart.id,
          propertyKey: 'customAttr',
          propertyValue: this.sourceCustomAttrStr
        })
        updateParams['customAttr'] = this.sourceCustomAttrStr
      } else if (param.custom === 'customStyle') {
        const sourceCustomStyle = JSON.parse(this.sourceCustomStyleStr)
        if (param.property === 'margin') {
          sourceCustomStyle[param.property] = param.value
        }
        sourceCustomStyle[param.property][param.value.modifyName] = param.value[param.value.modifyName]
        this.sourceCustomStyleStr = JSON.stringify(sourceCustomStyle)
        this.chart.customStyle = this.sourceCustomStyleStr
        this.$store.commit('updateComponentViewsData', {
          viewId: this.chart.id,
          propertyKey: 'customStyle',
          propertyValue: this.sourceCustomStyleStr
        })
        updateParams['customStyle'] = this.sourceCustomStyleStr
      }
      viewPropsSave(this.panelInfo.id, updateParams).then(rsp => {
        this.active && bus.$emit('current-component-change')
      })
      this.$store.commit('recordViewEdit', { viewId: this.chart.id, hasEdit: true })
      this.mergeScale()
    },
    optFromBatchThemeChange() {
      const updateParams = { 'id': this.chart.id }
      const sourceCustomAttr = JSON.parse(this.sourceCustomAttrStr)
      const sourceCustomStyle = JSON.parse(this.sourceCustomStyleStr)
      adaptCurTheme(sourceCustomStyle, sourceCustomAttr, this.chart.type)
      this.sourceCustomAttrStr = JSON.stringify(sourceCustomAttr)
      this.chart.customAttr = this.sourceCustomAttrStr
      updateParams['customAttr'] = this.sourceCustomAttrStr
      this.sourceCustomStyleStr = JSON.stringify(sourceCustomStyle)
      this.chart.customStyle = this.sourceCustomStyleStr
      updateParams['customStyle'] = this.sourceCustomStyleStr
      viewPropsSave(this.panelInfo.id, updateParams).then(rsp => {
        this.active && bus.$emit('current-component-change')
      })
      this.$store.commit('recordViewEdit', { viewId: this.chart.id, hasEdit: true })
      this.mergeScale()
    },
    resizeChart() {
      if (this.chart.type === 'map') {
        this.destroyTimeMachine()
        this.changeIndex++
        this.chartResize(this.changeIndex)
      } else if (this.$refs[this.element.propValue.id]) {
        this.chart.isPlugin
          ? this.$refs[this.element.propValue.id].callPluginInner({ methodName: 'chartResize' })
          : this.$refs[this.element.propValue.id].chartResize()
      }
    },
    pluginChartClick(param) {
      param.viewId && param.viewId === this.element.propValue.viewId && this.chartClick(param)
    },
    pluginJumpClick(param) {
      param.viewId && param.viewId === this.element.propValue.viewId && this.jumpClick(param)
    },
    pluginAddViewTrackFilter(param) {
      param.viewId && param.viewId === this.element.propValue.viewId && this.addViewTrackFilter(param)
    },
    viewInCache(param) {
      this.view = param.view
      if (this.view && this.view.customAttr) {
        this.currentPage.pageSize = parseInt(JSON.parse(this.view.customAttr).size.tablePageSize)
      }
      param.viewId && param.viewId === this.element.propValue.viewId && this.getDataEdit(param)
    },
    clearPanelLinkage(param) {
      if (param.viewId === 'all' || param.viewId === this.element.propValue.viewId) {
        try {
          this.$refs[this.element.propValue.id].reDrawView()
        } catch (e) {
          console.error('reDrawView-error：', this.element.propValue.id)
        }
      }
    },
    bindPluginEvent() {
      bus.$on('plugin-chart-click', this.pluginChartClick)
      bus.$on('plugin-jump-click', this.pluginJumpClick)
      bus.$on('plugin-add-view-track-filter', this.pluginAddViewTrackFilter)
      bus.$on('view-in-cache', this.viewInCache)
      bus.$on('batch-opt-change', this.batchOptChange)
      bus.$on('onSubjectChange', this.optFromBatchThemeChange)
      bus.$on('onThemeColorChange', this.optFromBatchThemeChange)
      bus.$on('onThemeAttrChange', this.optFromBatchSingleProp)
      bus.$on('clear_panel_linkage', this.clearPanelLinkage)
    },
    addViewTrackFilter(linkageParam) {
      this.$store.commit('addViewTrackFilter', linkageParam)
    },
    // 根据仪表板的缩放比例，修改视图内部参数
    mergeScale() {
      this.scale = Math.min(this.previewCanvasScale.scalePointWidth, this.previewCanvasScale.scalePointHeight) * this.scaleCoefficient
      const customAttrChart = JSON.parse(this.sourceCustomAttrStr)
      const customStyleChart = JSON.parse(this.sourceCustomStyleStr)
      recursionTransObj(customAttrTrans, customAttrChart, this.scale, this.scaleCoefficientType)
      recursionTransObj(customStyleTrans, customStyleChart, this.scale, this.scaleCoefficientType)
      // 移动端地图标签不显示
      if (this.chart.type === 'map' && this.scaleCoefficientType === 'mobile') {
        customAttrChart.label.show = false
      }
      this.chart = {
        ...this.chart,
        customAttr: JSON.stringify(customAttrChart),
        customStyle: JSON.stringify(customStyleChart)
      }
    },
    getData(id, cache = true, dataBroadcast = false) {
      if (id) {
        this.requestStatus = 'waiting'
        this.message = null

        // 增加判断 仪表板公共连接中使用viewInfo 正常使用viewData
        let method = viewData
        const token = this.$store.getters.token || getToken()
        const linkToken = this.$store.getters.linkToken || getLinkToken()
        if (!token && linkToken) {
          method = viewInfo
        }
        const requestInfo = {
          ...this.filter,
          cache: cache,
          queryFrom: this.isEdit ? 'panel_edit' : 'panel'
        }
        if (this.panelInfo.proxy) {
          // method = viewInfo
          requestInfo.proxy = { userId: this.panelInfo.proxy }
        }
        // table-info明细表增加分页
        if (this.view && this.view.customAttr) {
          const attrSize = JSON.parse(this.view.customAttr).size
          if (this.chart.type === 'table-info' && this.view.datasetMode === 0 && (!attrSize.tablePageMode || attrSize.tablePageMode === 'page')) {
            requestInfo.goPage = this.currentPage.page
            requestInfo.pageSize = this.currentPage.pageSize === parseInt(attrSize.tablePageSize) ? this.currentPage.pageSize : parseInt(attrSize.tablePageSize)
          }
        }
        if (this.isFirstLoad) {
          this.element.filters = this.filters?.length ? JSON.parse(JSON.stringify(this.filters)) : []
        }
        method(id, this.panelInfo.id, requestInfo).then(response => {
          // 将视图传入echart组件
          if (response.success) {
            this.chart = response.data
            this.view = response.data
            if (typeof this.view.alarmColor !== 'undefined' && this.view.alarmColor !== null) {
              this.$emit('set-alarm-color', this.view.alarmColor)
            } else {
              this.$emit('set-alarm-color', '')
            }
            if (this.chart.type.includes('table')) {
              this.$store.commit('setLastViewRequestInfo', { viewId: id, requestInfo: requestInfo })
            }
            this.buildInnerRefreshTimer(this.chart.refreshViewEnable, this.chart.refreshUnit, this.chart.refreshTime)
            this.$emit('fill-chart-2-parent', this.chart)
            this.getDataOnly(response.data, dataBroadcast)
            this.chart['position'] = this.inTab ? 'tab' : 'panel'
            // 记录当前数据
            this.panelViewDetailsInfo[id] = JSON.stringify(this.chart)
            if (this.element.needAdaptor) {
              const customStyleObj = JSON.parse(this.chart.customStyle)
              const customAttrObj = JSON.parse(this.chart.customAttr)
              adaptCurTheme(customStyleObj, customAttrObj)
              this.chart.customStyle = JSON.stringify(customStyleObj)
              this.chart.customAttr = JSON.stringify(customAttrObj)
              viewEditSave(this.panelInfo.id, {
                id: this.chart.id,
                customStyle: this.chart.customStyle,
                customAttr: this.chart.customAttr
              })
              this.$store.commit('adaptorStatusDisable', this.element.id)
            }
            this.sourceCustomAttrStr = this.chart.customAttr
            this.sourceCustomStyleStr = this.chart.customStyle
            this.chart.drillFields = this.chart.drillFields ? JSON.parse(this.chart.drillFields) : []
            if (!response.data.drill) {
              this.drillClickDimensionList.splice(this.drillClickDimensionList.length - 1, 1)
              this.resetDrill()
            }
            this.drillFilters = JSON.parse(JSON.stringify(response.data.drillFilters ? response.data.drillFilters : []))
            this.drillFields = JSON.parse(JSON.stringify(response.data.drillFields))
            this.requestStatus = 'merging'
            this.mergeScale()
            this.initCurFields(this.chart)
            this.requestStatus = 'success'
            this.httpRequest.status = true
          } else {
            console.error('err2-' + JSON.stringify(response))
            this.requestStatus = 'error'
            this.message = response.message
          }
          this.isFirstLoad = false
          return true
        }).catch(err => {
          console.error('err-' + err)
          this.requestStatus = 'error'
          if (err.message && err.message.indexOf('timeout') > -1) {
            this.message = this.$t('panel.timeout_refresh')
          } else if (!err.response) {
            this.httpRequest.status = false
          } else {
            if (err.response) {
              this.httpRequest.status = err.response.data.success
              this.httpRequest.msg = err.response.data.message
              if (err && err.response && err.response.data) {
                this.message = err.response.data.message
              } else {
                this.message = err
              }
            }
          }
          this.isFirstLoad = false
          return true
        })
      }
    },
    initCurFields(chartDetails) {
      this.curFields = []
      this.dataRowSelect = []
      this.dataRowNameSelect = []
      if (chartDetails.data && chartDetails.data.sourceFields) {
        const checkAllAxisStr = chartDetails.xaxis + chartDetails.xaxisExt + chartDetails.yaxis + chartDetails.yaxisExt
        chartDetails.data.sourceFields.forEach(field => {
          if (checkAllAxisStr.indexOf(field.id) > -1) {
            this.curFields.push(field)
          }
        })
        // Get the corresponding relationship between id and value
        const nameIdMap = chartDetails.data.fields.reduce((pre, next) => {
          pre[next['dataeaseName']] = next['id']
          return pre
        }, {})
        const sourceFieldNameIdMap = chartDetails.data.fields.reduce((pre, next) => {
          pre[next['dataeaseName']] = next['name']
          return pre
        }, {})
        const rowData = chartDetails.data.tableRow[0]
        if (chartDetails.type === 'richTextView') {
          let yAxis = []
          try {
            yAxis = JSON.parse(chartDetails.yaxis)
          } catch (err) {
            yAxis = JSON.parse(JSON.stringify(chartDetails.yaxis))
          }
          const yDataeaseNames = []
          const yDataeaseNamesCfg = []
          yAxis.forEach(yItem => {
            yDataeaseNames.push(yItem.dataeaseName)
            yDataeaseNamesCfg[yItem.dataeaseName] = yItem.formatterCfg
          })
          this.rowDataFormat(rowData, yDataeaseNames, yDataeaseNamesCfg)
        }
        for (const key in rowData) {
          this.dataRowSelect[nameIdMap[key]] = rowData[key]
          this.dataRowNameSelect[sourceFieldNameIdMap[key]] = rowData[key]
        }
      }
      Vue.set(this.element.propValue, 'innerType', chartDetails.type)
      Vue.set(this.element.propValue, 'render', chartDetails.render)
      if (chartDetails.type === 'richTextView') {
        this.$nextTick(() => {
          bus.$emit('initCurFields-' + this.element.id)
        })
      }
    },
    rowDataFormat(rowData, yDataeaseNames, yDataeaseNamesCfg) {
      for (const key in rowData) {
        if (yDataeaseNames.includes(key)) {
          const formatterCfg = yDataeaseNamesCfg[key]
          const value = rowData[key]
          if (value === null || value === undefined) {
            rowData[key] = '-'
          }
          if (formatterCfg) {
            const v = valueFormatter(value, formatterCfg)
            rowData[key] = v && v.includes('NaN') ? value : v
          } else {
            const v = valueFormatter(value, formatterItem)
            rowData[key] = v && v.includes('NaN') ? value : v
          }
        }
      }
    },
    viewIdMatch(viewIds, viewId) {
      return !viewIds || viewIds.length === 0 || viewIds.includes(viewId)
    },
    openChartDetailsDialog(params) {
      const tableChart = deepCopy(this.chart)
      tableChart.customAttr = JSON.parse(this.chart.customAttr)
      tableChart.customStyle = JSON.parse(this.chart.customStyle)
      tableChart.customAttr.color.tableHeaderBgColor = '#f8f8f9'
      tableChart.customAttr.color.tableItemBgColor = '#ffffff'
      tableChart.customAttr.color.tableHeaderFontColor = '#7c7e81'
      tableChart.customAttr.color.tableFontColor = '#7c7e81'
      tableChart.customAttr.color.tableStripe = true
      tableChart.customAttr.size.tablePageMode = 'pull'
      tableChart.customStyle.text.show = false
      tableChart.customAttr = JSON.stringify(tableChart.customAttr)
      tableChart.customStyle = JSON.stringify(tableChart.customStyle)

      this.showChartInfo = this.chart
      if (typeof this.chart.data === 'undefined') {
        this.chart.data = {}
      }
      if (typeof this.chart.data.fields !== 'undefined') {
        this.indexList = this.chart.data.fields.filter((x) => { return x.extField === 1 || x.extField === 2 })
      }
      const arr = []
      if (typeof this.chart.data.series !== 'undefined') {
        this.chart.data.series.forEach((item) => {
          if (typeof this.chart.data.fields !== 'undefined') {
            this.chart.data.fields.forEach((x) => {
              if (x.name === item.name) {
                arr.push(x)
              }
            })
          }
        })
      }
      arr.forEach((x) => {
        let flag = true
        this.indexList.forEach((y) => {
          if (y.name === x.name) {
            flag = false
          }
        })
        if (flag) {
          this.indexList.push(x)
        }
      })
      if (typeof this.chart.yaxis !== 'undefined') {
        var yarr = JSON.parse(this.chart.yaxis)
        yarr.forEach((item) => {
          var flag = true
          this.indexList.forEach((x) => {
            if (x.id === item.id) {
              flag = false
            }
          })
          if (flag) {
            this.indexList.push(item)
          }
        })
      }
      listScheduler({ chartId: this.chart.id }).then(response => {
        const list = response.data
        const arr = []
        this.indexList.forEach((y) => {
          let flag = true
          list.forEach((x) => {
            const index = JSON.parse(x.indexField)
            if (y.id === index.id) {
              flag = false
              x.name = y.name
              if (typeof x.format === 'undefined') {
                this.$set(x, 'format', 'yyyy-MM-dd')
              }
              if (typeof x.timeField === 'undefined') {
                this.$set(x, 'timeField', { name: '' })
              } else {
                x.timeField = JSON.parse(x.timeField)
              }
              if (typeof x.timeNumber === 'undefined') {
                this.$set(x, 'timeNumber', 1)
              }
              if (typeof x.timeType === 'undefined') {
                this.$set(x, 'timeType', '天')
              }
              if (typeof x.sends === 'undefined') {
                this.$set(x, 'sends', [])
                x.sends.push({ type: '飞书群组' })
              } else {
                x.sends = JSON.parse(x.sends)
              }
              if (typeof x.rules === 'undefined') {
                this.$set(x, 'rules', [])
                x.rules.push({ type: '固定值', operate: '高于', color: '#FF4500', send: { type: '飞书群组' }})
              } else {
                x.rules = JSON.parse(x.rules)
                x.rules.forEach(a => {
                  if (typeof a.send === 'undefined') {
                    this.$set(a, 'send', { type: '飞书群组' })
                  }
                })
              }
              arr.push(x)
            }
          })
          if (flag) {
            const temp = { name: y.name, indexField: JSON.stringify(y), rules: [{ type: '固定值', operate: '高于', color: '#FF4500', send: { type: '飞书群组' }}] }
            arr.push(temp)
          }
        })
        this.indexList = arr
      })
      this.showChartTableInfo = tableChart
      this.showChartInfoType = params.openType
      if (!this.inScreen) {
        bus.$emit('pcChartDetailsDialog', {
          showChartInfo: this.showChartInfo,
          showChartTableInfo: this.showChartTableInfo,
          showChartInfoType: this.showChartInfoType
        })
      } else if (this.terminal === 'pc') {
        this.chartDetailsVisible = true
      } else {
        this.mobileChartDetailsVisible = true
      }
    },
    closeCron() {
      this.dialogFormVisibleCron = false
    },
    handleCron(cron) {
      this.alarm.cron = cron
      this.closeCron()
    },
    chartClick(param) {
      if (this.drillClickDimensionList.length < this.chart.drillFields.length - 1) {
        (this.chart.type === 'map' || this.chart.type === 'buddle-map') && this.sendToChildren(param)
        this.drillClickDimensionList.push({ dimensionList: param.data.dimensionList })
        this.getData(this.element.propValue.viewId)
      } else if (this.chart.drillFields.length > 0) {
        this.$message({
          type: 'error',
          message: this.$t('chart.last_layer'),
          showClose: true
        })
      }
    },
    editAlarm(row) {
      this.alarm = JSON.parse(JSON.stringify(row))
      if (typeof this.alarm.format === 'undefined') {
        this.$set(this.alarm, 'format', 'yyyy-MM-dd')
      }
      if (typeof this.alarm.timeField === 'undefined') {
        this.$set(this.alarm, 'timeField', { name: '' })
      }
      if (typeof this.alarm.timeNumber === 'undefined') {
        this.$set(this.alarm, 'timeNumber', 1)
      }
      if (typeof this.alarm.timeType === 'undefined') {
        this.$set(this.alarm, 'timeType', '天')
      }
      if (typeof this.alarm.sends === 'undefined') {
        this.$set(this.alarm, 'sends', [])
        this.alarm.sends.push({ type: '飞书群组' })
      }
      if (typeof this.alarm.rules === 'undefined') {
        this.$set(this.alarm, 'rules', [])
        this.alarm.rules.push({ type: '固定值', operate: '高于' })
      }
      this.chartAlarmVisible = true
    },
    subAlarmSendType(index) {
      this.alarm.sends.splice(index, 1)
    },
    addAlarmSendType() {
      this.alarm.sends.push({ type: '飞书群组' })
    },
    subAlarmRuleType(index) {
      this.alarm.rules.splice(index, 1)
    },
    addAlarmRuleType() {
      this.alarm.rules.push({ type: '固定值', operate: '高于', color: '#FF4500', send: { type: '飞书群组' }})
    },
    logOut() {
      this.chartAlarmVisible = false
    },
    save() {
      if (typeof this.alarm.timeField === 'undefined' || this.alarm.timeField === '' || typeof this.alarm.cron === 'undefined' || this.alarm.cron === '') {
        this.$warning('定时必须或者时间字段必须被设置！！')
      } else {
        saveScheduler(this.chart.id, {
          ...this.alarm,
          timeField: JSON.stringify(this.alarm.timeField),
          rules: JSON.stringify(this.alarm.rules),
          sends: JSON.stringify(this.alarm.sends)
        }).then((res) => {
          const arr = []
          const x = res.data
          if (typeof x.indexField !== 'undefined') {
            x.name = JSON.parse(x.indexField).name
          }
          if (typeof x.format === 'undefined') {
            this.$set(x, 'format', 'yyyy-MM-dd')
          }
          if (typeof x.timeField === 'undefined') {
            this.$set(x, 'timeField', { name: '' })
          } else {
            x.timeField = JSON.parse(x.timeField)
          }
          if (typeof x.timeNumber === 'undefined') {
            this.$set(x, 'timeNumber', 1)
          }
          if (typeof x.timeType === 'undefined') {
            this.$set(x, 'timeType', '天')
          }
          if (typeof x.sends === 'undefined') {
            this.$set(x, 'sends', [])
            x.sends.push({ type: '飞书群组' })
          } else {
            x.sends = JSON.parse(x.sends)
          }
          if (typeof x.rules === 'undefined') {
            this.$set(x, 'rules', [])
            x.rules.push({ type: '固定值', operate: '高于' })
          } else {
            x.rules = JSON.parse(x.rules)
          }
          arr.push(JSON.parse(JSON.stringify(x)))
          this.indexList.forEach((y) => {
            let flag = true
            const index = JSON.parse(res.data.indexField)
            if (JSON.parse(y.indexField).id === index.id) {
              flag = false
            }
            if (flag) {
              arr.push(y)
            }
          })
          this.indexList = arr
          this.chartAlarmVisible = false
          this.openMessageSuccess('commons.save_success')
        })
      }
    },
    updateStatus(row) {
      statusScheduler({ id: row.id, chartId: this.chart.id }).then((res) => {
        if (res.data.type === 'success') {
          this.openMessageSuccess(res.data.msg)
        } else {
          this.openMessageSuccess(res.data.msg, 'error')
          row.status = false
        }
      })
    },
    jumpClick(param) {
      let dimension, jumpInfo, sourceInfo
      // 如果有名称name 获取和name匹配的dimension 否则倒序取最后一个能匹配的
      if (param.name) {
        param.dimensionList.forEach(dimensionItem => {
          if (dimensionItem.id === param.name || dimensionItem.value === param.name) {
            dimension = dimensionItem
            sourceInfo = param.viewId + '#' + dimension.id
            jumpInfo = this.nowPanelJumpInfo[sourceInfo]
          }
        })
      } else {
        for (let i = param.dimensionList.length - 1; i >= 0; i--) {
          dimension = param.dimensionList[i]
          sourceInfo = param.viewId + '#' + dimension.id
          jumpInfo = this.nowPanelJumpInfo[sourceInfo]
          if (jumpInfo) {
            break
          }
        }
      }
      if (jumpInfo) {
        param.sourcePanelId = this.panelInfo.id
        param.sourceViewId = param.viewId
        param.sourceFieldId = dimension.id
        // 内部仪表板跳转
        if (jumpInfo.linkType === 'inner') {
          if (jumpInfo.targetPanelId) {
            localStorage.setItem('jumpInfoParam', JSON.stringify(param))
            if (this.publicLinkStatus) {
              // 判断是否有公共链接ID
              if (jumpInfo.publicJumpId) {
                const url = '/link/' + jumpInfo.publicJumpId
                const currentUrl = window.location.href
                localStorage.setItem('beforeJumpUrl', currentUrl)
                this.windowsJump(url, jumpInfo.jumpType)
              } else {
                this.$message({
                  type: 'warn',
                  message: this.$t('panel.public_link_tips'),
                  showClose: true
                })
              }
            } else {
              const url = '#/preview/' + jumpInfo.targetPanelId
              this.windowsJump(url, jumpInfo.jumpType)
            }
          } else {
            this.$message({
              type: 'warn',
              message: '未指定跳转仪表板',
              showClose: true
            })
          }
        } else {
          const colList = [...param.dimensionList, ...param.quotaList]
          let url = this.setIdValueTrans('id', 'value', jumpInfo.content, colList)
          url = checkAddHttp(url)
          this.windowsJump(url, jumpInfo.jumpType)
        }
      } else {
        if (this.chart.type.indexOf('table') === -1) {
          this.$message({
            type: 'warn',
            message: '未获取跳转信息',
            showClose: true
          })
        }
      }
    },
    setIdValueTrans(from, to, content, colList) {
      if (!content) {
        return content
      }
      let name2Id = content
      const nameIdMap = colList.reduce((pre, next) => {
        pre[next[from]] = next[to]
        return pre
      }, {})
      const on = content.match(/\[(.+?)\]/g)
      if (on) {
        on.forEach(itm => {
          const ele = itm.slice(1, -1)
          name2Id = name2Id.replace(itm, nameIdMap[ele])
        })
      }
      return name2Id
    },
    windowsJump(url, jumpType) {
      try {
        window.open(url, jumpType)
      } catch (e) {
        this.$message({
          message: this.$t('panel.url_check_error') + ':' + url,
          type: 'error',
          showClose: true
        })
      }
    },
    resetDrill() {
      const length = this.drillClickDimensionList.length
      this.drillClickDimensionList = []
      if (this.chart.type === 'map' || this.chart.type === 'buddle-map') {
        this.backToParent(0, length)
        const current = this.$refs[this.element.propValue.id]

        if (this.chart.isPlugin) {
          current && current.callPluginInner && this.setDetailMapCode(null) && current.callPluginInner({
            methodName: 'registerDynamicMap',
            methodParam: null
          })
        } else {
          current && current.registerDynamicMap && this.setDetailMapCode(null) && current.registerDynamicMap(null)
        }
      }
    },
    drillJump(index) {
      const length = this.drillClickDimensionList.length
      this.drillClickDimensionList = this.drillClickDimensionList.slice(0, index)
      if (this.chart.type === 'map' || this.chart.type === 'buddle-map') {
        this.backToParent(index, length)
      }
      this.getData(this.element.propValue.viewId)
    },
    // 回到父级地图
    backToParent(index, length) {
      if (length <= 0) return
      const times = length - 1 - index

      let temp = times
      let tempNode = this.currentAcreaNode
      while (temp >= 0) {
        tempNode = this.findEntityByCode(tempNode.pcode, this.places)
        temp--
      }

      this.currentAcreaNode = tempNode
      const current = this.$refs[this.element.propValue.id]
      if (this.chart.isPlugin) {
        current && current.callPluginInner && this.setDetailMapCode(this.currentAcreaNode.code) && current.callPluginInner({
          methodName: 'registerDynamicMap',
          methodParam: this.currentAcreaNode.code
        })
      } else {
        current && current.registerDynamicMap && this.setDetailMapCode(this.currentAcreaNode.code) && current.registerDynamicMap(this.currentAcreaNode.code)
      }
    },
    setDetailMapCode(code) {
      this.element.DetailAreaCode = code
      bus.$emit('set-dynamic-area-code', code)
      return true
    },
    // 切换下一级地图
    sendToChildren(param) {
      const length = param.data.dimensionList.length
      const name = param.data.dimensionList[length - 1].value
      let aCode = null
      if (this.currentAcreaNode) {
        aCode = this.currentAcreaNode.code
      }
      const customAttr = JSON.parse(this.chart.customAttr)
      const currentNode = this.findEntityByCode(aCode || customAttr.areaCode, this.places)
      if (currentNode && currentNode.children && currentNode.children.length > 0) {
        const nextNode = currentNode.children.find(item => item.name === name)
        this.currentAcreaNode = nextNode
        const current = this.$refs[this.element.propValue.id]
        if (this.chart.isPlugin) {
          nextNode && current && current.callPluginInner && this.setDetailMapCode(nextNode.code) && current.callPluginInner({
            methodName: 'registerDynamicMap',
            methodParam: nextNode.code
          })
        } else {
          nextNode && current && current.registerDynamicMap && this.setDetailMapCode(nextNode.code) && current.registerDynamicMap(nextNode.code)
        }
      }
    },
    findEntityByCode(code, array) {
      if (array === null || array.length === 0) array = this.places
      for (let index = 0; index < array.length; index++) {
        const node = array[index]
        if (node.code === code) return node
        if (node.children && node.children.length > 0) {
          const temp = this.findEntityByCode(code, node.children)
          if (temp) return temp
        }
      }
    },
    initAreas() {
      Object.keys(this.places).length === 0 && areaMapping().then(res => {
        this.places = res.data
      })
    },
    doMapLink(linkFilters) {
      if (!linkFilters && linkFilters.length === 0) return
      const value = linkFilters[0].value
      if (!value && value.length === 0) return
      const name = value[0]
      if (!name) return
      const areaNode = this.findEntityByname(name, [])
      if (!areaNode) return
      const current = this.$refs[this.element.propValue.id]

      if (this.chart.isPlugin) {
        current && current.callPluginInner && current.callPluginInner({
          methodName: 'registerDynamicMap',
          methodParam: areaNode.code
        })
      } else {
        current && current.registerDynamicMap && current.registerDynamicMap(areaNode.code)
      }
    },
    // 根据地名获取areaCode
    findEntityByname(name, array) {
      if (array === null || array.length === 0) array = this.places
      for (let index = 0; index < array.length; index++) {
        const node = array[index]
        if (node.name === name) return node
        if (node.children && node.children.length > 0) {
          const temp = this.findEntityByname(name, node.children)
          if (temp) return temp
        }
      }
    },
    destroyTimeMachine() {
      this.timeMachine && clearTimeout(this.timeMachine)
      this.timeMachine = null
    },
    destroyScaleTimeMachine() {
      this.scaleTimeMachine && clearTimeout(this.scaleTimeMachine)
      this.scaleTimeMachine = null
    },
    // 边框变化
    chartResize(index) {
      if (this.$refs[this.element.propValue.id]) {
        this.timeMachine = setTimeout(() => {
          if (index === this.changeIndex) {
            this.chart.isPlugin
              ? this.$refs[this.element.propValue.id].callPluginInner({ methodName: 'chartResize' })
              : this.$refs[this.element.propValue.id].chartResize()
          }
          this.destroyTimeMachine()
        }, 50)
      }
    },
    // 边框变化 修改视图内部参数
    chartScale(index) {
      if (this.$refs[this.element.propValue.id]) {
        this.scaleTimeMachine = setTimeout(() => {
          if (index === this.changeScaleIndex) {
            this.mergeScale()
          }
          this.destroyScaleTimeMachine()
        }, 100)
      }
    },
    renderComponent() {
      return this.chart.render
    },
    getDataEdit(param) {
      this.$store.commit('canvasChange')
      if (param.type === 'propChange') {
        this.getData(param.viewId, false, true)
      } else if (param.type === 'styleChange') {
        this.chart.customAttr = param.viewInfo.customAttr
        this.chart.customStyle = param.viewInfo.customStyle
        this.chart.senior = param.viewInfo.senior
        this.chart.title = param.viewInfo.title
        this.chart.stylePriority = param.viewInfo.stylePriority
        this.sourceCustomAttrStr = this.chart.customAttr
        this.sourceCustomStyleStr = this.chart.customStyle
        if (this.componentViewsData[this.chart.id]) {
          this.componentViewsData[this.chart.id]['title'] = this.chart.title
          if (param.refreshProp) {
            this.componentViewsData[this.chart.id][param.refreshProp] = this.chart[param.refreshProp]
          }
        }
        this.mergeScale()
      }
    },
    getDataOnly(sourceResponseData, dataBroadcast) {
      if (this.isEdit) {
        if ((this.filter.filter && this.filter.filter.length) || (this.filter.linkageFilters && this.filter.linkageFilters.length)) {
          const requestInfo = {
            filter: [],
            drill: [],
            queryFrom: 'panel'
          }
          // table-info明细表增加分页
          if (this.view && this.view.customAttr) {
            const attrSize = JSON.parse(this.view.customAttr).size
            if (this.chart.type === 'table-info' && this.view.datasetMode === 0 && (!attrSize.tablePageMode || attrSize.tablePageMode === 'page')) {
              requestInfo.goPage = this.currentPage.page
              requestInfo.pageSize = this.currentPage.pageSize
            }
          }
          viewData(this.chart.id, this.panelInfo.id, requestInfo).then(response => {
            this.componentViewsData[this.chart.id] = response.data
            this.view = response.data
            if (dataBroadcast) {
              bus.$emit('prop-change-data')
            }
          })
        } else {
          this.componentViewsData[this.chart.id] = sourceResponseData
          if (dataBroadcast) {
            bus.$emit('prop-change-data')
          }
        }
      }
    },
    pageClick(page) {
      this.currentPage = page
      this.getData(this.element.propValue.viewId, false)
    }
  }
}
</script>

<style lang="scss" scoped>
.rect-shape {
  width: 100%;
  height: 100%;
  overflow: hidden;
  position: relative;
}

.add-item {
  display: flex;
  align-items: center;
  width: 100px;
  margin-top: 16px;
  margin-bottom: 30px;
  color: #3d90ff;;
  cursor: pointer;
}

.de-ds-form {
  width: 100%;
  height: 100%;

  .de-ds-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 56px;
    padding: 12px 24px;
    //box-shadow: 0px 2px 4px rgba(31, 35, 41, 0.08);

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

.chart-class {
  height: 100%;
  padding: 0px !important;
}

.table-class {
  height: 100%;
}

.chart-error-class {
  text-align: center;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #ece7e7;
}

.chart-error-message-class {
  font-size: 12px;
  color: #9ea6b2;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.active {

}

.active ::v-deep .icon-fangda {
  z-index: 2;
  display: block !important;
}

.mobile-dialog-css ::v-deep .el-dialog__headerbtn {
  top: 7px
}

.mobile-dialog-css ::v-deep .el-dialog__body {
  padding: 0px;
}
</style>
