<template>
  <div class="app-container">
    <vab-query-form>
      <vab-query-form-left-panel :span="12">
        <el-row :gutter="10">
          <el-col :span="1.5">
            <el-button :icon="Plus" type="primary" @click="handleAdd">
              新增
            </el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button
              :disabled="single"
              :icon="Edit"
              type="success"
              @click="handleUpdate"
            >
              修改
            </el-button>
          </el-col>
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
            <el-button :icon="Operation" type="info" @click="handleJobLog">
              日志
            </el-button>
          </el-col>
        </el-row>
      </vab-query-form-left-panel>
      <vab-query-form-right-panel :span="12">
        <el-form
          ref="queryRef"
          inline
          label-width="85px"
          :model="queryParams"
          @submit.prevent
        >
          <el-form-item label="搜索条件">
            <el-input
              v-model="queryParams.jobName"
              clearable
              placeholder="请输入任务名称"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item>
            <el-button :icon="Search" type="primary" @click="handleQuery">
              查询
            </el-button>
          </el-form-item>
        </el-form>
      </vab-query-form-right-panel>
    </vab-query-form>

    <el-table
      v-loading="loading"
      :data="jobList"
      @selection-change="handleSelectionChange"
    >
      <el-table-column align="center" type="selection" width="55" />
      <el-table-column
        align="center"
        label="任务编号"
        prop="jobId"
        width="100"
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
        label="cron执行表达式"
        prop="cronExpression"
        :show-overflow-tooltip="true"
      />
      <el-table-column align="center" label="状态">
        <template #default="scope">
          <el-switch
            v-model="scope.row.status"
            active-value="0"
            inactive-value="1"
            @change="handleStatusChange(scope.row)"
          />
        </template>
      </el-table-column>
      <el-table-column
        align="center"
        class-name="small-padding fixed-width"
        label="操作"
        width="200"
      >
        <template #default="scope">
          <el-button :icon="Edit" type="text" @click="handleUpdate(scope.row)">
            修改
          </el-button>
          <el-button
            :icon="Delete"
            type="text"
            @click="handleDelete(scope.row)"
          >
            删除
          </el-button>
          <el-dropdown
            @command="(command) => handleCommand(command, scope.row)"
          >
            <el-button :icon="Paperclip" type="text">更多</el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="handleRun" :icon="CaretRight">
                  执行一次
                </el-dropdown-item>
                <el-dropdown-item command="handleView" :icon="View">
                  任务详细
                </el-dropdown-item>
                <el-dropdown-item command="handleJobLog" :icon="Operation">
                  调度日志
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
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

    <!-- 添加或修改定时任务对话框 -->
    <el-dialog v-model="open" append-to-body :title="title" width="800px">
      <el-form ref="jobRef" label-width="120px" :model="form" :rules="rules">
        <el-row>
          <el-col :span="12">
            <el-form-item label="任务名称" prop="jobName">
              <el-input v-model="form.jobName" placeholder="请输入任务名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="任务名称" prop="jobGroup">
              <el-input v-model="form.jobGroup" placeholder="请输入任务分组" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item prop="invokeTarget">
              <template #label>
                <span>
                  调用方法
                  <el-tooltip placement="top">
                    <template #content>
                      <div>
                        Bean调用示例：ryTask.ryParams('jt')
                        <br />
                        Class类调用示例：com.jeethink.quartz.task.RyTask.ryParams('jt')
                        <br />
                        参数说明：支持字符串，布尔类型，长整型，浮点型，整型
                      </div>
                    </template>
                    <i class="el-icon-question"></i>
                  </el-tooltip>
                </span>
              </template>
              <el-input
                v-model="form.invokeTarget"
                placeholder="请输入调用目标字符串"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="cron表达式" prop="cronExpression">
              <el-input
                v-model="form.cronExpression"
                placeholder="请输入cron执行表达式"
              >
                <template #append>
                  <el-button type="primary" @click="handleShowCron">
                    生成表达式
                    <i class="el-icon-time el-icon--right"></i>
                  </el-button>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="执行策略" prop="misfirePolicy">
              <el-radio-group v-model="form.misfirePolicy" size="small">
                <el-radio-button label="1">立即执行</el-radio-button>
                <el-radio-button label="2">执行一次</el-radio-button>
                <el-radio-button label="3">放弃执行</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否并发" prop="concurrent">
              <el-radio-group v-model="form.concurrent" size="small">
                <el-radio-button label="0">允许</el-radio-button>
                <el-radio-button label="1">禁止</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="dialogFormVisible"
      title="cron表达式生成器"
      width="800px"
      @close="close"
    >
      <crontab :expression="expressionCron" @fill="handleCron" @hide="close" />
    </el-dialog>

    <!-- 任务日志详细 -->
    <el-dialog v-model="openView" append-to-body title="任务详细" width="700px">
      <el-form label-width="120px" :model="form">
        <el-row>
          <el-col :span="12">
            <el-form-item label="任务编号：">{{ form.jobId }}</el-form-item>
            <el-form-item label="任务名称：">{{ form.jobName }}</el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="任务分组：">
              {{ form.jobGroup }}
            </el-form-item>
            <el-form-item label="创建时间：">
              {{ form.createTime }}
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="cron表达式：">
              {{ form.cronExpression }}
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="下次执行时间：">
              {{ parseTime(form.nextValidTime) }}
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="调用目标方法：">
              {{ form.invokeTarget }}
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="任务状态：">
              <div v-if="form.status == 0">正常</div>
              <div v-else-if="form.status == 1">失败</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否并发：">
              <div v-if="form.concurrent == 0">允许</div>
              <div v-else-if="form.concurrent == 1">禁止</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="执行策略：">
              <div v-if="form.misfirePolicy == 0">默认策略</div>
              <div v-else-if="form.misfirePolicy == 1">立即执行</div>
              <div v-else-if="form.misfirePolicy == 2">执行一次</div>
              <div v-else-if="form.misfirePolicy == 3">放弃执行</div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="openView = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="DispatchJob">
  import {
    listJob,
    getJob,
    delJob,
    addJob,
    updateJob,
    runJob,
    changeJobStatus,
  } from '@/api/dispatch/job'

  import Crontab from '@/views/dispatch/Crontab'

  import {
    Search,
    Plus,
    Edit,
    Delete,
    Operation,
    Paperclip,
    CaretRight,
    View,
  } from '@element-plus/icons-vue'

  const router = useRouter()
  const $baseMessage = inject('$baseMessage')
  const $baseConfirm = inject('$baseConfirm')

  const jobList = ref([])
  const open = ref(false)
  const loading = ref(true)
  const ids = ref([])
  const single = ref(true)
  const multiple = ref(true)
  const total = ref(0)
  const title = ref('')
  const openView = ref(false)
  const expression = ref('')
  const dialogFormVisible = ref(false)
  const expressionCron = ref('')

  const data = reactive({
    form: {},
    jobRef: null,
    queryParams: {
      pageNum: 1,
      pageSize: 10,
      jobName: undefined,
      jobGroup: undefined,
      status: undefined,
    },
    layout: 'total, sizes, prev, pager, next, jumper',
    rules: {
      jobName: [
        { required: true, message: '任务名称不能为空', trigger: 'blur' },
      ],
      jobGroup: [
        { required: true, message: '任务分组不能为空', trigger: 'blur' },
      ],
      invokeTarget: [
        { required: true, message: '调用目标字符串不能为空', trigger: 'blur' },
      ],
      cronExpression: [
        { required: true, message: 'cron执行表达式不能为空', trigger: 'blur' },
      ],
    },
  })

  const { queryParams, form, rules, jobRef, layout } = toRefs(data)

  /** 查询定时任务列表 */
  function getList() {
    loading.value = true
    listJob({ ...queryParams.value }).then((response) => {
      jobList.value = response.data.records
      total.value = response.data.total
      loading.value = false
    })
  }
  /** 取消按钮 */
  function cancel() {
    open.value = false
    reset()
  }
  /** 表单重置 */
  function reset() {
    form.value = {
      jobId: undefined,
      jobName: undefined,
      jobGroup: undefined,
      invokeTarget: undefined,
      cronExpression: undefined,
      misfirePolicy: 1,
      concurrent: 1,
      status: '0',
    }
  }
  const handleCurrentChange = (val) => {
    queryParams.value.pageNum = val
    getList()
  }

  const handleSizeChange = (val) => {
    queryParams.value.pageSize = val
    getList()
  }
  const handleCron = (cron) => {
    form.value.cronExpression = cron
    close()
  }
  const close = () => {
    dialogFormVisible.value = false
  }
  /** 搜索按钮操作 */
  function handleQuery() {
    queryParams.value.pageNum = 1
    getList()
  }
  // 多选框选中数据
  function handleSelectionChange(selection) {
    ids.value = selection.map((item) => item.jobId)
    single.value = selection.length != 1
    multiple.value = !selection.length
  }
  // 更多操作触发
  function handleCommand(command, row) {
    switch (command) {
      case 'handleRun':
        handleRun(row)
        break
      case 'handleView':
        handleView(row)
        break
      case 'handleJobLog':
        handleJobLog(row)
        break
      default:
        break
    }
  }
  // 任务状态修改
  function handleStatusChange(row) {
    let text = row.status === '0' ? '启用' : '停用'
    $baseConfirm(
      '确认要"' + text + '""' + row.jobName + '"任务吗?',
      null,
      async () => {
        try {
          const { msg } = await changeJobStatus(row.jobId, row.status)
          $baseMessage(msg, 'success', 'vab-hey-message-success')
        } catch (e) {
          row.status = row.status === '0' ? '1' : '0'
        }
      }
    )
  }
  /* 立即执行一次 */
  function handleRun(row) {
    $baseConfirm(
      '确认要立即执行一次"' + row.jobName + '"任务吗?',
      null,
      async () => {
        const { msg } = await runJob(row.jobId, row.jobGroup)
        $baseMessage(msg, 'success', 'vab-hey-message-success')
      }
    )
  }
  /** 任务详细信息 */
  function handleView(row) {
    getJob(row.jobId).then((response) => {
      form.value = response.data
      openView.value = true
    })
  }
  /** cron表达式按钮操作 */
  function handleShowCron() {
    expression.value = form.value.cronExpression
    dialogFormVisible.value = true
  }
  /** 任务日志列表查询 */
  function handleJobLog(row) {
    const jobId = row.jobId || 0
    router.push({ path: '/dispatch/job/log', query: { jobId: jobId } })
  }
  /** 新增按钮操作 */
  function handleAdd() {
    reset()
    open.value = true
    title.value = '添加任务'
  }
  /** 修改按钮操作 */
  function handleUpdate(row) {
    reset()
    const jobId = row.jobId || ids.value
    getJob(jobId).then((response) => {
      form.value = response.data
      open.value = true
      title.value = '修改任务'
    })
  }
  /** 提交按钮 */
  function submitForm() {
    jobRef.value.validate((valid) => {
      if (valid) {
        if (form.value.jobId != undefined) {
          updateJob(form.value).then(() => {
            $baseMessage('修改成功', 'success', 'vab-hey-message-success')
            open.value = false
            getList()
          })
        } else {
          addJob(form.value).then(() => {
            $baseMessage('新增成功', 'success', 'vab-hey-message-success')
            open.value = false
            getList()
          })
        }
      }
    })
  }
  /** 删除按钮操作 */
  function handleDelete(row) {
    const jobIds = row.jobId || ids.value
    $baseConfirm(
      '是否确认删除定时任务编号为"' + jobIds + '"的数据项?',
      null,
      async () => {
        const { msg } = await delJob(jobIds)
        getList()
        $baseMessage(msg, 'success', 'vab-hey-message-success')
      }
    )
  }

  getList()
</script>
