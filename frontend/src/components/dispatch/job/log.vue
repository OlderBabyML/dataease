<template>
  <div class="app-container">
    <vab-query-form>
      <vab-query-form-left-panel :span="12">
        <el-row :gutter="10">
          <el-col :span="1.5">
            <el-button
              :disabled="multiple"
              :icon="Delete"
              type="danger"
              @click="handleDelete"
            >
              删除
            </el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button :icon="Delete" type="danger" @click="handleClean">
              清空
            </el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button :icon="Close" type="warning" @click="handleClose">
              关闭
            </el-button>
          </el-col>
        </el-row>
      </vab-query-form-left-panel>
      <vab-query-form-right-panel :span="12">
        <el-form
          v-show="showSearch"
          ref="queryRef"
          :inline="true"
          label-width="68px"
          :model="queryParams"
        >
          <el-form-item label="任务名称" prop="jobName">
            <el-input
              v-model="queryParams.jobName"
              clearable
              placeholder="请输入任务名称"
              style="width: 240px"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="执行状态" prop="status">
            <el-select
              v-model="queryParams.status"
              clearable
              placeholder="请选择执行状态"
              style="width: 240px"
            >
              <el-option
                v-for="dict in options.status"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button :icon="Search" type="primary" @click="handleQuery">
              搜索
            </el-button>
            <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </vab-query-form-right-panel>
    </vab-query-form>

    <el-table
      v-loading="loading"
      :data="jobLogList"
      @selection-change="handleSelectionChange"
    >
      <el-table-column align="center" type="selection" width="55" />
      <el-table-column
        align="center"
        label="日志编号"
        prop="jobLogId"
        width="80"
      />
      <el-table-column
        align="center"
        label="任务名称"
        prop="jobName"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        align="center"
        label="任务分组"
        prop="jobGroup"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        align="center"
        label="调用目标字符串"
        prop="invokeTarget"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        align="center"
        label="日志信息"
        prop="jobMessage"
        :show-overflow-tooltip="true"
      />
      <el-table-column align="center" label="执行状态" prop="status">
        <template #default="{ row }">
          <el-tag>{{ row.status === '0' ? '成功' : '失败' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column
        align="center"
        label="执行时间"
        prop="createTime"
        width="180"
      >
        <template #default="scope">
          <span>{{ scope.row.createTime }}</span>
        </template>
      </el-table-column>
      <el-table-column
        align="center"
        class-name="small-padding fixed-width"
        label="操作"
      >
        <template #default="scope">
          <el-button icon="View" type="text" @click="handleView(scope.row)">
            详细
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      background
      :current-page="queryParams.pageNum"
      :layout="layout"
      :page-size="queryParams.pageSize"
      :total="total"
      @current-change="handleCurrentChange"
      @size-change="handleSizeChange"
    />

    <!-- 调度日志详细 -->
    <el-dialog v-model="open" append-to-body title="调度日志详细" width="700px">
      <el-form label-width="100px" :model="form">
        <el-row>
          <el-col :span="12">
            <el-form-item label="日志序号：">{{ form.jobLogId }}</el-form-item>
            <el-form-item label="任务名称：">{{ form.jobName }}</el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="任务分组：">{{ form.jobGroup }}</el-form-item>
            <el-form-item label="执行时间：">
              {{ form.createTime }}
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="调用方法：">
              {{ form.invokeTarget }}
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="日志信息：">
              {{ form.jobMessage }}
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="执行状态：">
              <div v-if="form.status == 0">正常</div>
              <div v-else-if="form.status == 1">失败</div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item v-if="form.status == 1" label="异常信息：">
              {{ form.exceptionInfo }}
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="open = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="DispatchJobLog">
  import { getJob } from '@/api/dispatch/job'
  import { listJobLog, delJobLog, cleanJobLog } from '@/api/dispatch/jobLog'
  import { handleActivePath } from '@/utils/routes'
  import { useTabsStore } from '@/store/modules/tabs'
  import { Search, Refresh, Close, Delete } from '@element-plus/icons-vue'

  const jobLogList = ref([])
  const open = ref(false)
  const loading = ref(true)
  const showSearch = ref(true)
  const ids = ref([])
  const multiple = ref(true)
  const total = ref(0)
  const dateRange = ref([])
  const route = useRoute()
  const $baseMessage = inject('$baseMessage')
  const $baseConfirm = inject('$baseConfirm')

  const data = reactive({
    form: {},
    queryParams: {
      pageNum: 1,
      pageSize: 10,
      dictName: undefined,
      dictType: undefined,
      status: undefined,
    },
    layout: 'total, sizes, prev, pager, next, jumper',
    options: {
      status: [
        { label: '成功', value: '0' },
        { label: '失败', value: '1' },
      ],
    },
  })

  const router = useRouter()

  const { queryParams, form, options, layout } = toRefs(data)

  const tabsStore = useTabsStore()
  const { delVisitedRoute } = tabsStore

  const handleCurrentChange = (val) => {
    queryParams.value.pageNum = val
    getList()
  }

  const handleSizeChange = (val) => {
    queryParams.value.pageSize = val
    getList()
  }
  /** 查询调度日志列表 */
  function getList() {
    loading.value = true
    listJobLog({ ...queryParams.value }).then((response) => {
      jobLogList.value = response.data.records
      total.value = response.data.total
      loading.value = false
    })
  }
  // 返回按钮
  const handleClose = async () => {
    const detailPath = await handleActivePath(route, true)
    await router.push('/dispatch/job')
    await delVisitedRoute(detailPath)
  }
  /** 搜索按钮操作 */
  function handleQuery() {
    queryParams.value.pageNum = 1
    getList()
  }
  /** 重置按钮操作 */
  function resetQuery() {
    dateRange.value = []
    handleQuery()
  }
  // 多选框选中数据
  function handleSelectionChange(selection) {
    ids.value = selection.map((item) => item.jobLogId)
    multiple.value = !selection.length
  }
  /** 详细按钮操作 */
  function handleView(row) {
    open.value = true
    form.value = row
  }
  /** 删除按钮操作 */
  function handleDelete() {
    $baseConfirm(
      '是否确认删除调度日志编号为"' + ids.value + '"的数据项?',
      null,
      async () => {
        const { msg } = await delJobLog(ids.value)
        getList()
        $baseMessage(msg, 'success', 'vab-hey-message-success')
      }
    )
  }
  /** 清空按钮操作 */
  function handleClean() {
    $baseConfirm('是否确认清空所有调度日志数据项?', null, async () => {
      const { msg } = await cleanJobLog()
      getList()
      $baseMessage(msg, 'success', 'vab-hey-message-success')
    })
  }

  ;(() => {
    const jobId = route.query.jobId
    if (jobId !== undefined && jobId != 0) {
      getJob(jobId).then((response) => {
        queryParams.value.jobName = response.data.jobName
        queryParams.value.jobGroup = response.data.jobGroup
        getList()
      })
    } else {
      getList()
    }
  })()

  getList()
</script>
