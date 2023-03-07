package io.dataease.job.sechedule;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.dataease.commons.constants.SysLogConstants;
import io.dataease.commons.utils.CommonBeanFactory;
import io.dataease.commons.utils.DateUtils;
import io.dataease.dto.SysLogDTO;
import io.dataease.dto.chart.ChartViewDTO;
import io.dataease.dto.log.FolderItem;
import io.dataease.plugins.common.base.domain.PanelGroupWithBLOBs;
import io.dataease.plugins.common.base.domain.SchedulerIndexWithBLOBs;
import io.dataease.plugins.common.base.domain.SysUser;
import io.dataease.plugins.common.base.mapper.SchedulerIndexMapper;
import io.dataease.plugins.common.dto.chart.ChartCustomFilterItemDTO;
import io.dataease.plugins.common.dto.chart.ChartFieldCustomFilterDTO;
import io.dataease.plugins.common.dto.chart.ChartViewFieldDTO;
import io.dataease.service.chart.ChartViewService;
import io.dataease.service.panel.PanelGroupService;
import io.dataease.service.sys.log.LogService;
import lombok.SneakyThrows;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.util.ThreadContext;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import java.io.File;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.*;

@Component
public class AlarmScheduleJob implements Job {

    private ChartViewService chartViewService;

    private PanelGroupService panelGroupService;

    private SchedulerIndexMapper schedulerIndexMapper;

    private LogService logService;

    public AlarmScheduleJob() {
        logService = (LogService) CommonBeanFactory.getBean(LogService.class);
        chartViewService = (ChartViewService) CommonBeanFactory.getBean(ChartViewService.class);
        panelGroupService = (PanelGroupService) CommonBeanFactory.getBean(PanelGroupService.class);
        schedulerIndexMapper = (SchedulerIndexMapper) CommonBeanFactory.getBean(SchedulerIndexMapper.class);
        DefaultWebSecurityManager manager = new DefaultWebSecurityManager();
        ThreadContext.bind(manager);
    }

    @SneakyThrows
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Gson gson = new Gson();
        //JobKey jobKey = context.getTrigger().getJobKey();
        SysLogDTO sysLogDTO = new SysLogDTO();
        try {
            JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
            String chartId = jobDataMap.getString("chartId");
            Long id = jobDataMap.getLong("id");
            sysLogDTO.setSourceId(id.toString());
            sysLogDTO.setSourceType(SysLogConstants.SOURCE_TYPE.ALARM.getValue());
            SchedulerIndexWithBLOBs schedulerIndexWithBLOBs = schedulerIndexMapper.selectByPrimaryKey(id);
            ChartViewDTO view = chartViewService.getOne(chartId, null);
            PanelGroupWithBLOBs panel = panelGroupService.findPanelForChartId(view.getId());
            sysLogDTO.setSourceName(panel.getName() + ":" + view.getName() + "指标报警");
            Type tokenType = new TypeToken<ChartFieldCustomFilterDTO>() {
            }.getType();
            Type indexType = new TypeToken<ChartViewFieldDTO>() {
            }.getType();
            Type ruleType = new TypeToken<List<RuleAndSend>>() {
            }.getType();
            long endTimeMillis = System.currentTimeMillis();
            long startimeMillis = 0;
            String timeType = schedulerIndexWithBLOBs.getTimeType();
            Long timeNumber = schedulerIndexWithBLOBs.getTimeNumber();
            switch (timeType) {
                case "秒":
                    startimeMillis = endTimeMillis - timeNumber * 1000;
                    break;
                case "分钟":
                    startimeMillis = endTimeMillis - timeNumber * 1000 * 60;
                    break;
                case "小时":
                    startimeMillis = endTimeMillis - timeNumber * 1000 * 60 * 60;
                    break;
                case "天":
                    startimeMillis = endTimeMillis - timeNumber * 1000 * 60 * 60 * 24;
                    break;
            }
            ChartFieldCustomFilterDTO chartFieldCustomFilterDTO = gson.fromJson(schedulerIndexWithBLOBs.getTimeField(), tokenType);
            ArrayList<ChartCustomFilterItemDTO> chartCustomFilterItemDTOS = new ArrayList<>();
            ChartCustomFilterItemDTO chartCustomFilterItemDTO = new ChartCustomFilterItemDTO();
            chartCustomFilterItemDTOS.add(chartCustomFilterItemDTO);
            chartCustomFilterItemDTO.setFieldId(chartFieldCustomFilterDTO.getId());
            chartCustomFilterItemDTO.setTerm("ge");
            chartCustomFilterItemDTO.setValue(DateUtils.getDateString(startimeMillis, schedulerIndexWithBLOBs.getFormat()));

            ChartCustomFilterItemDTO chartCustomFilterItemDTO1 = new ChartCustomFilterItemDTO();
            chartCustomFilterItemDTOS.add(chartCustomFilterItemDTO1);
            chartCustomFilterItemDTO1.setFieldId(chartFieldCustomFilterDTO.getId());
            chartCustomFilterItemDTO1.setTerm("le");
            chartCustomFilterItemDTO1.setValue(DateUtils.getDateString(endTimeMillis, schedulerIndexWithBLOBs.getFormat()));
            ChartViewDTO chartViewDTO = chartViewService.calcData(view, schedulerIndexWithBLOBs.getTimeField(), false, chartCustomFilterItemDTOS);
            ArrayList<HashMap> tableRow = (ArrayList) chartViewDTO.getData().get("tableRow");
            ChartViewFieldDTO index = gson.fromJson(schedulerIndexWithBLOBs.getIndexField(), indexType);
            List<RuleAndSend> rules = gson.fromJson(schedulerIndexWithBLOBs.getRules(), ruleType);
            List<RuleAndSend> sends = gson.fromJson(schedulerIndexWithBLOBs.getSends(), ruleType);
            ArrayList<String> msg = new ArrayList<>();
            for (RuleAndSend rule : rules) {
                String operate = rule.getOperate();
                String type = rule.getType();
                BigDecimal value = BigDecimal.valueOf(Long.valueOf(rule.getValue()));
                switch (operate) {
                    case "高于":
                        if (type.equals("固定值")) {
                            for (HashMap hashMap : tableRow) {
                                BigDecimal o = (BigDecimal) hashMap.get(index.getDataeaseName());
                                if (o.compareTo(value) > 0) {
                                    msg.add(hashMap.get(chartFieldCustomFilterDTO.getDataeaseName()).toString() + ":" + o + "(高于设定的值" + value + ")");
                                }
                            }
                        } else if (type.equals("环比过去N天平均值")) {
                            Integer numDay = rule.getNumDay();
                            BigDecimal avgData = getAvgData(index.getDataeaseName(), schedulerIndexWithBLOBs.getTimeField(), view, numDay,
                                    chartFieldCustomFilterDTO.getId(), startimeMillis, endTimeMillis, schedulerIndexWithBLOBs.getFormat());
                            BigDecimal avg = avgData.divide(new BigDecimal(numDay));
                            BigDecimal decimal = value.add(avg.multiply(new BigDecimal(-1))).divide(avg).abs();
                            if (decimal.compareTo(value) > 0) {
                                msg.add(index.getDataeaseName() + "指标环比" + numDay + "日均值为:" + decimal + "(高于设定的值" + value + ")");
                            }
                        } else if (type.equals("同比上月同期")) {
                            BigDecimal avgData = getMonthData(index.getDataeaseName(), schedulerIndexWithBLOBs.getTimeField(), view,
                                    chartFieldCustomFilterDTO.getId(), startimeMillis, endTimeMillis, schedulerIndexWithBLOBs.getFormat());
                            BigDecimal decimal = value.add(avgData.multiply(new BigDecimal(-1))).divide(avgData).abs();
                            if (decimal.compareTo(value) > 0) {
                                msg.add(index.getDataeaseName() + "指标上月同比值为:" + decimal + "(高于设定的值" + value + ")");
                            }
                        }
                        break;
                    case "低于":
                        if (type.equals("固定值")) {
                            for (HashMap hashMap : tableRow) {
                                BigDecimal o = (BigDecimal) hashMap.get(index.getDataeaseName());
                                if (o.compareTo(value) < 0) {
                                    msg.add(hashMap.get(chartFieldCustomFilterDTO.getDataeaseName()).toString() + ":" + o + "(低于设定的值" + value + ")");
                                }
                            }
                        } else if (type.equals("环比过去N天平均值")) {
                            Integer numDay = rule.getNumDay();
                            BigDecimal avgData = getAvgData(index.getDataeaseName(), schedulerIndexWithBLOBs.getTimeField(), view, numDay,
                                    chartFieldCustomFilterDTO.getId(), startimeMillis, endTimeMillis, schedulerIndexWithBLOBs.getFormat());
                            BigDecimal avg = avgData.divide(new BigDecimal(numDay));
                            BigDecimal decimal = value.add(avg.multiply(new BigDecimal(-1))).divide(avg).abs();
                            if (decimal.compareTo(value) < 0) {
                                msg.add(index.getDataeaseName() + "指标环比" + numDay + "日均值为:" + decimal + "(低于设定的值" + value + ")");
                            }
                        } else if (type.equals("同比上月同期")) {
                            BigDecimal avgData = getMonthData(index.getDataeaseName(), schedulerIndexWithBLOBs.getTimeField(), view,
                                    chartFieldCustomFilterDTO.getId(), startimeMillis, endTimeMillis, schedulerIndexWithBLOBs.getFormat());
                            BigDecimal decimal = value.add(avgData.multiply(new BigDecimal(-1))).divide(avgData).abs();
                            if (decimal.compareTo(value) < 0) {
                                msg.add(index.getDataeaseName() + "指标上月同比值为:" + decimal + "(低于设定的值" + value + ")");
                            }
                        }
                        break;
                }
            }
            if (msg.size() > 0) {
                for (RuleAndSend send : sends) {
                    String type = send.getType();
                    List<SysUser> users = send.getUsers();
                    String link = send.getLink();
                    switch (type) {
                        case "邮箱":
                            for (SysUser user : users) {
                                Runtime.getRuntime().exec("python " + File.separator + "data" + File.separator + "data-platform" + File.separator + "tools" + File.separator + "index_alarm.py"
                                        + " -t mail -u " + user.getUsername()
                                        + " -s " + panel.getName() + ":" + view.getName() + "指标报警"
                                        + " -m " + StringUtils.join(msg, ","));
                            }
                            break;
                        case "飞书个人":
                            for (SysUser user : users) {
                                Runtime.getRuntime().exec("python " + File.separator + "data" + File.separator + "data-platform" + File.separator + "tools" + File.separator + "index_alarm.py"
                                        + " -t person -u " + user.getNickName()
                                        + " -i " + user.getToken()
                                        + " -s " + panel.getName() + ":" + view.getName() + "指标报警"
                                        + " -m " + StringUtils.join(msg, ","));
                            }
                            break;
                        case "电话":
                            for (SysUser user : users) {
                                String[] cmd = {"sh", "-c", "sh " + File.separator + "data" + File.separator + "data-platform" + File.separator + "tools" + File.separator + "callPhone.sh"
                                        + " " + user.getPhone().substring(3)
                                        + " '" + panel.getName() + ":" + view.getName() + "指标报警" + StringUtils.join(msg, ",") + "'"};
                                Runtime.getRuntime().exec(cmd);
                            }
                            break;
                        case "飞书群组":
                            Runtime.getRuntime().exec("python " + File.separator + "data" + File.separator + "data-platform" + File.separator + "tools" + File.separator + "index_alarm.py"
                                    + " -t group -H " + link
                                    + " -s " + panel.getName() + ":" + view.getName() + "指标报警"
                                    + " -m " + StringUtils.join(msg, ","));
                            break;
                    }
                }
            }
            ArrayList<FolderItem> folderItems = new ArrayList<>();
            FolderItem folderItem = new FolderItem();
            folderItem.setName("执行成功");
            sysLogDTO.setRemarks(folderItems);
            sysLogDTO.setOperateType(SysLogConstants.OPERATE_TYPE.SUCCESS.getValue());
            logService.saveAlarmLog(sysLogDTO);
        } catch (Exception e) {
            e.printStackTrace();
            ArrayList<FolderItem> folderItems = new ArrayList<>();
            FolderItem folderItem = new FolderItem();
            folderItem.setName(e.getMessage());
            sysLogDTO.setRemarks(folderItems);
            sysLogDTO.setOperateType(SysLogConstants.OPERATE_TYPE.FAILED.getValue());
            logService.saveAlarmLog(sysLogDTO);
        }
    }

    public BigDecimal getAvgData(String index, String timeField, ChartViewDTO view, Integer numDay, String fieldId, Long startimeMillis, Long endTimeMillis, String format) throws Exception {
        BigDecimal sum = new BigDecimal(0);
        int i = 1;
        while (i <= numDay) {
            ArrayList<ChartCustomFilterItemDTO> chartCustomFilterItemDTOList = new ArrayList<>();
            ChartCustomFilterItemDTO chartCustom = new ChartCustomFilterItemDTO();
            chartCustomFilterItemDTOList.add(chartCustom);
            chartCustom.setFieldId(fieldId);
            chartCustom.setTerm("ge");
            chartCustom.setValue(DateUtils.getDateString(startimeMillis - i * 1000 * 60 * 60 * 24, format));

            ChartCustomFilterItemDTO chartCustom1 = new ChartCustomFilterItemDTO();
            chartCustomFilterItemDTOList.add(chartCustom1);
            chartCustom1.setFieldId(fieldId);
            chartCustom1.setTerm("le");
            chartCustom1.setValue(DateUtils.getDateString(endTimeMillis - i * 1000 * 60 * 60 * 24, format));
            ChartViewDTO chartView = chartViewService.calcData(view, timeField, false, chartCustomFilterItemDTOList);
            ArrayList<HashMap> row = (ArrayList) chartView.getData().get("tableRow");
            for (HashMap hashMap : row) {
                BigDecimal o = (BigDecimal) hashMap.get(index);
                sum = sum.add(o);
            }
            i = i + 1;
        }
        return sum;
    }

    public BigDecimal getMonthData(String index, String timeField, ChartViewDTO view, String fieldId, Long startimeMillis, Long endTimeMillis, String format) throws Exception {
        BigDecimal sum = new BigDecimal(0);
        int i = 1;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(startimeMillis));
        calendar.add(Calendar.MONTH, -1);

        Calendar calendar1 = Calendar.getInstance();
        calendar1.setTime(new Date(endTimeMillis));
        calendar1.add(Calendar.MONTH, -1);

        ArrayList<ChartCustomFilterItemDTO> chartCustomFilterItemDTOList = new ArrayList<>();
        ChartCustomFilterItemDTO chartCustom = new ChartCustomFilterItemDTO();
        chartCustomFilterItemDTOList.add(chartCustom);
        chartCustom.setFieldId(fieldId);
        chartCustom.setTerm("ge");
        chartCustom.setValue(DateUtils.getDateString(calendar.getTime().getTime(), format));

        ChartCustomFilterItemDTO chartCustom1 = new ChartCustomFilterItemDTO();
        chartCustomFilterItemDTOList.add(chartCustom1);
        chartCustom1.setFieldId(fieldId);
        chartCustom1.setTerm("le");
        chartCustom1.setValue(DateUtils.getDateString(calendar1.getTime().getTime(), format));
        ChartViewDTO chartView = chartViewService.calcData(view, timeField, false, chartCustomFilterItemDTOList);
        ArrayList<HashMap> row = (ArrayList) chartView.getData().get("tableRow");
        for (HashMap hashMap : row) {
            BigDecimal o = (BigDecimal) hashMap.get(index);
            sum = sum.add(o);
        }
        return sum;
    }
}
